object Versions {

  object scala {
    val Scala212 = "2.12.13"
    val Scala213 = "2.13.6"

    val Default = Scala212
    val Cross = Seq(Scala212, Scala213)

    val CollectionCompat = "2.5.0"
  }

  val Http4s = "0.23.0"
  val Circe = "0.14.1"

  val GoogleOauth = "1.0.0"

  val ScalaTest = "3.2.9"

  def V = this
}
