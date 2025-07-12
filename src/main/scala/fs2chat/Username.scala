package fs2chat

import cats.Eq

case class Username(value: String):
  override def toString: String = value

object Username:
  given eqInstance: Eq[Username] = Eq.fromUniversalEquals[Username]
