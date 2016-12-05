package logic

import replicas.FileService
import utils.AppUtils
import utils.AppUtils._
import utils.ErrorUtils.MinReplicaException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * Created by toidiu on 11/24/16.
  */
object FsWriteLogic {

  def checkLockAndAcquireLock(key: String, list: List[FileService]): Future[Try[List[FileService]]] = {
    val futList = list.map(_.inspectOrCreateLock(key))
    Future.sequence(futList).map { lockList =>
      val availableFS = list.zip(lockList)
        .filter(tup => tup._2.isRight && !tup._2.right.get.locked)
        .map(_._1)
      availableFS match {
        case fsList if fsList.length >= AppUtils.repMin =>
          //acquire lock
          Success(fsList.map { fs => fs.acquireLock(key); fs })
        case fsList =>
          fsList.map(_.releaseLock(key))
          val serversList = fsList.foldLeft("")((a, b) => a + b.getClass.getSimpleName)
          Failure(new MinReplicaException("We don't meet the min replicas due to locks/availability. Available servers: " + serversList))
      }
    }
  }

  def checkFsConfigConstraints(mime: String, length: Long): Try[List[FileService]] = {
    //-----check: length, mime, enabled
    val retList = ALL_SERVICES.filter(isFsConfigValid(mime, length))

    //-----check: if we meet min replica
    retList match {
      case l if l.length >= repMin => Success(l)
      case l => {
        val serversList = l.foldLeft("")((a, b) => a + b.getClass.getSimpleName)
        Failure(new MinReplicaException("We don't meet the min replicas due to config restraints. Valid servers: " + serversList))
      }
    }
  }

  def isFsConfigValid(mime: String, length: Long): (FileService) => Boolean = { fs =>
    fs.isEnable && length < fs.maxLength && isMimeAllowed(mime, fs.isWhiteList, fs.mimeList)
  }


  def isMimeAllowed(mime: String, isWhiteList: Boolean, list: List[String]): Boolean =
    if (isWhiteList) list.contains(mime) else !list.contains(mime)

}
