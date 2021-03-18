package com.github.ingarabr.firebase

import cats.effect.{Blocker, Clock, ContextShift, IO, Timer}
import com.github.ingarabr.firebase.GoogleAccessToken.AuthType.ServiceAccountKey
import com.github.ingarabr.firebase.dto.SiteName
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.http4s.client.blaze.BlazeClientBuilder

import java.nio.file.Paths
import scala.concurrent.ExecutionContext

class DefaultFirebaseClientDeploySpec extends AnyFlatSpec with Matchers {

  private val blocker = Blocker.liftExecutionContext(ExecutionContext.global)
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
  implicit val clock: Clock[IO] = timer.clock

  it should "deploy to firebase" in withKeyPath { case (serviceAccount, firebaseSite) =>
    val resources =
      for {
        c <- BlazeClientBuilder[IO](ExecutionContext.global).resource
        fbClient <- FirebaseClient.resource(blocker, c, serviceAccount)
      } yield fbClient

    resources
      .use { client =>
        client
          .upload(
            firebaseSite,
            Paths.get(getClass.getResource("/simple-page").getFile)
          )
      }
      .unsafeRunSync()
  }

  private def withKeyPath[A](body: (ServiceAccountKey, SiteName) => A) = {
    body(
      loadKey("FIREBASE_TEST_SERVICE_ACCOUNT", path => ServiceAccountKey(path)),
      loadKey("FIREBASE_TEST_PROJECT", SiteName.apply)
    )
  }

  private def loadKey[A](key: String, fa: String => A) = {
    sys.env.get(key) match {
      case Some(value) => fa(value)
      case None        => cancel(s"Missing env key to run: $key")
    }
  }

}
