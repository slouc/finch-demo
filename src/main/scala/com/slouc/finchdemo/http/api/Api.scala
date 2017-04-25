package com.slouc.finchdemo.http.api

import com.twitter.finagle.http.Status
import com.twitter.util.Future
import io.finch._
import io.circe.generic.auto._
import io.finch.circe._

object Api {

  val endpoint1: Endpoint[String] = get("books" :: string :: param("minPrice") :: paramOption("maxPrice")) {
    (s: String, minPrice: String, maxPrice: Option[String]) =>
      // do something and return Output
      Ok(s"Cool request bro! Here are the params: $s, $minPrice" + maxPrice.getOrElse(s" and $maxPrice", ""))
  }

  val endpoint2: Endpoint[String] = post("books" :: jsonBody[Book]) {
    (book: Book) =>
      // do something and return Output
      Ok(s"You posted a book with title: ${book.title} and author: ${book.author}")
  }

  val endpoint3: Endpoint[MyResponse] = post("books" :: "json" :: jsonBody[Book]) {
    (book: Book) =>
      // do something and return Output
      Ok(MyResponse(200, "This is a response!"))
  }.rescue {
    case (t: Throwable) => Future(Output.payload(MyResponse(400, "Not cool dude!"), Status.BadRequest))
  }

  val endpoint4: Endpoint[FancyResponse[FancyResponseBody]] = post("books" :: "fancy" :: jsonBody[Book]) {
    (book: Book) =>
      // do something and return Output
      Ok(FancyResponse(200, "This is one fancy response!", FancyResponseBody("response")))
  }

}

case class Book(title: String, author: String)

case class MyResponse(code: Int, msg: String)

case class FancyResponse[T: io.circe.Encoder](code: Int, msg: String, body: T)

case class FancyResponseBody(msg: String)