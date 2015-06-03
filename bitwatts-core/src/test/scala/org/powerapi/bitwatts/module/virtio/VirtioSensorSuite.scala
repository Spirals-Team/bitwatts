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

  class MockableBufferedReader extends BufferedReader(mock[Reader])

  "A VirtioSensor" should "have a method for getting the power from a BufferedReader" in new Bus {
    val bufferMock = mock[MockableBufferedReader]
    val muid = UUID.randomUUID()
    val muid2 = UUID.randomUUID()
    lazy val tick = ClockTick("", 25.milliseconds)
    lazy val tick2 = ClockTick("", 25.milliseconds)
    lazy val tick3 = ClockTick("", 25.milliseconds)
    lazy val tick4 = ClockTick("", 25.milliseconds)
    lazy val tick5 = ClockTick("", 25.milliseconds)

    val sensor = TestActorRef(Props(classOf[VirtioSensorMock], eventBus, new OSHelper {
      def getCPUFrequencies: Set[Long] = Set()
      def getThreads(process: Process): Set[Thread] = Set()
      def getTimeInStates: TimeInStates = TimeInStates(Map())
      def getProcessCpuTime(process: Process): Option[Long] = None
      def getGlobalCpuTime: GlobalCpuTime = GlobalCpuTime(0l, 0l)
      def getProcesses(application: Application): Set[Process] = Set()
      def getProcessCpuPercent(muid: UUID, process: Process): TargetUsageRatio = TargetUsageRatio(0.0)
      def getGlobalCpuPercent(muid: UUID): TargetUsageRatio = TargetUsageRatio(0.0)
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

  it should "sense correctly a MonitorTick published in the event bus" in new Bus {
    val bufferMock = mock[MockableBufferedReader]
    bufferMock.readLine _ expects() returning "130"

    val processRatio1 = 0.25
    val globalRatio = 0.80

    val sensor = TestActorRef(Props(classOf[VirtioSensorMock], eventBus, new OSHelper {
      import org.powerapi.core.GlobalCpuTime

      private val targetUsage = Map[Target, Double](
        Process(1) -> processRatio1
      )

      private val globalUsages = globalRatio

      def getCPUFrequencies: Set[Long] = Set()

      def getProcesses(application: Application): Set[Process] = Set(Process(2), Process(3))

      def getThreads(process: Process): Set[Thread] = Set()

      def getProcessCpuTime(process: Process): Option[Long] = None

      def getGlobalCpuTime: GlobalCpuTime = GlobalCpuTime(0, 0)

      def getProcessCpuPercent(muid: UUID, process: Process) = TargetUsageRatio(targetUsage.getOrElse(process, 0.0))

      def getGlobalCpuPercent(muid: UUID) = TargetUsageRatio(globalUsages)

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
    msg.targetRatio should equal(TargetUsageRatio(processRatio1))
    msg.power should equal(0.W)

    java.lang.Thread.sleep(100.milliseconds.toMillis)

    publishMonitorTick(muid, 1, ClockTick("", 25.milliseconds))(eventBus)
    val msg2 = expectMsgClass(classOf[VirtioReport])
    msg2.muid should equal(muid)
    msg2.target should equal(Process(1))
    msg2.targetRatio should equal(TargetUsageRatio(processRatio1))
    msg2.power should equal(130.W)

    Await.result(gracefulStop(sensor, timeout.duration), timeout.duration)
  }
}
