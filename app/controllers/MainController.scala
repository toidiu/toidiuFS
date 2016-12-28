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
import logic.FsWriteLogic.fsListCheckConfigAndLock
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
      if read.isSuccess
      file = read.get._1
      meta = read.get._2
      resol = read.get._3
      stream = StreamConverters.fromInputStream(() => new FileInputStream(file))
      body = HttpEntity.Streamed(stream, Some(meta.bytes), Some(meta.mime))
      ret <- Future(Result(ResponseHeader(200), body))
      resolve <- resol.apply()
    } yield ret

    fut.recover { case err => BadRequest(err.toString) }
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
        Future.sequence(ret).map(resList => Ok(resList.toString))
      case Failure(err) => Future(BadRequest(err.getMessage))
    }
  }

}


