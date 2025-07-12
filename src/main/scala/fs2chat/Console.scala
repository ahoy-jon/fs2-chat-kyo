package fs2chat

import org.jline.reader.{EndOfFileException, LineReader, LineReaderBuilder, UserInterruptException}
import org.jline.utils.{AttributedStringBuilder, AttributedStyle}
import kyo.*
import cats.effect.{LiftIO, Sync as SyncC}

opaque type Console = Sync & Env[LineReader]

trait ConsoleF[F[_]]:
  def lift[A](v: A < Console): F[A]

object Console:

  def create[F[_]: SyncC: LiftIO]: F[ConsoleF[F]] =
    SyncC[F].delay(
      new ConsoleF[F] {
        val reader: LineReader                     = LineReaderBuilder.builder().appName("fs2chat").build()
        reader.setOpt(org.jline.reader.LineReader.Option.ERASE_LINE_ON_FINISH)

        override def lift[A](v: A < Console): F[A] =
          LiftIO[F].liftIO(Cats.run(Env.run(reader)(v)))
      }
    )

  extension [A](v: A < Console) def asF[F[_]](using consoleK: ConsoleF[F]): F[A] = consoleK.lift(v)

  def run[A, S](v: A < (Console & S)): A < (Sync & S) =
    Sync.defer {
      val reader: LineReader = LineReaderBuilder.builder().appName("fs2chat").build()
      reader.setOpt(org.jline.reader.LineReader.Option.ERASE_LINE_ON_FINISH)
      Env.run(reader)(v)
    }

  def println(msg: String): Unit < Console =
    Env.use[LineReader](reader => Sync.defer(reader.printAbove(msg)))

  def info(msg: String): Unit < Console =
    println("*** " + msg)

  def alert(msg: String): Unit < Console =
    println(
      new AttributedStringBuilder()
        .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
        .append("📢 " + msg)
        .toAnsi
    )

  def errorln(msg: String): Unit < Console =
    println(
      new AttributedStringBuilder()
        .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED))
        .append("❌ " + msg)
        .toAnsi
    )

  def readLine(prompt: String): Maybe[String] < Console =
    Env.use[LineReader] { reader =>
      Sync
        .defer(Maybe(reader.readLine(prompt)))
        .unpanic
        .recoverSome {
          case _: EndOfFileException     => Maybe.Absent
          case _: UserInterruptException => Maybe.Absent
        }
        .orPanic
    }
