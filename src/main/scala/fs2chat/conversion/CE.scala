package fs2chat.conversion

import cats.effect
import cats.effect.kernel
import cats.effect.kernel.{CancelScope, Sync => SyncC}
import kyo.*
import kyo.kernel.ArrowEffect

import scala.concurrent.duration.FiniteDuration

sealed trait CE[C[F[_]]] extends ArrowEffect[Const[Unit], Const[C[CE.F]]]

object CE:
  opaque type F[X] = X < Any

  class ToKyo[C[X[_]]]:
    def apply[A](body: C[F] ?=> F[A])(using Tag[CE[C]]): A < CE[C] =
      ArrowEffect.suspendWith[Any](Tag[CE[C]], ())(env => body(using env))

  def apply[C[X[_]]]: ToKyo[C] = new ToKyo[C]

  private object KyoSync extends effect.Sync[KyoSync.F] {
    type F[A] = A < Sync

    override def suspend[A](hint: SyncC.Type)(thunk: => A): F[A] = Sync.defer(thunk)

    override def monotonic: F[FiniteDuration] = ???

    override def realTime: F[FiniteDuration] = ???

    override def rootCancelScope: CancelScope = ???

    override def forceR[A, B](fa: F[A])(fb: F[B]): F[B] = ???

    override def uncancelable[A](body: kernel.Poll[F] => F[A]): F[A] = ???

    override def canceled: F[Unit] = ???

    override def onCancel[A](fa: F[A], fin: F[Unit]): F[A] = ???

    override def raiseError[A](e: Throwable): F[A] = ???

    override def handleErrorWith[A](fa: F[A])(f: Throwable => F[A]): F[A] =
      fa.unpanic.recover(f)

    override def pure[A](x: A): F[A] = x

    override def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] = fa.map(f)

    override def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]): F[B] = ???
  }

  def run[A, S](v: A < (S & CE[effect.Sync])): A < (S & Sync) =

    ArrowEffect.handle(Tag[CE[effect.Sync]], v)(
      [C] => (_, cont) => cont(KyoSync.asInstanceOf)
    )
