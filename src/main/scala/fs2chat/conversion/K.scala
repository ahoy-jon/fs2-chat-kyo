package fs2chat.conversion

import kyo.*

trait K[F[_], +S]:
  def asF[A](f: A < S): F[A]

object K:
  type Effect[S] = [F[_]] =>> K[F, S]

extension [A, S](v: A < S) def asF[F[_]](using k: K[F, S]): F[A] = k.asF(v)
