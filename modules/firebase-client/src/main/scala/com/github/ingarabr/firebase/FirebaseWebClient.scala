package com.github.ingarabr.firebase

import cats.effect._
import cats.syntax.flatMap._
import cats.syntax.traverse._
import cats.syntax.functor._
import cats.syntax.monadError._
import cats.syntax.show._
import com.google.auth.oauth2.AccessToken
import com.github.ingarabr.firebase.dto._
import fs2.Stream
import io.circe.Json
import io.circe.syntax._
import org.http4s.{Headers, MediaType, Request, Status}
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.headers.`Content-Type`

class FirebaseWebClient[F[_]: Async](
    client: Client[F],
    accessToken: F[AccessToken]
) extends Http4sDsl[F]
    with Http4sClientDsl[F] {

  private val apiHost = uri"https://firebasehosting.googleapis.com"
  private val uploadHost = uri"https://upload-firebasehosting.googleapis.com"

  def versionsCreate(siteName: SiteName, siteVersionRequest: SiteVersionRequest): F[SiteVersion] = {
    for {
      token <- accessToken.map(_.header)
      req = POST(
        siteVersionRequest.asJson,
        apiHost / "v1beta1" / "sites" / siteName.value / "versions",
        token
      )
      res <- client.run(req).use(_.asJsonDecode[SiteVersion])
    } yield res
  }.adaptError { case t =>
    new Exception(show"Failed to create new site version for site $siteName", t)
  }

  def populateFiles(
      siteName: SiteName,
      siteVersion: SiteVersion,
      files: PopulateFilesRequest
  ): F[Set[String]] =
    files.files
      .grouped(1000)
      .map(PopulateFilesRequest(_))
      .toList
      .map(f => {
        for {
          token <- accessToken.map(_.header)
          req = POST(
            f.asJson,
            apiHost / "v1beta1" / "sites" / siteName.value / "versions" / (siteVersion.version + ":populateFiles"),
            token
          )
          res <- client.run(req).use(_.asJsonDecode[PopulateFilesResponse])
        } yield res
      })
      .sequence
      .map(_.flatMap(r => r.uploadRequiredHashes).flatten.toSet)

  def upload(
      siteName: SiteName,
      siteVersion: SiteVersion,
      fileHash: String,
      file: Stream[F, Byte]
  ): F[Unit] =
    for {
      token <- accessToken.map(_.header)
      req = Request[F](
        method = POST,
        body = file,
        uri =
          uploadHost / "upload" / "sites" / siteName.value / "versions" / siteVersion.version / "files" / fileHash,
        headers = Headers(
          token,
          `Content-Type`(MediaType.application.`octet-stream`)
        )
      )
      res <- client
        .run(req)
        .use(r =>
          if (r.status == Status.Ok) Sync[F].unit
          else
            r.bodyText.foldMonoid.compile.string.flatMap(t =>
              Sync[F].raiseError[Unit](new Exception(t))
            )
        )
    } yield res

  def uploadingDone(siteName: SiteName, siteVersion: SiteVersion): F[Json] =
    for {
      token <- accessToken.map(_.header)
      req = PATCH(
        Json
          .obj("status" -> Json.fromString("FINALIZED")),
        (apiHost / "v1beta1" / "sites" / siteName.value / "versions" / siteVersion.version)
          .withQueryParam("update_mask", "status"),
        token
      )
      res <- client.run(req).use(_.asJson)
    } yield res

  def release(siteName: SiteName, siteVersion: SiteVersion): F[Json] =
    for {
      token <- accessToken.map(_.header)
      req = POST(
        (apiHost / "v1beta1" / "sites" / siteName.value / "releases")
          .withQueryParam(
            "versionName",
            show"sites/$siteName/versions/$siteVersion"
          ),
        token
      )
      res <- client.run(req).use(_.asJson)
    } yield res

}
