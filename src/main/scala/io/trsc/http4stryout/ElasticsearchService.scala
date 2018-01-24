package io.trsc.http4stryout

import cats.Applicative
import cats.effect.Effect
import cats.implicits._
import io.circe.Encoder
import io.circe.Json
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl._
import org.http4s.headers._
import org.http4s.MediaType._

class ElasticsearchService[F[_]: Effect](client: ElasticsearchClient[F]) extends Http4sDsl[F] {

  // Replace this following line with this once bug in http4s-circe is fixed
  // implicit def entityEncoder[A: Encoder]: EntityEncoder[F, A] = jsonEncoderOf[F, A]
  implicit def entityEncoder[A](implicit enc:  Encoder[A], ev: EntityEncoder[F, String]): EntityEncoder[F, A] =
    jsonEncoderOf[F, A](ev, implicitly[Applicative[F]], enc)

  val routes: HttpService[F] = HttpService[F] {
    case GET -> Root / "health" =>
      Ok(checkHealth())

    case req @ POST -> Root / "search" / index => for {
      query    ← req.as[Json]
      result   ← search(index, query)
      response ← Ok(result).map(_.putHeaders(`Content-Type`(`application/json`)))
    } yield response
  }

  def checkHealth(): F[String] = client.health().map { status ⇒
    status.status match {
      case "green"  ⇒ "The cluster is feeling fine!"
      case "yellow" ⇒ "The cluster is feeling so-so!"
      case _        ⇒ "The cluster is dead!"
    }
  }

  //TODO convert our DSL into ES DSL here
  def search(index: String, query: Json): F[List[String]] = client.search(index, query.spaces2).map { result ⇒
    result.hits.hits.map(_.id)
  }

}
