package io.trsc.http4stryout

import cats.Monad
import cats.data.{Kleisli, OptionT}
import io.trsc.http4stryout.CtpModel.TokenMeta
import org.http4s.headers.Authorization
import org.http4s.{AuthScheme, Credentials, HttpService}

class AuthenticationService[F[_]: Monad](ctpClient: CtpClient[F]) {

  def secure(service: HttpService[F]): HttpService[F] = Kleisli { request ⇒
    for {
      header                                      ← OptionT.fromOption[F](request.headers.get(Authorization))
      Credentials.Token(AuthScheme.Bearer, token) ← OptionT.pure[F](header.credentials)
      TokenMeta(true, _, Some(exp))               ← OptionT(ctpClient.introspect(token))
      if exp > System.currentTimeMillis()
      response ← service(request)
    } yield response
  }

}
