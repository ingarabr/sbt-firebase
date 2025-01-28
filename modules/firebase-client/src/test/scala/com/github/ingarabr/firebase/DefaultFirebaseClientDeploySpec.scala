package com.github.ingarabr.firebase

import cats.effect.unsafe.implicits.global
import cats.effect.IO
import com.github.ingarabr.firebase.GoogleAccessToken.AuthType.ServiceAccountKey
import com.github.ingarabr.firebase.dto.{SiteName, SiteVersionRequest}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.{File, FileInputStream}
import java.nio.file.Paths
import java.util.Properties
import fs2.io.file.Path
import org.http4s.ember.client.EmberClientBuilder

class DefaultFirebaseClientDeploySpec extends AnyFlatSpec with Matchers {

  it should "deploy to firebase" in withKeyPath { case (serviceAccount, firebaseSite) =>
    val resources =
      for {
        httpClient <-
          EmberClientBuilder
            .default[IO]
            .build
        fbClient <- FirebaseClient.resource(httpClient, serviceAccount)
      } yield fbClient

    val summary = resources
      .use { client =>
        client
          .upload(
            firebaseSite,
            Path.fromNioPath(Paths.get(getClass.getResource("/simple-page").getFile)),
            SiteVersionRequest.basic
          )
      }
      .unsafeRunSync()

    withClue("upload request") { summary.filesInUploadRequest shouldBe 4 }
    withClue("updated") { summary.filesRequiredToUpload should be <= 4 }
  }

  private def withKeyPath[A](body: (ServiceAccountKey, SiteName) => A) = {
    val props = List(new File("./ci-config.prop"), new File("./ci-config.prop"))
      .find(_.isFile())
      .map { f =>
        val p = new Properties()
        p.load(new FileInputStream(f))
        p
      }
    body(
      loadKey(props, "FIREBASE_TEST_SERVICE_ACCOUNT", path => ServiceAccountKey(path)),
      loadKey(props, "FIREBASE_TEST_PROJECT", SiteName.apply)
    )
  }

  private def loadKey[A](props: Option[Properties], key: String, fa: String => A): A = {
    val propKey = key.toLowerCase.replace("_", "-")
    props
      .flatMap { p => Option(p.getProperty(propKey)).map(fa) }
      .getOrElse(
        sys.env.get(key) match {
          case Some(value) => fa(value)
          case None        => cancel(s"Missing env key to run: $key")
        }
      )
  }

}
