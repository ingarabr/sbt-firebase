object Versions {

  object scala {
    val Scala212 = "2.12.13"
    val Scala213 = "2.13.5"

    val Default = Scala212
    val Cross = Seq(Scala212, Scala213)

    val CollectionCompat = "2.4.2"
  }

  val Http4s = "0.21.20"
  val Circe = "0.13.0"

  val GoogleOauth = "0.25.0"

  val ScalaTest = "3.2.6"

  def V = this
}
