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
package org.powerapi.bitwatts.module.virtio

import java.io.{FileReader, BufferedReader}
import org.powerapi.bitwatts.module.virtio.VirtioChannel.publishVirtioReport
import org.powerapi.core.MonitorChannel.MonitorTick
import org.powerapi.core.power._
import org.powerapi.core.target.{TargetUsageRatio, Application, All, Process}
import org.powerapi.core.{OSHelper, MessageBus}
import org.powerapi.module.SensorChannel.{MonitorStop, MonitorStopAll}
import org.powerapi.module.{CacheKey, Cache, SensorComponent}
import scala.reflect.ClassTag

/**
 * Reads global power consumption from a buffer and produces VirtioReport.
 *
 * @author <a href="mailto:maxime.colmant@gmail.com">Maxime Colmant</a>
 * @author <a href="mailto:mascha.kurpicz@unine.ch">Mascha Kurpicz</a>
 */
class VirtioSensor(eventBus: MessageBus, osHelper: OSHelper, port: Int) extends SensorComponent(eventBus) {
  val powerReader: Option[BufferedReader] = try {
    Some(new BufferedReader(new FileReader(s"/dev/virtio-ports/port.$port")))
  }
  catch {
    case ex: Throwable => log.warning("i/o exception: {}", s"${ex.getMessage}"); None
  }

  val timestamps = new Cache[Long]
  val powers = new Cache[Power]

  val processClaz = implicitly[ClassTag[Process]].runtimeClass
  val appClaz = implicitly[ClassTag[Application]].runtimeClass

  def targetCpuUsageRatio(monitorTick: MonitorTick): TargetUsageRatio = {
    val processClaz = implicitly[ClassTag[Process]].runtimeClass
    val appClaz = implicitly[ClassTag[Application]].runtimeClass

    monitorTick.target match {
      case target if processClaz.isInstance(target) || appClaz.isInstance(target) => {
        osHelper.getTargetCpuPercent(monitorTick.muid, target)
      }
      case All => osHelper.getGlobalCpuPercent(monitorTick.muid)
    }
  }

  def power(monitorTick: MonitorTick): Power = {
    val key = CacheKey(monitorTick.muid, All)
    val now = monitorTick.tick.timestamp
    val old = timestamps(key)(now)

    val power = if(now > old) {
      powerReader match {
        case Some(reader) => {
          reader.readLine() match {
            case power: String => powers(key) = power.toDouble.W; power.toDouble.W
            case _ => 0.W
          }
        }
        case _ => 0.W
      }
    }
    else powers(key)(0.W)

    timestamps(key) = now

    log.debug("Power: {}", s"$power")
    power
  }

  def sense(monitorTick: MonitorTick): Unit = {
    publishVirtioReport(monitorTick.muid, monitorTick.target, power(monitorTick), targetCpuUsageRatio(monitorTick), monitorTick.tick)(eventBus)
  }

  def monitorStopped(msg: MonitorStop): Unit = {
    timestamps -= msg.muid
    powers -= msg.muid
  }

  def monitorAllStopped(msg: MonitorStopAll): Unit = {
    timestamps.clear()
    powers.clear()
  }
}
