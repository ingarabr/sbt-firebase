package com.github.ingarabr.firebase.dto

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class ServingConfig(
    headers: List[Header],
    redirects: List[Redirect],
    rewrites: List[Rewrite],
    trailingSlashBehavior: String = "TRAILING_SLASH_BEHAVIOR_UNSPECIFIED",
    i18n: Option[I18nConfig] = None
)

object ServingConfig {
  implicit val codec: Codec[ServingConfig] = deriveCodec
}
