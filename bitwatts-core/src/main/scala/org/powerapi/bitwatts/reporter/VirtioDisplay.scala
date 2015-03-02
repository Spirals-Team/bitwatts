/*
 * This software is licensed under the GNU Affero General Public License, quoted below.
 *
 * This file is a part of BitWatts.
 *
 * Copyright (C) 2011-2014 Inria, University of Lille 1.
 *
 * BitWatts is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * BitWatts is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with BitWatts.
 *
 * If not, please consult http://www.gnu.org/licenses/agpl-3.0.html.
 */
package org.powerapi.bitwatts.reporter

import java.io.File
import java.net.Socket

import org.apache.logging.log4j.LogManager
import org.newsclub.net.unix.{AFUNIXSocketAddress, AFUNIXSocket}
import org.powerapi.PowerDisplay
import org.powerapi.core.power.Power
import org.powerapi.core.target.{Process, Target}

/**
 * This display is used to report data inside a JUnixSocket.
 * It works only if with Process targets because of one VM is associated to one port.
 *
 * @author <a href="mailto:maxime.colmant@gmail.com">Maxime Colmant</a>
 * @author <a href="mailto:mascha.kurpicz@unine.ch">Mascha Kurpicz</a>
 */
class VirtioDisplay(mappings: Map[Int, Int]) extends PowerDisplay with VirtioDisplayConfiguration {
  private[this] val log = LogManager.getLogger

  val sockets = scala.collection.mutable.Map[Process, Socket]()

  def initializeConnection(prefixPath: String, port: Int): Option[Socket] = {
    try {
      val sock = AFUNIXSocket.newInstance()
      val address = new AFUNIXSocketAddress(new File(s"${virtioPathPrefix}port$port"))
      sock.connect(address)
      Some(sock)
    }
    catch {
      case _: Throwable => log.warn("Connexion impossible for the port: {}", s"$port"); None
    }
  }

  def display(timestamp: Long, target: Target, device: String, power: Power): Unit = {
    target match {
      case process: Process => {
        if(!sockets.contains(process) && mappings.contains(process.pid)) {
          val port = mappings(process.pid)
          log.debug("pid: {}, port: {}", s"${process.pid}", s"$port")

          initializeConnection(virtioPathPrefix, port) match {
            case Some(socket) => sockets += process -> socket
            case _ => {}
          }
        }

        if(sockets.contains(process)) {
          try {
            log.debug(s"$power\n")
            val output = sockets(process.pid).getOutputStream
            output.write(s"${power.toWatts}\n".getBytes)
          }
          catch {
            case _: Throwable => log.warn("Connexion lost for the pid: {}", s"${process.pid}"); sockets -= process
          }
        }
      }
      case _ => {}
    }
  }
}
