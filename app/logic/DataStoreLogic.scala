package logic

import replicas.FileService
import replicas.dbx.DbxService
import replicas.s3.S3Service
import utils.AppUtils._

/**
  * Created by toidiu on 11/24/16.
  */
object DataStoreLogic {

  def configFilter(mime: String, length: Long): Either[String, List[FileService]] = {
    //-----check: length, mime, enabled
    val includeDbx =
    if (length < dbxMaxLength && isMimeAllowed(mime, dbxIsWhiteList, dbxMimeList) && dbxEnable)
      List(DbxService)
    else Nil

    val includeS3 =
      if (length < s3MaxLength && isMimeAllowed(mime, s3IsWhiteList, s3MimeList) && s3Enable)
        List(S3Service)
      else Nil

    //-----check: if we meet min replica
    val retList: List[FileService] = includeDbx ::: includeS3 ::: Nil
    retList match {
      case l if l.length >= repMin => Right(l)
      case l => Left("Not enough servers to meet min replication")
    }
  }

  def isMimeAllowed(mime: String, isWhiteList: Boolean, list: List[String]): Boolean =
    if (isWhiteList) list.contains(mime) else !list.contains(mime)


}
