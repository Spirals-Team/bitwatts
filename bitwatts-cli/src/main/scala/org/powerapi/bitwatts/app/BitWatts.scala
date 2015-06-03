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
package org.powerapi.bitwatts.app

import java.lang.management.ManagementFactory

import org.powerapi.bitwatts.reporter.{ThriftDisplay, VirtioDisplay}
import org.powerapi.core.LinuxHelper
import org.powerapi.core.target.{Application, All, Process, Target}
import org.powerapi.module.rapl.RAPLModule
import org.powerapi.reporter.{FileDisplay, JFreeChartDisplay, ConsoleDisplay}
import org.powerapi.{PowerMonitoring, PowerMeter}
import org.powerapi.bitwatts.module.virtio.VirtioModule
import org.powerapi.core.power._
import org.powerapi.module.cpu.dvfs.CpuDvfsModule
import org.powerapi.module.cpu.simple.{ProcFSCpuSimpleModule, SigarCpuSimpleModule}
import org.powerapi.module.libpfm.{LibpfmHelper, LibpfmCoreProcessModule, LibpfmCoreModule}
import org.powerapi.module.powerspy.PowerSpyModule
import scala.concurrent.duration.DurationInt
import scala.sys.process.stringSeqToProcess

/**
 * BitWatts CLI.
 *
 * @author <a href="mailto:maxime.colmant@gmail.com">Maxime Colmant</a>
 */
object BitWatts extends App {
  val modulesR = """(procfs-cpu-simple|sigar-cpu-simple|cpu-dvfs|libpfm-core|libpfm-core-process|powerspy|rapl|virtio)(,(procfs-cpu-simple|sigar-cpu-simple|cpu-dvfs|libpfm-core|libpfm-core-process|powerspy|rapl|virtio))*""".r
  val aggR = """max|min|geomean|logsum|mean|median|stdev|sum|variance""".r
  val durationR = """\d+""".r
  val pidR = """(\d+)""".r
  val appR = """(.+)""".r
  val thriftR = """(.+),([0-9]+),(.+),(.+)""".r

  @volatile var powerMeters = Seq[PowerMeter]()
  @volatile var monitors = Seq[PowerMonitoring]()

  val shutdownHookThread = scala.sys.ShutdownHookThread {
    monitors.foreach(monitor => monitor.cancel())
    monitors = Seq()
    powerMeters.foreach(powerMeter => powerMeter.shutdown())
    powerMeters = Seq()
  }

  def validateModules(str: String) = str match {
    case modulesR(_*) => true
    case _ => false
  }

  def validateAgg(str: String): Boolean = str match {
    case aggR(_*) => true
    case _ => false
  }

  implicit def aggStrToAggFunction(str: String): Seq[Power] => Power = {
    str match {
      case "max" => MAX
      case "min" => MIN
      case "geomean" => GEOMEAN
      case "logsum" => LOGSUM
      case "mean" => MEAN
      case "median" => MEDIAN
      case "stdev" => STDEV
      case "sum" => SUM
      case "variance" => VARIANCE
    }
  }

  def validateDuration(str: String): Boolean = str match {
    case durationR(_*) => true
    case _ => false
  }

  implicit def targetsStrToTargets(str: String): Seq[Target] = {
    val strTargets = if(str.split(",").contains("all")) {
      "all"
    }
    else str

    (for(target <- strTargets.split(",")) yield {
      target match {
        case "" => Process(ManagementFactory.getRuntimeMXBean.getName.split("@")(0).toInt)
        case "all" => All
        case pidR(pid) => Process(pid.toInt)
        case appR(app) => Application(app)
      }
    }).toSeq
  }

  def validateThrift(str: String): Boolean = str match {
    case thriftR(_*) => true
    case _ => false
  }

  def printHelp(): Unit = {
    val str =
      """
        |BitWatts, Spirals Team / University of Neuchatel"
        |
        |Build a software-defined power meter. Do not forget to configure correctly the modules.
        |You can use different settings per software-defined power meter for some modules by using the optional prefix option.
        |Please, refer to the documentation inside the GitHub wiki for further details.
        |
        |usage: ./bitwatts modules [procfs-cpu-simple|sigar-cpu-simple|cpu-dvfs|libpfm-core|libpfm-core-proces|powerspy|rapl|virtio,...] *--prefix [name]* \
        |                          monitor --frequency [ms] --targets [pid, ..., app, ...|all] --agg [max|min|geomean|logsum|mean|median|stdev|sum|variance] --[console,file [filepath],chart,virtio [filepath],thrift [ip,port,sender,topic]] \
        |                  duration [s]
        |
        |example: ./bitwatts modules libpfm-core-process monitor --frequency 1000 --targets 2355 --agg max --virtio /tmp/port2 \
        |                                                monitor --frequency 1000 --targets 2356 --agg max --virtio /tmp/port3 --thrift 193.51.236.198,5556,topic,bitwatts \
        |                    modules powerspy monitor --frequency 1000 --targets all --agg max --file output-powers.dat \
        |                    duration 30
      """.stripMargin

    println(str)
  }

  def cli(options: List[Map[Symbol, Any]], duration: String, args: List[String]): (List[Map[Symbol, Any]], String) = args match {
    case Nil => (options, duration)
    case "modules" :: value :: "--prefix" :: prefix :: "monitor" :: tail if validateModules(value) => {
      val (remainingArgs, monitors) = cliMonitorsSubcommand(List(), Map(), tail.map(_.toString))
      cli(options :+ Map('modules -> value, 'prefix -> Some(prefix), 'monitors -> monitors), duration, remainingArgs)
    }
    case "modules" :: value :: "monitor" :: tail if validateModules(value) => {
      val (remainingArgs, monitors) = cliMonitorsSubcommand(List(), Map(), tail.map(_.toString))
      cli(options :+ Map('modules -> value, 'prefix -> None, 'monitors -> monitors), duration, remainingArgs)
    }
    case "duration" :: value :: tail if validateDuration(value) => cli(options, value, tail)
    case option :: tail => println(s"unknown cli option $option"); sys.exit(1)
  }

  def cliMonitorsSubcommand(options: List[Map[Symbol, Any]], currentMonitor: Map[Symbol, Any], args: List[String]): (List[String], List[Map[Symbol, Any]]) = args match {
    case Nil => (List(), options :+ currentMonitor)
    case "modules" :: value :: "--prefix" :: prefix :: "monitor" :: tail if validateModules(value) => (List("modules", value, "--prefix", prefix, "monitor") ++ tail, options :+ currentMonitor)
    case "modules" :: value :: "monitor" :: tail if validateModules(value) => (List("modules", value, "monitor") ++ tail, options :+ currentMonitor)
    case "duration" :: value :: tail if validateDuration(value) => (List("duration", value) ++ tail, options :+ currentMonitor)
    case "monitor" :: tail => cliMonitorsSubcommand(options :+ currentMonitor, Map(), tail)
    case "--frequency" :: value :: tail if validateDuration(value) => cliMonitorsSubcommand(options, currentMonitor ++ Map('frequency -> value), tail)
    case "--targets" :: value :: tail => cliMonitorsSubcommand(options, currentMonitor ++ Map('targets -> value), tail)
    case "--agg" :: value :: tail if validateAgg(value) => cliMonitorsSubcommand(options, currentMonitor ++ Map('agg -> value), tail)
    case "--console" :: tail => cliMonitorsSubcommand(options, currentMonitor ++ Map('console -> "true"), tail)
    case "--file" :: value :: tail => cliMonitorsSubcommand(options, currentMonitor ++ Map('file -> value), tail)
    case "--chart" :: tail => cliMonitorsSubcommand(options, currentMonitor ++ Map('chart -> "true"), tail)
    case "--virtio" :: value :: tail => cliMonitorsSubcommand(options, currentMonitor ++ Map('virtio -> value), tail)
    case "--thrift" :: value :: tail if validateThrift(value) => cliMonitorsSubcommand(options, currentMonitor ++ Map('thrift -> value), tail)
    case option :: tail => println(s"unknown monitor option $option"); sys.exit(1)
  }

  if(args.size == 0) {
    printHelp()
    sys.exit(1)
  }

  else {
    if(System.getProperty("os.name").toLowerCase.indexOf("nix") >= 0 || System.getProperty("os.name").toLowerCase.indexOf("nux") >= 0) Seq("bash", "scripts/system.bash").!
    val (configuration, duration) = cli(List(), "3600", args.toList)

    var libpfmHelper: Option[LibpfmHelper] = None
    // Currently, our solution was only tested on Linux
    val linuxHelper = new LinuxHelper

    if(configuration.count(powerMeterConf => powerMeterConf('modules).toString.contains("libpfm")) != 0) {
      libpfmHelper = Some(new LibpfmHelper)
      libpfmHelper.get.init()
    }

    for(powerMeterConf <- configuration) {
      val modules = (for(module <- powerMeterConf('modules).toString.split(",")) yield {
        module match {
          case "procfs-cpu-simple" => ProcFSCpuSimpleModule()
          case "sigar-cpu-simple" => SigarCpuSimpleModule()
          case "cpu-dvfs" => CpuDvfsModule()
          case "libpfm-core" => LibpfmCoreModule(powerMeterConf('prefix).asInstanceOf[Option[String]], libpfmHelper.get)
          case "libpfm-core-process" => LibpfmCoreProcessModule(powerMeterConf('prefix).asInstanceOf[Option[String]], libpfmHelper.get)
          case "powerspy" => PowerSpyModule()
          case "rapl" => RAPLModule()
          case "virtio" => VirtioModule(powerMeterConf('prefix).asInstanceOf[Option[String]], linuxHelper)
        }
      }).toSeq

      val powerMeter = PowerMeter.loadModule(modules: _*)
      powerMeters :+= powerMeter

      for(monitorConf <- powerMeterConf('monitors).asInstanceOf[List[Map[Symbol, Any]]]) {
        val frequency = monitorConf.getOrElse('frequency, "1000").toString.toInt.milliseconds
        val targets: Seq[Target] = monitorConf.getOrElse('targets, "").toString.toLowerCase
        val agg: Seq[Power] => Power = aggStrToAggFunction(monitorConf.getOrElse('agg, "max").toString.toLowerCase)
        val console = monitorConf.getOrElse('console, "").toString
        val file = monitorConf.getOrElse('file, "").toString
        val chart = monitorConf.getOrElse('chart, "").toString
        val virtio = monitorConf.getOrElse('virtio, "").toString
        val thrift = monitorConf.getOrElse('thrift, "").toString

        val monitor = powerMeter.monitor(frequency)(targets: _*)(agg)
        monitors :+= monitor

        if(console != "") {
          val consoleDisplay = new ConsoleDisplay()
          monitor.to(consoleDisplay)
        }

        if(file != "") {
          val fileDisplay = new FileDisplay(file)
          monitor.to(fileDisplay)
        }

        if(chart != "") {
          val chartDisplay = new JFreeChartDisplay()
          monitor.to(chartDisplay)
        }

        if(virtio != "") {
          val virtioDisplay = new VirtioDisplay(virtio)
          monitor.to(virtioDisplay)
        }

        if(thrift != "") {
          val thriftParams = thrift.split(",")
          val thriftDisplay = new ThriftDisplay(thriftParams(0), thriftParams(1).toInt, thriftParams(2), thriftParams(3))
          monitor.to(thriftDisplay)
        }
      }
    }

    Thread.sleep(duration.toInt.seconds.toMillis)

    libpfmHelper match {
      case Some(helper) => helper.deinit()
      case _ => {}
    }
  }

  shutdownHookThread.start()
  shutdownHookThread.join()
  shutdownHookThread.remove()
  sys.exit(0)
}
