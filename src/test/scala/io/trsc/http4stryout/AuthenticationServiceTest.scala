package io.trsc.http4stryout

import cats.Id
import cats.data.{Kleisli, OptionT}
import cats.implicits._
import io.trsc.http4stryout.CtpModel.{Token, TokenMeta}
import org.http4s.headers.Authorization
import org.http4s.{AuthScheme, Credentials, Headers, HttpService, Request, Response, Status}
import org.scalatest.FlatSpec

class AuthenticationServiceTest extends FlatSpec {

  private val ctpClient = new CtpClient[Id] {
    override def token(scope: String): Id[CtpModel.Token] = Token("hello", -1, "scope")
    override def introspect(token: String): Id[Option[CtpModel.TokenMeta]] = token match {
      case "inactive-token" ⇒ TokenMeta(false, none, none).some
      case "active-valid-token" ⇒ TokenMeta(true, none, (System.currentTimeMillis() + 1000 * 60 * 10).some).some
      case "active-expired-token" ⇒ TokenMeta(true, none, (System.currentTimeMillis() - 1000 * 60 * 10).some).some
    }
  }

  def service: HttpService[Id] = new AuthenticationService[Id](ctpClient).secure(
    Kleisli.liftF(OptionT.some(Response(Status.Ok)))
  )

  "AuthenticationService" should "deny access in case of inactive token" in {
    val request = Request[Id](headers = Headers(Authorization(Credentials.Token(AuthScheme.Bearer, "inactive-token"))))
    assert(service(request) == OptionT.some[Id](Response(Status.Unauthorized)))
  }

  "AuthenticationService" should "deny access in case of active expired token" in {
    val request = Request[Id](headers = Headers(Authorization(Credentials.Token(AuthScheme.Bearer, "active-expired-token"))))
    assert(service(request) == OptionT.some[Id](Response(Status.Unauthorized)))
  }

  "AuthenticationService" should "allow access in case of active and valid token" in {
    val request = Request[Id](headers = Headers(Authorization(Credentials.Token(AuthScheme.Bearer, "active-valid-token"))))
    assert(service(request) == OptionT.some[Id](Response(Status.Ok)))
  }

}
