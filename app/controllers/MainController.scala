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
import models.{Meta, MetaDate}
import play.api.mvc.{Action, Controller}
import utils.{AppUtils, FutureUtil}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.{FiniteDuration, _}
import scala.util.{Failure, Success}

/**
  * Created by toidiu on 11/2/16.
  */

class MainController extends Controller {
  implicit val timeout = new Timeout(20 seconds)
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def index = Action.async(Future(Ok("Hi")))

  def getFile(key: String) = Action.async { req =>
    val dbx = DbxService.getFile(key)
    val s3 = S3Service.getFile(key)

    FutureUtil.first(Seq(dbx, s3)).flatMap {
      case (Success(res), _) => Future(Ok.chunked(res))
      case (Failure(err), res) => res.head.map(e => Ok.chunked(e))
    }
  }

  def getMeta(key: String) = Action.async { req =>
    val meta = for {
      d <- DbxService.getMeta(key)
      s <- S3Service.getMeta(key)
    } yield (d, s)

    meta.map { rep =>
      Ok("[" + rep._1 + "," + rep._2 + "]")
    }
  }

  def postFile(key: String) = Action.async(AppUtils.streamBP) { req =>
    import utils.SplittableInputStream
    val mime = req.headers.get("Content-Type").getOrElse(throw new Exception("no mime type"))
    val length = req.headers.get("content-length").getOrElse(throw new Exception("no content length"))
    val meta = Meta(length.toLong, mime, MetaDate.asString)

    //get meta data
    val infoBytes: ByteString = FileService.buildMeta(meta)
    val infoStream = Source.single(infoBytes).runWith(StreamConverters.asInputStream(FiniteDuration(5, TimeUnit.SECONDS)))

    //file stream
    val inputStream: InputStream = req.body.runWith(StreamConverters.asInputStream(FiniteDuration(5, TimeUnit.SECONDS)))
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


