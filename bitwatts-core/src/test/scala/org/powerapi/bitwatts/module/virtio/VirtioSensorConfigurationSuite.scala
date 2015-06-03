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

import org.powerapi.bitwatts.UnitTest

class VirtioSensorConfigurationSuite extends UnitTest {

  "The VirtioSensorConfiguration" should "read correctly the values from a resource file" in {
    val configuration1 = new VirtioSensorConfiguration(None)
    val configuration2 = new VirtioSensorConfiguration(Some("prefix"))
    configuration1.port should equal(4)
    configuration2.port should equal(1)
  }
}
