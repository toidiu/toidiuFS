package utils

import java.time.format.DateTimeFormatter
import java.time.{ZoneOffset, ZonedDateTime}

/**
  * Created by toidiu on 11/23/16.
  */
object TimeUtils {
  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")

  def zoneTimeAsString = getZoneTimeNow.format(formatter)

  def getZoneTimeNow = ZonedDateTime.now(ZoneOffset.UTC)

  def zoneTimeAsString(time: ZonedDateTime) = time.format(formatter)

  def zoneTimeFromString(string: String) = ZonedDateTime.parse(string, formatter)
}

