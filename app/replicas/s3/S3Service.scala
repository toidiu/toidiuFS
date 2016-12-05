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
import models.Status.PostFileStatus
import models.{FSLock, MetaDetail, MetaError, MetaServer}
import org.apache.commons.io.IOUtils
import replicas.FileService
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
    } yield Success(PostFileStatus("s3", true))

    fut.recover { case e: Exception => Failure(e) }
  }

  override def getFile(key: String): Future[Either[String, Source[ByteString, _]]] = {
    Future {
      var stream: () => InputStream = null
      try {
        val op = client.getObject(bucket.getName, key)
        stream = op.getObjectContent
        Right(StreamConverters.fromInputStream(stream))
      } catch {
        case e: Exception => Left(e.toString)
      }
    }
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
  override def inspectOrCreateLock(key: String): Future[Either[_, FSLock]] = {
    try {
      val op = client.getObject(bucket.getName, getLockKey(key))
      Future(op.getObjectContent).map { is =>
        val writer: StringWriter = new StringWriter()
        IOUtils.copy(is, writer, StandardCharsets.UTF_8)
        decode[FSLock](writer.toString)
      }
    } catch {
      case e: AmazonS3Exception =>
        releaseLock(key).map(_ => Right(FSLock(false, TimeUtils.zoneAsString)))
    }

  }

  override def acquireLock(key: String): Future[Either[_, FSLock]] = {
    val ret: FSLock = FSLock(true, TimeUtils.zoneAsString)
    val lockContent = ret.asJson.noSpaces

    val fut = for {
      s <- Future(client.putObject(bucket.getName, getLockKey(key), lockContent))
    } yield Right(ret)

    fut.recover { case e: Exception => Left(e.toString) }
  }

  override def releaseLock(key: String): Future[Either[_, FSLock]] = {
    val ret: FSLock = FSLock(false, TimeUtils.zoneAsString)
    val lockContent = ret.asJson.noSpaces

    val fut = for {
      s <- Future(client.putObject(bucket.getName, getLockKey(key), lockContent))
    } yield Right(ret)

    fut.recover { case e: Exception => Left(e.toString) }
  }

}


