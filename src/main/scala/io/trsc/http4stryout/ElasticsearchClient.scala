package io.trsc.http4stryout

import cats.effect._
import cats.implicits._
import io.circe.{Decoder, Json}
import io.trsc.http4stryout.ElasticsearchModel.{Health, Results}
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.client.blaze.Http1Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, Uri}

trait ElasticsearchClient[F[_]] {
  def health(): F[Health]
  def search(index: String, query: Json): F[Results]
}

class EffectElasticsearchClient[F[_] : Effect](client: Client[F], elasticsearchUrl: String)
  extends ElasticsearchClient[F]
    with Http4sClientDsl[F]
    with Http4sDsl[F] {

  private val endpoint: Uri = Uri.unsafeFromString(elasticsearchUrl)

  implicit def entityDecoder[A: Decoder]: EntityDecoder[F, A] = jsonOf[F, A]

  def health(): F[Health] =
    client.expect[Health](endpoint.withPath("/_cluster/health"))

  def search(index: String, query: Json): F[Results] =
    client.expect[Results](POST(endpoint.withPath(s"/$index/_search"), query))

}

object EffectElasticsearchClient {
  def apply[F[_]: Effect](elasticsearchUrl: String): F[EffectElasticsearchClient[F]] =
    Http1Client[F]().map(client ⇒ new EffectElasticsearchClient(client, elasticsearchUrl))
}
