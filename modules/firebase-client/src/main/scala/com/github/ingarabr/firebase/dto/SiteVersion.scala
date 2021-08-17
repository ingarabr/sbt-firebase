package com.github.ingarabr.firebase.dto

import cats.Show
import io.circe.{Decoder, DecodingFailure, Json}

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
