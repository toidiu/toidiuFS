package utils

import java.time.format.DateTimeFormatter
import java.time.{ZoneOffset, ZonedDateTime}

/**
  * Created by toidiu on 11/23/16.
  */
object TimeUtils {
  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")

  def zoneAsString = getZoneTimeNow.format(formatter)

  def getZoneTimeNow = ZonedDateTime.now(ZoneOffset.UTC)

  def zoneAsString(time: ZonedDateTime) = time.format(formatter)

  def zoneFromString(string: String) = ZonedDateTime.parse(string, formatter)
}

