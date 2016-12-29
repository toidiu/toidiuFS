package controllers

import java.io.FileInputStream

import akka.stream.scaladsl.StreamConverters
import akka.util.ByteString
import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import logic.FsReadLogic
import logic.FsWriteLogic.checkConfigAcquireLock
import play.api.http.HttpEntity
import play.api.mvc.{Action, Controller, ResponseHeader, Result}
import replicas.FileService
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
    val fut = for {
      read <- FsReadLogic.readFileFromServers(key)
      (file, meta, resolution) = read.get
      stream = StreamConverters.fromInputStream(() => new FileInputStream(file))
      body = HttpEntity.Streamed(stream, Some(meta.bytes), Some(meta.mime))
      ret <- Future(Result(ResponseHeader(200), body)).andThen { case _ => resolution(file) }
    } yield ret

    fut.recover { case err => BadRequest(err.toString) }
  }

  def getMeta(key: String) = Action.async { req =>
    val fut = for {
      json <- FsReadLogic.getAllMetaList(key)
      body = HttpEntity.Strict(ByteString(json.get.noSpaces), Some("application/json"))
      ret <- Future(Result(ResponseHeader(200), body))
    } yield ret

    fut.recover { case err => BadRequest(err.toString) }
  }

  def postFile(key: String) = Action.async(parse.temporaryFile) { req =>
    lazy val mime = req.headers.get("Content-Type").getOrElse(throw new Exception("no mime type"))
    val length = req.body.file.length()

    val fut = checkConfigAcquireLock(key, mime, length).flatMap {
      case Success(lockList) =>
        //attempt upload of file
        val listFut = for (fs <- lockList) yield {
          val metaBytes = fs.buildMetaBytes(length, mime, TimeUtils.zoneAsString, key)
          fs.postFile(metaBytes, key, new FileInputStream(req.body.file))
        }
        Future.sequence(listFut).map(resList => Ok(resList.toString))
      case Failure(err) => Future(BadRequest(err.getMessage))
    }

    fut.recover { case e => BadRequest(e.getMessage) }
  }


}


