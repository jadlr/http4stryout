package io.trsc.http4stryout

import fs2.StreamApp.ExitCode
import fs2.{Stream, StreamApp}
import monix.eval._
import monix.execution.Scheduler.Implicits.global
import org.http4s.dsl._
import org.http4s.server.blaze._

object Main extends StreamApp[Task] with Http4sDsl[Task] {

  private val elasticsearchClient: ElasticsearchClient[Task] = new EffectElasticsearchClient[Task]("localhost", 9201)
  private val elasticsearchService: ElasticsearchService[Task] = new ElasticsearchService[Task](elasticsearchClient)

  override def stream(args: List[String], requestShutdown: Task[Unit]): Stream[Task, ExitCode] =
    BlazeBuilder[Task]
      .bindHttp(8080, "localhost")
      .mountService(elasticsearchService.routes, "/")
      .serve
}
