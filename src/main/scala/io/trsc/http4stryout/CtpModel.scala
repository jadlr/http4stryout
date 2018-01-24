package io.trsc.http4stryout

import io.circe.Decoder
import io.circe.generic.semiauto._

object CtpModel {

  case class Token(accessToken: String, expiresAt: Long, scope: String)
  object Token {
    implicit val decoder: Decoder[Token] = Decoder.forProduct3("access_token", "expires_in", "scope")(Token.apply)
  }

  case class TokenMeta(active: Boolean, scope: Option[String], exp: Option[Long])
  object TokenMeta {
    implicit val decoder: Decoder[TokenMeta] = deriveDecoder
  }

}
