package com.github.ingarabr.firebase.dto

import org.scalatest.flatspec.AnyFlatSpec
import io.circe.syntax._
import org.scalatest.matchers.should.Matchers

class SiteVersionRequestSpec extends AnyFlatSpec with Matchers {
  it should "encode and decode json" in {
    val originRequest = SiteVersionRequest(
      status = VersionStatus.Created,
      labels = Map("lk1" -> "lv1"),
      config = ServingConfig(
        headers = List(Header(Map("hk1" -> "hv1"), MatchPattern.Glob("**"))),
        redirects = List(Redirect(200, "tada", MatchPattern.Regex(".*"))),
        rewrites = List(Rewrite(MatchPattern.Regex("/tada/.*"), RewriteBehavior.Path("foo"))),
        i18n = Some(I18nConfig("locale"))
      )
    )

    val decoded = originRequest.asJson.as[SiteVersionRequest]

    decoded shouldBe Right(originRequest)
  }
}
