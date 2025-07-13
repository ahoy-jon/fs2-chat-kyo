package fs2chat.conversion

import cats.effect
import cats.effect.kernel
import cats.effect.kernel.{CancelScope, Sync => SyncC}
import kyo.*
import kyo.kernel.ArrowEffect

import scala.concurrent.duration.FiniteDuration

sealed trait CE[C[F[_]]] extends ArrowEffect[Const[Unit], Const[C[[A] =>> A < Any]]]

object CE:
  opaque type F[X] = X < Any

  class ToKyo[C[X[_]]]:
    def apply[A](body: C[F] ?=> F[A])(using Tag[CE[C]]): A < CE[C] =
      ArrowEffect.suspendWith[Any](Tag[CE[C]], ())(env => body(using env))

  def apply[C[X[_]]]: ToKyo[C] = new ToKyo[C]

  inline def lift[A](inline body: SyncK ?=> A < Sync): A < Sync = body(using new SyncK)

  class SyncK extends effect.Sync[[A] =>> A < Sync] {
    override inline def suspend[A](hint: SyncC.Type)(thunk: => A): A < Sync =
      Sync.defer(thunk)

    override def handleErrorWith[A](fa: A < Sync)(f: Throwable => A < Sync): A < Sync =
      fa.unpanic.recover(f)

    override def pure[A](x: A): A < Any = x

    override def flatMap[A, B](fa: A < Sync)(f: A => B < Sync): B < Sync = fa.map(f)

    override def canceled: Unit < Sync = Kyo.unit

    override def rootCancelScope: CancelScope = ???

    override def forceR[A, B](fa: A < Sync)(fb: B < Sync): B < Sync = ???

    override def uncancelable[A](body: kernel.Poll[[X] =>> X < Sync] => A < Sync): A < Sync = ???

    override def onCancel[A](fa: A < Sync, fin: Unit < Sync): A < Sync = Sync.ensure(fin)(fa)

    override def raiseError[A](e: Throwable): A < Sync = Abort.panic(e)

    override def monotonic: FiniteDuration < Sync = ???

    override def realTime: FiniteDuration < Sync = ???

    override def tailRecM[A, B](a: A)(f: A => Either[A, B] < Sync): B < Sync =
      Loop(a): a =>
        f(a).map:
          case Left(a) => Loop.continue(a)
          case Right(b) => Loop.done(b)
  }
