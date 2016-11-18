package fileUtils.dropbox

import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import fileUtils.FileService
import play.api.libs.ws.StreamedBody
import utils.AppUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by toidiu on 11/2/16.
  */

object DBoxService extends FileService {

  import io.circe._
  import io.circe.generic.auto._
  import io.circe.syntax._

  override def postFile(meta: ByteString, key: String, stream: Source[ByteString, _]): Future[Either[_, Boolean]] = {
    val saveStream: Source[ByteString, _] = Source.single(meta).concat(stream)

    val baseUrl: String = "https://content.dropboxapi.com/2/files/upload"
    case class UploadArgs(path: String, mode: String = "overwrite", mute: Boolean = false)
    val args = UploadArgs("/" + key).asJson.noSpaces

    val rep = wsClient.url(baseUrl).withHeaders(
      ("Authorization", "Bearer " + AppUtils.dropboxToken)
      , ("Dropbox-API-Arg", args)
      , ("Content-Type", "application/octet-stream"))
      .withBody(
        StreamedBody(saveStream)
      ).execute("POST")

    rep.map(r => r.status match {
      case 200 => Right(true)
      case e => Left(e)
    })
  }

  override def getFile(key: String): Future[Source[ByteString, _]] = {
    //  override def getFile(key: String): Future[WSResponse] = {
    val baseUrl: String = "https://content.dropboxapi.com/2/files/download"
    case class DownloadArgs(path: String)
    val args = DownloadArgs("/" + key).asJson.noSpaces

    val res = wsClient.url(baseUrl).withHeaders(
      ("Authorization", "Bearer " + AppUtils.dropboxToken)
      , ("Dropbox-API-Arg", args)
    )
      .withMethod("POST").stream()


    res.map { rep =>
      var one = true
      val fileStream: Source[ByteString, _] = rep.body.map(bs =>
        if (one) {
          one = false
          bs.drop(FileService.bufferByte)
        }
        else bs
      )
      fileStream
    }
  }

  override def getMeta(key: String): Future[Source[ByteString, _]] = {
    val baseUrl: String = "https://content.dropboxapi.com/2/files/download"
    case class DownloadArgs(path: String)
    val args = DownloadArgs("/" + key).asJson.noSpaces

    val stream = wsClient.url(baseUrl).withHeaders(
      ("Authorization", "Bearer " + AppUtils.dropboxToken)
      , ("Dropbox-API-Arg", args)
    ).withMethod("POST").stream()

    stream.map { rep =>
      rep.body.take(1).map(bs => bs.take(FileService.bufferByte))
    }
  }

}


