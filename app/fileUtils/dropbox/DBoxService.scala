package fileUtils.dropbox

import akka.stream.scaladsl.Source
import akka.util.ByteString
import fileUtils.FileService
import play.api.libs.ws.{StreamedBody, WSResponse}
import utils.AppUtils

import scala.concurrent.Future

/**
  * Created by toidiu on 11/2/16.
  */

object DBoxService extends FileService {
  private val baseUrl: String = "https://content.dropboxapi.com/2/files/upload"

  import io.circe.syntax._
  import io.circe._
  override def uploadFile(pathFile: String, stream: Source[ByteString, _]): Future[WSResponse] = {
    case class UploadArgs(path: String, mode: String = "overwrite", mute: Boolean = false)
    val args = UploadArgs(pathFile).asJson.noSpaces

    wsClient.url(baseUrl).withHeaders(
      ("Authorization", "Bearer " + AppUtils.dropboxToken)
      , ("Dropbox-API-Arg", args)
      , ("Content-Type", "application/octet-stream"))
      .withBody(StreamedBody(stream)).execute("POST")
  }

  override def getFile: Unit = ???

  override def getMeta: Unit = ???
}


