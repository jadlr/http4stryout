package io.trsc.http4stryout

import cats.effect._
import cats.implicits._
import io.circe.Decoder
import io.trsc.http4stryout.ElasticsearchModel.Health
import org.http4s.{EntityDecoder, Uri}
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.client.blaze.Http1Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl

trait ElasticsearchClient[F[_]] {
  def health(): F[Health]
}

class EffectElasticsearchClient[F[_] : Effect](esHost: String, esPort: Int)
  extends ElasticsearchClient[F]
    with Http4sClientDsl[F]
    with Http4sDsl[F] {

  private val httpClientF: F[Client[F]] = Http1Client[F]()

  private val endpoint: Uri = Uri.unsafeFromString(s"http://$esHost:$esPort")

  implicit def entityDecoder[A: Decoder]: EntityDecoder[F, A] = jsonOf[F, A]

  def health(): F[Health] = httpClientF.flatMap(_.expect[Health](endpoint.withPath("/_cluster/health")))

}
