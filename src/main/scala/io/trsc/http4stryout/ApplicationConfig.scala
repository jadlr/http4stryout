package io.trsc.http4stryout

import cats.effect.Sync
import pureconfig.loadConfigOrThrow

case class ApplicationConfig(
  authWsUrl: String,
  ctpClientId: String,
  ctpClientSecret: String,
  elasticsearchUrl: String
)

object ApplicationConfig {

  def apply[F[_]: Sync]: F[ApplicationConfig] = Sync[F].delay {
    loadConfigOrThrow[ApplicationConfig]("http4stryout")
  }

}
