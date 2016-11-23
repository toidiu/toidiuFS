package fileUtils.s3

import java.io.InputStream
import java.util

import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.{AmazonS3, AmazonS3Client}
import fileUtils.FileService
import utils.AppUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by toidiu on 11/17/16.
  */
object S3Service extends FileService {

  val cred = new BasicAWSCredentials(AppUtils.s3AccessKey, AppUtils.s3SecretKey)
  val s3: AmazonS3 = new AmazonS3Client(cred)
  val bucket = s3.createBucket(AppUtils.s3Bucket)

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

//  override def getMeta(key: String): Future[Source[ByteString, _]] = {
    override def getMeta(key: String): Future[String] = {
    val op: Future[ObjectMetadata] = Future(s3.getObjectMetadata(bucket.getName, key))

    op.flatMap(e => Future(e.getUserMetaDataOf("meta")))


    //    Future(Source.fromFuture(
    //      op.map { m =>
    //        ByteString(m.getUserMetaDataOf("meta"))
    //      }
    //    ))
  }

}


