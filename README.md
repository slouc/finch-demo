# Simple REST API server with finch

### What is finch?

Let's start by quoting the [official GitHub page](https://github.com/finagle/finch): "Finch is a thin layer of purely functional basic blocks atop of Finagle for building composable HTTP APIs". That's a beautiful definition because it's both concise and complete; while Twitter's Finagle is the raw machinery under the hood that deals with RPCs, protocols, concurrency, connection pools, load balancers and things like that, Finch is a thin layer of composable abstract stuff on top of all that machinery.

Finagle's mantra is: *server is a function*. That makes sense; when we abstract it out and forget about the mechanical parts, it really comes down to a simple `Req => Future[Rep]` (note that this is Twitter's Future). Of course, we can't simply "forget" about the mechanical parts, but we can at least move them away from the abstract part. And that's exactly what Finagle does - it separates the netty-powered engine from the functional, composable, type-safe abstractions that live on top of it. And Finch takes that part a bit futher.

### Working with Endpoints

Basic building block in Finch is the `Endpoint`. Again, this is a function, this time `Input => EndpointResult[A]`, where `A` denotes the type of result (we'll get back to this type soon). Note that you're most likely not going to be constructing the `EndpointResult` yourself. I guess it's best explained with an example. 

Here's a basic endpoint:

    import io.finch._
    
    val endpoint = get("hello" :: "world" :: string)
    
This descibes an endpoint `/hello/world/{URL_PARAM}`, which means that our endpoint is a function of one String parameter. Here's how we can also include query params and/or request body:

    import io.finch._
    
    case class Book(title: String, author: String)
    
    val endpoint1 = get("books" :: string :: param ("minPrice") :: paramOption("maxPrice"))
    val endpoint2 = post("books" :: jsonBody[Book])

First endpoint matches e.g. `GET /books/scienceFiction?minPrice=20&maxPrice=150` while second one matches `POST /books` with request body e.g. `{ "title": "1984", "author": "G. Orwell" }`. Note that min price is required and max price is optional, which probably doesn't make a lot of sense in the real world, but I'm just showing the possibilities.

Values provided to `get` and `post` are `HList`s from [shapeless](https://github.com/milessabin/shapeless). If you are not familiar with Shapeless, that's fine (although you should put it on your TODO list because it's really useful). All you need to know for now is that "HList" is short for a Heterogenous List, which is basically a List that can contain different types. In functional programming terminology this is known as a *product* (that would have been the name of shapeless HList too if it hadn't already been taken in standard Scala library). The HList we passed to `endpoint1` was a product of: "books", one URL path parameter, one required query parameter and one optional query parameter. So, endpoint is *all of those things combined*. This is in contrast to product's dual, *coproduct*. A coproduct of those things would mean that the final value is only one of them. It's like AND vs OR in first-order logic (hooray for [Curry-Howard correspondence](https://en.wikipedia.org/wiki/Curry%E2%80%93Howard_correspondence)). In standard Scala terms, product is like a TupleN while coproduct is a bunch of nested Eithers (or just one, in case of a coproduct of only two types). We can also model product as a case class and coproduct as a bunch of case classes which extend a common sealed trait. We're only using products for now in Finch, but we'll also use the coproducts later, hence this small digression.

Now let's see how to provide our endpoints-as-functions with their bodies. This is where "not constructing the EndpointResult yourself" comes into play. What you need to do is simply provide each endpoint with a function from its parameters to some `Output[A]` and Finch takes care of the rest (for those who are interested - this function will be implicitly transformed into an instance of `Mapper` typeclass, which is then passed to `Endpoint`'s `apply()` method).

Here's how to provide our functions with bodies:

    import io.finch._
    
    case class Book(title: String, author: String)
    
    val endpoint1 = get("books" :: string :: param("minPrice") :: paramOption("maxPrice")) { 
      (s: String, minPrice: String, maxPrice: Option[String]) =>  
        // do something and return Output
        Ok(s"Cool request bro! Here are the params: $s, $minPrice" + maxPrice.getOrElse(s" and $maxPrice", ""))
    }
    
    val endpoint2 = post("books" :: jsonBody[Book]) {
      (book: Book) => 
        // do something and return Output
        Ok(s"You posted a book with title: ${book.title} and author: ${book.author}")
    }
    
Type `A` that I mentioned before is the type we parameterize `Output` with, and therefore the type of result. In the previous example it was a simple String. Now let's return a JSON:

    import io.circe.generic.auto._
    import io.finch.circe._

    case class MyResponse(code: Int, msg: String)

    val endpoint3 = post("books" :: jsonBody[Book]) {
      (book: Book) => 
        // do something and return Output
         Ok(Response(200, "This is a response!"))
    }
    
You might be wondering how exactly are we returning a JSON since we never even mentioned the word "json", we are just returning a `MyResponse`. Magic is in those two imports. They contain implicit conversions (powered by [circe](https://github.com/circe/circe)) that automatically construct a result in viable format. 

Let's get even more sophisticated:

    import io.circe.generic.auto._
    import io.finch.circe._

    case class FancyResponse[T: io.circe.Encoder](code: Int, msg: String, body: T)
    case class FancyResponseBody(msg: String)

    val endpoint4 = post("books" :: "fancy" :: jsonBody[Book]) {
      (book: Book) =>
        // do something and return Output
        Ok(FancyResponse(200, "This is one fancy response!", FancyResponseBody("response")))
    }

Here in `FancyResponse` we have a generic type `T`. Having just `T` as a type parameter would not satisfy the compiler since there is no information about the type so there's no guarantee that Finch will know how to encode it into some output type such as JSON. But by declaring the type as `[T: io.circe.Encoder]` we are saying that implicit implementation of Encoder typeclass must exist in scope for the given T. When we later on use `FancyResponseBody` in place of `T`, compiler is happy because there indeed exists a needed typeclass instance (it's in the imports). 

Some types (such as `Option[T]`) are contained inside the imports, and some (such as `scalaz.Maybe[T]`, at least in the time of writing) are not. But even for those that are not, it's simple to build your own conversions. They are not in the scope of this text and you don't need them for now anyway, but let's just say that Finch documentation and [gitter channel](https://gitter.im/finagle/finch) should help you when you do get there (not to mention that Travis Brown supplied ridiculous amounts of Finch+Circe information on StackOverflow).

To summarize - endpoint is a function whose input is a product of path/query/body function parameters and whose return value is `Endpoint[SomeResult]`, where `SomeResult` can be any type (most likely a string, an array/vector/list or a case class, all of which are automatically transformed to their JSON counterparts). A bit of terminology - we can say that Scala's String, Array/Seq and case class are *isomorphic* to JsString, JsArray and JsObject because we can go from one to the other and back without losing any information. 

We can visualize the type transformations like this:

    Request[AE] ------> AD ------> BD ------> Response[BE]

where `AE` is "A Encoded" (e.g. JSON request body), `AD` is "A Decoded" (e.g. a corresponding case class), `BD` is "B Decoded" (result of applying the business logic to `AD`) and `BE` is the encoded version of `BD` (e.g. from case class to JSON).

Each endpoint is constructed in two steps, first by providing an HList that describes the endpoint URL and parameter(s), and then by composing that with a function which describes what happens with the input parameters (if any) and constructs the result (the "body" of the endpoint function). 

Let's now see how to define the server.

### Implementing the Server

Once you get the hang of working with endpoints, defining the basic implementation of a server is almost trivial (later on you will perhaps want to add various filters and stuff, but we're keeping it simple here).

I said we will be working with coproducts later. This is exactly what our server will be - a coproduct of endpoints. It's like Schrödinger's cat; server is potentially all endpoints at the same time, but once you make the request it's actually materialized as just one of them. Well, kind of, but it's an interesting way of looking at it. When a request is made, each endpoint is probed until a match is found or the end has been reached. If some endpoint matches the request (e.g. request is `GET /foo/bar` and an endpoint `get("foo" :: "bar")` is matched), that endpoint is triggered and the search stops. If more than one endpoint matches the request, first one is chosen. It's just like the good old pattern matching.

Here's a simple implementation of a server. Even though it's not necessary for a schoolbook example, in the real world you will want to extend the `TwitterServer` (this is the official [best practice](https://finagle.github.io/finch/best-practices.html#use-twitterserver)). Other than that, everything should be pretty straightforward. You will notice that the syntax for joining things into a coproduct is `:+:` (also known as the "space invader" operator).

    object Server extends TwitterServer {

      val api: Service[Request, Response] =
        (Api.endpoint1 :+: Api.endpoint2 :+: Api.endpoint3 :+: Api.endpoint4)
          .toService

      def main(): Unit = {
        val server = Http.server.serve(":8080", api)
        onExit { server.close() }
        Await.ready(adminHttpServer)
      }
      
    }
    



