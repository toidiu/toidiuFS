package replicas

import java.io.InputStream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.{ByteString, Timeout}
import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import models.FSLock

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by toidiu on 11/2/16.
  */
trait FileService {
  implicit val timeout = new Timeout(20 seconds)
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val isEnable: Boolean
  val isWhiteList: Boolean
  val mimeList: List[String]
  val maxLength: Long


  def postFile(meta: ByteString, key: String, inputStream: InputStream): Future[Either[_, Boolean]]

  def getFile(key: String): Future[Source[ByteString, _]]

  def getMeta(key: String): Future[String]

  def buildMetaBytes(bytes: Long, mime: String, uploadedAt: String,
                     key: String): ByteString //= ByteString(meta.asJson.noSpaces)

  //-=-=-=-=-=-=-=-==-==-==-==-=-=-=-=-=-=-
  //Lock
  //-=-=-=-=-=-=-=-==-==-==-==-=-=-=-=-=-=-
  def inspectOrCreateLock(key: String): Future[Either[_, FSLock]]

  def acquireLock(key: String): Future[Either[_, FSLock]]

  def releaseLock(key: String): Future[Either[_, FSLock]]
}

object FileService {

  val bufferByte: Int = 1000

  //  def buildS3Meta(meta: S3Meta): ByteString = ByteString(meta.asJson.noSpaces)
  //
  //  def buildDbxMeta(meta: DbxMeta): ByteString = {
  //    val metaByteString = ByteString(meta.asJson.noSpaces)
  //    metaByteString ++ ByteString.fromArray(new Array[Byte](FileService.bufferByte - metaByteString.size))
  //  }
}
