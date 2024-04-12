val scala3Version = "3.4.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "ox-exercise",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "com.softwaremill.ox" %% "core" % "0.0.25"
    ),
    libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test
  )
