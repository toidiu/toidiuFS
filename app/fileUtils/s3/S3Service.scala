package fileUtils.s3

import java.io.InputStream
import java.util
import java.util.concurrent.TimeUnit

import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.{AmazonS3, AmazonS3Client}
import fileUtils.FileService
import utils.AppUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

/**
  * Created by toidiu on 11/17/16.
  */
object S3Service extends FileService {

  val cred = new BasicAWSCredentials(AppUtils.s3AccessKey, AppUtils.s3SecretKey)
  val s3: AmazonS3 = new AmazonS3Client(cred)
  val bucket = s3.createBucket(AppUtils.s3Bucket)

  override def postFile(meta: ByteString, key: String, stream: Source[ByteString, _]): Future[Either[_, Boolean]] = {
    val inputStream: InputStream = stream.runWith(StreamConverters.asInputStream(FiniteDuration(3, TimeUnit.SECONDS)))

    val meta: ObjectMetadata = new ObjectMetadata()
    val map: util.Map[String, String] = new util.HashMap()
    map.put("meta", meta.toString)
    meta.setUserMetadata(map)

    val fut: Future[Either[_, Boolean]] = for {
      s <- Future(s3.putObject(bucket.getName, key, inputStream, meta))
    } yield Right(true)


    fut.recover { case e: Exception => Left(e.toString) }
  }

  override def getFile(key: String): Future[Source[ByteString, _]] = {
    val op = s3.getObject(bucket.getName, key)
    Future(StreamConverters.fromInputStream(op.getObjectContent))
  }

  override def getMeta(key: String): Future[Source[ByteString, _]] = {
    val op: Future[ObjectMetadata] = Future(s3.getObjectMetadata(bucket.getName, key))

    Future(Source.fromFuture {
      op.map(m => ByteString(m.getUserMetaDataOf("meta")))
    })
  }

}


