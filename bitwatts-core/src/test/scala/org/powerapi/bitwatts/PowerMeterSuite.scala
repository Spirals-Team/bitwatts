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
package org.powerapi.bitwatts

import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestActorRef, ImplicitSender, TestKit}
import akka.util.Timeout
import org.powerapi.bitwatts.module.virtio.VirtioModule
import org.powerapi.PowerMeterActor
import org.powerapi.core.MessageBus
import scala.concurrent.duration.DurationInt

class PowerMeterSuite(system: ActorSystem) extends TestKit(system) with ImplicitSender with UnitTest {

  def this() = this(ActorSystem("PowerMeterSuite"))

  override def afterAll() = {
    TestKit.shutdownActorSystem(system)
  }

  trait EventBus {
    val eventBus = new MessageBus()
  }

  "The PowerMeter actor" should "load the VirtioModule" in new EventBus {
    val actor = TestActorRef(Props(classOf[PowerMeterActor], eventBus, Seq(VirtioModule()), Timeout(1.seconds)))(system)
    actor.children.size should equal(4)
  }
}
