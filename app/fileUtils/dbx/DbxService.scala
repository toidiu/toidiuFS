package fileUtils.dbx

import java.io.InputStream
import java.sql.Date
import java.util.concurrent.TimeUnit

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
import scala.concurrent.duration.FiniteDuration

/**
  * Created by toidiu on 11/2/16.
  */

object DbxService extends FileService {

  val config: DbxRequestConfig = DbxRequestConfig.newBuilder("toidiuFS").withUserLocale("en_US").build()
  val client: DbxClientV2 = new DbxClientV2(config, AppUtils.dropboxToken)

  def getPath(key: String): String = AppUtils.dropboxPath + key

  override def postFile(meta: ByteString, key: String, inputStream: InputStream): Future[Either[_, Boolean]] = {

    val metaIs = Source.single(meta).runWith(StreamConverters.asInputStream(FiniteDuration(5, TimeUnit.SECONDS)))
    val saveIs = new java.io.SequenceInputStream(metaIs, inputStream)

    val metadata = client.files().uploadBuilder(getPath(key))
      .withMode(WriteMode.OVERWRITE)
      .withClientModified(new Date(System.currentTimeMillis()))
      .uploadAndFinish(saveIs)

    saveIs.close()

    val fut: Future[Either[_, Boolean]] = for {
      s <- Future(metadata)
    } yield Right(true)

    fut.recover { case e: Exception => Left(e.toString) }
  }

  override def getFile(key: String): Future[Source[ByteString, _]] = {
    lazy val stream: () => InputStream = client.files().download(getPath(key)).getInputStream
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
    val stream = client.files().download(getPath(key)).getInputStream
    val meta = Array.ofDim[Byte](3)
    try {
      stream.read(meta)
    } finally {
      stream.close()
    }

    Future(ByteString(meta.filterNot(_ == 0)).utf8String)
  }

}


