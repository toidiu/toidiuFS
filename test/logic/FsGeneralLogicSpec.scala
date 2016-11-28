package logic

import logic.FsGeneralLogic.mimeToExtension
import org.specs2._


/**
  * Created by toidiu on 11/28/16.
  */
class FsGeneralLogicSpec extends Specification {
  def is =
    s2"""

    mime should give the correct extension
      for txt $s1
      for png $s1
      for pdf $s1

  """

  def s1 =
    mimeToExtension("text/plain") mustEqual ".txt"
  def s2 =
    mimeToExtension("image/png") mustEqual ".png"
  def s3 =
    mimeToExtension("application/pdf") mustEqual ".pdf"

}
