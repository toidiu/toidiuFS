package replicas

import java.io.InputStream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.{ByteString, Timeout}
import models.{DbxMeta, Meta, MetaServer, S3Meta}

import scala.concurrent.Future
import scala.concurrent.duration._
import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._

import scala.language.postfixOps

/**
  * Created by toidiu on 11/2/16.
  */
trait FileService {
  implicit val timeout = new Timeout(20 seconds)
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def postFile(meta: ByteString, key: String, inputStream: InputStream): Future[Either[_, Boolean]]

  def getFile(key: String): Future[Source[ByteString, _]]

  def getMetaString(key: String): Future[String]

  def buildMetaBytes(bytes: Long, mime: String, uploadedAt: String,
                     key:String): ByteString //= ByteString(meta.asJson.noSpaces)
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
