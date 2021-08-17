package com.github.ingarabr.firebase.dto

import com.github.ingarabr.firebase.circe.CirceOneOf
import io.circe.Decoder.Result
import io.circe.{ACursor, Json, JsonObject}

/** The api accepts only one of the values but is encoded as
  * different fields in the json.
  */
sealed trait MatchPattern {
  def value: String
  def asJsonObject: JsonObject = MatchPattern.toJsonObject(this)

}

object MatchPattern extends CirceOneOf[MatchPattern] {
  case class Regex(value: String) extends MatchPattern
  case class Glob(value: String) extends MatchPattern

  private val regexKey = "regex"
  private val globKey = "glob"

  override def convertToJson: MatchPattern => (String, Json) = {
    case Regex(value) => regexKey -> Json.fromString(value)
    case Glob(value)  => globKey -> Json.fromString(value)
  }

  override def mappings: Map[String, ACursor => Result[MatchPattern]] =
    Map(
      regexKey -> (_.as[String].map(Regex.apply)),
      globKey -> (_.as[String].map(Glob.apply))
    )
}
