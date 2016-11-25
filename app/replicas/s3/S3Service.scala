package replicas.s3

import java.io.InputStream
import java.util

import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.{AmazonS3, AmazonS3Client}
import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import models.{DbxMeta, MetaDetail, S3Meta}
import replicas.FileService
import utils.AppUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by toidiu on 11/17/16.
  */
object S3Service extends FileService {

  lazy val cred = new BasicAWSCredentials(AppUtils.s3AccessKey, AppUtils.s3SecretKey)
  lazy val s3: AmazonS3 = new AmazonS3Client(cred)
  lazy val bucket = s3.createBucket(AppUtils.s3Bucket)

  override def postFile(meta: ByteString, key: String, inputStream: InputStream): Future[Either[_, Boolean]] = {
    val metaObj: ObjectMetadata = new ObjectMetadata()
    val map: util.Map[String, String] = new util.HashMap()
    map.put("meta", meta.utf8String)
    metaObj.setUserMetadata(map)

    val fut: Future[Either[_, Boolean]] = for {
      s <- Future(s3.putObject(bucket.getName, key, inputStream, metaObj))
    } yield Right(true)

    fut.recover { case e: Exception => Left(e.toString) }
  }

  override def getFile(key: String): Future[Source[ByteString, _]] = {
    val op = s3.getObject(bucket.getName, key)
    Future(StreamConverters.fromInputStream(op.getObjectContent))
  }

  override def getMetaString(key: String): Future[String] = {
    val op: Future[ObjectMetadata] = Future(s3.getObjectMetadata(bucket.getName, key))

    op.flatMap(e => Future(e.getUserMetaDataOf("meta")))
  }

  def buildS3Meta(meta: S3Meta): ByteString = ByteString(meta.asJson.noSpaces)

  override def buildMetaBytes(bytes: Long, mime: String,
                              uploadedAt: String, key: String): ByteString = {
    val detail: MetaDetail = MetaDetail(None, Some(AppUtils.s3Bucket), Some(key))
    val jsonString = S3Meta(bytes, mime, uploadedAt, "s3", detail).asJson.noSpaces
    ByteString(jsonString)
  }
}

