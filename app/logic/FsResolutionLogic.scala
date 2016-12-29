package logic

import java.io.{File, FileInputStream}

import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import models.MetaServer
import replicas.FileService
import utils.TimeUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by toidiu on 11/27/16.
  */
object FsResolutionLogic {
  implicit val t = FileService.timeout
  implicit val s = FileService.system
  implicit val m = FileService.materializer

  type Resolution = File => Future[List[Any]]

  def attemptResolution(key: String, meta: MetaServer, updated: FileService, needsRes: List[FileService]): Resolution = (file: File) => {
    val parCheckFsConfig = FsWriteLogic.isFsConfigValid(meta.mime, meta.bytes)

    needsRes match {
      case (resList) if resList.nonEmpty =>
        //        updated.getFile(key).flatMap {
        //          case Success(file) =>
        val futList = resList.map(res => if (parCheckFsConfig(res)) {
          //              val is = source.runWith(StreamConverters.asInputStream(FiniteDuration(5, TimeUnit.SECONDS)))
          val is = new FileInputStream(file)
          val metaBytes = res.buildMetaBytes(meta.bytes, meta.mime, TimeUtils.zoneAsString, key)
          res.postFile(metaBytes, key, is).map(_ => file.delete())
        } else Future(Nil))
        Future.sequence(futList)
      //          case Failure(err) => Future(Nil)
      //        }
      case _ => Future(Nil)
    }
  }

}
