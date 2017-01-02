package utils

/**
  * Created by toidiu on 12/4/16.
  */
object ErrorUtils {

  final class FsReadException(msg: String) extends Exception(msg)

  final class FsMinReplicaException(msg: String) extends Exception(msg)

  case class MetaError(backend: String, error: String)

}
