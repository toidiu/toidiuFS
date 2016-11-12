package controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.{ByteString, Timeout}
import fileUtils.dropbox.DBoxService
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
  val bufferByte: Int = 150

  def index = Action.async(Future(Ok("Hi")))

  def getFile(key: String) = Action.async { req =>
    DBoxService.getFile(key).map { rep =>
      var one = true
      val fileStream: Source[ByteString, _] = rep.body.map(bs =>
        if (one) {
          one = false;
          bs.drop(bufferByte)
        }
        else bs
      )
      Ok.chunked(fileStream)
    }
  }

  def getMeta(key: String) = Action.async { req =>
    DBoxService.getFile(key).map { rep =>
      val fileStream: Source[ByteString, _] = rep.body.take(1).map(bs => bs.take(bufferByte))
      Ok.chunked(fileStream)
    }
  }

  def postFile(key: String) = Action.async(AppUtils.streamBP) { req =>
    val mime = req.headers.get("Content-Type").getOrElse(throw new Exception("no mime type"))
    val length = req.headers.get("content-length").getOrElse(throw new Exception("no content length"))

    println("-=-=1")
    //prepend info bytes to the file stream
    val join: String = List(mime, length).mkString(",")
    val mimeLengthBytes = ByteString(join)
    val infoBytes = mimeLengthBytes ++ ByteString.fromArray(new Array[Byte](bufferByte - mimeLengthBytes.size))
    println(infoBytes.length)
    println(infoBytes.utf8String)

    //save the file
    val saveStream = Source.single(infoBytes).concat(req.body)
    //    val saveStream = req.body
    DBoxService.uploadFile(key, saveStream).map { rep =>
      Ok(rep.toString)
    }
  }
}


