package fs2chat.conversion

import fs2.io.net.Network
import kyo.{<, Console, Kyo, Sync}

object TestConv:
  val allInterfaces = CE[Network](network => network.interfaces.getAll)

  val prg: Unit < (CE[Network] & Sync) = allInterfaces.map: map =>
    Kyo.foreachDiscard(map.toSeq):
      case ((k, v)) => Console.printLine(s"$k: $v")
