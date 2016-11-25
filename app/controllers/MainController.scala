package controllers

import java.io.FileInputStream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.Timeout
import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import logic.DataStoreLogic
import models._
import play.api.http.HttpEntity
import play.api.mvc.{Action, Controller, ResponseHeader, Result}
import replicas.dbx.DbxService
import replicas.s3.S3Service
import utils.{FutureUtil, TimeUtils}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
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

    FutureUtil.first(Seq(s3, dbx)).flatMap {
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
      d <- DbxService.getMetaString(key)
      s <- S3Service.getMetaString(key)
    } yield (d, s)

    meta.map(strTup => (decode[DbxMeta](strTup._1), decode[S3Meta](strTup._2))).map {
      case (Right(d), Right(s)) => Ok(Meta((d, s)).asJson.spaces2)
      case (Right(d), Left(s)) => Ok(Json.arr(d.asJson, MetaError("s3", s.getMessage).asJson).spaces2)
      case (Left(d), Right(s)) => Ok(Json.arr(s.asJson, MetaError("dropbox", d.getMessage).asJson).spaces2)
      case (Left(d), Left(s)) =>
        val dJson = MetaError("dropbox", d.toString).asJson
        val sJson = MetaError("s3", s.getMessage).asJson
        BadRequest(Json.arr(dJson, sJson).spaces2)
    }
  }

  def postFile(key: String) = Action.async(parse.temporaryFile) { req =>


    val mime = req.headers.get("Content-Type").getOrElse(throw new Exception("no mime type"))
    val length = req.body.file.length()

    val futConfig = DataStoreLogic.configFilter(mime, length) match {
      case Right(list) =>


        //check for lock file
        //check for server availability



        //attempt upload of file
      val uploadTime: String = TimeUtils.zoneAsString
        val f = for (i <- list) yield {
          val metaBytes = i.buildMetaBytes(length, mime, uploadTime, key)
          i.postFile(metaBytes, key, new FileInputStream(req.body.file))
        }
        Future.sequence(f).map(d => Ok)

      case Left(err) => Future(BadRequest(err))
    }

    futConfig
  }


}


