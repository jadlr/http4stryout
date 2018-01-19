package io.trsc.http4stryout

import cats.effect.Effect
import cats.implicits._
import org.http4s._
import org.http4s.dsl._

class ElasticsearchService[F[_]: Effect](client: ElasticsearchClient[F]) extends Http4sDsl[F] {

  val routes: HttpService[F] = HttpService[F] {
    case GET -> Root / "health" =>
      val response = client.health().map { status ⇒
        if (status.status == "red") "Ohhh noes"
        else "Allll fine!"
      }
      Ok(response)
  }

}
