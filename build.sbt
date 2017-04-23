enablePlugins(JavaAppPackaging)


name := "toidiufs"

version := "0.0.4"

lazy val toidiufs = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.2"


libraryDependencies ++= {

  val akkaV = "2.5.0"
  val circeV = "0.7.1"
  val s3V = "1.11.123"
  val DbxV = "3.0.2"
  val commonsV = "2.5"
  val spec2V = "3.8.9"
  Seq(
    "com.typesafe.akka" %% "akka-slf4j" % akkaV
    , "com.typesafe.akka" %% "akka-stream" % akkaV

    , "com.amazonaws" % "aws-java-sdk-s3" % s3V
    , "com.dropbox.core" % "dropbox-core-sdk" % DbxV
    , "commons-io" % "commons-io" % commonsV

    , "io.circe" %% "circe-core" % circeV
    , "io.circe" %% "circe-generic" % circeV
    , "io.circe" %% "circe-parser" % circeV

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

enablePlugins(EcrPlugin)
region           in ecr := Region.getRegion(Regions.US_EAST_1)
repositoryName   in ecr := (packageName in Docker).value
localDockerImage in ecr := (packageName in Docker).value + ":" + (version in Docker).value
version          in ecr := (version in Docker).value

// Create the repository before authentication takes place (optional)
login in ecr <<= (login in ecr) dependsOn (createRepository in ecr)

// Authenticate and publish a local Docker image before pushing to ECR
push in ecr <<= (push in ecr) dependsOn (publishLocal in Docker, login in ecr)

