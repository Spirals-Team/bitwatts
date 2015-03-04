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

import org.powerapi.bitwatts.module.virtio.VirtioChannel.{subscribeVirtioReport, VirtioReport}
import org.powerapi.core.MessageBus
import org.powerapi.module.FormulaComponent
import org.powerapi.module.PowerChannel.publishPowerReport

/**
 * Uses VirtioReport to compute the target powers.
 *
 * @author <a href="mailto:maxime.colmant@gmail.com">Maxime Colmant</a>
 * @author <a href="mailto:mascha.kurpicz@unine.ch">Mascha Kurpicz</a>
 */
class VirtioFormula(eventBus: MessageBus) extends FormulaComponent[VirtioReport](eventBus) {
  def subscribeSensorReport(): Unit = {
    subscribeVirtioReport(eventBus)(self)
  }

  def compute(sensorReport: VirtioReport): Unit = {
    log.debug("utilization: {}", s"${sensorReport.targetRatio}")

    lazy val power = sensorReport.power * sensorReport.targetRatio.ratio
    publishPowerReport(sensorReport.muid, sensorReport.target, power, "cpu", sensorReport.tick)(eventBus)
  }
}
