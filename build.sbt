import Versions.V

inThisBuild(
  Seq(
    scalaVersion := V.scala.Default,
    organization := "com.github.ingarabr"
  )
)

lazy val `sbt-firebase-root` = (project in file("."))
  .settings(
    crossScalaVersions := Nil
  )
  .aggregate(`firebase-client`, `sbt-firebase`)

lazy val `firebase-client` = (project in file("modules/firebase-client"))
  .settings(
    crossScalaVersions := V.scala.Cross,
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-dsl" % V.Http4s,
      "org.http4s" %% "http4s-blaze-client" % V.Http4s,
      "org.http4s" %% "http4s-circe" % V.Http4s,
      "io.circe" %% "circe-literal" % V.Circe,
      "io.circe" %% "circe-generic" % V.Circe,
      "org.scalatest" %% "scalatest" % V.ScalaTest % Test,
      "com.google.auth" % "google-auth-library-oauth2-http" % V.GoogleOauth,
      "org.scala-lang.modules" %% "scala-collection-compat" % V.scala.CollectionCompat
    )
  )

lazy val `sbt-firebase` = (project in file("modules/sbt-firebase"))
  .settings(
    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.2.8"
      }
    }
  )
  .dependsOn(`firebase-client`)
  .enablePlugins(SbtPlugin)
