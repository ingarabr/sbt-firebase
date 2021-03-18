package com.github.ingarabr.firebase

import cats.implicits._
import cats.effect.{Blocker, Clock, Concurrent, ContextShift, Resource, Sync, Timer}
import com.github.ingarabr.firebase.DefaultFirebaseClient.UploadSummary
import com.github.ingarabr.firebase.GoogleAccessToken.AuthType
import fs2.io.file.{readAll, tempDirectoryResource, writeAll}
import fs2.Stream
import org.http4s.client.Client
import com.github.ingarabr.firebase.dto._

import java.nio.file.{Path, Paths}

trait FirebaseClient[F[_]] {
  def upload(siteName: SiteName, path: Path): F[UploadSummary]
}

object FirebaseClient {
  def resource[F[_]: Concurrent: ContextShift: Timer](
      blocker: Blocker,
      client: Client[F],
      authType: AuthType
  ): Resource[F, FirebaseClient[F]] = {
    implicit val clock: Clock[F] = Timer[F].clock
    GoogleAccessToken
      .cached(authType)
      .map(auth => new DefaultFirebaseClient[F](blocker, new FirebaseWebClient[F](client, auth)))

  }
}

/** The steps are documented in
  * https://firebase.google.com/docs/hosting/api-deploy
  */
class DefaultFirebaseClient[F[_]: Concurrent: ContextShift](
    blocker: Blocker,
    webClient: FirebaseWebClient[F]
) extends FirebaseClient[F] {
  override def upload(siteName: SiteName, path: Path): F[UploadSummary] = {
    val tempDir = tempDirectoryResource(
      blocker,
      Paths.get(sys.props("java.io.tmpdir")),
      "fb-upload"
    )

    def gzipToTempFolderAndCalculateDigests(dir: Path): F[List[(Path, Path, String)]] =
      fs2.io.file
        .walk(blocker, path)
        .drop(1)
        .map(source => {
          val relative = path.relativize(source)
          val target = dir.resolve(relative.resolveSibling(relative.getFileName.toString ++ ".gz"))
          (source, relative, target)
        })
        .flatMap { case (source, relative, target) =>
          DefaultFirebaseClient
            .zipAndDigest(blocker, source, target)
            .map(hashValue => (relative, target, hashValue))
        }
        .compile
        .toList

    tempDir.use(dir =>
      for {
        zipWithDigest <- gzipToTempFolderAndCalculateDigests(dir)
        populateFilesRequest = PopulateFilesRequest(zipWithDigest)

        siteVersion <- webClient.versionsCreate(siteName)
        toUpload <- webClient.populateFiles(siteName, siteVersion, populateFilesRequest)

        _ <- zipWithDigest
          .filter { case (_, _, digest) => toUpload.contains(digest) }
          .traverse { case (source, target, hashValue) =>
            webClient
              .upload(
                siteName,
                siteVersion,
                hashValue,
                readAll(target, blocker, 1024)
              )
              .adaptError { case t => new Exception(s"Failed to upload file $source", t) }
          }
        _ <- webClient.uploadingDone(siteName, siteVersion)
        _ <- webClient.release(siteName, siteVersion)

      } yield UploadSummary(populateFilesRequest.files.size, toUpload.size)
    )
  }

}

object DefaultFirebaseClient {
  def digestHexStr[F[_]: Sync](s: fs2.Stream[F, Byte]): F[String] =
    s.through(fs2.hash.sha256).map("%02x".format(_)).compile.foldMonoid

  def zipAndDigest[F[_]: ContextShift: Concurrent](
      blocker: Blocker,
      source: Path,
      target: Path
  ): fs2.Stream[F, String] = {
    val gzipped = readAll(source, blocker, 1024).through(fs2.compression.gzip())
    gzipped
      .concurrently(gzipped.through(writeAll[F](target, blocker)))
      .through(s => Stream.eval(digestHexStr(s)))

  }

  case class UploadSummary(
      filesInUploadRequest: Int,
      filesRequiredToUpload: Int
  )
}
