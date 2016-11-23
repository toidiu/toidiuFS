package controllers

import java.io.InputStream
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.{ByteString, Timeout}
import fileUtils.FileService
import fileUtils.dbx.DbxService
import fileUtils.s3.S3Service
import play.api.mvc.{Action, Controller}
import utils.AppUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.{FiniteDuration, _}

/**
  * Created by toidiu on 11/2/16.
  */

class MainController extends Controller {
  implicit val timeout = new Timeout(20 seconds)
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def index = Action.async(Future(Ok("Hi")))

  def getFile(key: String) = Action.async { req =>
    DbxService.getFile(key).map { rep => Ok.chunked(rep) }
    //    S3Service.getFile(key).map { rep => Ok.chunked(rep) }
  }

  def getMeta(key: String) = Action.async { req =>
    val meta = for {
      d <- DbxService.getMeta(key)
      s <- S3Service.getMeta(key)
    } yield (d,s)

    meta.map { rep =>
      Ok(rep._1 +"|"+ rep._2)
    }
  }

  def postFile(key: String) = Action.async(AppUtils.streamBP) { req =>
    import utils.SplittableInputStream
    val mime = req.headers.get("Content-Type").getOrElse(throw new Exception("no mime type"))
    val length = req.headers.get("content-length").getOrElse(throw new Exception("no content length"))

    //get meta data
    val infoBytes: ByteString = FileService.buildMeta(mime, length)
    val infoStream = Source.single(infoBytes).runWith(StreamConverters.asInputStream(FiniteDuration(3, TimeUnit.SECONDS)))

    val inputStream: InputStream = req.body.runWith(StreamConverters.asInputStream(FiniteDuration(3, TimeUnit.SECONDS)))
    val s3IS = new SplittableInputStream(inputStream)
    val dbxIS = new java.io.SequenceInputStream(infoStream, s3IS.split)

    //save the file
    val s3 = S3Service.postFile(infoBytes, key, s3IS)
    val dbx = DbxService.postFile(infoBytes, key, dbxIS)
    val res = for {
      s <- s3
      d <- dbx
    } yield (s, d)
    s3IS.close()
    dbxIS.close()

    res.map(d => Ok(d.toString))
  }


}


