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
  var dropboxEnable = conf.getBoolean("dropbox.enable")
  var dropboxKey :String = conf.getString("dropbox.key")
  var dropboxSecret :String = conf.getString("dropbox.secret")
  var dropboxToken :String = conf.getString("dropbox.token")


  def streamBP: BodyParser[Source[ByteString, _]] = BodyParser { _ =>
    import scala.concurrent.ExecutionContext.Implicits.global
    // Return the source directly. We need to return
    // an Accumulator[Either[Result, T]], so if we were
    // handling any errors we could map to something like
    // a Left(BadRequest("error")). Since we're not
    // we just wrap the source in a Right(...)
    Accumulator.source[ByteString]
      .map(Right.apply)
  }
}
