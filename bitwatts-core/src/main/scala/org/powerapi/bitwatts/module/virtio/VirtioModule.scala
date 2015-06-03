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

import org.powerapi.PowerModule
import org.powerapi.core.OSHelper

class VirtioModule(osHelper: OSHelper, port: Int) extends PowerModule {
  lazy val underlyingSensorsClasses  = Seq((classOf[VirtioSensor], Seq(osHelper, port)))
  lazy val underlyingFormulaeClasses = Seq((classOf[VirtioFormula], Seq()))
}

object VirtioModule {
  def apply(prefixConf: Option[String] = None, osHelper: OSHelper): VirtioModule = {
    val virtioSensorConf = new VirtioSensorConfiguration(prefixConf)

    new VirtioModule(osHelper, virtioSensorConf.port)
  }
}
