package logic

import java.io.{File, FileInputStream}

import akka.stream.scaladsl.StreamConverters
import akka.util.ByteString
import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import logic.FsResolutionLogic.{Resolution, ResolutionPart, attemptResolution}
import models.MetaServer
import play.api.http.HttpEntity
import play.api.mvc.{ResponseHeader, Result}
import replicas.FileService
import utils.AppUtils.ALL_SERVICES
import utils.ErrorUtils.FsReadException
import utils.TimeUtils.zoneFromString

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * Created by toidiu on 11/24/16.
  */
object FsReadLogic {

  //-=-=-=-=-=-=-=-==-==-==-==-=-=-=-=-=-=-
  //META
  //-=-=-=-=-=-=-=-==-==-==-==-=-=-=-=-=-=-
  def resultMetaList(key: String): Future[Result] = {
    for {
      json <- FsReadLogic.getAllMetaList(key)
      body = HttpEntity.Strict(ByteString(json.get.noSpaces), Some("application/json"))
      ret <- Future(Result(ResponseHeader(200), body))
    } yield ret
  }

  private def getAllMetaList(key: String): Future[Try[Json]] = {
    Future.sequence(ALL_SERVICES.map(_.getMeta(key))).map { metaList =>
      val jsonList = for {
        metaEither <- metaList
        json <- List(if (metaEither.isRight) metaEither.right.get.asJson else metaEither.left.get.asJson)
      } yield json
      Success(jsonList.asJson)
    }
  }

  //-=-=-=-=-=-=-=-==-==-==-==-=-=-=-=-=-=-
  //FILE
  //-=-=-=-=-=-=-=-==-==-==-==-=-=-=-=-=-=-
  def resultFile(key: String): Future[Result] = {
    for {
      read <- FsReadLogic.readFileFromServers(key)
      (file, meta, resolution) = read.get
      stream = StreamConverters.fromInputStream(() => new FileInputStream(file))
      body = HttpEntity.Streamed(stream, Some(meta.bytes), Some(meta.mime))
      ret <- Future(Result(ResponseHeader(200), body)).andThen { case _ => resolution.apply() }
    } yield ret
  }


  private def readFileFromServers(key: String): Future[Try[(File, MetaServer, Resolution)]] = {
    getMostUpdatedServers(key).flatMap {
      case Success((metaList, fsList, resolution)) =>
        Future.sequence(fsList.map(_.getFile(key)))
          .map { fileList =>
            val l = fileList.zip(metaList)
              .filter { case (file, meta) => file.isSuccess }
              .map { case (file, meta) => Success(file.get, meta) }

            val fileListSucc = l.collect { case (Success(s)) => s._1 }
            l.head.map { e => (e._1, e._2, resolution(fileListSucc)) }
          }
      case Failure(err) => Future(Failure(err))
    }
  }

  private def getMostUpdatedServers(key: String): Future[Try[(List[MetaServer], List[FileService], ResolutionPart)]] = {
    val zipFsMeta = Future.sequence(ALL_SERVICES.map(_.getMeta(key)))
      .map(metaEither => ALL_SERVICES.zip(metaEither))

    //get meta for all servers
    zipFsMeta.map { futTup =>
      //filter most up-to-date
      val mostUpdated = futTup
        .withFilter { case (fs, meta) => meta.isRight }
        .map { case (fs, meta) => (fs, meta.right.get) }
        .foldLeft(Nil: List[(FileService, MetaServer)])(filterMostUpdatedService)
        .unzip

      mostUpdated match {
        case (fsList, metaList) if fsList.nonEmpty =>
          val funcResolution = attemptResolution(key, metaList.head, ALL_SERVICES.filterNot(_.equals(fsList)))(_)
          Success((metaList, fsList, funcResolution))
        case (Nil, Nil) => Failure(new FsReadException("No servers available to get file."))
      }
    }
  }


  private[logic] def filterMostUpdatedService(list: List[(FileService, MetaServer)], nxt: (FileService, MetaServer)): List[(FileService, MetaServer)] = {
    val nxtTime = zoneFromString(nxt._2.uploadedAt)
    list match {
      case (_, meta) :: t if zoneFromString(meta.uploadedAt).isAfter(nxtTime) => list
      case (_, meta) :: t if zoneFromString(meta.uploadedAt).isEqual(nxtTime) => nxt :: list
      case _ => List(nxt)
    }
  }

}
