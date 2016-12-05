package logic

import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import logic.FsResolutionLogic.attemptResolution
import models.MetaServer
import replicas.FileService
import utils.AppUtils
import utils.AppUtils.ALL_SERVICES
import utils.TimeUtils.zoneFromString

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by toidiu on 11/24/16.
  */
object FsReadLogic {

  def getAllMetaList(key: String): Future[Json] = {
    val futDecodeList = AppUtils.ALL_SERVICES.map(_.getMeta(key))
    Future.sequence(futDecodeList)
      .map { metaObj =>
        metaObj.map {
          case Right(meta) => meta.asJson
          case Left(metaError) => metaError.asJson
        }
      }
      .map(jsonList => Json.fromValues(jsonList))
  }

  def getMostUpdatedServers(key: String): Future[Either[String, (List[MetaServer], List[FileService], () => Future[List[Any]])]] = {
    //get meta for all servers
    val futList = ALL_SERVICES.map(_.getMeta(key))
    Future.sequence(futList).map { futMetaList =>
      //filter most up-to-date
      val mostUpdated = ALL_SERVICES.zip(futMetaList)
        .withFilter(_._2.isRight)
        .map(a => (a._1, a._2.right.get))
        .foldLeft(Nil: List[(FileService, MetaServer)])(filterMostUpdated)
        .unzip

      mostUpdated match {
        case (fsList, metaList) if fsList.nonEmpty =>
          //attempt resolution
          val attemptRes = attemptResolution(key, metaList.head, ALL_SERVICES.partition(f => fsList.contains(f)))
          Right((metaList, fsList, attemptRes))
        case (Nil, Nil) => Left("No servers available to get file.")

      }
    }
  }


  private[logic] def filterMostUpdated(list: List[(FileService, MetaServer)], nxt: (FileService, MetaServer)): List[(FileService, MetaServer)] = {
    val nxtTime = zoneFromString(nxt._2.uploadedAt)
    list match {
      case (_, meta) :: t if zoneFromString(meta.uploadedAt).isAfter(nxtTime) => list
      case (_, meta) :: t if zoneFromString(meta.uploadedAt).isEqual(nxtTime) => nxt :: list
      case _ => List(nxt)
    }
  }

}
