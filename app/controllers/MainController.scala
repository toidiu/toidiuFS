package controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.{ByteString, Timeout}
import fileUtils.FileService
import fileUtils.dropbox.DBoxService
import fileUtils.s3.S3Service
import play.api.mvc.{Action, Controller}
import utils.AppUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Created by toidiu on 11/2/16.
  */

class MainController extends Controller {
  implicit val timeout = new Timeout(20 seconds)
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def index = Action.async(Future(Ok("Hi")))

  S3Service

  def getFile(key: String) = Action.async { req =>
    DBoxService.getFile(key).map { rep => Ok.chunked(rep) }
  }

  def getMeta(key: String) = Action.async { req =>
    DBoxService.getMeta(key).map { rep => Ok.chunked(rep) }
  }

  def postFile(key: String) = Action.async(AppUtils.streamBP) { req =>
    val mime = req.headers.get("Content-Type").getOrElse(throw new Exception("no mime type"))
    val length = req.headers.get("content-length").getOrElse(throw new Exception("no content length"))

    //get meta data
    val infoBytes: ByteString = FileService.buildMeta(mime, length)
    //save the file
    val s3 = S3Service.postFile(infoBytes, key, req.body)
    val db = DBoxService.postFile(infoBytes, key, req.body)
    for {
      s <- s3
      d <- db
    } yield s

    DBoxService.postFile(infoBytes, key, req.body).map { rep => Ok(rep.toString) }
  }
}


