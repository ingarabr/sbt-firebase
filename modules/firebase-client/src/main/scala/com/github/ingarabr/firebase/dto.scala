package com.github.ingarabr.firebase

import cats.Show
import io.circe.{Codec, Decoder, DecodingFailure, Json}
import io.circe.generic.semiauto.deriveCodec

import java.nio.file.Path

object dto {

  case class SiteVersion(
      version: String,
      underlying: Json
  )

  object SiteVersion {
    implicit val decode: Decoder[SiteVersion] = json =>
      for {
        name <- json.downField("name").as[String]
        v <- name
          .split("/")
          .lastOption
          .toRight(DecodingFailure(s"No version found in $name", List.empty))
      } yield SiteVersion(v, json.value)

    implicit val show: Show[SiteVersion] = _.version
  }

  /** For your default Hosting site, the SITE_NAME is your Firebase project ID (which is used to create your Firebase subdomains SITE_NAME.web.app and SITE_NAME.firebaseapp.com).
    * If you’ve created multiple sites in your Firebase project, make sure that you’re using the SITE_NAME of the site to which you’d like to deploy.
    */
  case class SiteName(value: String) extends AnyVal

  object SiteName {
    implicit val show: Show[SiteName] = _.value
  }

  case class PopulateFilesRequest(files: Map[String, String])

  object PopulateFilesRequest {
    implicit val coded: Codec[PopulateFilesRequest] = deriveCodec

    def apply(files: List[(Path, Path, String)]): PopulateFilesRequest =
      PopulateFilesRequest(files.map(v => s"/${v._1.toString}" -> v._3).toMap)
  }

  case class PopulateFilesResponse(
      uploadRequiredHashes: Option[List[String]],
      uploadUrl: String
  )

  object PopulateFilesResponse {
    implicit val coded: Codec[PopulateFilesResponse] = deriveCodec
  }

}
