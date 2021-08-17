package com.github.ingarabr.firebase.dto

import cats.Show
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

import java.nio.file.Path

case class PopulateFilesRequest(
    files: Map[String, String]
)

object PopulateFilesRequest {

  def apply(files: List[(Path, Path, String)]): PopulateFilesRequest =
    PopulateFilesRequest(files.map(v => s"/${v._1.toString}" -> v._3).toMap)

  implicit val coded: Codec[PopulateFilesRequest] = deriveCodec

  implicit val show: Show[PopulateFilesRequest] = req => coded(req).spaces2

}
