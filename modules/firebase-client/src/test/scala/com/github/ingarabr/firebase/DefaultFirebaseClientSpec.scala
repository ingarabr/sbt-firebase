package com.github.ingarabr.firebase

import cats.syntax.all._
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import fs2.Stream
import fs2.compression.Compression
import fs2.io.file.Files
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.nio.file.Paths

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
    val js = Paths.get(getClass.getResource("/simple-page/app.js").getPath)

    val (digest, unzipped, original) = Files[IO]
      .tempDirectory()
      .use { tempDir =>
        val targetPath = tempDir.resolve("app.js.gz")

        val digest = DefaultFirebaseClient
          .zipAndDigest[IO](js, targetPath)
          .compile
          .lastOrError

        val original = Files[IO]
          .readAll(js, 1024)
          .through(fs2.text.utf8Decode[IO])
          .compile
          .foldMonoid
        val unzipped =
          Files[IO]
            .readAll(targetPath, 1024)
            .through(Compression[IO].gunzip())
            .flatMap(_.content)
            .through(fs2.text.utf8Decode[IO])
            .compile
            .foldMonoid

        (digest, unzipped, original).tupled

      }
      .unsafeRunSync()

    withClue("file content") {
      unzipped shouldBe original
    }
    withClue("digest") {
      digest shouldBe "216b8dce1d09488078f9ce9144d17b59cc82983c3e0673c203b666b21dfc0b7d"
    }

  }

}
