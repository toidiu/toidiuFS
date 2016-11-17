package fileUtils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.{ByteString, Timeout}
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


  def postFile(meta: ByteString, key: String, stream: Source[ByteString, _]): Future[Either[_, Boolean]]

  def getFile(key: String): Future[Source[ByteString, _]]

  def getMeta(key: String): Future[Source[ByteString, _]]
}


object FileService {
  val bufferByte: Int = 150
}
