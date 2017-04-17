import sbt.Keys.libraryDependencies

name := "team_gehem"

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.9"

scalacOptions += "-feature"

libraryDependencies ++= Seq(
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % Test
)

libraryDependencies += "org.scala-lang.modules" %% "scala-async" % "0.9.6"

// slick
libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-slick" % "2.0.2",
  "com.typesafe.play" %% "play-slick-evolutions" % "2.0.2",
  "org.postgresql" % "postgresql" % "42.0.0"
)

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

//bootstrap
libraryDependencies ++= Seq(
  "com.adrianhurt" %% "play-bootstrap" % "1.1.1-P25-B3-SNAPSHOT",
  "org.webjars" % "bootstrap" % "3.3.7",
  "org.webjars" % "jquery" % "3.2.0",
  "org.webjars" % "font-awesome" % "4.7.0",
  "org.webjars" % "bootstrap-datepicker" % "1.6.4"
)

// google'sHTML Compressor
libraryDependencies += "com.mohiva" %% "play-html-compressor" % "0.6.3"

libraryDependencies += "com.google.api-client" % "google-api-client" % "1.22.0"