package replicas.dbx

import java.io.InputStream
import java.sql.Date
import java.util.concurrent.TimeUnit

import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.WriteMode
import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import models.{DbxMeta, MetaDetail}
import replicas.FileService
import utils.AppUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

/**
  * Created by toidiu on 11/2/16.
  */

object DbxService extends FileService {

  lazy val config: DbxRequestConfig = DbxRequestConfig.newBuilder("toidiuFS").withUserLocale("en_US").build()
  lazy val client: DbxClientV2 = new DbxClientV2(config, AppUtils.dbxToken)

  def getPath(key: String): String = AppUtils.dbxPath + key

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

  override def getMetaString(key: String): Future[String] = {
    //fixme catch exception
    val stream = client.files().download(getPath(key)).getInputStream

    val meta = Array.ofDim[Byte](FileService.bufferByte)
    try {
      stream.read(meta)
    } finally {
      stream.close()
    }

    Future(ByteString(meta.filterNot(_ == 0)).utf8String)
  }

  override def buildMetaBytes(bytes: Long, mime: String,
                              uploadedAt: String, key: String): ByteString = {
    val detail: MetaDetail = MetaDetail(Some(DbxService.getPath(key)))
    val jsonString = DbxMeta(bytes, mime, uploadedAt, "dropbox", detail).asJson.noSpaces
    val metaByteString = ByteString(jsonString)
    metaByteString ++ ByteString.fromArray(new Array[Byte](FileService.bufferByte - metaByteString.size))
  }

}


