package com.github.ingarabr.firebase

import cats.syntax.all._
import cats.effect.IO
import cats.effect.kernel.Resource
import cats.effect.unsafe.implicits.global
import fs2.Stream
import fs2.io.file.Files
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

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

  it should "calculate digest from tmp file" in {
    val setup = for {
      temporaryTargetLocation <- Files[IO].tempDirectory
      sourceFile <- Files[IO].tempDirectory.flatMap { p =>
        val filePath = p.resolve("app.js")
        Resource.eval(
          Stream
            .iterable(
              ("sdfijo34thpgsndpew[0iu4htofewaiep;fhl;0psa'ofpko4pj39hwtgafeo;jf" * 100).getBytes()
            )
            .through(Files[IO].writeAll(filePath))
            .compile
            .drain
            .as(filePath)
        )

      }
    } yield (temporaryTargetLocation, sourceFile)
    val (digest, expectedDigest) = setup
      .use { case (tempTarget, sourceFile) =>
        val zippedFile = tempTarget.resolve("app.js.gz")

        val digest = DefaultFirebaseClient
          .zipAndDigest[IO](sourceFile, zippedFile)
          .compile
          .lastOrError

        val expectedDigest =
          IO {
            val digestCmd = List("openssl", "dgst", "-sha256", zippedFile.toString)
            sys.process.Process(digestCmd).!!
          }.map(_.trim().replaceAll("SHA256\\(.*\\)= ", ""))

        (digest, expectedDigest).tupled
      }
      .unsafeRunSync()

    withClue("expected digest") {
      digest shouldBe expectedDigest
    }
    withClue("digest") {
      // the digest should be stable
      digest shouldBe "5afdb27ba7a9dcb26a1438f02919b2c92c11ee395ef441bb486ddd13daa499ec"
    }

  }

}
