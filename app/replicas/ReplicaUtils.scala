package replicas

import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.JavaConversions._

/**
  * Created by toidiu on 11/2/16.
  */
private[replicas] object ReplicaUtils {
  val conf: Config = ConfigFactory.load()

  val dbxEnable: Boolean = conf.getBoolean("toidiufs.dropbox.enable")
  val dbxToken: String = conf.getString("toidiufs.dropbox.token")
  val dbxPath: String = conf.getString("toidiufs.dropbox.path")
  val dbxIsWhiteList: Boolean = conf.getBoolean("toidiufs.dropbox.isWhiteList")
  val dbxMimeList: List[String] = conf.getStringList("toidiufs.dropbox.mimeList").toList
  val dbxMaxLength: Long = conf.getLong("toidiufs.dropbox.maxLength")

  val s3Enable: Boolean = conf.getBoolean("toidiufs.s3.enable")
  val s3AccessKey: String = conf.getString("toidiufs.s3.accessKey")
  val s3SecretKey: String = conf.getString("toidiufs.s3.secretKey")
  val s3Bucket: String = conf.getString("toidiufs.s3.bucket")
  val s3IsWhiteList: Boolean = conf.getBoolean("toidiufs.s3.isWhiteList")
  val s3MimeList: List[String] = conf.getStringList("toidiufs.s3.mimeList").toList
  val s3MaxLength: Long = conf.getLong("toidiufs.s3.maxLength")

}
