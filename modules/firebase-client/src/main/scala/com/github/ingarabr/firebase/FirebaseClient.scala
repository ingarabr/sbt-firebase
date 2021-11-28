package com.github.ingarabr.firebase

import cats.syntax.all._
import cats.effect.{Async, Resource}
import com.github.ingarabr.firebase.DefaultFirebaseClient.UploadSummary
import com.github.ingarabr.firebase.GoogleAccessToken.AuthType
import com.github.ingarabr.firebase.dto._
import fs2.io.file.Files
import fs2.Stream
import fs2.compression.Compression
import org.http4s.client.Client

import java.nio.file.Paths
import fs2.io.file.Path

trait FirebaseClient[F[_]] {
  def upload(
      siteName: SiteName,
      path: Path,
      siteVersionRequest: SiteVersionRequest
  ): F[UploadSummary]
}

object FirebaseClient {
  def resource[F[_]: Async](
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

  implicit class ErrorMsgOnStep[A](f: F[A]) {
    def adaptErrorMsg(msg: => String): F[A] =
      f.adaptError { case t => new Exception(msg, t) }
  }

  override def upload(
      siteName: SiteName,
      path: Path,
      siteVersionRequest: SiteVersionRequest
  ): F[UploadSummary] = {
    val tempDirResource = Files[F].tempDirectory(
      dir = Some(Paths.get(sys.props("java.io.tmpdir"))).map(Path.fromNioPath),
      prefix = "fb-upload",
      permissions = None
    )

    def gzipToTempFolderAndCalculateDigests(tempDir: Path): F[List[(Path, Path, String)]] =
      Files[F]
        .walk(path)
        .adaptError { case t => new Exception(show"Failed to walk $path", t) }
        .filter(p => p.toNioPath.toFile.isFile)
        .map(source => {
          val relative = path.relativize(source)
          val target =
            tempDir.resolve(relative.resolveSibling(relative.fileName.toString ++ ".gz"))
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
          .adaptErrorMsg(show"Failed during Gzip and calculating file digest step $tempDir")
        populateFilesRequest = PopulateFilesRequest(zipWithDigest)

        siteVersion <- webClient
          .versionsCreate(siteName, siteVersionRequest)
          .adaptErrorMsg("Failed to create firebase version")
        toUpload <- webClient
          .populateFiles(siteName, siteVersion, populateFilesRequest)
          .adaptErrorMsg("Failed to upload files")

        _ <- zipWithDigest
          .filter { case (_, _, digest) => toUpload.contains(digest) }
          .traverse { case (source, target, hashValue) =>
            webClient
              .upload(
                siteName,
                siteVersion,
                hashValue,
                Files[F].readAll(target)
              )
              .adaptErrorMsg(s"Failed to upload file $source with sha $hashValue from $target")
          }

        _ <- webClient
          .uploadingDone(siteName, siteVersion)
          .adaptErrorMsg("Failed on marking upload done")
        _ <- webClient.release(siteName, siteVersion).adaptErrorMsg("Failed on releasing")

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
      .readAll(source)
      .through(
        Compression[F].gzip(
          fileName = Some(source.fileName.toString),
          modificationTime = None
        )
      )

    val folder = Stream.eval(
      Async[F]
        .blocking { if (target.parent.forall(_.toNioPath.toFile.isDirectory)) false else true }
        .flatMap {
          case true =>
            target.parent.fold(Async[F].unit)((par: Path) => Files[F].createDirectories(par).void)
          case false => Async[F].unit
        }
    )

    folder >> gzipped
      .through(s => Files[F].writeAll(target)(s) ++ Stream.eval(digestHexStr(s)))
  }

  case class UploadSummary(
      filesInUploadRequest: Int,
      filesRequiredToUpload: Int
  )
}
