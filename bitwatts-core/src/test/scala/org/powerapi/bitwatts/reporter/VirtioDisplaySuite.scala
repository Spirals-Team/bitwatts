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

import java.io.ByteArrayOutputStream
import java.net.Socket
import org.powerapi.bitwatts.UnitTest
import org.powerapi.core.target._
import org.powerapi.core.power._
import org.scalamock.scalatest.MockFactory

class VirtioDisplaySuite extends UnitTest with MockFactory {

  "The VirtioDisplay" should "write data inside a socket" in {
    val socketMock = mock[Socket]
    val output = new ByteArrayOutputStream()

    val display = new VirtioDisplay(1) {
      override def initializeConnection(prefixPath: String, port: Int): Option[Socket] = {
        Some(socketMock)
      }
    }

    display.display(System.currentTimeMillis, Set(1), Set("cpu"), 10.W)
    new String(output.toByteArray) should equal("")

    socketMock.getOutputStream _ expects() returning output

    display.display(System.currentTimeMillis, Set(1), Set("cpu"), 10.W)
    new String(output.toByteArray) should equal(s"${10.W.toWatts}\n")
  }
}
