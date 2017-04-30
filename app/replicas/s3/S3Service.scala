package replicas.s3

import java.io.{File, InputStream, StringWriter}
import java.nio.charset.StandardCharsets
import java.util

import akka.util.ByteString
import cache.CacheUtils
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.model.{AmazonS3Exception, Bucket, ObjectMetadata}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3Client}
import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import models.{FSLock, MetaDetail, MetaServer}
import org.apache.commons.io.IOUtils
import replicas.{FileService, ReplicaUtils}
import utils.ErrorUtils.MetaError
import utils.StatusUtils.PostFileStatus
import utils.TimeUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * Created by toidiu on 11/17/16.
  */
object S3Service extends FileService {

  lazy val cred = new BasicAWSCredentials(ReplicaUtils.s3AccessKey, ReplicaUtils.s3SecretKey)
  lazy val client: AmazonS3 = new AmazonS3Client(cred)
  lazy val bucket: Bucket = client.createBucket(ReplicaUtils.s3Bucket)
  override val serviceName: String = "s3"
  override val isEnable: Boolean = ReplicaUtils.s3Enable
  override val isWhiteList: Boolean = ReplicaUtils.s3IsWhiteList
  override val mimeList: List[String] = ReplicaUtils.s3MimeList
  override val maxLength: Long = ReplicaUtils.s3MaxLength
  private val META_OBJ_KEY: String = "meta"

  override def postFile(meta: ByteString, key: String, inputStream: InputStream): Future[Try[PostFileStatus]] = {
    val metaObj: ObjectMetadata = new ObjectMetadata()
    val map: util.Map[String, String] = new util.HashMap()
    map.put(META_OBJ_KEY, meta.utf8String)
    metaObj.setUserMetadata(map)

    val fut = for {
      s <- Future(client.putObject(bucket.getName, key, inputStream, metaObj))
      l <- releaseLock(key)
    } yield Success(PostFileStatus(serviceName, success = true))

    fut.recover { case e: Exception =>
      releaseLock(key)
      Failure(e)
    }
  }

  override def releaseLock(key: String): Future[Try[FSLock]] = {
    updateLock(key, FSLock(locked = false, TimeUtils.zoneTimeAsString))
  }

  override def getFile(key: String): Future[Try[File]] = {
    for {
      op <- Future(client.getObject(bucket.getName, key))
      stream <- Future(op.getObjectContent)
      file <- CacheUtils.saveCachedFile(key, "s3", stream)
    } yield file
  }

  override def getMeta(key: String): Future[Either[MetaError, MetaServer]] = {
    Future {
      try {
        val str = client.getObjectMetadata(bucket.getName, key).getUserMetaDataOf(META_OBJ_KEY)
        decode[MetaServer](str) match {
          case Right(meta) => Right(meta)
          case Left(e) => Left(MetaError(serviceName, e.toString))
        }
      } catch {
        case e: Exception => Left(MetaError(serviceName, e.toString))
      }
    }
  }

  override def buildMetaBytes(bytes: Long, mime: String,
                              uploadedAt: String, key: String): ByteString = {
    val detail: MetaDetail = MetaDetail(None, Some(ReplicaUtils.s3Bucket), Some(key))
    val jsonString = MetaServer(bytes, mime, uploadedAt, serviceName, detail).asJson.noSpaces
    ByteString(jsonString)
  }

  //-=-=-=-=-=-=-=-==-==-==-==-=-=-=-=-=-=-
  //Lock
  //-=-=-=-=-=-=-=-==-==-==-==-=-=-=-=-=-=-
  override def inspectOrCreateLock(key: String): Future[Try[FSLock]] = {
    Future {
      val op = client.getObject(bucket.getName, getLockKey(key))
      val writer = new StringWriter()
      IOUtils.copy(op.getObjectContent, writer, StandardCharsets.UTF_8)
      val eitherJson = decode[FSLock](writer.toString)
      eitherJson match {
        case Right(fsLock) => Success(fsLock)
        case Left(err) => Failure(new Exception(err))
      }
    }.recoverWith { case _: AmazonS3Exception => createLock(key) }
  }

  override def createLock(key: String): Future[Try[FSLock]] = {
    updateLock(key, FSLock(locked = false, TimeUtils.zoneTimeAsString))
  }

  override def acquireLock(key: String): Future[Try[FSLock]] = {
    updateLock(key, FSLock(locked = true, TimeUtils.zoneTimeAsString))
  }

  private def updateLock(key: String, ret: FSLock): Future[Try[FSLock]] = {
    val lockContent = ret.asJson.noSpaces
    Future {
      client.putObject(bucket.getName, getLockKey(key), lockContent)
      Success(ret)
    }
  }

  private def getLockKey(key: String) = "lock/" + key

}


