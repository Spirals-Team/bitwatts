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
package org.powerapi.bitwatts.app

import java.lang.management.ManagementFactory

import org.powerapi.bitwatts.reporter.VirtioDisplay
import org.powerapi.core.target.{Application, All, Process, Target}
import org.powerapi.reporter.{JFreeChartDisplay, ConsoleDisplay}
import org.powerapi.{PowerMonitoring, PowerMeter, PowerModule}
import org.powerapi.bitwatts.module.virtio.VirtioModule
import org.powerapi.core.power._
import org.powerapi.module.cpu.dvfs.CpuDvfsModule
import org.powerapi.module.cpu.simple.CpuSimpleModule
import org.powerapi.module.libpfm.{LibpfmHelper, LibpfmCoreProcessModule, LibpfmCoreModule}
import org.powerapi.module.powerspy.PowerSpyModule
import scala.concurrent.duration.DurationInt
import scala.sys.process.stringSeqToProcess
import scalax.io.Resource

class FileDisplay(filepath: String) extends org.powerapi.reporter.FileDisplay {
  override lazy val output = Resource.fromFile(filepath)
}

/**
 * BitWatts CLI.
 *
 * @author <a href="mailto:maxime.colmant@gmail.com">Maxime Colmant</a>
 */
object BitWatts extends App {
  val modulesR = """(cpu-simple|cpu-dvfs|libpfm-core|libpfm-core-process|powerspy|virtio)(,(cpu-simple|cpu-dvfs|libpfm-core|libpfm-core-process|powerspy|virtio))*""".r
  val aggR = """max|min|geomean|logsum|mean|median|stdev|sum|variance""".r
  val durationR = """\d+""".r
  val virtioR = """([0-9]+:[0-9]+)(,([0-9]+:[0-9]+))*""".r
  val pidR = """(\d+)""".r
  val appR = """(.+)""".r

  @volatile var powerMeters = Seq[PowerMeter]()
  @volatile var monitors = Seq[PowerMonitoring]()

  val shutdownHookThread = scala.sys.ShutdownHookThread {
    println("It's the time for sleeping! ...")

    monitors.foreach(monitor => monitor.cancel())
    monitors = Seq()
    powerMeters.foreach(powerMeter => powerMeter.shutdown())
    powerMeters = Seq()
  }

  def validateModules(str: String) = str match {
    case modulesR(_*) => true
    case _ => false
  }

  implicit def modulesStrToPowerModules(str: String): Seq[PowerModule] = {
    (for(module <- str.split(",")) yield {
      module match {
        case "cpu-simple" => CpuSimpleModule()
        case "cpu-dvfs" => CpuDvfsModule()
        case "libpfm-core" => LibpfmCoreModule()
        case "libpfm-core-process" => LibpfmCoreProcessModule()
        case "powerspy" => PowerSpyModule()
        case "virtio" => VirtioModule()
      }
    }).toSeq
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

  def validateVirtio(str: String): Boolean = str match {
    case virtioR(_*) => true
    case _ => false
  }

  implicit def virtioStrToMapping(str: String): Map[Int, Int] = {
    var mappings = Map[Int, Int]()

    for(mapping <- str.split(",")) {
      mappings += mapping.split(":")(0).toInt -> mapping.split(":")(1).toInt
    }

    mappings
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

  def printHelp(): Unit = {
    val str =
      """
        |BitWatts (c) Spirals Team / University of Neuchatel"
        |
        |Build a software-defined power meter. Don't forget to configure correctly the modules (see the documentation).
        |
        |usage: ./bitwatts-cli modules [cpu-simple|cpu-dvfs|libpfm-core|libpfm-core-proces|powerspy|virtio, ...]
        |                              monitor --frequency [ms] --targets [pid, ..., app, ...)|all] --agg [max|min|geomean|logsum|mean|median|stdev|sum|variance] --[console,file [filepath],chart,virtio [pid:port, ...]]
        |                      duration [s]
        |
        |example: ./bitwatts-cli modules cpu-simple monitor --frequency 1000 --targets firefox --agg max --console monitor --targets chrome --agg max --console
        |                        modules powerspy monitor --frequency 1000 --targets all -agg max --console
        |                        duration 30
      """.stripMargin

    println(str)
  }

  def cli(options: List[Map[Symbol, Any]], duration: String, args: List[String]): (List[Map[Symbol, Any]], String) = args match {
    case Nil => (options, duration)
    case "modules" :: value :: "monitor" :: tail if validateModules(value) => {
      val (remainingArgs, monitors) = cliMonitorsSubcommand(List(), Map(), tail.map(_.toString))
      cli(options :+ Map('modules -> value, 'monitors -> monitors), duration, remainingArgs)
    }
    case "duration" :: value :: tail if validateDuration(value) => cli(options, value, tail)
    case option :: tail => println(s"unknown cli option $option"); sys.exit(1)
  }

  def cliMonitorsSubcommand(options: List[Map[Symbol, Any]], currentMonitor: Map[Symbol, Any], args: List[String]): (List[String], List[Map[Symbol, Any]]) = args match {
    case Nil => (List(), options :+ currentMonitor)
    case "modules" :: value :: "monitor" :: tail if validateModules(value) => (List("modules", value, "monitor") ++ tail, options :+ currentMonitor)
    case "duration" :: value :: tail if validateDuration(value) => (List("duration", value) ++ tail, options :+ currentMonitor)
    case "monitor" :: tail => cliMonitorsSubcommand(options :+ currentMonitor, Map(), tail)
    case "--frequency" :: value :: tail if validateDuration(value) => cliMonitorsSubcommand(options, currentMonitor ++ Map('frequency -> value), tail)
    case "--targets" :: value :: tail => cliMonitorsSubcommand(options, currentMonitor ++ Map('targets -> value), tail)
    case "--agg" :: value :: tail if validateAgg(value) => cliMonitorsSubcommand(options, currentMonitor ++ Map('agg -> value), tail)
    case "--console" :: tail => cliMonitorsSubcommand(options, currentMonitor ++ Map('console -> "true"), tail)
    case "--file" :: value :: tail => cliMonitorsSubcommand(options, currentMonitor ++ Map('file -> value), tail)
    case "--chart" :: tail => cliMonitorsSubcommand(options, currentMonitor ++ Map('chart -> "true"), tail)
    case "--virtio" :: value :: tail if validateVirtio(value) => cliMonitorsSubcommand(options, currentMonitor ++ Map('virtio -> value), tail)
    case option :: tail => println(s"unknown monitor option $option"); sys.exit(1)
  }

  if(args.size == 0) {
    printHelp()
    sys.exit(1)
  }

  else {
    Seq("bash", "scripts/system.bash").!
    val (configuration, duration) = cli(List(), "3600", args.toList)

    for(powerMeterConf <- configuration) {
      val modules = powerMeterConf('modules).toString
      if(modules.contains("libpfm-core") || modules.contains("libpfm-core-process")) LibpfmHelper.init()

      val powerMeter = PowerMeter.loadModule(powerMeterConf('modules).toString: _*)
      powerMeters :+= powerMeter

      for(monitorConf <- powerMeterConf('monitors).asInstanceOf[List[Map[Symbol, Any]]]) {
        val frequency = monitorConf.getOrElse('frequency, "1000").toString.toInt.milliseconds
        val targets: Seq[Target] = monitorConf.getOrElse('targets, "").toString.toLowerCase
        val agg: Seq[Power] => Power = aggStrToAggFunction(monitorConf.getOrElse('agg, "max").toString.toLowerCase)
        val console = monitorConf.getOrElse('console, "").toString
        val file = monitorConf.getOrElse('file, "").toString
        val chart = monitorConf.getOrElse('chart, "").toString
        val virtio = monitorConf.getOrElse('virtio, "").toString

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
      }
    }

    Thread.sleep(duration.toInt.seconds.toMillis)

    val isLibpfmInit = configuration.count(powerMeterConf => powerMeterConf('modules).toString.contains("libpfm-core") || powerMeterConf('modules).toString.contains("libpfm-core-process")) != 0
    if(isLibpfmInit) LibpfmHelper.deinit()
  }

  shutdownHookThread.start()
  shutdownHookThread.join()
  shutdownHookThread.remove()
  sys.exit(0)
}
