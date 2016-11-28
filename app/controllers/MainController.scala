package controllers

import java.io.FileInputStream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.Timeout
import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import logic.{FsReadLogic, FsWriteLogic}
import play.api.http.HttpEntity
import play.api.mvc.{Action, Controller, ResponseHeader, Result}
import replicas.dbx.DbxService
import replicas.s3.S3Service
import utils.{AppUtils, TimeUtils}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by toidiu on 11/2/16.
  */

class MainController extends Controller {
  implicit val timeout = new Timeout(20 seconds)
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def index = Action.async(Future(Ok("Hi")))

  def getFile(key: String) = Action.async { req =>
    FsReadLogic.getMostUpdatedServers(key).flatMap {
      case Right(fsMetaList) =>
        val metaList = fsMetaList._1
        val getFileList = fsMetaList._2.map(_.getFile(key))

        Future.sequence(getFileList)
          .map { f => f.zip(metaList)
            .filter(_._1.isRight)
            .map(a => (a._1.right.get, a._2))
          }.map {
          case h :: t =>
            Result(
              //              header = ResponseHeader(200, Map("Content-Disposition" -> ("attachment; filename=" + key + mimeToExtension(meta.mime)))),
              header = ResponseHeader(200, Map.empty),
              body = HttpEntity.Streamed(h._1, None, Some(h._2.mime))
            )
          case Nil =>
            BadRequest("Error reading from server")
        }
      case Left(err) => Future(BadRequest(err))
    }
  }

  def getMeta(key: String) = Action.async { req =>
    val futDecodeList = AppUtils.ALL_SERVICES.map(_.getMeta(key))

    val retList = Future.sequence(futDecodeList)
      .map { a =>
        a.map {
          case Right(r) => r.asJson
          case Left(l) => l.asJson
        }
      }
      .map(jsonList => Json.fromValues(jsonList).noSpaces)

    retList.map(d => Ok(d))
  }

  def postFile(key: String) = Action.async(parse.temporaryFile) { req =>
    val mime = req.headers.get("Content-Type").getOrElse(throw new Exception("no mime type"))
    val length = req.body.file.length()

    //check config restraints
    FsWriteLogic.checkConfigRestraints(mime, length) match {
      case Right(configList) =>
        //check for lock file/ availability
        FsWriteLogic.checkAndAcquireLock(key, configList).flatMap {
          case Right(lockList) =>
            //attempt upload of file
            val uploadTime: String = TimeUtils.zoneAsString
            val ret = for (i <- lockList) yield {
              val metaBytes = i.buildMetaBytes(length, mime, uploadTime, key)
              i.postFile(metaBytes, key, new FileInputStream(req.body.file))
            }
            Future.sequence(ret).map(d => Ok(d.toString()))
          case Left(err) => Future(BadRequest(err))
        }
      case Left(err) => Future(BadRequest(err))
    }
  }

  def acquireLock(key: String) = Action.async { req =>
    S3Service.acquireLock(key).flatMap(a =>
      DbxService.acquireLock(key).map(b => Ok(a.toString + b.toString))
    )
  }

  def getLockInfo(key: String) = Action.async { req =>
    S3Service.inspectOrCreateLock(key).flatMap(a =>
      DbxService.inspectOrCreateLock(key).map(b => Ok(a.toString + b.toString))
    )
  }

  def releaseLock(key: String) = Action.async { req =>
    S3Service.releaseLock(key).flatMap(a =>
      DbxService.releaseLock(key).map(b => Ok(a.toString + b.toString))
    )
  }


}


