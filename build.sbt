import Versions.V

inThisBuild(
  Seq(
    scalaVersion := V.scala,
    organization := "com.github.ingarabr"
  )
)

lazy val `sbt-firebase-root` = (project in file("."))
  .aggregate(`firebase-client`)

lazy val `firebase-client` = (project in file("modules/firebase-client"))
  .settings(
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-dsl" % V.http4s,
      "org.http4s" %% "http4s-blaze-client" % V.http4s,
      "org.http4s" %% "http4s-circe" % V.http4s,
      "io.circe" %% "circe-literal" % V.circe,
      "io.circe" %% "circe-generic" % V.circe,
      "org.scalatest" %% "scalatest" % V.scalaTest % Test,
      "com.google.auth" % "google-auth-library-oauth2-http" % V.googleOauth
    )
  )
