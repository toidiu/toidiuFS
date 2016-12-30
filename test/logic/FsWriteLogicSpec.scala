package logic

import logic.FsWriteFileLogic._
import org.specs2._
import replicas._


/**
  * Created by toidiu on 11/28/16.
  */
class FsWriteLogicSpec extends Specification {
  def is =
    s2"""

    config check should
      return true for general mock service $s1
      return false for disabled service $s2
      return false if byte max is over the config $s3
      return false if mime is not in white list $s4
      return true if mime is in white list $s5
      return false if mime is in black list $s6
      return true if mime is in black list $s7

  """


  def s1 = isFsConfigValid("", 1)(MockService1) mustEqual true

  def s2 = isFsConfigValid("", 1)(MockDisabledService) mustEqual false

  def s3 = isFsConfigValid("", 11)(Mock10MaxLengthService) mustEqual false

  def s4 = isFsConfigValid("not txt", 1)(MockWhiteTxtService) mustEqual false

  def s5 = isFsConfigValid("text/plain", 1)(MockWhiteTxtService) mustEqual true

  def s6 = isFsConfigValid("text/plain", 1)(MockBlackTxtService) mustEqual false

  def s7 = isFsConfigValid("not txt", 1)(MockBlackTxtService) mustEqual true



}
