name := """toidiuFS"""

version := "0.0.1"

lazy val toidiufs = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"


libraryDependencies ++= {

  val akkaV = "2.4.10"
  val circeV = "0.6.1"
  val s3V = "1.11.55"
  val DbxV = "2.1.2"
  val commonsV = "2.5"
  val spec2V = "3.8.5"
  Seq(
    "com.typesafe.akka" %% "akka-slf4j" % akkaV
    , "com.typesafe.akka" %% "akka-stream" % akkaV

    , "com.amazonaws" % "aws-java-sdk-s3" % s3V
    , "com.dropbox.core" % "dropbox-core-sdk" % DbxV
    , "commons-io" % "commons-io" % commonsV

    , "io.circe" %% "circe-core" % circeV
    , "io.circe" %% "circe-generic" % circeV
    , "io.circe" %% "circe-parser" % circeV
    //    , "io.circe" %% "circe-jawn"

    , "commons-io" % "commons-io" % "2.4"

    , "org.specs2" %% "specs2-core" % spec2V % "test"
    , "org.specs2" %% "specs2-junit" % spec2V % "test"
  )


}

resolvers += Resolver.sonatypeRepo("snapshots")
