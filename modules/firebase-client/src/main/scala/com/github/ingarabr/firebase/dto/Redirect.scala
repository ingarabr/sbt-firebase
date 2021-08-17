package com.github.ingarabr.firebase.dto

import cats.syntax.semigroup._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json, JsonObject}

case class Redirect(
    statusCode: Int,
    location: String,
    matchPattern: MatchPattern
)

object Redirect {
  implicit val encoder: Encoder[Redirect] = obj =>
    Json.fromJsonObject(
      obj.matchPattern.asJsonObject |+|
        JsonObject(
          "statusCode" -> obj.statusCode.asJson,
          "location" -> obj.location.asJson
        )
    )

  implicit val decoder: Decoder[Redirect] = cur =>
    for {
      c <- cur.get[Int]("statusCode")
      l <- cur.get[String]("location")
      p <- MatchPattern.fromJson(cur)
    } yield Redirect(c, l, p)

}
