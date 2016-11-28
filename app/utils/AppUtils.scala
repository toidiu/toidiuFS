package utils

import com.typesafe.config.ConfigFactory
import replicas.dbx.DbxService
import replicas.s3.S3Service

import scala.collection.JavaConversions._

/**
  * Created by toidiu on 11/2/16.
  */
object AppUtils {
  val conf = ConfigFactory.load()

  val dbxEnable = conf.getBoolean("toidiufs.dropbox.enable")
  val dbxToken: String = conf.getString("toidiufs.dropbox.token")
  val dbxPath: String = conf.getString("toidiufs.dropbox.path")
  val dbxIsWhiteList = conf.getBoolean("toidiufs.dropbox.isWhiteList")
  val dbxMimeList = conf.getStringList("toidiufs.dropbox.mimeList").toList
  val dbxMaxLength = conf.getLong("toidiufs.dropbox.maxLength")

  val s3Enable = conf.getBoolean("toidiufs.s3.enable")
  val s3AccessKey: String = conf.getString("toidiufs.s3.accessKey")
  val s3SecretKey: String = conf.getString("toidiufs.s3.secretKey")
  val s3Bucket: String = conf.getString("toidiufs.s3.bucket")
  val s3IsWhiteList = conf.getBoolean("toidiufs.s3.isWhiteList")
  val s3MimeList = conf.getStringList("toidiufs.s3.mimeList").toList
  val s3MaxLength = conf.getLong("toidiufs.s3.maxLength")

  val repMin: Int = conf.getInt("toidiufs.replication.min")


  lazy val ALL_SERVICES = List(DbxService, S3Service).filter(_.isEnable)
}
