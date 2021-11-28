package com.github.ingarabr.firebase

import cats.effect.{Async, Sync}
import org.http4s.client.Client
import org.http4s.client.middleware.{RequestLogger, ResponseLogger}

object Http4sConsoleLogger {

  def apply[F[_]: Async](c: Client[F]): Client[F] = {
    val consoleWriter: String => F[Unit] = (msg: String) => Sync[F].delay(println(msg))
    RequestLogger.apply[F](
      logHeaders = false,
      logBody = true,
      logAction = Some(str => consoleWriter(s">> $str"))
    )(
      ResponseLogger.apply[F](
        logHeaders = false,
        logBody = true,
        logAction = Some(str => consoleWriter(s"<< $str"))
      )(c)
    )
  }

}
