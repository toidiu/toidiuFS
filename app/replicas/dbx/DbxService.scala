package replicas.dbx

import java.io.{ByteArrayInputStream, File, InputStream}
import java.sql.Date
import java.util.concurrent.TimeUnit

import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import cache.CacheUtils
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.{DownloadErrorException, WriteMode}
import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import models.{FSLock, MetaDetail, MetaServer}
import replicas.FileService
import utils.ErrorUtils.MetaError
import utils.StatusUtils.PostFileStatus
import utils.{AppUtils, TimeUtils}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}

/**
  * Created by toidiu on 11/2/16.
  */

object DbxService extends FileService {

  lazy val config: DbxRequestConfig = DbxRequestConfig.newBuilder("toidiuFS").withUserLocale("en_US").build()
  lazy val client: DbxClientV2 = new DbxClientV2(config, AppUtils.dbxToken)

  def getPath(key: String): String = AppUtils.dbxPath + key

  def getLockPath(key: String): String = "/lock/" + key + ".lock"

  override val isEnable: Boolean = AppUtils.dbxEnable
  override val isWhiteList: Boolean = AppUtils.dbxIsWhiteList
  override val mimeList: List[String] = AppUtils.dbxMimeList
  override val maxLength: Long = AppUtils.dbxMaxLength

  override def postFile(meta: ByteString, key: String, inputStream: InputStream): Future[Try[PostFileStatus]] = {
    lazy val saveIs = new java.io.SequenceInputStream(new ByteArrayInputStream(meta.toArray), inputStream)
    lazy val fileMetadata = Future {
      client.files().uploadBuilder(getPath(key))
        .withMode(WriteMode.OVERWRITE)
        .withClientModified(new Date(System.currentTimeMillis()))
        .uploadAndFinish(saveIs)
    }

    val fut = for {
      s <- fileMetadata
      close <- Future(saveIs.close())
      l <- releaseLock(key)
    } yield Success(PostFileStatus("dropbox", success = true))

    fut.recover { case e: Exception =>
      releaseLock(key)
      Failure(e)
    }
  }

  override def getFile(key: String): Future[Try[File]] = {
    var dropOne = true
    for {
      stream <- Future(client.files().download(getPath(key)).getInputStream)
      trim = Future(stream.skip(FileService.bufferByte.toLong))
      file <- CacheUtils.saveCachedFile(key, stream)
    } yield file

  }

  override def getMeta(key: String): Future[Either[MetaError, MetaServer]] = {
    Future {
      var stream: InputStream = null
      try {
        stream = client.files().download(getPath(key)).getInputStream
        val meta = Array.ofDim[Byte](FileService.bufferByte)
        stream.read(meta)
        val str = ByteString(meta.filterNot(_ == 0)).utf8String
        decode[MetaServer](str) match {
          case Right(s) => Right(s)
          case Left(e) => Left(MetaError("dropbox", e.toString))
        }
      } catch {
        case e: Exception => Left(MetaError("dropbox", e.toString))
      }
      finally {
        if (stream != null)
          stream.close()
      }
    }
  }

  override def buildMetaBytes(bytes: Long, mime: String,
                              uploadedAt: String, key: String): ByteString = {
    val detail: MetaDetail = MetaDetail(Some(DbxService.getPath(key)))
    val jsonString = MetaServer(bytes, mime, uploadedAt, "dropbox", detail).asJson.noSpaces
    val metaByteString = ByteString(jsonString)
    metaByteString ++ ByteString.fromArray(new Array[Byte](FileService.bufferByte - metaByteString.size))
  }

  //-=-=-=-=-=-=-=-==-==-==-==-=-=-=-=-=-=-
  //Lock
  //-=-=-=-=-=-=-=-==-==-==-==-=-=-=-=-=-=-
  override def createLock(key: String): Future[Try[FSLock]] = {
    updateLock(key, FSLock(locked = false, TimeUtils.zoneAsString))
  }

  override def inspectOrCreateLock(key: String): Future[Try[FSLock]] = {
    Future {
      val stream = client.files().download(getLockPath(key)).getInputStream
      val lock = Array.ofDim[Byte](largeLockArray)
      stream.read(lock)
      stream.close()
      val eitherJson = decode[FSLock](ByteString(lock.filterNot(_ == 0)).utf8String)
      eitherJson match {
        case Right(fsLock) => Success(fsLock)
        case Left(err) => Failure(new Exception(err))
      }
    }.recoverWith { case e: DownloadErrorException => createLock(key) }
  }

  override def acquireLock(key: String): Future[Try[FSLock]] = {
    updateLock(key, FSLock(locked = true, TimeUtils.zoneAsString))
  }

  override def releaseLock(key: String): Future[Try[FSLock]] = {
    updateLock(key, FSLock(locked = false, TimeUtils.zoneAsString))
  }

  private def updateLock(key: String, ret: FSLock): Future[Try[FSLock]] = {
    val lockContent = ByteString(ret.asJson.noSpaces)
    val lockIs = Source.single(lockContent).runWith(StreamConverters.asInputStream(FiniteDuration(5, TimeUnit.SECONDS)))

    Future {
      client.files().uploadBuilder(getLockPath(key))
        .withMode(WriteMode.OVERWRITE)
        .withClientModified(new Date(System.currentTimeMillis()))
        .uploadAndFinish(lockIs)
      lockIs.close()
      Success(ret)
    }
  }
}


