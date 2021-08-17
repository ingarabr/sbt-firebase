package com.github.ingarabr.firebase.dto

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class SiteVersionRequest(
    status: VersionStatus,
    labels: Map[String, String],
    config: ServingConfig
)

object SiteVersionRequest {
  implicit val codec: Codec[SiteVersionRequest] = deriveCodec

  val basic: SiteVersionRequest =
    SiteVersionRequest(
      status = VersionStatus.Created,
      labels = Map.empty,
      config = ServingConfig(
        headers = List(
          Header(
            headers = Map("Cache-Control" -> "max-age=1800"),
            matchPattern = MatchPattern.Glob("**")
          )
        ),
        redirects = List.empty,
        rewrites = List.empty
      )
    )
}
