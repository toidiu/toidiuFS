package logic

import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import models.MetaServer
import replicas.FileService
import replicas.dbx.DbxService
import replicas.s3.S3Service
import utils.AppUtils
import utils.TimeUtils.zoneFromString
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

/**
  * Created by toidiu on 11/24/16.
  */
object FsReadLogic {

  def getMostUpdatedServers(key: String): Future[Either[String, List[(FileService, MetaServer)]]] = {
    val services: Seq[FileService] = Seq(DbxService, S3Service)
    //get meta for all servers
    val futList = services.map(_.getMeta(key))
    Future.sequence(futList).map { futMetaList =>
      val metaList = futMetaList.map(m => decode[MetaServer](m))
      //filter most up-to-date
      val mostUpdated = services.zip(metaList).view
        .filter(_._2.isRight)
        .map(a => (a._1, a._2.right.get))
        .foldLeft(Nil: List[(FileService, MetaServer)])(filterMostUpdated)

      mostUpdated match {
        case fsList => Right(fsList)
        case Nil => Left("No servers available to get file.")
      }
    }
  }


  private def filterMostUpdated(list: List[(FileService, MetaServer)], nxt: (FileService, MetaServer)): List[(FileService, MetaServer)] = {
    val elemTime = zoneFromString(nxt._2.uploadedAt)
    list match {
      case h :: t if zoneFromString(h._2.uploadedAt).isAfter(elemTime) => list
      case h :: t if zoneFromString(h._2.uploadedAt).isEqual(elemTime) => nxt :: list
      case _ => List(nxt)
    }
  }

}
