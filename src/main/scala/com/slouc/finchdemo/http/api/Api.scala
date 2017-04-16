package com.slouc.finchdemo.http.api

import io.finch._
import io.circe.generic.auto._
import io.finch.circe._

object Api {

  val helloWorldEndpoint: Endpoint[Response[HelloWorldResponse]] = get("hello" :: "world" :: string){
    (s: String) => {
      // some logic
      Ok(Response(200, "", HelloWorldResponse(s"hello, $s!")))
    }
  }
  val postStuffEndpoint: Endpoint[Response[PostStuffResponse]] = post("post" :: "stuff" :: jsonBody[PostStuffRequest]){
    (psr: PostStuffRequest) => {
      // some logic
      Ok(Response(200, "", PostStuffResponse(s"hello, ${psr.stuff}!")))
    }
  }

}

case class Response[T : io.circe.Encoder : io.circe.Decoder](code: Int, msg: String, body: T)
case class HelloWorldResponse(name: String)
case class PostStuffRequest(stuff: String)
case class PostStuffResponse(stuff: String)