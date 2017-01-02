package replicas

import java.io.{File, InputStream}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.{ByteString, Timeout}
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
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try

/**
  * Created by toidiu on 11/2/16.
  */
trait FileService {
  implicit val t: Timeout = FileService.timeout
  implicit val s: ActorSystem = FileService.system
  implicit val m: ActorMaterializer = FileService.materializer
  val largeLockArray = 500

  val serviceName: String
  val isEnable: Boolean
  val isWhiteList: Boolean
  val mimeList: List[String]
  val maxLength: Long

  def postFile(meta: ByteString, key: String, inputStream: InputStream): Future[Try[PostFileStatus]]

  def getFile(key: String): Future[Try[File]]

  def getMeta(key: String): Future[Either[MetaError, MetaServer]]

  def buildMetaBytes(bytes: Long, mime: String, uploadedAt: String,
                     key: String): ByteString

  //-=-=-=-=-=-=-=-==-==-==-==-=-=-=-=-=-=-
  //Lock
  //-=-=-=-=-=-=-=-==-==-==-==-=-=-=-=-=-=-
  def createLock(key: String): Future[Try[FSLock]]

  def inspectOrCreateLock(key: String): Future[Try[FSLock]]

  def acquireLock(key: String): Future[Try[FSLock]]

  def releaseLock(key: String): Future[Try[FSLock]]
}

object FileService {
  implicit val timeout = new Timeout(20 seconds)
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  //used to store meta data for DBX storage
  val bufferByte: Int = 1000
}
