package com.github.ingarabr.firebase.dto

import cats.Show

/** For your default Hosting site, the SITE_NAME is your Firebase project
  * ID (which is used to create your Firebase subdomains SITE_NAME.web.app
  * and SITE_NAME.firebaseapp.com).
  * If you’ve created multiple sites in your Firebase project, make sure
  * that you’re using the SITE_NAME of the site to which you’d like to
  * deploy.
  */
case class SiteName(
    value: String
) extends AnyVal

object SiteName {
  implicit val show: Show[SiteName] = _.value
}
