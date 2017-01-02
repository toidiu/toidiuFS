package utils

import com.typesafe.config.ConfigFactory
import replicas.dbx.DbxService
import replicas.s3.S3Service

/**
  * Created by toidiu on 11/2/16.
  */
object AppUtils {
  lazy val ALL_SERVICES = List(DbxService, S3Service).filter(_.isEnable)
  val conf = ConfigFactory.load()
  val repMin: Int = conf.getInt("toidiufs.replication.min")
}
