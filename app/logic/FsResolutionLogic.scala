package logic

import java.io.{File, FileInputStream}

import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import models.MetaServer
import replicas.FileService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

/**
  * Created by toidiu on 11/27/16.
  */
object FsResolutionLogic {

  type ResolutionPart = List[File] => Resolution
  type Resolution = () => Future[Unit]

  def partResolution(key: String, meta: MetaServer, needsRes: List[FileService]): ResolutionPart = {
    attemptResolution(key, meta, needsRes)(_)
  }

  private def attemptResolution(key: String, meta: MetaServer, needsRes: List[FileService])(fileList: List[File]): Resolution = () => {
    val isConfigValid = FsWriteFileLogic.isFsConfigValid(meta.mime, meta.bytes)

    val copyFileToServicePart = copyFileToService(key, meta, fileList.head)
    val listFut = needsRes
      .withFilter(fs => isConfigValid(fs))
      .map { fs => copyFileToServicePart(fs) }
    Future.sequence(listFut).map(_ => fileList.foreach(_.delete()))
  }

  private def copyFileToService(key: String, meta: MetaServer, file: File) = { fs: FileService =>
    val is = new FileInputStream(file)
    val metaBytes = fs.buildMetaBytes(meta.bytes, meta.mime, meta.uploadedAt, key)
    fs.postFile(metaBytes, key, is).map(_ => Try(is.close()))
  }

}
