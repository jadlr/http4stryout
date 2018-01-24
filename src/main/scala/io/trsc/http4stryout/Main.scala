package io.trsc.http4stryout

import fs2.StreamApp.ExitCode
import fs2.{Stream, StreamApp}
import monix.eval._
import monix.execution.Scheduler.Implicits.global
import org.http4s.dsl._
import org.http4s.server.blaze._

object Main extends StreamApp[Task] with Http4sDsl[Task] {

  override def stream(args: List[String], requestShutdown: Task[Unit]): Stream[Task, ExitCode] = (for {
    config              ← ApplicationConfig[Task]
    elasticsearchClient ← EffectElasticsearchClient[Task](config.elasticsearchUrl)
    ctpClient           ← EffectCtpClient[Task](config.authWsUrl, config.ctpClientId, config.ctpClientSecret)
  } yield {

    val elasticsearchService = new ElasticsearchService[Task](elasticsearchClient)

    BlazeBuilder[Task]
      .bindHttp(8080, "localhost")
      .mountService(elasticsearchService.routes, "/")
      .serve

  }).toIO.unsafeRunSync()

}
