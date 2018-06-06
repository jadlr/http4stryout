package io.trsc.http4stryout

import cats.effect.Effect
import cats.implicits._
import fs2.StreamApp.ExitCode
import fs2.{Stream, StreamApp}
import monix.eval._
import monix.execution.Scheduler.Implicits.global
import org.http4s.HttpService
import org.http4s.dsl._
import org.http4s.server.blaze._

object Main extends StreamApp[Task] with Http4sDsl[Task] {

  def init[F[_]: Effect]: F[HttpService[F]] = for {
    config              ← ApplicationConfig[F]
    elasticsearchClient ← EffectElasticsearchClient[F](config.elasticsearchUrl)
    ctpClient           ← CachedEffectCtpClient[F](config.authWsUrl, config.ctpClientId, config.ctpClientSecret)
  } yield {

    val authService = new AuthenticationService[F](ctpClient)
    val elasticsearchService = new ElasticsearchService[F](elasticsearchClient)

    authService.secure(elasticsearchService.routes)
  }

  override def stream(args: List[String], requestShutdown: Task[Unit]): Stream[Task, ExitCode] = {
    Stream.eval(init[Task]).flatMap { service ⇒
      BlazeBuilder[Task]
        .bindHttp(8080, "localhost")
        .mountService(service, "/")
        .serve
    }
  }
}
