name := "team_gehem_homepage"

version := "0.3"

lazy val `team_gehem_homepage` =
  (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(ehcache, ws, specs2 % Test, guice, evolutions)

unmanagedResourceDirectories in Test <+= baseDirectory(
  _ / "target/web/public/test")

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

//bootstrap
libraryDependencies ++= Seq(
  "com.adrianhurt" %% "play-bootstrap" % "1.2-P26-B3" exclude("org.webjars", "jquery"),
  "org.webjars" % "bootstrap" % "3.3.7",
  "org.webjars" % "jquery" % "3.2.1",
  "org.webjars" % "font-awesome" % "4.7.0",
  "org.webjars" % "bootstrap-datepicker" % "1.6.4"
)

// slick
libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-slick" % "3.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "3.0.0",
  "org.postgresql" % "postgresql" % "42.1.3"
)