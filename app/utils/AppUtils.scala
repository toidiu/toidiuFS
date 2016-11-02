package utils

import com.typesafe.config.ConfigFactory

/**
  * Created by toidiu on 11/2/16.
  */
object AppUtils {
  val conf = ConfigFactory.load()
  var dropboxEnable = conf.getBoolean("dropbox.enable")
  var dropboxKey :String = conf.getString("dropbox.key")
  var dropboxSecret :String = conf.getString("dropbox.secret")
  var dropboxToken :String = conf.getString("dropbox.token")

}
