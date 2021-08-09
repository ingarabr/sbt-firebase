package com.github.ingarabr

import cats.effect.{Clock, Sync}
import cats.syntax.functor._
import com.google.auth.oauth2.AccessToken
import org.http4s.{AuthScheme, Credentials}
import org.http4s.headers.Authorization

package object firebase {

  implicit class TokenExtra(token: AccessToken) {
    def hasExpired[F[_]: Sync: Clock]: F[Boolean] =
      Clock[F].realTime.map(_.toMillis >= token.getExpirationTime.getTime)

    def header: Authorization =
      Authorization(Credentials.Token(AuthScheme.Bearer, token.getTokenValue))
  }
}