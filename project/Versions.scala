object Versions {

  object scala {
    val Scala212 = "2.12.15"
    val Scala213 = "2.13.7"

    val Default: String = Scala212
    val Cross = Seq(Scala212, Scala213)

    val CollectionCompat = "2.6.0"
  }

  val Http4s = "0.23.6"
  val Circe = "0.14.1"

  val GoogleOauth = "1.3.0"

  val ScalaTest = "3.2.10"

  def V = this
}
