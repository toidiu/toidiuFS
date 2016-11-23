package controllers

import java.io.InputStream
import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source, StreamConverters}
import akka.util.{ByteString, Timeout}
import fileUtils.FileService
import fileUtils.dbx.DbxService
import fileUtils.s3.S3Service
import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import models._
import play.api.http.HttpEntity
import play.api.mvc.{Action, Controller, ResponseHeader, Result}
import utils.{AppUtils, FutureUtil, SplittableInputStream, TimeUtils}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
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

    FutureUtil.first(Seq(s3, s3)).flatMap {
      //      case (Success(res), _) => Future(Ok.chunked(res))
      case (Success(res), _) =>
        Future(Result(
          header = ResponseHeader(200, Map.empty),
          body = HttpEntity.Streamed(res, None, Some("image/png"))
        ))
      case (Failure(err), res) => res.head.map(e => Ok.chunked(e))
    }
  }

  def getMeta(key: String) = Action.async { req =>
    val meta = for {
      d <- DbxService.getMeta(key)
      s <- S3Service.getMeta(key)
    } yield (d, s)



    meta.map(strTup => (decode[DbxMeta](strTup._1), decode[S3Meta](strTup._2))).map {
      case (Right(a), Right(b)) => val ret = Meta((a, b)).asJson.spaces2; Ok(ret)
      case (Right(a), Left(b)) => val ret = a.asJson.spaces2; Ok(ret + "\n" + b.toString)
      case (Left(a), Right(b)) => val ret = b.asJson.spaces2; Ok(a.toString + "\n" + ret)
      case (Left(a), Left(b)) => BadRequest(a.toString + "\n" + b.toString)
    }
  }


  def postFile(key: String) = Action.async(AppUtils.streamBP) {
    req =>

      val reqs = Source(0 to 10)
      val otherSink: Sink[Int, NotUsed] =
        Flow[Int].alsoTo(Sink.foreach(println(_))).to(Sink.foreach(println))
      //req.runWith(otherSink)
      val s = reqs.runWith(otherSink)
//      s.run()



//      req.body.mapMaterializedValue(d => (d, d))
//      val otherSink: Sink[ByteString, NotUsed] = Flow[ByteString].alsoTo(Sink.foreach(println(_))).to(Sink.ignore)
//      req.body.runWith(otherSink)
//      req.body.to(otherSink).run()

      val mime = req.headers.get("Content-Type").getOrElse(throw new Exception("no mime type"))
      val length = req.headers.get("content-length").getOrElse(throw new Exception("no content length"))
      require(length forall Character.isDigit)

      //get meta data
      val uploadTime: String = TimeUtils.zoneAsString
      val dbxMeta: DbxMeta = DbxMeta(length.toLong, mime, uploadTime, "dropbox", DbxDteail(DbxService.getPath(key)))
      val dbxBytes = FileService.buildDbxMeta(dbxMeta)
      val s3Meta: S3Meta = S3Meta(length.toLong, mime, uploadTime, "s3", S3Dteail(AppUtils.s3Bucket, key))
      val s3Bytes: ByteString = FileService.buildS3Meta(s3Meta)

      //file stream


      val inputStream: InputStream = req.body.runWith(StreamConverters.asInputStream(FiniteDuration(5, TimeUnit.SECONDS)))
            val s3IS = new SplittableInputStream(inputStream)
//            val dbxIS = s3IS.split

      //save the file
//      val s3 = S3Service.postFile(s3Bytes, key, inputStream)
      val s3 = S3Service.postFile(s3Bytes, key, s3IS)
//      val dbx = DbxService.postFile(dbxBytes, key, dbxIS)
      val res = for {
        s <- s3
//        d <- dbx
      } yield (s)
//      inputStream.close()
//            s3IS.close()
//            dbxIS.close()

      res.map(d => Ok(d.toString))
  }


}


