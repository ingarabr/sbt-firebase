package com.github.ingarabr.firebase

import cats.Semigroup
import io.circe.JsonObject

package object dto {

  implicit val semigroupJsonObject: Semigroup[JsonObject] =
    (x: JsonObject, y: JsonObject) => x.deepMerge(y)

}
