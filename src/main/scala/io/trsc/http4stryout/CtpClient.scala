package io.trsc.http4stryout

import cats.data.OptionT
import cats.effect.{Effect, Sync}
import cats.implicits._
import com.github.benmanes.caffeine.cache.Caffeine
import io.circe.Decoder
import io.trsc.http4stryout.CtpModel.{Token, TokenMeta}
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.client.blaze.Http1Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.http4s.{BasicCredentials, EntityDecoder, Headers, Method, Request, Uri, UrlForm}

import scala.concurrent.duration._
import scalacache.CatsEffect.asyncForCatsEffectAsync
import scalacache.caffeine.CaffeineCache
import scalacache.{Async, Cache, Entry, Mode}

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
  def apply[F[_]: Effect](authWsUrl: String, id: String, secret: String): F[CtpClient[F]] =
    Http1Client[F]().map(client ⇒ new EffectCtpClient[F](client, authWsUrl, id, secret))
}

class CachedEffectCtpClient[F[_]: Effect](cache: Cache[TokenMeta], ctpClient: CtpClient[F])(implicit m: Mode[F])
  extends CtpClient[F] {

  // not cached
  override def token(scope: String): F[Token] = ctpClient.token(scope)

  override def introspect(token: String): F[Option[TokenMeta]] = {
    OptionT(cache.get[F](token)).orElse {
      OptionT(ctpClient.introspect(token)).semiflatMap {
        case tokenMeta @ TokenMeta(true, _, Some(exp)) ⇒
          val ttl = Duration(exp - System.currentTimeMillis(), MILLISECONDS)
          cache.put[F](token)(tokenMeta, ttl.some).map(_ ⇒ tokenMeta)
        case tokenMeta ⇒
          tokenMeta.pure[F]
      }
    }.value
  }
}

object CachedEffectCtpClient {

  private def cache[F[_]: Sync]: F[Cache[TokenMeta]] = Sync[F].delay {
    val underlyingCaffeineCache = Caffeine.newBuilder().maximumSize(10000L).build[String, Entry[TokenMeta]]
    CaffeineCache(underlyingCaffeineCache)
  }

  def apply[F[_]: Effect](authWsUrl: String, id: String, secret: String): F[CtpClient[F]] = {
    EffectCtpClient[F](authWsUrl, id, secret) → cache[F] mapN { (client, cache) ⇒
      implicit val io: Mode[F] = new Mode[F] {
        override val M: Async[F] = asyncForCatsEffectAsync[F]
      }
      new CachedEffectCtpClient[F](cache, client)
    }
  }

}
