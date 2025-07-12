package fs2chat
package client

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits.*
import com.comcast.ip4s.*
import com.monovore.decline.*
import kyo.KyoSchedulerIOApp

object ClientApp extends KyoSchedulerIOApp:
  private val argsParser: Command[(Username, SocketAddress[IpAddress])] =
    Command("fs2chat-cliegnt", "FS2 Chat Client") {
      (
        Opts
          .option[String]("username", "Desired username", "u")
          .map(Username.apply),
        Opts
          .option[String]("address", "Address of chat server")
          .withDefault("127.0.0.1")
          .mapValidated(p => IpAddress.fromString(p).toValidNel("Invalid IP address")),
        Opts
          .option[Int]("port", "Port of chat server")
          .withDefault(5555)
          .mapValidated(p => Port.fromInt(p).toValidNel("Invalid port number"))
      ).mapN { case (desiredUsername, ip, port) =>
        desiredUsername -> SocketAddress(ip, port)
      }
    }

  def run(args: List[String]): IO[ExitCode] =
    argsParser.parse(args) match
      case Left(help)                        => IO(System.err.println(help)).as(ExitCode.Error)
      case Right((desiredUsername, address)) =>
        Console
          .create[IO]
          .flatMap { implicit console =>
            Client
              .start[IO](address, desiredUsername)
              .compile
              .drain
          }
          .as(ExitCode.Success)
