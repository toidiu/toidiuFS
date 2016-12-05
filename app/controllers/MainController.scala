package controllers

import java.io.FileInputStream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.{ByteString, Timeout}
import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import logic.FsReadLogic
import logic.FsWriteLogic.checkConfigAndLock
import play.api.http.HttpEntity
import play.api.mvc.{Action, Controller, ResponseHeader, Result}
import replicas.dbx.DbxService
import replicas.s3.S3Service
import utils.TimeUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success

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
      case Right((metaList, fsList, attemptResolution)) =>
        val getFileList = fsList.map(_.getFile(key))

        Future.sequence(getFileList)
          .map { f =>
            f.zip(metaList)
              .withFilter(_._1.isRight)
              .map(a => (a._1.right.get, a._2))
          }
          .map {
            case (byte, meta) :: t => Result(ResponseHeader(200), HttpEntity.Streamed(byte, Some(meta.bytes), Some(meta.mime)))
            case Nil => BadRequest("Error reading from server")
          }.andThen { case _ => attemptResolution.apply() }
      case Left(err) => Future(BadRequest(err))
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

    checkConfigAndLock(key, mime, length).flatMap { case Success(lockList) =>
      //attempt upload of file
      val ret = for (fs <- lockList) yield {
        val metaBytes = fs.buildMetaBytes(length, mime, TimeUtils.zoneAsString, key)
        fs.postFile(metaBytes, key, new FileInputStream(req.body.file))
      }
      Future.sequence(ret).map(resList => Ok(resList.toString()))
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


