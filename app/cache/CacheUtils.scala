package cache

import java.io._
import java.util.UUID

import org.apache.commons.io.IOUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * Created by toidiu on 12/27/16.
  */
object CacheUtils {

  val CACHE_FOLDER = "localFileCache"
  val cacheFolder = new File(CACHE_FOLDER)
  cacheFolder.mkdirs()

  //Currently there is no cache functionality. The file is deleted after each request.
  def saveCachedFile(key: String, in: InputStream): Future[Try[File]] = {
    val f = new File(cacheFolder, key + "_" + System.currentTimeMillis() + UUID.randomUUID())
    val out = new FileOutputStream(f)
    Future {
      IOUtils.copy(in, out)
      in.close()
      out.close()
      Success(f)
    }.recover {
      case e: Exception =>
        Try(out.close())
        Try(in.close())
        Failure(e)
    }
  }

}
