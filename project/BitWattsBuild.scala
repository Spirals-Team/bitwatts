import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import sbt._

object BitWattsBuild extends Build {
  lazy val powerapi = Project(id = "bitwatts", base = file(".")).aggregate(bitwattsCore, bitwattsCli)

  lazy val bitwattsCore = Project(id = "bitwatts-core", base = file("bitwatts-core"))
  lazy val bitwattsCli = Project(id = "bitwatts-cli", base = file("bitwatts-cli")).dependsOn(bitwattsCore % "compile -> compile").enablePlugins(JavaAppPackaging)
  lazy val bitwattsEurosys2015 = Project(id = "bitwatts-eurosys-2015", base = file("bitwatts-eurosys-2015")).dependsOn(bitwattsCore % "compile -> compile").enablePlugins(JavaAppPackaging)
}
