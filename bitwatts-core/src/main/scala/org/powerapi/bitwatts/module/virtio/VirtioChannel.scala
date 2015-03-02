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

import akka.actor.ActorRef
import java.util.UUID
import org.powerapi.core.power.Power
import org.powerapi.core.{Channel, MessageBus}
import org.powerapi.core.ClockChannel.ClockTick
import org.powerapi.core.target.{TargetUsageRatio, Target}
import org.powerapi.module.SensorChannel.SensorReport

/**
 * VirtioChannel channel and messages.
 *
 * @author <a href="mailto:maxime.colmant@gmail.com">Maxime Colmant</a>
 */
object VirtioChannel extends Channel {
  type M = org.powerapi.module.SensorChannel.M

  /**
   * VirtioReport is represented as a dedicated type of message.
   *
   * @param topic: subject used for routing the message.
   * @param muid: monitor unique identifier (MUID), which is at the origin of the report flow.
   * @param target: monitor target.
   * @param targetRatio: target cpu ratio usage.
   * @param tick: tick origin.
   */
  case class VirtioReport(topic: String,
                          muid: UUID,
                          target: Target,
                          power: Power,
                          targetRatio: TargetUsageRatio,
                          tick: ClockTick) extends SensorReport

  /**
   * Topic for communicating with the Formula actors.
   */
  private val topic = "sensor:virtio"

  /**
   * Publish a VirtioReport in the event bus.
   */
  def publishVirtioReport(muid: UUID, target: Target, power: Power, targetRatio: TargetUsageRatio, tick: ClockTick): MessageBus => Unit = {
    publish(VirtioReport(topic = topic,
      muid = muid,
      target = target,
      power = power,
      targetRatio = targetRatio,
      tick = tick))
  }

  /**
   * External method used by the Formula for interacting with the bus.
   */
  def subscribeVirtioReport: MessageBus => ActorRef => Unit = {
    subscribe(topic)
  }
}
