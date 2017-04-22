enablePlugins(JavaAppPackaging)


name := "toidiufs"

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

resolvers ++= Seq(Resolver.sonatypeRepo("snapshots"))



//docker
dockerRepository := Some("toidiu")
packageName in Docker := dockerRepository.value.map(_ + "/").getOrElse("") + name.value
version in Docker := version.value

import com.amazonaws.regions.{Region, Regions}

region in ecr := Region.getRegion(Regions.US_EAST_1)
repositoryName in ecr := (packageName in Docker).value
localDockerImage in ecr := (packageName in Docker).value + ":" + (version in Docker).value

push in ecr <<= (push in ecr) dependsOn (publishLocal in Docker)

