package logic

import java.io.{File, FileInputStream}

import akka.stream.scaladsl.StreamConverters
import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import logic.FsResolutionLogic.{Resolution, ResolutionPart, partResolution}
import models.MetaServer
import play.api.http.HttpEntity
import play.api.mvc.Results.BadRequest
import play.api.mvc.{ResponseHeader, Result}
import replicas.FileService
import utils.AppUtils.ALL_SERVICES
import utils.ErrorUtils.{FsReadException, MetaError}
import utils.TimeUtils.zoneFromString

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * Created by toidiu on 11/24/16.
  */
object FsReadFileLogic {

  def resultFile(key: String): Future[Result] = {
    val fut = for {
      (file, meta, resolution) <- readFileFromServers(key)
      stream = StreamConverters.fromInputStream(() => new FileInputStream(file))
      body = HttpEntity.Streamed(stream, Some(meta.bytes), Some(meta.mime))
      ret <- Future(Result(ResponseHeader(200), body)).andThen { case _ => resolution.apply() }
    } yield ret

    fut.recover { case err: Exception => BadRequest(s"Failure. ${err.getMessage}") }
  }

  private def readFileFromServers(key: String): Future[(File, MetaServer, Resolution)] = {
    val updatedWithResolutionPart = for {
      fsListTry <- getUpdatedFsAndResolutionPart(key)
      if fsListTry.isSuccess
      (metaList, fsList, resolutionPart) = fsListTry.get
      fileList <- Future.sequence(fsList.map(_.getFile(key)))
      zipped = fileList.zip(metaList)
    } yield for {
      (fsTry, meta) <- zipped
      if fsTry.isSuccess
    } yield (fsTry.get, meta, resolutionPart)

    updatedWithResolutionPart.map { fsMetaResList =>
      val fsList = fsMetaResList.map(_._1)
      fsMetaResList match {
        case (file, meta, resPart) :: t => (file, meta, resPart(fsList))
        case Nil => throw new FsReadException("No servers available to read file.")
      }
    }
  }

  private def getUpdatedFsAndResolutionPart(key: String): Future[Try[(List[MetaServer], List[FileService], ResolutionPart)]] = {
    val zipFsMeta = for {
      q <- Future.sequence(ALL_SERVICES.map(_.getMeta(key)))
    } yield ALL_SERVICES.zip(q)

    for {
      zipped <- zipFsMeta
      (updatedFs, updatedMeta) <- Future(getUpdatedFs(key, zipped))
      resolutionPart <- Future(buildResolution(key, updatedFs, updatedMeta))
    } yield Success(updatedMeta, updatedFs, resolutionPart)

  }

  private def getUpdatedFs(key: String, futTup: List[(FileService, Either[MetaError, MetaServer])]) = {
    val successfulMeta = for {
      fsMeta <- futTup
      (fs, meta) = fsMeta
      if meta.isRight
    } yield (fs, meta.right.get)

    val mostUpdated = successfulMeta.foldLeft(Nil: List[(FileService, MetaServer)])(filterMostUpdatedService).unzip
    require(mostUpdated._1.nonEmpty, "Unable to read servers, failed while trying to read meta.")
    mostUpdated
  }

  private[logic] def filterMostUpdatedService(list: List[(FileService, MetaServer)], nxt: (FileService, MetaServer)): List[(FileService, MetaServer)] = {
    val nxtTime = zoneFromString(nxt._2.uploadedAt)
    list match {
      case (_, meta) :: t if zoneFromString(meta.uploadedAt).isAfter(nxtTime) => list
      case (_, meta) :: t if zoneFromString(meta.uploadedAt).isEqual(nxtTime) => nxt :: list
      case _ => List(nxt)
    }
  }

  private def buildResolution(key: String, updatedFs: List[FileService], updatedMeta: List[MetaServer]): ResolutionPart = {
        val notUpToDate = ALL_SERVICES.filter(fs => !updatedFs.contains(fs))

        println("-=-=-")
        println(notUpToDate)
        val funcResolution = partResolution(key, updatedMeta.head, notUpToDate)
        println("-=-=-")
        println(funcResolution)
        funcResolution
  }

}
