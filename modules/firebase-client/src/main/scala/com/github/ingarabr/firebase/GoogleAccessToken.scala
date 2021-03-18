package com.github.ingarabr.firebase

import cats.effect.concurrent.Ref
import cats.implicits._
import cats.effect.{Clock, Resource, Sync}
import com.google.auth.oauth2.{AccessToken, GoogleCredentials}
import org.http4s.{AuthScheme, Credentials}
import org.http4s.headers.Authorization

import java.io.FileInputStream
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import scala.jdk.CollectionConverters._

object GoogleAccessToken {

  def getAccessToken[F[_]: Sync](authType: AuthType): F[AccessToken] =
    Sync[F].delay {
      println("creating new token!")
      val scopes = List(
        // View and manage your data across Google Cloud Platform services
        "https://www.googleapis.com/auth/cloud-platform",
        // View and administer all your Firebase data and settings
        "https://www.googleapis.com/auth/firebase"
      ).asJava
      val baseGoogleCredential =
        (authType match {
          case ApplicationDefault =>
            GoogleCredentials.getApplicationDefault()
          case ServiceAccountKey(path) =>
            GoogleCredentials.fromStream(new FileInputStream(path.toFile))
        })
      val scopedGoogleCredential = baseGoogleCredential.createScoped(scopes)
      scopedGoogleCredential.refresh()
      scopedGoogleCredential.getAccessToken
    }

  implicit class TokenExtra(token: AccessToken) {
    def hasExpired[F[_]: Sync: Clock]: F[Boolean] =
      Clock[F]
        .realTime(TimeUnit.MILLISECONDS)
        .map(_ >= token.getExpirationTime.getTime)

    def header: Authorization =
      Authorization(Credentials.Token(AuthScheme.Bearer, token.getTokenValue))
  }

  def cached[F[_]: Sync: Clock](authType: AuthType): Resource[F, F[AccessToken]] = {
    Resource
      .make(getAccessToken[F](authType).flatMap(Ref.of(_)))(_ => Sync[F].unit)
      .map(ref =>
        ref.get.flatMap(token =>
          token.hasExpired[F].flatMap {
            case true  => getAccessToken[F](authType).flatMap(t => ref.updateAndGet(_ => t))
            case false => token.pure[F]
          }
        )
      )
  }

  sealed trait AuthType
  case object ApplicationDefault extends AuthType
  case class ServiceAccountKey(path: Path) extends AuthType
}
