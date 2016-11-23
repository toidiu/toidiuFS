package utils

import java.time.{ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter

/**
  * Created by toidiu on 11/23/16.
  */
object TimeUtils {
  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")

  def getZoneTimeNow = ZonedDateTime.now(ZoneOffset.UTC)

  def zoneAsString = getZoneTimeNow.format(formatter)

  def zoneFromString(string: String) = ZonedDateTime.parse(string, formatter)
}
