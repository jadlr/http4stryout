package io.trsc.http4stryout

import cats.effect.Effect
import cats.implicits._
import io.circe.Decoder
import io.trsc.http4stryout.CtpModel.{Token, TokenMeta}
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.client.blaze.Http1Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.http4s.{BasicCredentials, EntityDecoder, Headers, Method, Request, Response, Status, Uri, UrlForm}

trait CtpClient[F[_]] {
  def token(scope: String): F[Token]
  def introspect(token: String): F[Option[TokenMeta]]
}

class EffectCtpClient[F[_]: Effect](client: Client[F], authWsUrl: String, id: String, secret: String)
  extends CtpClient[F]
    with Http4sClientDsl[F]
    with Http4sDsl[F] {

  private val endpoint: Uri = Uri.unsafeFromString(authWsUrl)

  implicit def entityDecoder[A: Decoder]: EntityDecoder[F, A] = jsonOf[F, A]

  override def token(scope: String): F[Token] = {
    val req = Request[F](
      method = Method.POST,
      uri = endpoint.withPath("/oauth/internal/token"),
      headers = Headers(Authorization(BasicCredentials(id, secret)))
    ).withBody(UrlForm(
      "grant_type" → "client_credentials",
      "scope" → scope
    ))

    client.expect[Token](req)
  }

  override def introspect(token: String): F[Option[TokenMeta]] = {
    val req = Request[F](
      method = Method.POST,
      uri = endpoint.withPath("/oauth/introspect"),
      headers = Headers(Authorization(BasicCredentials(id, secret)))
    ).withBody(UrlForm(
      "token" → token
    ))

    client.fetch(req)(_.attemptAs[TokenMeta].toOption.value)
  }
}

object EffectCtpClient {
  def apply[F[_]: Effect](authWsUrl: String, id: String, secret: String): F[EffectCtpClient[F]] =
    Http1Client[F]().map(client ⇒ new EffectCtpClient[F](client, authWsUrl, id, secret))
}
