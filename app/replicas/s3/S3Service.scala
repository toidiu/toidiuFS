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
import utils.{AppUtils, TimeUtils}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

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

  override def postFile(meta: ByteString, key: String, inputStream: InputStream): Future[Either[_, Boolean]] = {
    val metaObj: ObjectMetadata = new ObjectMetadata()
    val map: util.Map[String, String] = new util.HashMap()
    map.put(META_OBJ_KEY, meta.utf8String)
    metaObj.setUserMetadata(map)

    val fut: Future[Either[_, Boolean]] = for {
      s <- Future(client.putObject(bucket.getName, key, inputStream, metaObj))
      l <- releaseLock(key)
    } yield Right(true)

    fut.recover { case e: Exception => Left(e.toString) }
  }

  override def getFile(key: String): Future[Source[ByteString, _]] = {
    val op = client.getObject(bucket.getName, key)
    Future(StreamConverters.fromInputStream(op.getObjectContent))
  }

  override def getMeta(key: String): Future[String] = {
    val op: Future[ObjectMetadata] = Future(client.getObjectMetadata(bucket.getName, key))
    op.flatMap(e => Future(e.getUserMetaDataOf(META_OBJ_KEY)))
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


