package fs2chat
package client

import cats.ApplicativeError
import cats.effect.{Concurrent, Temporal}
import com.comcast.ip4s.{IpAddress, SocketAddress}
import fs2.{RaiseThrowable, Stream}
import fs2.io.net.Network
import kyo.Maybe

import java.net.ConnectException
import scala.concurrent.duration.*

object Client:
  def start[F[_]: Temporal: Network: ConsoleF](
      address: SocketAddress[IpAddress],
      desiredUsername: Username
  ): Stream[F, Unit] =
    connect(address, desiredUsername).handleErrorWith {
      case _: ConnectException =>
        val retryDelay = 5.seconds
        Stream.exec(
          Console.errorln(s"Failed to connect. Retrying in $retryDelay.").asF
        ) ++
          start(address, desiredUsername)
            .delayBy(retryDelay)
      case _: UserQuit         => Stream.empty
      case t                   => Stream.raiseError(t)
    }

  private def connect[F[_]: Concurrent: Network: ConsoleF](
      address: SocketAddress[IpAddress],
      desiredUsername: Username
  ): Stream[F, Unit] =
    Stream.exec(Console.info(s"Connecting to server $address").asF) ++
      Stream
        .resource(Network[F].connect(address))
        .flatMap { socket =>
          Stream.exec(Console.info("🎉 Connected! 🎊").asF) ++
            Stream
              .eval(
                MessageSocket(
                  socket,
                  Protocol.ServerCommand.codec,
                  Protocol.ClientCommand.codec,
                  128
                )
              )
              .flatMap { messageSocket =>
                Stream.exec(
                  messageSocket.write1(Protocol.ClientCommand.RequestUsername(desiredUsername))
                ) ++
                  processIncoming(messageSocket).concurrently(
                    processOutgoing(messageSocket)
                  )
              }
        }

  private def processIncoming[F[_]: ConsoleF](
      messageSocket: MessageSocket[F, Protocol.ServerCommand, Protocol.ClientCommand]
  )(implicit F: ApplicativeError[F, Throwable]): Stream[F, Unit] =
    messageSocket.read.evalMap {
      case Protocol.ServerCommand.Alert(txt)             =>
        Console.alert(txt).asF
      case Protocol.ServerCommand.Message(username, txt) =>
        Console.println(s"$username> $txt").asF
      case Protocol.ServerCommand.SetUsername(username)  =>
        Console.alert("Assigned username: " + username).asF
      case Protocol.ServerCommand.Disconnect             =>
        F.raiseError[Unit](new UserQuit)
    }

  private def processOutgoing[F[_]: RaiseThrowable: ConsoleF](
      messageSocket: MessageSocket[F, Protocol.ServerCommand, Protocol.ClientCommand]
  ): Stream[F, Unit] =
    Stream
      .repeatEval(Console.readLine("> ").asF)
      .flatMap {
        case Maybe.Present(txt) => Stream(txt)
        case Maybe.Absent       => Stream.raiseError[F](new UserQuit)
      }
      .map(txt => Protocol.ClientCommand.SendMessage(txt))
      .evalMap(messageSocket.write1)
