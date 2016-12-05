package utils

/**
  * Created by toidiu on 12/4/16.
  */
object ErrorUtils {

  final class MinReplicaException(msg:String) extends Exception(msg)

  case class MetaError(backend: String, error: String)

}
