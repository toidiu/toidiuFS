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


//  def postFile(key: String) = Action.async(verbatimBodyParser) { req =>
  def postFile(key: String) = Action.async(parse.temporaryFile) { req =>
    println(req.headers.get("Content-Type").getOrElse("no mime"))
    println(req.headers.get("content-length").getOrElse("no length"))
    println(req.body)

    DBoxService.uploadFile("test.txt", req.body.file).map { r =>
      println("-=-=2")
      println(r)
      Ok.chunked(r.body)
//      Ok(r.body)
    }

//        Future(Ok.chunked(req.body))

    //    req.body.asBytes().ast

    //    Future(Ok("Hi"))
  }

  def postMeta(key: String) = Action.async { req =>
    println(req)

    Future(Ok("Hi"))
  }
}


