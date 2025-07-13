package fs2chat.conversion

import cats.effect
import fs2.io.net.Network
import kyo.{<, Console, Kyo, KyoApp, Sync}

object SyncExample {
  import cats.effect.{IO, IOApp, Sync}
  import cats.syntax.all._

  def riskyOperation[F[_]: Sync](input: String): F[Int] =
    Sync[F].delay(input.toInt).handleErrorWith { e =>
      Sync[F].delay(println(s"Error: ${e.getMessage}")) *> Sync[F].pure(0)
    }

  def program[F[_]: Sync]: F[Unit] =
    for {
      _    <- Sync[F].delay(println("Enter a number:"))
      line <- Sync[F].delay(scala.io.StdIn.readLine())
      res  <- riskyOperation(line)
      _    <- Sync[F].delay(println(s"Result: $res"))
    } yield ()

}

object TestConv extends KyoApp:
  def riskyOperation(str: String): Int < CE[effect.Sync] =
    CE[effect.Sync](SyncExample.riskyOperation(str))

  run:
    CE.run(riskyOperation("toto"))
