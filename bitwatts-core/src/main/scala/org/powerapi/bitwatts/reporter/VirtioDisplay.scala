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
 *
 * @author <a href="mailto:maxime.colmant@gmail.com">Maxime Colmant</a>
 * @author <a href="mailto:mascha.kurpicz@unine.ch">Mascha Kurpicz</a>
 */
class VirtioDisplay(path: String) extends PowerDisplay {
  private[this] val log = LogManager.getLogger
  private[this] var output: Option[Socket] = None

  def initializeConnection(): Option[Socket] = {
    try {
      val sock = AFUNIXSocket.newInstance()
      val address = new AFUNIXSocketAddress(new File(path))
      sock.connect(address)
      Some(sock)
    }
    catch {
      case _: Throwable => log.warn("Connexion impossible, path: {}", path); None
    }
  }

  def initOutput(): Unit = {
    if(output == None) {
      initializeConnection() match {
        case option: Option[Socket] => {
          log.debug("socket opened, path {}", path)
          output = option
        }
        case _ => {}
      }
    }
  }

  def writePower(targets: Set[Target], power: Power): Unit = {
    output match {
      case Some(socket) => {
        try {
          log.debug(s"{} has been written for targets {} in {}", s"${power.toWatts}", s"${targets.mkString(",")}", path)
          socket.getOutputStream.write(s"${power.toWatts}\n".getBytes)
        }
        catch {
          case _: Throwable => {
            log.warn("Connexion lost, path {}", path)
            output = None
          }
        }
      }
      case _ => {}
    }
  }

  def display(timestamp: Long, targets: Set[Target], device: Set[String], power: Power): Unit = {
    initOutput()
    writePower(targets, power)
  }
}
