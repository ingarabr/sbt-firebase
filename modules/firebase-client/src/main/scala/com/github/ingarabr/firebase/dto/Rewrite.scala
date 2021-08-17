package com.github.ingarabr.firebase.dto

import cats.syntax.semigroup._
import io.circe.{Decoder, Encoder, Json}

case class Rewrite(
    matchPattern: MatchPattern,
    rewriteBehavior: RewriteBehavior
)

object Rewrite {
  implicit val decoder: Decoder[Rewrite] = cur =>
    for {
      mp <- MatchPattern.fromJson(cur)
      rb <- RewriteBehavior.fromJson(cur)
    } yield Rewrite(mp, rb)

  implicit val encoder: Encoder[Rewrite] = rewrite =>
    Json.fromJsonObject(
      rewrite.matchPattern.asJsonObject |+|
        rewrite.rewriteBehavior.asJsonObject
    )

}
