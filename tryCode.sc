val l = List("text/plain", "image/png")

val s = "text/plain; charset=UTF-8"


l.foldLeft(false)((a, b) => a || s.contains(b))


//import java.time.format.DateTimeFormatter
//import java.time.{ZoneOffset, ZonedDateTime}
//
import scala.concurrent.Future
//
////import models.{ Mime}
////import models.{Length}
////import models.{Meta}
//import io.circe._
//import io.circe.generic.JsonCodec
//import io.circe.generic.auto._
//import io.circe.generic.semiauto._
//import io.circe.parser.{decode, _}
//import io.circe.syntax._
//
//
//case class Meta(bytes: Long, mime: String, copies: (DbxMeta, S3Meta))
//
//case class DbxDteail(path: Option[String]=None, bucket: Option[String] = None, key: Option[String] = None)
//
//case class DbxMeta(uploadedAt: String, backend: String = "dropbox", detail: DbxDteail) {
//  require(backend.equals("dropbox"))
//}
//
//case class S3Dteail(bucket: String, key: String)
//
//case class S3Meta(uploadedAt: String, backend: String = "s3", detail: S3Dteail) {
//  require(backend.equals("s3"))
//}
//
//
////--------
////DbxMeta.`<init>$default$2`
////DbxDteail("path").asJson
////val m = Meta(3L, "mime", (DbxMeta("timeNow", "dropbox", DbxDteail("path")), S3Meta("timeNow", "s3", S3Dteail("buc", "key"))))
////m.asJson.noSpaces
//
//
////--------
//def getNow = ZonedDateTime.now(ZoneOffset.UTC)
//val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
//getNow.format(formatter)

//val l = List("a","b","c")
//l.zipWithIndex
//
//
//def d: () => String = () => "hi"
//
//d.apply()
