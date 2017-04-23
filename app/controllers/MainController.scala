package controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.Timeout
import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import logic.{FsMetaLogic, FsReadFileLogic, FsWriteFileLogic}
import play.api.mvc.{Action, Controller, Result}
import replicas.FileService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

/**
  * Created by toidiu on 11/2/16.
  */

class MainController extends Controller {
  implicit val t: Timeout = FileService.timeout
  implicit val s: ActorSystem = FileService.system
  implicit val m: ActorMaterializer = FileService.materializer

  def index = Action.async(Future(Ok("toidiufs")))

  def getFile(key: String) = Action.async { req =>
    val fut: Future[Result] = FsReadFileLogic.resultFile(key)
    fut.recover { case err => BadRequest(err.toString) }
  }

  def getMeta(key: String) = Action.async { req =>
    val fut = FsMetaLogic.resultMetaList(key)
    fut.recover { case err => BadRequest(err.toString) }
  }

  def postFile(key: String) = Action.async(parse.temporaryFile) { req =>
    val optMime = Try(req.headers.get("Content-Type").get)
    optMime match {
      case Success(mime) =>
        val length = req.body.file.length()
        val fut = FsWriteFileLogic.resultPostFile(key, mime, length, req.body)
        fut.recover { case e => BadRequest(e.getMessage) }
      case Failure(_) => Future(BadRequest("Error, specify the mime: Content-Type\n"))
    }
  }


}


