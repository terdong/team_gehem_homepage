name := "team_gehem_homepage"

version := "0.5.5"

lazy val `team_gehem_homepage` = (project in file(".")).enablePlugins(PlayScala, LauncherJarPlugin)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(ehcache, ws, specs2 % Test, guice, evolutions)

unmanagedResourceDirectories in Test <+= baseDirectory(
  _ / "target/web/public/test")

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

resolvers += "Central Repository" at "http://central.maven.org/maven2/"

//bootstrap
libraryDependencies ++= Seq(
  "com.adrianhurt" %% "play-bootstrap" % "1.2-P26-B3" exclude("org.webjars", "jquery")
)

// slick
libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-slick" % "3.0.1",
  "com.typesafe.play" %% "play-slick-evolutions" % "3.0.1",
  "org.postgresql" % "postgresql" % "42.1.3"
)

// https://mvnrepository.com/artifact/com.sksamuel.scrimage/scrimage-core_2.12
libraryDependencies += "com.sksamuel.scrimage" % "scrimage-core_2.12" % "2.1.8"

// https://mvnrepository.com/artifact/com.google.api-client/google-api-client
libraryDependencies += "com.google.api-client" % "google-api-client" % "1.22.0"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.13.4" % "test"

mappings in Universal  += file ( "eb/Procfile" ) ->  "Procfile"