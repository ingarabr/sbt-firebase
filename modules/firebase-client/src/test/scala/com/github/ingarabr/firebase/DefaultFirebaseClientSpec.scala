package com.github.ingarabr.firebase

import cats.effect.IO
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import fs2.Stream

class DefaultFirebaseClientSpec extends AnyFlatSpec with Matchers {

  it should "digest" in {
    //Equal to `echo 1234567890 | openssl dgst -sha256`
    val value = Stream.iterable("1234567890\n".getBytes())
    val digest = DefaultFirebaseClient.digestHexStr[IO](value).unsafeRunSync()
    digest shouldBe "4795a1c2517089e4df569afd77c04e949139cf299c87f012b894fccf91df4594"
  }

}
