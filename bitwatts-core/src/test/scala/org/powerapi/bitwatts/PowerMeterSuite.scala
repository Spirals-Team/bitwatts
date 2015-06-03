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
package org.powerapi.bitwatts

import java.util.UUID

import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestActorRef, ImplicitSender, TestKit}
import akka.util.Timeout
import org.powerapi.bitwatts.module.virtio.VirtioModule
import org.powerapi.PowerMeterActor
import org.powerapi.core.{GlobalCpuTime, TimeInStates, OSHelper, MessageBus, Thread}
import org.powerapi.core.target.{TargetUsageRatio, Process, Application}
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
    val osHelper = new OSHelper {
      override def getThreads(process: Process): Set[Thread] = Set()
      override def getTimeInStates: TimeInStates = TimeInStates(Map())
      override def getGlobalCpuPercent(muid: UUID): TargetUsageRatio = TargetUsageRatio(0.0)
      override def getCPUFrequencies: Set[Long] = Set()
      override def getProcessCpuPercent(muid: UUID, process: Process): TargetUsageRatio = TargetUsageRatio(0.0)
      override def getProcessCpuTime(process: Process): Option[Long] = None
      override def getGlobalCpuTime: GlobalCpuTime = GlobalCpuTime(0l, 0l)
      override def getProcesses(application: Application): Set[Process] = Set()
    }
    val actor = TestActorRef(Props(classOf[PowerMeterActor], eventBus, Seq(VirtioModule(None, osHelper)), Timeout(1.seconds)))(system)
    actor.children.size should equal(4)
  }
}
