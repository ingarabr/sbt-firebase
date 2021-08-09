package com.github.ingarabr.firebase

import cats.implicits._
import cats.effect.{Async, Clock, Resource}
import com.github.ingarabr.firebase.DefaultFirebaseClient.UploadSummary
import com.github.ingarabr.firebase.GoogleAccessToken.AuthType
import com.github.ingarabr.firebase.dto._
import fs2.io.file.Files
import fs2.Stream
import fs2.compression.Compression
import org.http4s.client.Client
import java.nio.file.{Path, Paths}
import java.time.Instant

trait FirebaseClient[F[_]] {
  def upload(siteName: SiteName, path: Path): F[UploadSummary]
}

object FirebaseClient {
  def resource[F[_]: Async: Clock](
      client: Client[F],
      authType: AuthType
  ): Resource[F, FirebaseClient[F]] = {
    GoogleAccessToken
      .cached(authType)
      .map(auth => new DefaultFirebaseClient[F](new FirebaseWebClient[F](client, auth)))

  }
}

/** The steps are documented in
  * https://firebase.google.com/docs/hosting/api-deploy
  */
class DefaultFirebaseClient[F[_]: Async: Files](
    webClient: FirebaseWebClient[F]
) extends FirebaseClient[F] {
  override def upload(siteName: SiteName, path: Path): F[UploadSummary] = {
    val tempDirResource = Files[F].tempDirectory(
      Some(Paths.get(sys.props("java.io.tmpdir"))),
      "fb-upload"
    )

    def gzipToTempFolderAndCalculateDigests(tempDir: Path): F[List[(Path, Path, String)]] =
      Files[F]
        .walk(path)
        .drop(1)
        .map(source => {
          val relative = path.relativize(source)
          val target =
            tempDir.resolve(relative.resolveSibling(relative.getFileName.toString ++ ".gz"))
          (source, relative, target)
        })
        .flatMap { case (source, relative, target) =>
          DefaultFirebaseClient
            .zipAndDigest(source, target)
            .map(hashValue => (relative, target, hashValue))
        }
        .compile
        .toList

    tempDirResource.use(tempDir =>
      for {
        zipWithDigest <- gzipToTempFolderAndCalculateDigests(tempDir)
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
                Files[F].readAll(target, 1024)
              )
              .adaptError { case t =>
                new Exception(s"Failed to upload file $source with sha $hashValue from $target", t)
              }
          }
        _ <- webClient.uploadingDone(siteName, siteVersion)
        _ <- webClient.release(siteName, siteVersion)

      } yield UploadSummary(populateFilesRequest.files.size, toUpload.size)
    )
  }

}

object DefaultFirebaseClient {

  def digestHexStr[F[_]: Async](s: fs2.Stream[F, Byte]): F[String] =
    s.through(fs2.hash.sha256)
      .map(b => String.format("%02x", Byte.box(b)))
      .compile
      .foldMonoid

  def zipAndDigest[F[_]: Async: Compression: Files](
      source: Path,
      target: Path
  ): fs2.Stream[F, String] = {
    val gzipped = Files[F]
      .readAll(source, 1024)
      .through(
        Compression[F].gzip(
          fileName = Some(source.getFileName.toString),
          modificationTime = Some(Instant.ofEpochMilli(source.toFile.lastModified()))
        )
      )
    gzipped
      .concurrently(gzipped.through(Files[F].writeAll(target)))
      .through(s => Stream.eval(digestHexStr(s)))

  }

  case class UploadSummary(
      filesInUploadRequest: Int,
      filesRequiredToUpload: Int
  )
}
