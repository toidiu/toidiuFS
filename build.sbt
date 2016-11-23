name := """toidiuFS"""

version := "0.0.1"

lazy val toidiufs = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"


libraryDependencies ++= {

  val akkaV = "2.4.10"
  val catsV = "0.8.0"
  val circeV = "0.5.1"
  val s3V = "1.11.55"
  val DbxV: String = "2.1.2"
  Seq(
    "com.typesafe.akka" %% "akka-slf4j" % akkaV
    , "com.typesafe.akka" %% "akka-stream" % akkaV
    , ws
    , "io.circe" %% "circe-core" % circeV
    , "io.circe" %% "circe-generic" % circeV
    , "io.circe" %% "circe-jawn" % circeV
    , "org.typelevel" %% "cats" % catsV

    , "com.amazonaws" % "aws-java-sdk-s3" % s3V
    , "com.dropbox.core" % "dropbox-core-sdk" % DbxV
    , "com.janschulte" %% "akvokolekta" % "0.1.0-SNAPSHOT"
  )
}

resolvers += Resolver.sonatypeRepo("snapshots")
