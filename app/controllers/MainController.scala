package controllers

import java.io.FileInputStream

import akka.util.ByteString
import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import logic.FsReadLogic
import logic.FsWriteLogic.fsListCheckConfigAndLock
import play.api.http.HttpEntity
import play.api.mvc.{Action, Controller, ResponseHeader, Result}
import replicas.FileService
import replicas.dbx.DbxService
import replicas.s3.S3Service
import utils.TimeUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
  * Created by toidiu on 11/2/16.
  */

class MainController extends Controller {
  implicit val t = FileService.timeout
  implicit val s = FileService.system
  implicit val m = FileService.materializer

  def index = Action.async(Future(Ok("Hi")))

  def getFile(key: String) = Action.async { req =>
    FsReadLogic.readFileFromServers(key).flatMap {
      case Success((byte, meta, resolution)) =>
        Future(Result(ResponseHeader(200), HttpEntity.Streamed(byte, Some(meta.bytes), Some(meta.mime))))
          .andThen { case _ => resolution.apply() }
      case Failure(err) => Future(BadRequest(err.toString))
    }
  }

  def getMeta(key: String) = Action.async { req =>
    FsReadLogic.getAllMetaList(key).map { json =>
      Result(ResponseHeader(200), HttpEntity.Strict(ByteString(json.noSpaces), Some("application/json")))
    }
  }

  def postFile(key: String) = Action.async(parse.temporaryFile) { req =>
    val mime = req.headers.get("Content-Type").getOrElse(throw new Exception("no mime type"))
    val length = req.body.file.length()

    fsListCheckConfigAndLock(key, mime, length).flatMap {
      case Success(lockList) =>
        //attempt upload of file
        val ret = for (fs <- lockList) yield {
          val metaBytes = fs.buildMetaBytes(length, mime, TimeUtils.zoneAsString, key)
          fs.postFile(metaBytes, key, new FileInputStream(req.body.file))
        }
        Future.sequence(ret).map(resList => Ok(resList.toString()))
      case Failure(err) => Future(BadRequest(err.getMessage))
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


