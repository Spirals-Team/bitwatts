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
package org.powerapi.bitwatts.eurosys

import java.util.concurrent.TimeUnit
import org.powerapi.PowerMeter
import org.powerapi.core.target.All
import org.powerapi.core.power.MEAN
import org.powerapi.core.{Configuration, ConfigValue}
import org.powerapi.module.libpfm.{LibpfmCoreModule, LibpfmHelper, LibpfmCoreProcessModule}
import org.powerapi.module.powerspy.PowerSpyModule
import org.powerapi.reporter.FileDisplay
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.sys.process.stringSeqToProcess
import scalax.file.Path

/**
 * EuroSys '15 host experiments.
 *
 * @author <a href="mailto:maxime.colmant@gmail.com">Maxime Colmant</a>
 */
object BitWatts extends Configuration(None) with App {
  /**
   * Main configuration.
   */
  lazy val parsecP = load { _.getString("powerapi.eurosys.parsec-benchmarks-path") } match {
    case ConfigValue(value) => value
    case _ => ""
  }

  lazy val interval: FiniteDuration = load { _.getDuration("powerapi.eurosys.interval", TimeUnit.NANOSECONDS) } match {
    case ConfigValue(value) => value.nanoseconds
    case _ => 1l.seconds
  }

  lazy val figure9P: String = load { _.getString("powerapi.eurosys.figure9-data-path") } match {
    case ConfigValue(value) => value
    case _ => "figure9-data"
  }

  lazy val figure10P: String = load { _.getString("powerapi.eurosys.figure10-data-path") } match {
    case ConfigValue(value) => value
    case _ => "figure10-data"
  }

  lazy val separator = "="
  lazy val PSFormat = """\s*([\d]+)\s.*""".r

  val currentPid = java.lang.management.ManagementFactory.getRuntimeMXBean.getName.split("@")(0).toInt
  val libpfmHelper = new LibpfmHelper
  libpfmHelper.init()

  @volatile var powerMeters = Seq[PowerMeter]()

  val shutdownHookThread = scala.sys.ShutdownHookThread {
    powerMeters.foreach(powerMeter => powerMeter.shutdown())
    powerMeters = Seq()
    libpfmHelper.deinit()
  }

  def figure9Experiment(): Unit = {
    val benchmarks = Array("blackscholes", "bodytrack", "facesim", "fluidanimate", "freqmine", "swaptions", "vips")

    Path(s"$figure9P", '/').deleteRecursively(force = true)
    Path(s"$figure9P", '/').createDirectory()
    Path(s"/tmp/$figure9P", '/').deleteRecursively(force = true)
    Path(s"/tmp/$figure9P", '/').createDirectory()

    /**
     * Cleans old runs.
     */
    (Seq("bash", "-c", "ps -Ao pid,command") #> Seq("bash", "-c", "grep inst/amd64-linux.gcc/bin/") #> Seq("bash", "-c", "grep -v grep")).lineStream_!.toArray.foreach {
      _ match {
        case PSFormat(pid) => Seq("kill", "-9", pid)
        case _ => None
      }
    }

    val powerapi = PowerMeter.loadModule(LibpfmCoreModule(None, libpfmHelper))
    powerMeters :+= powerapi
    val externalPMeter = PowerMeter.loadModule(PowerSpyModule())
    powerMeters :+= externalPMeter

    for(benchmark <- benchmarks) {
      Path(s"/tmp/$figure9P/$benchmark", '/').createDirectory()

      val cmdBenchmark = Seq("bash", "-c", "ps -Ao pid,command") #> Seq("bash", "-c", s"grep /$benchmark/inst/amd64-linux.gcc/bin") #> Seq("bash", "-c", "grep -v grep")

      val externalOutput = new FileDisplay(s"/tmp/$figure9P/$benchmark/output-external.dat")
      val powerapiOutput = new FileDisplay(s"/tmp/$figure9P/$benchmark/output-powerapi.dat")

      /**
       * Synchronization.
       */
      var allExPMeter = externalPMeter.monitor(interval)(All)(MEAN).to(externalOutput)
      var allPapi = powerapi.monitor(interval)(All)(MEAN).to(powerapiOutput)
      Thread.sleep(20l.seconds.toMillis)
      allExPMeter.cancel()
      allPapi.cancel()

      Path(s"/tmp/$figure9P/$benchmark/output-external.dat", '/').delete(true)
      Path(s"/tmp/$figure9P/$benchmark/output-powerapi.dat", '/').delete(true)

      /**
       * Launch the benchmark.
       */
      Seq("bash", s"scripts/internal/start_parsec.bash", parsecP, benchmark).!
      allExPMeter = externalPMeter.monitor(interval)(All)(MEAN).to(externalOutput)
      allPapi = powerapi.monitor(interval)(All)(MEAN).to(powerapiOutput)

      var output = Array[String]()
      while(output.isEmpty) {
        output = cmdBenchmark.lineStream_!.toArray
        Thread.sleep(20l.seconds.toMillis)
      }

      var pid = 0
      output(0) match {
        case PSFormat(p) => pid = p.trim.toInt
        case _ => {}
      }

      while(Seq("kill", "-0", s"$pid").! == 0) {
        Thread.sleep(15l.seconds.toMillis)
      }

      allExPMeter.cancel()
      allPapi.cancel()

      (Path(s"/tmp/$figure9P/$benchmark", '/') * "*.dat").foreach(path => {
        path.moveTo(Path(s"$figure9P/$benchmark/${path.name}", '/'), true)
      })
    }

    externalPMeter.shutdown()
    powerapi.shutdown()
    powerMeters = Seq()
  }

  def figure10Experiment(): Unit = {
    val benchmark1 = "freqmine"
    val benchmark2 = "x264"

    val cmdBenchmark1 = Seq("bash", "-c", "ps -Ao pid,command") #> Seq("bash", "-c", s"grep /$benchmark1/inst/amd64-linux.gcc/bin") #> Seq("bash", "-c", "grep -v grep")
    val cmdBenchmark2 = Seq("bash", "-c", "ps -Ao pid,command") #> Seq("bash", "-c", s"grep /$benchmark2/inst/amd64-linux.gcc/bin") #> Seq("bash", "-c", "grep -v grep")

    Path(s"$figure10P", '/').deleteRecursively(force = true)
    Path(s"/tmp/$figure10P", '/').deleteRecursively(force = true)
    Path(s"/tmp/$figure10P", '/').createDirectory()

    /**
     * Cleans old runs.
     */
    (Seq("bash", "-c", "ps -Ao pid,command") #> Seq("bash", "-c", "grep inst/amd64-linux.gcc/bin/") #> Seq("bash", "-c", "grep -v grep")).lineStream_!.toArray.foreach {
      _ match {
        case PSFormat(pid) => Seq("kill", "-9", pid)
        case _ => None
      }
    }

    val powerapi = PowerMeter.loadModule(LibpfmCoreProcessModule(None, libpfmHelper))
    powerMeters :+= powerapi
    val externalPMeter = PowerMeter.loadModule(PowerSpyModule())
    powerMeters :+= externalPMeter

    val externalOutput = new FileDisplay(s"/tmp/$figure10P/output-external.dat")
    val powerapiOutput = new FileDisplay(s"/tmp/$figure10P/output-powerapi.dat")
    val p1Output = new FileDisplay(s"/tmp/$figure10P/output-powerapi-p1.dat")
    val p2Output = new FileDisplay(s"/tmp/$figure10P/output-powerapi-p2.dat")

    /**
     * Synchronization.
     */
    var allExPMeter = externalPMeter.monitor(interval)(All)(MEAN).to(externalOutput)
    var self = powerapi.monitor(interval)(currentPid)(MEAN).to(powerapiOutput)
    Thread.sleep(20l.seconds.toMillis)
    allExPMeter.cancel()
    self.cancel()
    Path(s"/tmp/$figure10P/output-external.dat", '/').delete(true)
    Path(s"/tmp/$figure10P/output-powerapi.dat", '/').delete(true)

    /**
     * Launch the first benchmark.
     */
    Seq("bash", s"scripts/internal/start_parsec.bash", parsecP, benchmark1).!
    var output = Array[String]()
    while(output.isEmpty) {
      output = cmdBenchmark1.lineStream_!.toArray
      Thread.sleep(1l.seconds.toMillis)
    }

    var pidBench1 = 0
    output(0) match {
      case PSFormat(p) => pidBench1 = p.trim.toInt
      case _ => {}
    }

    allExPMeter =  externalPMeter.monitor(interval)(All)(MEAN).to(externalOutput)
    self = powerapi.monitor(interval)(currentPid)(MEAN).to(powerapiOutput)
    val pid1 = powerapi.monitor(interval)(pidBench1)(MEAN).to(p1Output)
    Thread.sleep(60l.seconds.toMillis)
    (Path(s"/tmp/$figure10P", '/') * "*.dat").foreach(path => path.append(s"$separator\n"))

    /**
     * Launch the second benchmark.
     */
    Seq("bash", s"scripts/internal/start_parsec.bash", parsecP, benchmark2).!
    output = Array[String]()
    while(output.isEmpty) {
      output = cmdBenchmark2.lineStream_!.toArray
      Thread.sleep(1l.seconds.toMillis)
    }

    var pidBench2 = 0
    output(0) match {
      case PSFormat(p) => pidBench2 = p.trim.toInt
      case _ => {}
    }

    val pid2 = powerapi.monitor(interval)(pidBench2)(MEAN).to(p2Output)
    Thread.sleep(60l.seconds.toMillis)

    Seq("kill", "-9", pidBench1.toString).lineStream_!
    Seq("kill", "-9", pidBench2.toString).lineStream_!

    allExPMeter.cancel()
    self.cancel()
    pid1.cancel()
    pid2.cancel()
    externalPMeter.shutdown()
    powerapi.shutdown()

    Path(s"$figure10P", '/').createDirectory()
    (Path(s"/tmp/$figure10P", '/') * "*.dat").foreach(path => {
      path.moveTo(Path(s"$figure10P/${path.name}", '/'), true)
    })

    powerMeters = List()
  }

  def printHelp(): Unit = {
    val str =
      """
        |BitWatts Spirals Team / University of Neuchâtel"
        |
        |EuroSys host experiments.
        |
        |usage: ./bitwatts --experiment [figure9|figure10]
      """.stripMargin

    println(str)
  }

  def validateExperiment(str: String): Boolean = str match {
    case "figure9"|"figure10" => true
    case _ => false
  }

  def cli(options: Map[Symbol, Any], args: List[String]): Map[Symbol, Any] = args match {
    case Nil => options
    case "--experiment" :: value :: Nil if validateExperiment(value) => cli(options + ('experiment -> value), Nil)
    case option :: tail => println(s"unknown cli option $option"); sys.exit(1)
  }

  if(args.size == 0) {
    printHelp()
    sys.exit(1)
  }

  val configuration = cli(Map(), args.toList)
  configuration('experiment).toString match {
    case "figure9" => figure9Experiment()
    case "figure10" => figure10Experiment()
  }

  shutdownHookThread.start()
  shutdownHookThread.join()
  shutdownHookThread.remove()
  sys.exit(0)
}
