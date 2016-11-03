package fileUtils.dropbox

import java.io.File

import akka.stream.scaladsl.Source
import akka.util.ByteString
import fileUtils.FileService
import io.circe.{Json, JsonNumber, JsonObject}
import play.api.libs.ws.{FileBody, StreamedBody, StreamedResponse, WSResponse}
import utils.AppUtils

import scala.concurrent.Future
import scala.util.parsing.json.JSONArray

/**
  * Created by toidiu on 11/2/16.
  */
//object DBoxService {
//  val wsClient = NingWSClient()
//  private val baseUrl: String = "https://content.dropboxapi.com/2/files/upload"
//
//
//  //  --header "Authorization: Bearer 5MfLOCYZvmQAAAAAAAAbt8UOr_FbV22cV8VGhy2OtblYGi73pBq32P1SbhPIyvJJ" \
//  //    --header "Dropbox-API-Arg: {\"path\": \"/Homework/math/Matrices.txt\",\"mode\": \"add\",\"autorename\": true,\"mute\": false}" \
//  //    --header "Content-Type: application/octet-stream" \
//  //    --data-binary @local_file.txt
//
//}

object DBoxService extends FileService {
  private val baseUrl: String = "https://content.dropboxapi.com/2/files/upload"

  //  import DBoxService._

  import io.circe._, io.circe.generic.auto._, io.circe.syntax._

//  override def uploadFile(pathFile: String, stream: Source[ByteString, _]): Future[WSResponse] = {
  override def uploadFile(pathFile: String, stream: File): Future[StreamedResponse] = {
    case class Te (path: String)
    val t = Te(pathFile)
    println("-=-=-=")
    val d = t.asJson
    println(d.toString)
    println(stream)
    println("-=-=-=")

    wsClient.url(baseUrl).withHeaders(
      ("Authorization", "Bearer " + AppUtils.dropboxToken)
//      , ("Dropbox-API-Arg", "{\"path\": \"" + pathFile + "\",\"mode\": \"add\",\"autorename\": true,\"mute\": false}")
      , ("Dropbox-API-Arg", "{\"path\":\"/blat.txt\"}")
      , ("Content-Type", "application/octet-stream"))
//      .withBody(StreamedBody(stream)).execute("POST")
        .withMethod("POST").withBody(FileBody(stream)).stream()
  }

  override def getFile: Unit = ???

  override def getMeta: Unit = ???
}


