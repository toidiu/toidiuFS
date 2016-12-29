package logic

import java.io.FileInputStream

import play.api.libs.Files.TemporaryFile
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, Ok}
import replicas.FileService
import utils.AppUtils.{ALL_SERVICES, repMin}
import utils.ErrorUtils.FsMinReplicaException
import utils.TimeUtils

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
      confFSList <- Future(getConfigValidServices(mime, length))
      lockFSList <- acquireLock(key, confFSList.get)
    } yield lockFSList
  }

  private def acquireLock(key: String, list: List[FileService]): Future[Try[List[FileService]]] = {
    val futCheckLock = for {
      inspectLock <- Future.sequence(list.map(_.inspectOrCreateLock(key)))
    } yield for {
      (fs, lock) <- list.zip(inspectLock)
      if lock.isSuccess && !lock.get.locked
    } yield fs

    val futAcqLockList = for {
      fsList <- futCheckLock
      if fsList.length >= repMin
    } yield for {
      fs <- fsList
    } yield for {
      acqLock <- fs.acquireLock(key)
      if acqLock.isSuccess
    } yield fs

    (for {
      flatten <- futAcqLockList
      fsList <- Future.sequence(flatten)
    } yield Success(fsList)) recover { case _ =>
      Failure(new FsMinReplicaException("We don't meet the min replicas due to locks/availability."))
    }
  }

  private def getConfigValidServices(mime: String, length: Long): Try[List[FileService]] = {
    ALL_SERVICES.filter(isFsConfigValid(mime, length)) match {
      case fsList if fsList.length >= repMin => Success(fsList)
      case fsList =>
        val serversList = fsList.map(_.getClass.getSimpleName).mkString(", ")
        Failure(new FsMinReplicaException("We don't meet the min replicas due to config restraints. Valid servers: " + serversList))
    }
  }

  private[logic] def isFsConfigValid(mime: String, length: Long): (FileService) => Boolean = { fs =>
    fs.isEnable && length < fs.maxLength && isMimeAllowed(mime, fs.isWhiteList, fs.mimeList)
  }

  private[logic] def isMimeAllowed(mime: String, isWhiteList: Boolean, list: List[String]): Boolean =
    if (isWhiteList) list.contains(mime) else !list.contains(mime)

}
