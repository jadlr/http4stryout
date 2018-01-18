package io.trsc.http4stryout

import fs2.StreamApp.ExitCode
import fs2.{Stream, StreamApp}
import monix.eval._
import monix.execution.Scheduler.Implicits.global
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.blaze.Http1Client
import org.http4s.dsl._
import org.http4s.server.blaze._

object Main extends StreamApp[Task] with Http4sDsl[Task] {

  private val httpClient: Task[Client[Task]] = Http1Client[Task]()
  private val elasticsearchClient: ElasticsearchClient[Task] = new ElasticsearchClient[Task](httpClient, "localhost", 9201)

  val elasticsearchService = HttpService[Task] {
    case GET -> Root / "health" => {
      val response = elasticsearchClient.Cluster.health().map { status ⇒
        if (status.status == "red") "Ohhh noes"
        else "Allll fine!"
      }
      Ok(response)
    }
  }

  override def stream(args: List[String], requestShutdown: Task[Unit]): Stream[Task, ExitCode] =
    BlazeBuilder[Task]
      .bindHttp(8080, "localhost")
      .mountService(elasticsearchService, "/")
      .serve
}
