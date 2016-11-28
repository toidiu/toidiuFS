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
import utils.AppUtils.ALL_SERVICES
import utils.TimeUtils.zoneFromString

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by toidiu on 11/24/16.
  */
object FsReadLogic {

  def getMostUpdatedServers(key: String): Future[Either[String, (List[MetaServer], List[FileService], () => Future[List[Any]])]] = {
    //get meta for all servers
    val futList = ALL_SERVICES.map(_.getMeta(key))
    Future.sequence(futList).map { futMetaList =>
      //filter most up-to-date
      val mostUpdated = ALL_SERVICES.zip(futMetaList)
        .filter(_._2.isRight)
        .map(a => (a._1, a._2.right.get))
        .foldLeft(Nil: List[(FileService, MetaServer)])(filterMostUpdated)
        .unzip

      mostUpdated match {
        case metaFsList if metaFsList._1.nonEmpty =>
          //attempt resolution
          val attemptRes = attemptResolution(key, metaFsList._2.head, ALL_SERVICES.partition(f => metaFsList._1.contains(f)))
          Right((metaFsList._2, metaFsList._1, attemptRes))
        case (Nil, Nil) => Left("No servers available to get file.")

      }
    }
  }


  private[logic] def filterMostUpdated(list: List[(FileService, MetaServer)], nxt: (FileService, MetaServer)): List[(FileService, MetaServer)] = {
    val nxtTime = zoneFromString(nxt._2.uploadedAt)
    list match {
      case h :: t if zoneFromString(h._2.uploadedAt).isAfter(nxtTime) => list
      case h :: t if zoneFromString(h._2.uploadedAt).isEqual(nxtTime) => nxt :: list
      case _ => List(nxt)
    }
  }

}
