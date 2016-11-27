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
import logic.FsGeneralLogic.mimeToExtension
import logic.{FsReadLogic, FsWriteLogic}
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

    //fixme check its its enabled... and also abstract out to list
    FsReadLogic.getMostUpdatedServers(key).flatMap {
      case Right(fsMetaList) =>
        //because all the most recent versions should have the same meta
        val meta = fsMetaList.head._2
        val getFileList = fsMetaList.map(_._1.getFile(key))

        FutureUtil.first(getFileList).flatMap {
          case (Success(res), _) =>
            Future(Result(
//              header = ResponseHeader(200, Map("Content-Disposition" -> ("attachment; filename=" + key + mimeToExtension(meta.mime)))),
              header = ResponseHeader(200, Map.empty),
              body = HttpEntity.Streamed(res, None, Some(meta.mime))
            ))

          case (Failure(err), res) => res.head.map(e => BadRequest(err.toString))
        }
      case Left(err) => Future(BadRequest(err))
    }
  }

  def getMeta(key: String) = Action.async { req =>

    //fixme check its its enabled... and also abstract out to list
    val meta = for {
      d <- DbxService.getMeta(key)
      s <- S3Service.getMeta(key)
    } yield (d, s)

    meta.map(strTup => (decode[MetaServer](strTup._1), decode[MetaServer](strTup._2))).map {
      case (Right(d), Right(s)) => Ok(Meta(d, s).asJson.spaces2)
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

    //check config restraints
    FsWriteLogic.checkConfigRestraints(mime, length) match {
      case Right(configList) =>
        //check for lock file/ availability
        FsWriteLogic.checkAndAcquireLock(key, configList).flatMap {
          case Right(lockList) =>
            //attempt upload of file
            val uploadTime: String = TimeUtils.zoneAsString
            val ret = for (i <- lockList) yield {
              val metaBytes = i.buildMetaBytes(length, mime, uploadTime, key)
              i.postFile(metaBytes, key, new FileInputStream(req.body.file))
            }
            Future.sequence(ret).map(d => Ok(d.toString()))
          case Left(err) => Future(BadRequest(err))
        }
      case Left(err) => Future(BadRequest(err))
    }
  }

  def acquireLock(key: String) = Action.async { req =>
    S3Service.acquireLock(key).flatMap(a =>
      DbxService.acquireLock(key).map(b => Ok(a.toString + b.toString))
    )
  }

  def getLockInfo(key: String) = Action.async { req =>
    S3Service.inspectOrCreateLock(key).flatMap(a =>
      DbxService.inspectOrCreateLock(key).map(b => Ok(a.toString + b.toString))
    )
  }

  def releaseLock(key: String) = Action.async { req =>
    S3Service.releaseLock(key).flatMap(a =>
      DbxService.releaseLock(key).map(b => Ok(a.toString + b.toString))
    )
  }


}


