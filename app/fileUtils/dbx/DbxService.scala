package fileUtils.dbx

import java.io.InputStream
import java.sql.Date

import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.WriteMode
import fileUtils.FileService
import fileUtils.FileService.bufferByte
import utils.AppUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by toidiu on 11/2/16.
  */

object DbxService extends FileService {

  val config: DbxRequestConfig = DbxRequestConfig.newBuilder("toidiuFS").withUserLocale("en_US").build()
  val client: DbxClientV2 = new DbxClientV2(config, AppUtils.dropboxToken)

  override def postFile(meta: ByteString, key: String, inputStream: InputStream): Future[Either[_, Boolean]] = {
    val metadata = client.files().uploadBuilder("/" + key)
      .withMode(WriteMode.OVERWRITE)
      .withClientModified(new Date(System.currentTimeMillis()))
      .uploadAndFinish(inputStream)

    val fut: Future[Either[_, Boolean]] = for {
      s <- Future(metadata)
    } yield Right(true)

    fut.recover { case e: Exception => Left(e.toString) }
  }

  override def getFile(key: String): Future[Source[ByteString, _]] = {
    lazy val stream: () => InputStream = client.files().download("/" + key).getInputStream
    var one = true

    Future(StreamConverters.fromInputStream(stream).map { bs =>
      if (one) {
        one = false
        bs.drop(FileService.bufferByte)
      }
      else bs
    })
  }

  override def getMeta(key: String): Future[String] = {
    val stream = client.files().download("/" + key).getInputStream
    val meta = Array.ofDim[Byte](bufferByte)
    try {
      stream.read(meta)
    } finally {
      stream.close()
    }

    Future(ByteString(meta).utf8String)
  }

}


