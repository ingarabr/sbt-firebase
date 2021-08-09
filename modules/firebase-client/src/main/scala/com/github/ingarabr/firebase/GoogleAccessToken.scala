package com.github.ingarabr.firebase

import cats.Show
import cats.effect.Ref
import cats.effect.{Clock, Resource, Sync}
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.show._
import com.github.ingarabr.firebase.GoogleAccessToken.AuthType._
import com.google.auth.oauth2.{AccessToken, GoogleCredentials}

import java.io.{File, FileInputStream}
import java.nio.file.{Path, Paths}
import scala.jdk.CollectionConverters._

object GoogleAccessToken {

  def getAccessToken[F[_]: Sync](authType: AuthType): F[AccessToken] =
    Sync[F].delay {
      val scopes = List(
        // View and manage your data across Google Cloud Platform services
        "https://www.googleapis.com/auth/cloud-platform",
        // View and administer all your Firebase data and settings
        "https://www.googleapis.com/auth/firebase"
      ).asJava
      val baseGoogleCredential =
        authType match {
          case ApplicationDefault =>
            GoogleCredentials.getApplicationDefault()
          case ServiceAccountKey(path) =>
            GoogleCredentials.fromStream(new FileInputStream(path.toFile))
        }
      val scopedGoogleCredential = baseGoogleCredential.createScoped(scopes)
      scopedGoogleCredential.refresh()
      scopedGoogleCredential.getAccessToken
    }

  def cached[F[_]: Sync: Clock](authType: AuthType): Resource[F, F[AccessToken]] = {
    Resource
      .make(getAccessToken[F](authType).flatMap(Ref[F].of))(_ => Sync[F].unit)
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
  object AuthType {
    case object ApplicationDefault extends AuthType
    case class ServiceAccountKey(path: Path) extends AuthType

    object ServiceAccountKey {
      def apply(serviceAccountPath: String) = new ServiceAccountKey(Paths.get(serviceAccountPath))
      def apply(serviceAccountFile: File) = new ServiceAccountKey(serviceAccountFile.toPath)
    }

    implicit val show: Show[AuthType] = {
      case ApplicationDefault      => "ApplicationDefault"
      case ServiceAccountKey(path) => show"ServiceAccountKey(${path.toString})"
    }

  }

}
