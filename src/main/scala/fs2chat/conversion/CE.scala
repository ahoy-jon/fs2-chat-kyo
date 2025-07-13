package fs2chat.conversion

import kyo.*

opaque type CE[C[F[_]]] <: Env[C[CE.F]] = Env[C[CE.F]]

object CE:
  opaque type F[X] = X < Any

  class ToKyo[C[X[_]]]:
    def apply[A](body: C[F] => F[A])(using Tag[C[F]]): A < CE[C] = Env.use[C[F]](body)

  def apply[C[X[_]]]: ToKyo[C] = new ToKyo[C]
