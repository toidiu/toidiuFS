package replicas

import java.io.InputStream

import akka.stream.scaladsl.Source
import akka.util.ByteString
import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import models.{FSLock, MetaError, MetaServer}

import scala.concurrent.Future

/**
  * Created by toidiu on 11/17/16.
  */
abstract class MockAbstractService extends FileService {

  override def postFile(meta: ByteString, key: String, inputStream: InputStream): Future[Either[_, Boolean]] = {
    ???
  }

  override def getFile(key: String): Future[Either[String, Source[ByteString, _]]] = {
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
  override def inspectOrCreateLock(key: String): Future[Either[_, FSLock]] = {
    ???
  }

  override def acquireLock(key: String): Future[Either[_, FSLock]] = {
    ???
  }

  override def releaseLock(key: String): Future[Either[_, FSLock]] = {
    ???
  }

}


