package utils

import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import play.api.libs.streams.Accumulator
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.Future

/**
  * Created by toidiu on 11/2/16.
  */
object AppUtils {
  val conf = ConfigFactory.load()

  val dbxEnable = conf.getBoolean("dropbox.enable")
  val dbxToken: String = conf.getString("dropbox.token")
  val dbxPath: String = conf.getString("dropbox.path")
  val dbxIsWhiteList = conf.getBoolean("dropbox.isWhiteList")
  val dbxMimeList = conf.getStringList("dropbox.mimeList")
  val dbxMaxLength = conf.getLong("dropbox.maxLength")

  val s3Enable = conf.getBoolean("s3.enable")
  val s3AccessKey: String = conf.getString("s3.accessKey")
  val s3SecretKey: String = conf.getString("s3.secretKey")
  val s3Bucket: String = conf.getString("s3.bucket")
  val s3IsWhiteList = conf.getBoolean("s3.isWhiteList")
  val s3MimeList = conf.getStringList("s3.mimeList")
  val s3MaxLength = conf.getLong("s3.maxLength")

  val repMin: Int = conf.getInt("toidiufs.replication.min")

}
