package fileUtils

import java.io.File

import akka.stream.scaladsl.Source
import akka.util.{ByteString, Timeout}
import play.api.libs.ws.{StreamedResponse, WSResponse}
import play.api.libs.ws.ning.NingWSClient

import scala.concurrent.Future
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.concurrent.duration._

/**
  * Created by toidiu on 11/2/16.
  */
trait FileService {
  implicit val timeout = new Timeout(20 seconds)
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val wsClient = NingWSClient()


  def uploadFile(pathFile:String, stream: Source[ByteString, _]) :Future[WSResponse]

  def getFile

  def getMeta

}
