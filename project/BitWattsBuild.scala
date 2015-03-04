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
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import sbt._

object BitWattsBuild extends Build {
  lazy val powerapi = Project(id = "bitwatts", base = file(".")).aggregate(bitwattsCore, bitwattsCli)

  lazy val bitwattsCore = Project(id = "bitwatts-core", base = file("bitwatts-core"))
  lazy val bitwattsCli = Project(id = "bitwatts-cli", base = file("bitwatts-cli")).dependsOn(bitwattsCore % "compile -> compile").enablePlugins(JavaAppPackaging)
}
