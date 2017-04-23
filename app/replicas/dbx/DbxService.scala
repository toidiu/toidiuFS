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
import replicas.{FileService, ReplicaUtils}
import utils.ErrorUtils.MetaError
import utils.StatusUtils.PostFileStatus
import utils.TimeUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}

/**
  * Created by toidiu on 11/2/16.
  */

object DbxService extends FileService {

  lazy val config: DbxRequestConfig = DbxRequestConfig.newBuilder("toidiuFS").withUserLocale("en_US").build()
  lazy val client: DbxClientV2 = new DbxClientV2(config, ReplicaUtils.dbxToken)
  override val serviceName: String = "dropbox"
  override val isEnable: Boolean = ReplicaUtils.dbxEnable
  override val isWhiteList: Boolean = ReplicaUtils.dbxIsWhiteList
  override val mimeList: List[String] = ReplicaUtils.dbxMimeList
  override val maxLength: Long = ReplicaUtils.dbxMaxLength

  override def postFile(meta: ByteString, key: String, inputStream: InputStream): Future[Try[PostFileStatus]] = {
    lazy val saveIs = new java.io.SequenceInputStream(new ByteArrayInputStream(meta.toArray), inputStream)
    lazy val fileMetadata = Future {
      client.files().uploadBuilder(getPath(key)).withMode(WriteMode.OVERWRITE)
        .withClientModified(new Date(System.currentTimeMillis()))
        .uploadAndFinish(saveIs)
    }

    //noinspection ScalaUnusedSymbol
    val fut = for {
      s <- fileMetadata
      close <- Future(saveIs.close())
      l <- releaseLock(key)
    } yield Success(PostFileStatus(serviceName, success = true))

    fut.recover { case e: Exception =>
      releaseLock(key)
      Failure(e)
    }
  }

  def getPath(key: String): String = ReplicaUtils.dbxPath + key

  override def releaseLock(key: String): Future[Try[FSLock]] = {
    updateLock(key, FSLock(locked = false, TimeUtils.zoneAsString))
  }

  override def getFile(key: String): Future[Try[File]] = {
    //noinspection ScalaUnusedSymbol
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
          case Right(metaServer) => Right(metaServer)
          case Left(e) => Left(MetaError(serviceName, e.toString))
        }
      } catch {
        case e: Exception => Left(MetaError(serviceName, e.toString))
      }
      finally {
        if (stream != null) stream.close()
      }
    }
  }

  override def buildMetaBytes(bytes: Long, mime: String,
                              uploadedAt: String, key: String): ByteString = {
    val detail: MetaDetail = MetaDetail(Some(DbxService.getPath(key)))
    val jsonString = MetaServer(bytes, mime, uploadedAt, serviceName, detail).asJson.noSpaces
    val metaByteString = ByteString(jsonString)
    metaByteString ++ ByteString.fromArray(new Array[Byte](FileService.bufferByte - metaByteString.size))
  }

  //-=-=-=-=-=-=-=-==-==-==-==-=-=-=-=-=-=-
  //Lock
  //-=-=-=-=-=-=-=-==-==-==-==-=-=-=-=-=-=-
  override def inspectOrCreateLock(key: String): Future[Try[FSLock]] = {
    val readLockArray = 500
    Future {
      val stream = client.files().download(getLockPath(key)).getInputStream
      val readLock = Array.ofDim[Byte](readLockArray)
      stream.read(readLock)
      stream.close()
      val eitherJson = decode[FSLock](ByteString(readLock.filterNot(_ == 0)).utf8String)
      eitherJson match {
        case Right(fsLock) => Success(fsLock)
        case Left(err) => Failure(new Exception(err))
      }
    }.recoverWith { case _: DownloadErrorException => createLock(key) }
  }

  private def getLockPath(key: String): String = "/lock/" + key + ".lock"

  override def createLock(key: String): Future[Try[FSLock]] = {
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

  override def acquireLock(key: String): Future[Try[FSLock]] = {
    updateLock(key, FSLock(locked = true, TimeUtils.zoneAsString))
  }
}


