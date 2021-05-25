package com.github.ingarabr.firebase

import cats.effect.{Blocker, Clock, ContextShift, IO, Timer}
import com.github.ingarabr.firebase.GoogleAccessToken.AuthType.ServiceAccountKey
import com.github.ingarabr.firebase.dto.SiteName
import org.http4s.client.Client
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.middleware.RequestLogger
import org.http4s.client.middleware.ResponseLogger

import java.io.{File, FileInputStream}
import java.nio.file.Paths
import java.util.Properties
import scala.concurrent.ExecutionContext

class DefaultFirebaseClientDeploySpec extends AnyFlatSpec with Matchers {

  private val blocker = Blocker.liftExecutionContext(ExecutionContext.global)
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
  implicit val clock: Clock[IO] = timer.clock

  private def logger = (str: String) => IO(println(str))

  val clientLogger: Client[IO] => Client[IO] = c =>
    RequestLogger.apply[IO](
      logHeaders = false,
      logBody = false,
      logAction = Some(str => logger(s">> $str"))
    )(
      ResponseLogger.apply[IO](
        logHeaders = false,
        logBody = true,
        logAction = Some(str => logger(s">> $str"))
      )(c)
    )

  it should "deploy to firebase" in withKeyPath { case (serviceAccount, firebaseSite) =>
    val resources =
      for {
        httpClient <- BlazeClientBuilder[IO](ExecutionContext.global).resource.map(clientLogger)
        fbClient <- FirebaseClient.resource(blocker, httpClient, serviceAccount)
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
