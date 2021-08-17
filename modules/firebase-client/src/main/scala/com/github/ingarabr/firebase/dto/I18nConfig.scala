package com.github.ingarabr.firebase.dto

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class I18nConfig(
    root: String
)

object I18nConfig {

  implicit val codec: Codec[I18nConfig] = deriveCodec

}
