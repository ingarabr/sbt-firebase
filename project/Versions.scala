object Versions {

  object scala {
    val Scala212 = "2.12.16"
    val Scala213 = "2.13.15"
    val Scala3 = "3.3.4"

    val Default: String = Scala212
    val Cross = Seq(Scala212, Scala213, Scala3)

    val CollectionCompat = "2.12.0"
  }

  val Http4s = "0.23.30"
  val Circe = "0.14.10"

  val GoogleOauth = "1.30.1"

  val ScalaTest = "3.2.19"

  def V = this
}
