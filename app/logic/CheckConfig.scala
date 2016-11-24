package logic

import replicas.FileService
import utils.AppUtils

/**
  * Created by toidiu on 11/24/16.
  */
object CheckConfig {

  def configFilter(key: String, mime: String, length: Long): Either[String, List[FileService]] = {

    //test whitelist
    //test content length
    val includeDbx = if (AppUtils.dbxIsWhiteList) AppUtils.dbxMimeList.contains(mime) else !AppUtils.dbxMimeList.contains(mime)



    AppUtils.s3IsWhiteList
    AppUtils.s3MimeList

    AppUtils.repMin

    Right(List())
  }


}
