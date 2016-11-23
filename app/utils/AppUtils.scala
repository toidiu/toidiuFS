package utils

import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import play.api.libs.streams.Accumulator
import play.api.mvc.BodyParser

/**
  * Created by toidiu on 11/2/16.
  */
object AppUtils {
  val conf = ConfigFactory.load()
  val dbxEnable = conf.getBoolean("dropbox.enable")
  val dbxToken: String = conf.getString("dropbox.token")
  val dbxPath: String = conf.getString("dropbox.path")
//  val dbxWhitelist = conf.getList("dropbox.whitelist")
//  val dbxBlacklist = conf.getList("dropbox.blacklist")

  val s3Enable = conf.getBoolean("s3.enable")
  val s3AccessKey: String = conf.getString("s3.accessKey")
  val s3SecretKey: String = conf.getString("s3.secretKey")
  val s3Bucket: String = conf.getString("s3.bucket")
//  val s3Whitelist = conf.getList("s3.whitelist")
//  val s3Blacklist = conf.getList("s3.blacklist")

  val repMin: Int = conf.getInt("toidiufs.replicationMin")


  //  replicatio minimum

  //-=-=-=-=-=-=-=-==-==-==-==-=-=-=-=-=-=-
  //left to do
  //-=-=-=-=-=-=-=-==-==-==-==-=-=-=-=-=-=-
  //  WHITE/BLACK LIST
  //    mime allowed
  //    size allowed


  def streamBP: BodyParser[Source[ByteString, _]] = BodyParser { _ =>
    import scala.concurrent.ExecutionContext.Implicits.global
    // Return the source directly. We need to return
    // an Accumulator[Either[Result, T]], so if we were
    // handling any errors we could map to something like
    // a Left(BadRequest("error")). Since we're not
    // we just wrap the source in a Right(...)
    val s = Accumulator.source[ByteString]
      .map(d => Right.apply(d))
    s
  }
}
