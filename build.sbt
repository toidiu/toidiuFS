name := """toidiuFS"""

version := "0.0.1"

lazy val toidiufs = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"


libraryDependencies ++= {

  val akkaV = "2.4.10"
  val catsV = "0.8.0"
  val circeV = "0.5.1"
  Seq(
    "com.typesafe.akka" %% "akka-slf4j" % akkaV
    ,  ws
    , "com.typesafe.akka" %% "akka-stream" % akkaV
    , "io.circe" %% "circe-core" % circeV
    , "io.circe" %% "circe-generic" % circeV
    , "io.circe" %% "circe-jawn" % circeV
    , "org.typelevel" %% "cats" % catsV
  )
}

resolvers += Resolver.sonatypeRepo("snapshots")
