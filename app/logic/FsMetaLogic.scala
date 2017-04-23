package logic

import akka.util.ByteString
import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import models.MetaServer
import play.api.http.HttpEntity
import play.api.mvc.{ResponseHeader, Result}
import utils.AppUtils.ALL_SERVICES
import utils.ErrorUtils.MetaError

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Success, Try}

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
    def _resolveMetaJson(metaEither: Either[MetaError, MetaServer]): List[Json] = {
      List(if (metaEither.isRight) metaEither.right.get.asJson else metaEither.left.get.asJson)
    }

    Future.sequence(ALL_SERVICES.map(_.getMeta(key))).map { metaList =>
      val jsonList = for {
        metaEither <- metaList
        json <- _resolveMetaJson(metaEither)
      } yield json
      Success(jsonList.asJson)
    }
  }

}
