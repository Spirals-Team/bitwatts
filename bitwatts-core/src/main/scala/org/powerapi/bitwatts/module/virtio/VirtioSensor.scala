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
package org.powerapi.bitwatts.module.virtio

import java.io.{FileReader, BufferedReader}
import org.powerapi.bitwatts.module.virtio.VirtioChannel.publishVirtioReport
import org.powerapi.core.MonitorChannel.MonitorTick
import org.powerapi.core.power._
import org.powerapi.core.target.{TargetUsageRatio, Application, All, Process}
import org.powerapi.core.{GlobalCpuTime, OSHelper, MessageBus}
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
    Some(new BufferedReader(new FileReader(s"/dev/virtio-ports/$port")))
  }
  catch {
    case ex: Throwable => log.warning("i/o exception: {}", s"${ex.getMessage}"); None
  }

  val cpuTimesCache = new Cache[(Long, Long)]
  val timestamps = new Cache[Long]
  val powers = new Cache[Power]

  val processClaz = implicitly[ClassTag[Process]].runtimeClass
  val appClaz = implicitly[ClassTag[Application]].runtimeClass

  def targetUsageRatio(monitorTick: MonitorTick): TargetUsageRatio = {
    val key = CacheKey(monitorTick.muid, monitorTick.target)

    lazy val (globalCpuTime, activeCpuTime) = osHelper.getGlobalCpuTime match {
      case GlobalCpuTime(globalTime, activeTime) => (globalTime, activeTime)
    }

    lazy val now = monitorTick.target match {
      case target if processClaz.isInstance(target) || appClaz.isInstance(target) => {
        lazy val targetCpuTime = osHelper.getTargetCpuTime(target) match {
          case Some(time) => time
          case _ => 0l
        }

        (targetCpuTime, globalCpuTime)
      }
      case All => (activeCpuTime, globalCpuTime)
    }

    val old = cpuTimesCache(key)(now)

    val diffTimes = (now._1 - old._1, now._2 - old._2)

    diffTimes match {
      case diff: (Long, Long) => {
        if(old == now) {
          cpuTimesCache(key) = now
          TargetUsageRatio(0.0)
        }

        else if (diff._1 > 0 && diff._2 > 0 && diff._1 <= diff._2) {
          cpuTimesCache(key) = now
          TargetUsageRatio(diff._1.toDouble / diff._2)
        }

        else TargetUsageRatio(0.0)
      }
      case _ => TargetUsageRatio(0.0)
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
            case power: String => power.toDouble.W
            case _ => 0.W
          }
        }
        case _ => 0.W
      }
    }
    else powers(key)(0.W)

    powers(key) = power
    timestamps(key) = now

    log.debug("Power: {}", s"$power")
    power
  }

  def sense(monitorTick: MonitorTick): Unit = {
    publishVirtioReport(monitorTick.muid, monitorTick.target, power(monitorTick), targetUsageRatio(monitorTick), monitorTick.tick)(eventBus)
  }

  def monitorStopped(msg: MonitorStop): Unit = {
    cpuTimesCache -= msg.muid
    timestamps -= msg.muid
    powers -= msg.muid
  }

  def monitorAllStopped(msg: MonitorStopAll): Unit = {
    cpuTimesCache.clear()
    timestamps.clear()
    powers.clear()
  }
}
