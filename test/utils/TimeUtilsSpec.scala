package utils

import org.specs2._
import utils.TimeUtils._


/**
  * Created by toidiu on 11/28/16.
  */
class TimeUtilsSpec extends Specification {
  def is =
    s2"""

    TimeUtils should
      convert to String and back $s1

  """

  def s1 = {
    val nowStr = zoneTimeAsString
    val now = zoneTimeFromString(nowStr)
    val nowStr2 = zoneTimeAsString(now)
    nowStr2 mustEqual nowStr
  }

}
