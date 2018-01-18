package io.trsc.http4stryout

import cats.effect._
import cats.implicits._
import io.circe.Decoder
import io.trsc.http4stryout.ElasticsearchModel.Health
import org.http4s.{EntityDecoder, Uri}
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl

class ElasticsearchClient[F[_] : Effect](clientF: F[Client[F]], esHost: String, esPort: Int) extends Http4sClientDsl[F] with Http4sDsl[F]{

  private val endpoint: Uri = Uri.unsafeFromString(s"http://$esHost:$esPort")

  implicit def entityDecoder[A: Decoder]: EntityDecoder[F, A] = jsonOf[F, A]

  object Cluster {
    def health(): F[Health] = clientF.flatMap(_.expect[Health](endpoint.withPath("/_cluster/health")))
  }

}
