import Versions.V

inThisBuild(
  Seq(
    scalaVersion := V.scala.Default,
    organization := "com.github.ingarabr",
    homepage := Some(url("https://github.com/ingarabr/sbt-firebase")),
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    developers := List(
      Developer(
        "ingarabr",
        "Ingar Abrahamsen",
        "ingar.abrahamasen@gmail.com",
        url("https://github.com/ingarabr/")
      )
    ),
    githubWorkflowJavaVersions += JavaSpec.temurin("17")
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
    scalacOptions -= "-Ywarn-unused:params", //
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-dsl" % V.Http4s,
      "org.http4s" %% "http4s-client" % V.Http4s,
      "org.http4s" %% "http4s-ember-client" % V.Http4s % Test,
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
    libraryDependencies += "org.http4s" %% "http4s-ember-client" % V.Http4s,
    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.10.7"
      }
    }
  )
  .dependsOn(`firebase-client`)
  .enablePlugins(SbtPlugin)
