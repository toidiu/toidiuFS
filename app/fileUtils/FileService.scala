package fileUtils

import java.io.InputStream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.{ByteString, Timeout}
import models.Meta
import play.api.libs.ws.ning.NingWSClient

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Created by toidiu on 11/2/16.
  */
trait FileService {
  implicit val timeout = new Timeout(20 seconds)
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val wsClient = NingWSClient()


  def postFile(meta: ByteString, key: String, inputStream: InputStream): Future[Either[_, Boolean]]

  def getFile(key: String): Future[Source[ByteString, _]]

  def getMeta(key: String): Future[String]
}

object FileService {

  import io.circe._
  import io.circe.generic.auto._
  import io.circe.parser._
  import io.circe.syntax._

  val bufferByte: Int = 150

  def buildMeta(meta: Meta): ByteString = {
    val mimeLengthBytes = ByteString(meta.asJson.noSpaces)
    mimeLengthBytes ++ ByteString.fromArray(new Array[Byte](FileService.bufferByte - mimeLengthBytes.size))
  }
}
