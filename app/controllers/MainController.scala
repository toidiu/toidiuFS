package controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.{ByteString, Timeout}
import fileUtils.dropbox.DBoxService
import play.api.libs.streams.Accumulator
import play.api.mvc.{Action, BodyParser, Controller}

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

  def getFile(key: String) = Action.async { req =>
    println(req)

    Future(Ok("Hi"))
  }

  def verbatimBodyParser: BodyParser[Source[ByteString, _]] = BodyParser { _ =>
    // Return the source directly. We need to return
    // an Accumulator[Either[Result, T]], so if we were
    // handling any errors we could map to something like
    // a Left(BadRequest("error")). Since we're not
    // we just wrap the source in a Right(...)
    Accumulator.source[ByteString]
      .map(Right.apply)
  }


  def postFile(key: String) = Action.async(verbatimBodyParser) { req =>
    val mime = req.headers.get("Content-Type").getOrElse(throw new Exception("no mime type"))
    val length = req.headers.get("content-length").getOrElse(throw new Exception("no content length"))

    println("-=-=1")
    //prepend info bytes to the file stream
    val join: String = List(mime, length).mkString(",")
    val mimeLengthBytes = ByteString(join)
    val infoBytes = mimeLengthBytes ++ ByteString.fromArray(new Array[Byte](150 - mimeLengthBytes.size))
    println(infoBytes.length)
    println(infoBytes.utf8String)

    //save the file
    val saveStream = Source.single(infoBytes).concat(req.body)
    DBoxService.uploadFile("/" + key, saveStream).map { r =>
      Ok(r.toString)
    }
  }

  def postMeta(key: String) = Action.async { req =>
    println(req)

    Future(Ok("Hi"))
  }
}


