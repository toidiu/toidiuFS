package fileUtils.dropbox

import akka.stream.scaladsl.Source
import akka.util.ByteString
import fileUtils.FileService
import play.api.libs.ws.{StreamedBody, StreamedResponse, WSResponse}
import utils.AppUtils

import scala.concurrent.Future

/**
  * Created by toidiu on 11/2/16.
  */

object DBoxService extends FileService {

  import io.circe._
  import io.circe.generic.auto._
  import io.circe.syntax._

  override def getFile(key: String): Future[StreamedResponse] = {
    //  override def getFile(key: String): Future[WSResponse] = {
    val baseUrl: String = "https://content.dropboxapi.com/2/files/download"
    case class DownloadArgs(path: String)
    val args = DownloadArgs("/" + key).asJson.noSpaces

    wsClient.url(baseUrl).withHeaders(
      ("Authorization", "Bearer " + AppUtils.dropboxToken)
      , ("Dropbox-API-Arg", args)
      //      , ("Content-Type", "application/octet-stream")
    )
      .withMethod("POST").stream()
      //      .execute("POST")
      //          .withBody()
    //      , ("Dropbox-API-Arg", args)
  }

  override def uploadFile(key: String, stream: Source[ByteString, _]): Future[WSResponse] = {
    val baseUrl: String = "https://content.dropboxapi.com/2/files/upload"
    case class UploadArgs(path: String, mode: String = "overwrite", mute: Boolean = false)
    val args = UploadArgs("/" + key).asJson.noSpaces

    wsClient.url(baseUrl).withHeaders(
      ("Authorization", "Bearer " + AppUtils.dropboxToken)
      , ("Dropbox-API-Arg", args)
      , ("Content-Type", "application/octet-stream"))
      .withBody(StreamedBody(stream)).execute("POST")
  }

  override def getMeta: Unit = ???
}


