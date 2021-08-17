package com.github.ingarabr.firebase.dto

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class PopulateFilesResponse(
    uploadRequiredHashes: Option[List[String]],
    uploadUrl: String
)

object PopulateFilesResponse {
  implicit val coded: Codec[PopulateFilesResponse] = deriveCodec
}
