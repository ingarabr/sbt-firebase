package com.github.ingarabr.firebase.dto

import com.github.ingarabr.firebase.circe.CirceOneOf
import io.circe.Decoder.Result
import io.circe.{ACursor, Json, JsonObject}

/** The api accepts only one of the values but is encoded as
  * different fields in the json.
  */
sealed trait RewriteBehavior {
  def asJsonObject: JsonObject = RewriteBehavior.toJsonObject(this)
}

object RewriteBehavior extends CirceOneOf[RewriteBehavior] {
  case class Path(value: String) extends RewriteBehavior
  case class Function(value: String) extends RewriteBehavior
  case class CloudRunRewrite(serviceId: String, region: String) extends RewriteBehavior

  private val pathKey = "path"
  private val functionKey = "function"
  private val cloudRunKey = "run"

  override def convertToJson: RewriteBehavior => (String, Json) = {
    case Path(value)     => pathKey -> Json.fromString(value)
    case Function(value) => functionKey -> Json.fromString(value)
    case CloudRunRewrite(serviceId, region) =>
      cloudRunKey -> Json.obj(
        "serviceId" -> Json.fromString(serviceId),
        "region" -> Json.fromString(region)
      )
  }

  override def mappings: Map[String, ACursor => Result[RewriteBehavior]] =
    Map(
      pathKey -> (_.as[String].map(Path.apply)),
      functionKey -> (_.as[String].map(Function.apply)),
      cloudRunKey -> (cur =>
        for {
          s <- cur.get[String]("serviceId")
          r <- cur.get[String]("region")
        } yield CloudRunRewrite(s, r)
      )
    )

}
