package com.github.ingarabr.firebase.dto

import cats.syntax.semigroup._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json, JsonObject}

case class Header(
    headers: Map[String, String],
    matchPattern: MatchPattern
)

object Header {

  implicit val encoder: Encoder[Header] = obj =>
    Json.fromJsonObject(
      obj.matchPattern.asJsonObject |+|
        JsonObject("headers" -> obj.headers.asJson)
    )

  implicit val decoder: Decoder[Header] = cur =>
    for {
      h <- cur.get[Map[String, String]]("headers")
      p <- MatchPattern.fromJson(cur)
    } yield Header(h, p)

}
