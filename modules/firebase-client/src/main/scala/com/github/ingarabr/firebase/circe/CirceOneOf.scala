package com.github.ingarabr.firebase.circe

import cats.syntax.show._
import io.circe.{ACursor, Decoder, DecodingFailure, HCursor, Json, JsonObject}

/** Some api models requires only one out of multiple fields
  * to contain a value. This trait make it a bit easier to do
  * so and use a sealed instance in scala.
  */
trait CirceOneOf[A] {

  def convertToJson: A => (String, Json)
  def mappings: Map[String, ACursor => Decoder.Result[A]]

  def toJsonObject(a: A): JsonObject = JsonObject(convertToJson(a))

  def fromJson(cur: HCursor): Decoder.Result[A] = {
    val keys =
      cur.keys
        .fold[List[String]](List.empty)(_.toList)
        .filter(mappings.keys.toList.contains)
    keys match {
      case one :: Nil => mappings(one)(cur.downField(one))
      case other =>
        val msg =
          show"expected one of ${keys.mkString("[", ", ", "]")} got ${other.mkString("[", ", ", "]")}"
        Left(DecodingFailure(msg, List.empty))
    }

  }

}
