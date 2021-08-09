package com.github.ingarabr.firebase

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.flatMap._
import com.github.ingarabr.firebase.GoogleAccessToken.AuthType
import org.http4s.blaze.client.BlazeClientBuilder
import sbt.Keys.streams
import sbt.{Def, _}

import scala.concurrent.ExecutionContext

object FirebasePlugin extends AutoPlugin {

  object autoImport {
    val firebaseSiteName = taskKey[String]("The some of the firebase project/site.")
    val firebaseHostingFolder = taskKey[File]("The location of the hosting files.")
    val firebaseAuth =
      taskKey[AuthType]("How to authenticate with firebase. Using google library defaults")

    val firebaseDeploy = taskKey[Unit]("Deploy hosting files to firebase")
  }

  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      firebaseAuth := AuthType.ApplicationDefault,
      firebaseDeploy := {
        val dir = firebaseHostingFolder.value
        val name = firebaseSiteName.value
        val auth = firebaseAuth.value
        val log = streams.value.log

        log.info(
          s"""|Firebase deploy
              | - site name:   $name
              | - auth method: $auth
              | - from path:   $dir
              |""".stripMargin
        )

        firebaseClientResource(auth)
          .use(c => c.upload(dto.SiteName(name), dir.toPath))
          .flatTap(summary =>
            IO(
              log.info(
                s"""|Firebase deploy summary:
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
      http4sClient <- BlazeClientBuilder[IO](ExecutionContext.global).resource
      firebaseClient <- FirebaseClient.resource[IO](http4sClient, auth)
    } yield firebaseClient
  }
}
