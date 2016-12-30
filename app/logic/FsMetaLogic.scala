package logic

import java.io.{File, FileInputStream}

import akka.stream.scaladsl.StreamConverters
import akka.util.ByteString
import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import logic.FsResolutionLogic.{Resolution, ResolutionPart, attemptResolution}
import models.MetaServer
import play.api.http.HttpEntity
import play.api.mvc.{ResponseHeader, Result}
import replicas.FileService
import utils.AppUtils.ALL_SERVICES
import utils.ErrorUtils.FsReadException
import utils.TimeUtils.zoneFromString

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * Created by toidiu on 11/24/16.
  */
object FsMetaLogic {

  //-=-=-=-=-=-=-=-==-==-==-==-=-=-=-=-=-=-
  //META
  //-=-=-=-=-=-=-=-==-==-==-==-=-=-=-=-=-=-
  def resultMetaList(key: String): Future[Result] = {
    for {
      json <- getAllMetaList(key)
      body = HttpEntity.Strict(ByteString(json.get.noSpaces), Some("application/json"))
      ret <- Future(Result(ResponseHeader(200), body))
    } yield ret
  }

  private def getAllMetaList(key: String): Future[Try[Json]] = {
    Future.sequence(ALL_SERVICES.map(_.getMeta(key))).map { metaList =>
      val jsonList = for {
        metaEither <- metaList
        json <- List(if (metaEither.isRight) metaEither.right.get.asJson else metaEither.left.get.asJson)
      } yield json
      Success(jsonList.asJson)
    }
  }

}
