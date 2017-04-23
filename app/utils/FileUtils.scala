package utils

import play.api.http.FileMimeTypes

/**
  * Created by toidiu on 4/23/17.
  */

class SimpleFileMimeTypes(mime:String) extends FileMimeTypes {
  def forFileName(name: String): Option[String] = Some(mime)
}

object SimpleFileMimeTypes {
  def apply(mime: String): SimpleFileMimeTypes = new SimpleFileMimeTypes(mime)
}
