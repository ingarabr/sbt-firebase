package com.github.ingarabr.firebase

import cats.effect.IO
import cats.syntax.show._
import cats.effect.unsafe.implicits.global
import com.github.ingarabr.firebase.GoogleAccessToken.AuthType
import com.github.ingarabr.firebase.dto.SiteVersionRequest
import org.http4s.ember.client.EmberClientBuilder
import sbt.Keys.streams
import sbt.{Def, _}
import fs2.io.file.{Path => Fs2Path}
import scala.concurrent.ExecutionContext

object FirebaseHostingPlugin extends AutoPlugin {

  object autoImport {
    val firebaseSiteName = taskKey[String]("The some of the firebase project/site.")
    val firebaseVersionConfig = taskKey[SiteVersionRequest](
      """The firebase site version configuration.
        |For details see https://firebase.google.com/docs/reference/hosting/rest/v1beta1/sites.versions
        |""".stripMargin
    )
    val firebaseHostingFolder = taskKey[File]("The location of the hosting files.")
    val firebaseAuth =
      taskKey[AuthType]("How to authenticate with firebase. Using google library defaults")

    val firebaseDeployHosting = taskKey[Unit]("Deploy hosting files to firebase")
  }

  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      firebaseAuth := AuthType.ApplicationDefault,
      firebaseVersionConfig := SiteVersionRequest.basic,
      firebaseDeployHosting := {
        val dir = firebaseHostingFolder.value
        val name = firebaseSiteName.value
        val auth = firebaseAuth.value
        val log = streams.value.log
        val cfg = firebaseVersionConfig.value

        log.info(
          show"""|Firebase deploy:
                 | - site name:   $name
                 | - auth method: $auth
                 | - from path:   ${dir.getPath}
                 |""".stripMargin
        )

        firebaseClientResource(auth)
          .use(c => c.upload(dto.SiteName(name), Fs2Path.fromNioPath(dir.toPath), cfg))
          .flatTap(summary =>
            IO(
              log.info(
                show"""|Firebase deploy summary:
                       | - files requested to be uploaded: ${summary.filesInUploadRequest}
                       | - files needed to be uploaded:    ${summary.filesRequiredToUpload}
                       | """.stripMargin
              )
            )
          )
          .void
          .unsafeRunSync()

      }
    )

  private def firebaseClientResource(auth: AuthType) = {
    for {
      http4sClient <-
        EmberClientBuilder
          .default[IO]
          .build
      firebaseClient <- FirebaseClient.resource[IO](http4sClient, auth)
    } yield firebaseClient
  }
}
