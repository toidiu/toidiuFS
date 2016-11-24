name := """toidiuFS"""

version := "0.0.1"

lazy val toidiufs = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"


libraryDependencies ++= {

  val akkaV = "2.4.10"
//  val catsV = "0.8.1"
  val circeV = "0.6.1"
  val s3V = "1.11.55"
  val DbxV: String = "2.1.2"
  Seq(
    "com.typesafe.akka" %% "akka-slf4j" % akkaV
    , "com.typesafe.akka" %% "akka-stream" % akkaV

//    , "org.typelevel" %% "cats" % catsV
    , "com.amazonaws" % "aws-java-sdk-s3" % s3V
    , "com.dropbox.core" % "dropbox-core-sdk" % DbxV
  ) ++
    Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
    //    , "io.circe" %% "circe-jawn"
    ).map(_ % circeV)

}

resolvers += Resolver.sonatypeRepo("snapshots")
