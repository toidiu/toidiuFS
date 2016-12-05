package replicas.s3

import java.io.{InputStream, StringWriter}
import java.nio.charset.StandardCharsets
import java.util

import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.model.{AmazonS3Exception, ObjectMetadata}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3Client}
import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import models.{FSLock, MetaDetail, MetaServer}
import org.apache.commons.io.IOUtils
import replicas.FileService
import utils.ErrorUtils.MetaError
import utils.StatusUtils.PostFileStatus
import utils.{AppUtils, TimeUtils}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * Created by toidiu on 11/17/16.
  */
object S3Service extends FileService {

  lazy val cred = new BasicAWSCredentials(AppUtils.s3AccessKey, AppUtils.s3SecretKey)
  lazy val client: AmazonS3 = new AmazonS3Client(cred)
  lazy val bucket = client.createBucket(AppUtils.s3Bucket)

  private val META_OBJ_KEY: String = "meta"
  private val LOCK_OBJ_KEY: String = "lock"

  def getLockKey(key: String) = "lock/" + key

  override val isEnable: Boolean = AppUtils.s3Enable
  override val isWhiteList: Boolean = AppUtils.s3IsWhiteList
  override val mimeList: List[String] = AppUtils.s3MimeList
  override val maxLength: Long = AppUtils.s3MaxLength

  override def postFile(meta: ByteString, key: String, inputStream: InputStream): Future[Try[PostFileStatus]] = {
    val metaObj: ObjectMetadata = new ObjectMetadata()
    val map: util.Map[String, String] = new util.HashMap()
    map.put(META_OBJ_KEY, meta.utf8String)
    metaObj.setUserMetadata(map)

    val fut = for {
      s <- Future(client.putObject(bucket.getName, key, inputStream, metaObj))
      l <- releaseLock(key)
    } yield Success(PostFileStatus("s3", success = true))

    fut.recover { case e: Exception => Failure(e) }
  }

  override def getFile(key: String): Future[Try[Source[ByteString, _]]] = {
    Future {
      var stream: () => InputStream = null
      val op = client.getObject(bucket.getName, key)
      stream = op.getObjectContent
      Success(StreamConverters.fromInputStream(stream))
    }.recover { case e: Exception => Failure(e) }
  }

  override def getMeta(key: String): Future[Either[MetaError, MetaServer]] = {
    Future {
      try {
        val str = client.getObjectMetadata(bucket.getName, key).getUserMetaDataOf(META_OBJ_KEY)
        decode[MetaServer](str) match {
          case Right(s) => Right(s)
          case Left(e) => Left(MetaError("s3", e.toString))
        }
      } catch {
        case e: Exception => Left(MetaError("s3", e.toString))
      }
    }
  }

  def buildS3Meta(meta: MetaServer): ByteString = ByteString(meta.asJson.noSpaces)

  override def buildMetaBytes(bytes: Long, mime: String,
                              uploadedAt: String, key: String): ByteString = {
    val detail: MetaDetail = MetaDetail(None, Some(AppUtils.s3Bucket), Some(key))
    val jsonString = MetaServer(bytes, mime, uploadedAt, "s3", detail).asJson.noSpaces
    ByteString(jsonString)
  }

  //-=-=-=-=-=-=-=-==-==-==-==-=-=-=-=-=-=-
  //Lock
  //-=-=-=-=-=-=-=-==-==-==-==-=-=-=-=-=-=-
  override def createLock(key: String): Future[Try[FSLock]] = {
    updateLock(key, FSLock(locked = false, TimeUtils.zoneAsString))
  }

  override def inspectOrCreateLock(key: String): Future[Try[FSLock]] = {
    Future {
      val op = client.getObject(bucket.getName, getLockKey(key))
      val writer: StringWriter = new StringWriter()
      IOUtils.copy(op.getObjectContent, writer, StandardCharsets.UTF_8)
      val eitherJson = decode[FSLock](writer.toString)
      eitherJson match {
        case Right(fsLock) => Success(fsLock)
        case Left(err) => Failure(new Exception(err))
      }
    }.recoverWith { case e: AmazonS3Exception => createLock(key) }
  }

  override def acquireLock(key: String): Future[Try[FSLock]] = {
    updateLock(key, FSLock(locked = true, TimeUtils.zoneAsString))
  }

  override def releaseLock(key: String): Future[Try[FSLock]] = {
    updateLock(key, FSLock(locked = false, TimeUtils.zoneAsString))
  }

  private def updateLock(key: String, ret: FSLock) = {
    val lockContent = ret.asJson.noSpaces
    Future {
      client.putObject(bucket.getName, getLockKey(key), lockContent)
      Success(ret)
    }.recover { case e: Exception => Failure(e) }
  }

}


