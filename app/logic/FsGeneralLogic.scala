package logic

/**
  * Created by toidiu on 11/27/16.
  */
object FsGeneralLogic {

  def mimeToExtension(mime: String): String =
    mime match {
      case "text/plain" => ".txt"
      case "image/png" => ".png"
      case "application/pdf" => ".pdf"
    }

}
