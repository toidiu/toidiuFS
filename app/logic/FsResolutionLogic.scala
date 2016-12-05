package logic

import java.util.concurrent.TimeUnit

import akka.stream.scaladsl.StreamConverters
import akka.util.ByteString
import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import models.MetaServer
import replicas.FileService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

/**
  * Created by toidiu on 11/27/16.
  */
object FsResolutionLogic {
  implicit val t = FileService.timeout
  implicit val s = FileService.system
  implicit val m = FileService.materializer

  type Resolution = () => Future[List[Any]]

  def attemptResolution(key: String, metaServer: MetaServer, updatedAndNeedRes: (List[FileService], List[FileService])): Resolution = () => {
    val parCheckFsConfig = FsWriteLogic.isFsConfigValid(metaServer.mime, metaServer.bytes)

    updatedAndNeedRes match {
      case (hUp :: t, res) if res.nonEmpty =>
        hUp.getFile(key).flatMap {
          case Success(source) =>
            val is = source.runWith(StreamConverters.asInputStream(FiniteDuration(5, TimeUnit.SECONDS)))

            val futList = res.map(resFs => parCheckFsConfig(resFs) match {
              case true => resFs.postFile(ByteString(metaServer.asJson.noSpaces), key, is)
              case false => Future(Nil)
            })
            Future.sequence(futList)
          case Failure(err) => Future(Nil)
        }
      case _ => Future(Nil)
    }
  }

}
