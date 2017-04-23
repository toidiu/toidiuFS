package replicas

import java.io.{File, InputStream}

import akka.stream.scaladsl.Source
import akka.util.ByteString
import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import models.{FSLock, MetaServer}
import utils.ErrorUtils.MetaError
import utils.StatusUtils.PostFileStatus

import scala.concurrent.Future
import scala.util.Try

/**
  * Created by toidiu on 11/17/16.
  */
class MockService extends FileService {

  override val isEnable: Boolean = true
  override val isWhiteList: Boolean = false
  override val mimeList: List[String] = Nil
  override val maxLength: Long = 1000000000
  override val serviceName: String = "mockService"

  override def postFile(meta: ByteString, key: String, inputStream: InputStream): Future[Try[PostFileStatus]] = {
    ???
  }

  override def getFile(key: String): Future[Try[File]] = {
    ???
  }

  override def getMeta(key: String): Future[Either[MetaError, MetaServer]] = {
    ???
  }

  override def buildMetaBytes(bytes: Long, mime: String,
                              uploadedAt: String, key: String): ByteString = {
    ???
  }

  //-=-=-=-=-=-=-=-==-==-==-==-=-=-=-=-=-=-
  //Lock
  //-=-=-=-=-=-=-=-==-==-==-==-=-=-=-=-=-=-
  override def inspectOrCreateLock(key: String): Future[Try[FSLock]] = {
    ???
  }

  override def createLock(key: String): Future[Try[FSLock]] = {
    ???
  }

  override def acquireLock(key: String): Future[Try[FSLock]] = {
    ???
  }

  override def releaseLock(key: String): Future[Try[FSLock]] = {
    ???
  }

}


