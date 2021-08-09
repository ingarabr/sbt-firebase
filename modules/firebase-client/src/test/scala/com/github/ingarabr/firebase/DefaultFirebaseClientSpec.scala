package com.github.ingarabr.firebase

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import fs2.Stream
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.nio.file.{Files, Paths}

class DefaultFirebaseClientSpec extends AnyFlatSpec with Matchers {

  it should "calculate digest" in {
    List(
      //Equal to `echo -n 1234567890 | openssl dgst -sha256`
      "1234567890" -> "c775e7b757ede630cd0aa1113bd102661ab38829ca52a6422ab782862f268646",
      "1234567890\n" -> "4795a1c2517089e4df569afd77c04e949139cf299c87f012b894fccf91df4594",
      "foo 12 baz bar" -> "cc2fd858027577ecc07e71bad71ffe9ebf82a4647747bb04b91279737de44218"
    ).foreach { case (input, expected) =>
      val value = Stream.iterable(input.getBytes())
      val digest = DefaultFirebaseClient.digestHexStr[IO](value).unsafeRunSync()
      digest shouldBe expected
    }
  }

  it should "calculate digest from from file" in {
    val js = getClass.getResource("/simple-page/app.js").getPath

    val tempDir = Files.createTempDirectory("test")
    val digest = DefaultFirebaseClient
      .zipAndDigest[IO](
        Paths.get(js),
        tempDir.resolve("app.js.gz")
      )
      .compile
      .lastOrError
      .unsafeRunSync()

    digest shouldBe "17bd3fe1380858a5a023d65218738e76f089b4fd9c200a88c1e7d667cc412996"

  }

}
