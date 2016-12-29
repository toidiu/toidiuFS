package logic

import java.io.FileInputStream

import play.api.libs.Files.TemporaryFile
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, Ok}
import replicas.FileService
import utils.AppUtils._
import utils.ErrorUtils.FsMinReplicaException
import utils.{AppUtils, TimeUtils}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * Created by toidiu on 11/24/16.
  */
object FsWriteLogic {

  def resultPostFile(key: String, mime: String, length: Long, tempFile: TemporaryFile): Future[Result] = {
    checkConfigAcquireLock(key, mime, length).flatMap {
      case Success(lockList) =>
        //attempt upload of file
        val listFut = for (fs <- lockList) yield {
          val metaBytes = fs.buildMetaBytes(length, mime, TimeUtils.zoneAsString, key)
          fs.postFile(metaBytes, key, new FileInputStream(tempFile.file))
        }
        Future.sequence(listFut).map(resList => Ok(resList.toString))
      case Failure(err) => Future(BadRequest(err.getMessage))
    }
  }

  private def checkConfigAcquireLock(key: String, mime: String, length: Long): Future[Try[List[FileService]]] = {
    for {
      confFSList <- Future(FsWriteLogic.checkFsConfigConstraints(mime, length))
      lockFSList <- FsWriteLogic.checkLockAndAcquireLock(key, confFSList.get)
    } yield lockFSList
  }

  private def checkLockAndAcquireLock(key: String, list: List[FileService]): Future[Try[List[FileService]]] = {
    val futList = list.map(_.inspectOrCreateLock(key))
    Future.sequence(futList).map { lockList =>
      val availableFS = list.zip(lockList)
        .withFilter {
          case (fs, Success(metaEither)) => !metaEither.locked
          case (_, _) => false
        }
        .map { case (fs, metaEither) => fs }
      availableFS match {
        case fsList if fsList.length >= AppUtils.repMin =>
          //acquire lock
          Success(fsList.map { fs => fs.acquireLock(key); fs })
        case fsList =>
          fsList.map(_.releaseLock(key))
          val serversList = fsList.foldLeft("")((a, b) => a + b.getClass.getSimpleName)
          Failure(new FsMinReplicaException("We don't meet the min replicas due to locks/availability. Available servers: " + serversList))
      }
    }
  }

  private def checkFsConfigConstraints(mime: String, length: Long): Try[List[FileService]] = {
    //-----check: length, mime, enabled
    val retList = ALL_SERVICES.filter(isFsConfigValid(mime, length))

    //-----check: if we meet min replica
    retList match {
      case l if l.length >= repMin => Success(l)
      case l =>
        val serversList = l.foldLeft("")((a, b) => a + b.getClass.getSimpleName)
        Failure(new FsMinReplicaException("We don't meet the min replicas due to config restraints. Valid servers: " + serversList))
    }
  }

  private[logic] def isFsConfigValid(mime: String, length: Long): (FileService) => Boolean = { fs =>
    fs.isEnable && length < fs.maxLength && isMimeAllowed(mime, fs.isWhiteList, fs.mimeList)
  }


  private[logic] def isMimeAllowed(mime: String, isWhiteList: Boolean, list: List[String]): Boolean =
    if (isWhiteList) list.contains(mime) else !list.contains(mime)

}
