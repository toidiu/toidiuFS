name := """toidiuFS"""

version := "0.0.1"

lazy val toidiufs = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"


libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.10"
  , "com.typesafe.akka" %% "akka-stream" % "2.4.10"
  , "io.circe" %% "circe-core" % "0.3.0"
  , "io.circe" %% "circe-generic" % "0.3.0"
  , "io.circe" %% "circe-jawn" % "0.3.0"
  , "org.typelevel" %% "cats" % "0.7.2"
)

resolvers += Resolver.sonatypeRepo("snapshots")
