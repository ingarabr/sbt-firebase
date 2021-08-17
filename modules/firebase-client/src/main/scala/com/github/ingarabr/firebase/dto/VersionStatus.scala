package com.github.ingarabr.firebase.dto

import cats.syntax.show._
import io.circe.{Decoder, Encoder}

import scala.collection.Set

sealed abstract class VersionStatus(val value: String)

object VersionStatus {
  case object VersionStatusUnspecified extends VersionStatus("VERSION_STATUS_UNSPECIFIED")
  case object Created extends VersionStatus("CREATED")
  case object Finalized extends VersionStatus("FINALIZED")
  case object Deleted extends VersionStatus("DELETED")
  case object Abandoned extends VersionStatus("ABANDONED")
  case object Expired extends VersionStatus("EXPIRED")
  case object Cloning extends VersionStatus("CLONING")

  val values: Set[VersionStatus] =
    Set(
      VersionStatusUnspecified,
      Created,
      Finalized,
      Deleted,
      Abandoned,
      Expired,
      Cloning
    )

  implicit val encoder: Encoder[VersionStatus] =
    Encoder.encodeString.contramap(_.value)

  implicit val decoder: Decoder[VersionStatus] =
    Decoder.decodeString.emap(str =>
      values.find(_.value == str).toRight(show"No matching value from $str")
    )
}
