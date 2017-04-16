package com.slouc.finchdemo.http.server

import com.slouc.finchdemo.http.api.Api
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.param.Stats
import com.twitter.server._
import com.twitter.finagle.{Http, Service}
import com.twitter.util.Await
import io.circe.generic.auto._
import io.finch.circe._

object Server extends TwitterServer {

  val api: Service[Request, Response] =
    (Api.helloWorldEndpoint :+: Api.postStuffEndpoint)
      .toService

  def main(): Unit = {
    val server = Http.server
      .configured(Stats(statsReceiver))
      .serve(":8080", api)

    onExit {
      server.close()
    }

    Await.ready(adminHttpServer)
  }
}
