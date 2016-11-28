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
    val nowStr = zoneAsString
    val now = zoneFromString(nowStr)
    val nowStr2 = zoneAsString(now)
    nowStr2 mustEqual nowStr
  }

}
