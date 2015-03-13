name := "bitwatts-eurosys-2015"

lazy val downloadBluecoveEurosys = taskKey[File]("download-bluecove-eurosys")
lazy val downloadBluecoveGplEurosys = taskKey[File]("download-bluecove-gpl-eurosys")

downloadBluecoveEurosys := {
  val locationBluecove = baseDirectory.value / "lib" / "bluecove-2.1.0.jar"
  if(!locationBluecove.getParentFile.exists()) locationBluecove.getParentFile.mkdirs()
  if(!locationBluecove.exists()) IO.download(url("https://bluecove.googlecode.com/files/bluecove-2.1.0.jar"), locationBluecove)
  locationBluecove
}

downloadBluecoveGplEurosys := {
  val locationBluecoveGpl = baseDirectory.value / "lib" / "bluecove-gpl-2.1.0.jar"
  if(!locationBluecoveGpl.getParentFile.exists()) locationBluecoveGpl.getParentFile.mkdirs()
  if(!locationBluecoveGpl.exists()) IO.download(url("https://bluecove.googlecode.com/files/bluecove-gpl-2.1.0.jar"), locationBluecoveGpl)
  locationBluecoveGpl
}

mappings in Universal += downloadBluecoveEurosys.value -> s"lib/${downloadBluecoveEurosys.value.name}"

mappings in Universal += downloadBluecoveGplEurosys.value -> s"lib/${downloadBluecoveGplEurosys.value.name}"

mappings in Universal ++= {
  ((file("../") * "README*").get map {
    readmeFile: File =>
      readmeFile -> readmeFile.getName
  }) ++
    ((file("../") * "LICENSE*").get map {
      licenseFile: File =>
        licenseFile -> licenseFile.getName
    })
}

scriptClasspath ++= Seq("../conf", "../scripts")

NativePackagerKeys.executableScriptName := "bitwatts"
