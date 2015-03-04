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

import java.io.{Reader, BufferedReader}
import java.util.UUID

import akka.actor.{ActorSystem, Props}
import akka.pattern.gracefulStop
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import akka.util.Timeout
import org.powerapi.bitwatts.UnitTest
import org.powerapi.core.ClockChannel.ClockTick
import org.powerapi.core.{GlobalCpuTime, TimeInStates, OSHelper, MessageBus, Thread}
import org.powerapi.core.MonitorChannel.{MonitorTick, publishMonitorTick}
import org.powerapi.core.target._
import org.powerapi.core.power._
import org.powerapi.module.CacheKey
import org.powerapi.bitwatts.module.virtio.VirtioChannel.{VirtioReport, subscribeVirtioReport}
import org.scalamock.scalatest.MockFactory
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class VirtioSensorMock(eventBus: MessageBus, osHelper: OSHelper, port: Int, reader: BufferedReader) extends VirtioSensor(eventBus, osHelper, port) {
  override val powerReader = Some(reader)
}

class VirtioSensorSuite(system: ActorSystem) extends TestKit(system) with ImplicitSender with UnitTest with MockFactory {
  def this() = this(ActorSystem("VirtioSensorSuite"))

  override def afterAll() = {
    TestKit.shutdownActorSystem(system)
  }

  trait Bus {
    val eventBus = new MessageBus
  }

  val timeout = Timeout(1.seconds)

  trait Times {
    val globalElapsedTime1: Long = 43171 + 1 + 24917 + 25883594 + 1160 + 19 + 1477 + 0
    val activeElapsedTime1: Long = globalElapsedTime1 - 25883594
    val globalElapsedTime2: Long = 43173 + 1 + 24917 + 25883594 + 1160 + 19 + 1477 + 0
    val activeElapsedTime2: Long = globalElapsedTime2 - 25883594
    val globalElapsedTime3: Long = 43175 + 1 + 24917 + 25883594 + 1160 + 19 + 1477 + 0
    val activeElapsedTime3: Long = globalElapsedTime3 - 25883594
    val p1ElapsedTime1: Long = 33 + 2
    val p1ElapsedTime2: Long = 33 + 4
    val p2ElapsedTime: Long = 10 + 5
    val p3ElapsedTime: Long = 3 + 5
    val appElapsedTime: Long = p2ElapsedTime + p3ElapsedTime

    val oldP1ElapsedTime1 = p1ElapsedTime1 / 2
    val oldP1ElapsedTime2 = p1ElapsedTime1 / 2
    val oldP2ElapsedTime = p2ElapsedTime / 2
    val oldP3ElapsedTime = p3ElapsedTime / 2
    val oldAppElapsedTime = oldP2ElapsedTime + oldP3ElapsedTime
    val (oldGlobalElapsedTime1, oldActiveElapsedTime1) = (globalElapsedTime1 / 2, activeElapsedTime1 / 2)
    val (oldGlobalElapsedTime2, oldActiveElapsedTime2) = (globalElapsedTime2 / 2, activeElapsedTime2 / 2)
    val (oldGlobalElapsedTime3, oldActiveElapsedTime3) = (globalElapsedTime3 / 2, activeElapsedTime3 / 2)

    val processRatio1 = TargetUsageRatio((p1ElapsedTime1 - oldP1ElapsedTime1).toDouble / (activeElapsedTime1 - oldActiveElapsedTime1))

    val processRatio2 = TargetUsageRatio((p1ElapsedTime2 - oldP1ElapsedTime2).toDouble / (activeElapsedTime2 - oldActiveElapsedTime2))
    val appRatio = TargetUsageRatio((appElapsedTime - oldAppElapsedTime).toDouble / (activeElapsedTime2 - oldActiveElapsedTime2))

    val allRatio = TargetUsageRatio((activeElapsedTime3 - oldActiveElapsedTime3).toDouble / (activeElapsedTime3 - oldActiveElapsedTime3))
  }

  class MockableBufferedReader extends BufferedReader(mock[Reader])

  "A VirtioSensor" should "compute the target usage ratio" in new Bus with Times {
    val muid1 = UUID.randomUUID()
    val muid2 = UUID.randomUUID()
    val muid3 = UUID.randomUUID()

    val sensor = TestActorRef(Props(classOf[VirtioSensor], eventBus, new OSHelper {
      import org.powerapi.core.GlobalCpuTime

      private var targetTimes = Map[Target, List[Long]](
        Process(1) -> List(oldP1ElapsedTime1, oldP1ElapsedTime2, p1ElapsedTime1, p1ElapsedTime2),
        Process(2) -> List(oldP2ElapsedTime, p2ElapsedTime),
        Process(3) -> List(oldP3ElapsedTime, p3ElapsedTime)
      )

      private var globalTimes = List[(Long, Long)](
        (oldGlobalElapsedTime1, oldActiveElapsedTime1), (oldGlobalElapsedTime2, oldActiveElapsedTime2), (oldGlobalElapsedTime2, oldActiveElapsedTime2), (oldGlobalElapsedTime3, oldActiveElapsedTime3),
        (globalElapsedTime1, activeElapsedTime1), (globalElapsedTime2, activeElapsedTime2), (globalElapsedTime2, activeElapsedTime2), (globalElapsedTime3, activeElapsedTime3)
      )

      def getCPUFrequencies(topology: Map[Int, Iterable[Int]]): Iterable[Long] = Iterable()

      def getProcesses(application: Application): Iterable[Process] = Iterable(Process(2), Process(3))

      def getThreads(process: Process): Iterable[Thread] = Iterable()

      def getProcessCpuTime(process: Process): Option[Long] = {
        targetTimes.getOrElse(process, List()) match {
          case times if times.length > 0 => {
            targetTimes += process -> times.tail
            Some(times.head)
          }
          case _ => None
        }
      }

      def getGlobalCpuTime: GlobalCpuTime = {
        globalTimes.headOption match {
          case Some((globalTime, activeTime)) => {
            globalTimes = globalTimes.tail
            GlobalCpuTime(globalTime, activeTime)
          }
          case _ => GlobalCpuTime(0, 0)
        }
      }

      def getTimeInStates: TimeInStates = TimeInStates(Map())
    }, 0), "virtio-sensor")(system)

    sensor.underlyingActor.asInstanceOf[VirtioSensor].targetUsageRatio(MonitorTick("", muid1, 1, ClockTick("", 25.milliseconds))) should equal(TargetUsageRatio(0.0))
    sensor.underlyingActor.asInstanceOf[VirtioSensor].targetUsageRatio(MonitorTick("", muid2, 1, ClockTick("", 25.milliseconds))) should equal(TargetUsageRatio(0.0))
    sensor.underlyingActor.asInstanceOf[VirtioSensor].targetUsageRatio(MonitorTick("", muid2, "app", ClockTick("", 25.milliseconds))) should equal(TargetUsageRatio(0.0))
    sensor.underlyingActor.asInstanceOf[VirtioSensor].targetUsageRatio(MonitorTick("", muid3, All, ClockTick("", 25.milliseconds))) should equal(TargetUsageRatio(0.0))

    sensor.underlyingActor.asInstanceOf[VirtioSensor].targetUsageRatio(MonitorTick("", muid1, 1, ClockTick("", 25.milliseconds))) should equal(processRatio1)
    sensor.underlyingActor.asInstanceOf[VirtioSensor].cpuTimesCache(CacheKey(muid1, 1))(0, 0) match {
      case times => times should equal(p1ElapsedTime1, activeElapsedTime1)
    }

    sensor.underlyingActor.asInstanceOf[VirtioSensor].targetUsageRatio(MonitorTick("", muid2, 1, ClockTick("", 25.milliseconds))) should equal(processRatio2)
    sensor.underlyingActor.asInstanceOf[VirtioSensor].cpuTimesCache(CacheKey(muid2, 1))(0, 0) match {
      case times => times should equal(p1ElapsedTime2, activeElapsedTime2)
    }
    sensor.underlyingActor.asInstanceOf[VirtioSensor].targetUsageRatio(MonitorTick("", muid2, "app", ClockTick("", 25.milliseconds))) should equal(appRatio)
    sensor.underlyingActor.asInstanceOf[VirtioSensor].cpuTimesCache(CacheKey(muid2, "app"))(0, 0) match {
      case times => times should equal(appElapsedTime, activeElapsedTime2)
    }
    sensor.underlyingActor.asInstanceOf[VirtioSensor].targetUsageRatio(MonitorTick("", muid3, All, ClockTick("", 25.milliseconds))) should equal(allRatio)
    sensor.underlyingActor.asInstanceOf[VirtioSensor].cpuTimesCache(CacheKey(muid3, All))(0, 0) match {
      case times => times should equal(activeElapsedTime3, activeElapsedTime3)
    }

    Await.result(gracefulStop(sensor, timeout.duration), timeout.duration)
  }

  it should "have a method for getting the power from a BufferedReader" in new Bus {
    val bufferMock = mock[MockableBufferedReader]
    val muid = UUID.randomUUID()
    val muid2 = UUID.randomUUID()
    lazy val tick = ClockTick("", 25.milliseconds)
    lazy val tick2 = ClockTick("", 25.milliseconds)
    lazy val tick3 = ClockTick("", 25.milliseconds)
    lazy val tick4 = ClockTick("", 25.milliseconds)
    lazy val tick5 = ClockTick("", 25.milliseconds)

    val sensor = TestActorRef(Props(classOf[VirtioSensorMock], eventBus, new OSHelper {
      def getCPUFrequencies(topology: Map[Int, Iterable[Int]]): Iterable[Long] = ???
      def getThreads(process: Process): Iterable[Thread] = ???
      def getTimeInStates: TimeInStates = ???
      def getProcessCpuTime(process: Process): Option[Long] = ???
      def getGlobalCpuTime: GlobalCpuTime = ???
      def getProcesses(application: Application): Iterable[Process] = ???
    }, 0, bufferMock), "virtio-sensor1")(system)

    bufferMock.readLine _ expects() returning "120"
    bufferMock.readLine _ expects() returning "130"
    bufferMock.readLine _ expects() returning "135"

    sensor.underlyingActor.asInstanceOf[VirtioSensorMock].power(MonitorTick("", muid, 1, tick)) should equal(0.W)
    sensor.underlyingActor.asInstanceOf[VirtioSensorMock].power(MonitorTick("", muid, "app", tick)) should equal(0.W)
    sensor.underlyingActor.asInstanceOf[VirtioSensorMock].power(MonitorTick("", muid2, All, tick2)) should equal(0.W)
    sensor.underlyingActor.asInstanceOf[VirtioSensorMock].timestamps(CacheKey(muid, All))(0l) should equal(tick.timestamp)
    sensor.underlyingActor.asInstanceOf[VirtioSensorMock].timestamps(CacheKey(muid2, All))(0l) should equal(tick2.timestamp)

    java.lang.Thread.sleep(100.milliseconds.toMillis)

    sensor.underlyingActor.asInstanceOf[VirtioSensorMock].power(MonitorTick("", muid, 1, tick3)) should equal(120.W)
    sensor.underlyingActor.asInstanceOf[VirtioSensorMock].powers(CacheKey(muid, All))(0.W) should equal(120.W)
    sensor.underlyingActor.asInstanceOf[VirtioSensorMock].power(MonitorTick("", muid2, All, tick4)) should equal(130.W)
    sensor.underlyingActor.asInstanceOf[VirtioSensorMock].powers(CacheKey(muid2, All))(0.W) should equal(130.W)
    sensor.underlyingActor.asInstanceOf[VirtioSensorMock].power(MonitorTick("", muid, "app", tick3)) should equal(120.W)
    sensor.underlyingActor.asInstanceOf[VirtioSensorMock].powers(CacheKey(muid, All))(0.W) should equal(120.W)
    sensor.underlyingActor.asInstanceOf[VirtioSensorMock].timestamps(CacheKey(muid, All))(0l) should equal(tick3.timestamp)
    sensor.underlyingActor.asInstanceOf[VirtioSensorMock].timestamps(CacheKey(muid2, All))(0l) should equal(tick4.timestamp)

    sensor.underlyingActor.asInstanceOf[VirtioSensorMock].power(MonitorTick("", muid, 1, tick5)) should equal(135.W)
    sensor.underlyingActor.asInstanceOf[VirtioSensorMock].powers(CacheKey(muid, All))(0.W) should equal(135.W)
    sensor.underlyingActor.asInstanceOf[VirtioSensorMock].timestamps(CacheKey(muid, All))(0l) should equal(tick5.timestamp)
    sensor.underlyingActor.asInstanceOf[VirtioSensorMock].timestamps(CacheKey(muid2, All))(0l) should equal(tick4.timestamp)

    Await.result(gracefulStop(sensor, timeout.duration), timeout.duration)
  }

  it should "sense correctly a MonitorTick published in the event bus" in new Bus with Times {
    val bufferMock = mock[MockableBufferedReader]
    bufferMock.readLine _ expects() returning "130"

    val sensor = TestActorRef(Props(classOf[VirtioSensorMock], eventBus, new OSHelper {
      import org.powerapi.core.GlobalCpuTime

      private var targetTimes = Map[Target, List[Long]](
        Process(1) -> List(oldP1ElapsedTime1, p1ElapsedTime1)
      )

      private var globalTimes = List[(Long, Long)](
        (oldGlobalElapsedTime1, oldActiveElapsedTime1), (globalElapsedTime1, activeElapsedTime1)
      )

      def getCPUFrequencies(topology: Map[Int, Iterable[Int]]): Iterable[Long] = Iterable()

      def getProcesses(application: Application): Iterable[Process] = Iterable(Process(2), Process(3))

      def getThreads(process: Process): Iterable[Thread] = Iterable()

      def getProcessCpuTime(process: Process): Option[Long] = {
        targetTimes.getOrElse(process, List()) match {
          case times if times.length > 0 => {
            targetTimes += process -> times.tail
            Some(times.head)
          }
          case _ => None
        }
      }

      def getGlobalCpuTime: GlobalCpuTime = {
        globalTimes.headOption match {
          case Some((globalTime, activeTime)) => {
            globalTimes = globalTimes.tail
            GlobalCpuTime(globalTime, activeTime)
          }
          case _ => GlobalCpuTime(0, 0)
        }
      }

      def getTimeInStates: TimeInStates = TimeInStates(Map())
    }, 0, bufferMock), "virtio-sensor2")(system)

    subscribeVirtioReport(eventBus)(testActor)

    val muid = UUID.randomUUID()
    lazy val monitorTick = MonitorTick("", muid, 1, ClockTick("", 25.milliseconds))
    lazy val monitorTick2 = MonitorTick("", muid, 1, ClockTick("", 25.milliseconds))

    publishMonitorTick(muid, 1, ClockTick("", 25.milliseconds))(eventBus)
    val msg = expectMsgClass(classOf[VirtioReport])
    msg.muid should equal(muid)
    msg.target should equal(Process(1))
    msg.targetRatio should equal(TargetUsageRatio(0.0))
    msg.power should equal(0.W)

    java.lang.Thread.sleep(100.milliseconds.toMillis)

    publishMonitorTick(muid, 1, ClockTick("", 25.milliseconds))(eventBus)
    val msg2 = expectMsgClass(classOf[VirtioReport])
    msg2.muid should equal(muid)
    msg2.target should equal(Process(1))
    msg2.targetRatio should equal(processRatio1)
    msg2.power should equal(130.W)

    Await.result(gracefulStop(sensor, timeout.duration), timeout.duration)
  }
}
