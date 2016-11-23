package models

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}

import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

/**
  * Created by toidiu on 11/22/16.
  */
case class Meta(length: Long, mime: String, ts: String) {
  //case class Meta(length: Length, mime: Mime, ts: MetaDate) {
}

//case class Length(long: Long)

//
//case class Mime(string: String)

//
//case class MetaDate(dateTime: ZonedDateTime)
//
//  override def toString: String = MetaDate.asString(this)
//}

//-=-=-=-=-=-=-=-==-==-==-==-=-=-=-=-=-=-
//Objects
//-=-=-=-=-=-=-=-==-==-==-==-=-=-=-=-=-=-
object Meta {
  import io.circe._, io.circe.generic.semiauto._

//  implicit val fooDecoder: Decoder[Meta] = deriveDecoder[Meta]
  //  def asString(meta: Meta) = meta.asJson.noSpaces

  //  def asMeta(string: String) = decode[Meta](string)
}

object MetaDate {
  def getNow = ZonedDateTime.now(ZoneOffset.UTC)

  //  def getNowString :String = LocalDateTime.now(ZoneOffset.UTC).format(MetaDate.formatter)

  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")

  //
  def asString = getNow.format(MetaDate.formatter)

  def asMetaDate(string: String) = ZonedDateTime.parse(string, formatter)
}
