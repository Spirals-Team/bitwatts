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

import java.util.UUID

import akka.actor.{Props, ActorSystem}
import akka.pattern.gracefulStop
import akka.testkit.{TestActorRef, ImplicitSender, TestKit}
import akka.util.Timeout
import org.powerapi.bitwatts.UnitTest
import org.powerapi.bitwatts.module.virtio.VirtioChannel.publishVirtioReport
import org.powerapi.core.ClockChannel.ClockTick
import org.powerapi.core.MessageBus
import org.powerapi.core.power._
import org.powerapi.core.target._
import org.powerapi.module.PowerChannel.{PowerReport, subscribePowerReport}
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class VirtioFormulaSuite(system: ActorSystem) extends TestKit(system) with ImplicitSender with UnitTest {
  def this() = this(ActorSystem("VirtioFormulaSuite"))

  override def afterAll() = {
    TestKit.shutdownActorSystem(system)
  }

  trait Bus {
    val eventBus = new MessageBus
  }

  val timeout = Timeout(1.seconds)

  "A VirtioFormula" should "compute the power when a VirtioReport is received" in new Bus {
    val formula = TestActorRef(Props(classOf[VirtioFormula], eventBus))(system)
    val muid = UUID.randomUUID()
    subscribePowerReport(muid)(eventBus)(testActor)

    publishVirtioReport(muid, 1, 30.W, TargetUsageRatio(0.5), ClockTick("", 25.milliseconds))(eventBus)
    expectMsgClass(classOf[PowerReport]) match {
      case report: PowerReport => report.muid should equal(muid); report.device should equal("cpu"); report.target should equal(Process(1)); report.power should equal(15.W)
    }

    Await.result(gracefulStop(formula, timeout.duration), timeout.duration)
  }
}