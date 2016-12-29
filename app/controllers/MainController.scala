package controllers

import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import logic.{FsReadLogic, FsWriteLogic}
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
  implicit val t = FileService.timeout
  implicit val s = FileService.system
  implicit val m = FileService.materializer

  def index = Action.async(Future(Ok("Hi")))

  def getFile(key: String) = Action.async { req =>
    val fut: Future[Result] = FsReadLogic.resultFile(key)
    fut.recover { case err => BadRequest(err.toString) }
  }

  def getMeta(key: String) = Action.async { req =>
    val fut = FsReadLogic.resultMetaList(key)
    fut.recover { case err => BadRequest(err.toString) }
  }

  def postFile(key: String) = Action.async(parse.temporaryFile) { req =>
    val optMime = Try(req.headers.get("Content-Type").get)
    optMime match {
      case Success(mime) =>
        val length = req.body.file.length()
        val fut = FsWriteLogic.resultPostFile(key, mime, length, req.body)
        fut.recover { case e => BadRequest(e.getMessage) }
      case Failure(_) => Future(BadRequest("Error: No Mime\n"))
    }
  }


}


