import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter

////import models.{ Mime}
////import models.{Length}
////import models.{Meta}
////import models.{MetaDate}
//import java.time.LocalDateTime
//
//import io.circe._
//import io.circe.generic.auto._
//import io.circe.parser._
//import io.circe.syntax._
//import org.joda.time.DateTime
//
//
//case class Meta(length: Length, mime: String, ts: DateTime)
//
//case class Length(long: Long)
//
////case class Length(long: Long)
////2
////val m = Meta(Length(4L), Mime("asfd"), MetaDate(MetaDate.getNow)).toString
//val m = Meta(Length(4L), "asdf", DateTime.now()) //.toString
//
//m.asJson.noSpaces

def getNow = ZonedDateTime.now(ZoneOffset.UTC)

//  def getNowString :String = LocalDateTime.now(ZoneOffset.UTC).format(MetaDate.formatter)

val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")


getNow.format(formatter)