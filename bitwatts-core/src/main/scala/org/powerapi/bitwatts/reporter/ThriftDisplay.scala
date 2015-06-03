/*
 * This software is licensed under the GNU Affero General Public License, quoted below.
 *
 * This file is a part of BitWatts.
 *
 * Copyright (C) 2011-2015 Inria, University of Lille 1,
 * University of Neuchâtel.
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

import java.util.UUID
import org.apache.thrift.TSerializer
import org.apache.thrift.protocol.TBinaryProtocol
import org.powerapi.PowerDisplay
import org.powerapi.bitwatts.reporter.thrift.Message
import org.powerapi.core.power.Power
import org.powerapi.core.target.Target
import org.zeromq.ZMQ
import scala.collection.JavaConversions._

/**
 * This display is used to report data into a ZeroMQ broker.
 *
 * @author <a href="mailto:maxime.colmant@gmail.com">Maxime Colmant</a>
 * @author <a href="mailto:mascha.kurpicz@unine.ch">Mascha Kurpicz</a>
 */
class ThriftDisplay(ip: String, port: Int, sender: String, topic: String) extends PowerDisplay {

  val context = ZMQ.context(1)
  val publisher = context.socket(ZMQ.PUB)
  publisher.connect(s"tcp://$ip:$port")
  val serializer = new TSerializer(new TBinaryProtocol.Factory())

  var interval = 0

  def display(muid: UUID, timestamp: Long, targets: Set[Target], devices: Set[String], power: Power): Unit = {
    val data: java.util.Map[String, String] = Map("power" -> s"${power.toWatts}", "type" -> "bitwatts", "interval_index" -> s"$interval")
    val message = serializer.serialize(new Message(data))
    publisher.sendMore(topic)
    publisher.sendMore(sender)
    publisher.send(message, 0)
    interval += 1
  }
}
