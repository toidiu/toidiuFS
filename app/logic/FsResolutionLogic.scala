package logic

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.StreamConverters
import akka.util.ByteString
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
import scala.concurrent.duration.FiniteDuration

/**
  * Created by toidiu on 11/27/16.
  */
object FsResolutionLogic {
  //  implicit val timeout = new Timeout(20 seconds)
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def attemptResolution(key: String, metaServer: MetaServer, updatedAndNeedRes: (List[FileService], List[FileService])): () => Future[List[Any]] = () => {
    val parCheckFsConfig = FsWriteLogic.isFsConfigValid(metaServer.mime, metaServer.bytes)

    updatedAndNeedRes match {
      case (hUp :: t, res) if res.nonEmpty =>
        hUp.getFile(key).flatMap {
          case Right(source) =>
            val is = source.runWith(StreamConverters.asInputStream(FiniteDuration(5, TimeUnit.SECONDS)))

            val futList = res.map(resFs => parCheckFsConfig(resFs) match {
              case true => resFs.postFile(ByteString(metaServer.asJson.noSpaces), key, is)
              case false => Future(Nil)
            })
            Future.sequence(futList)
          case Left(err) => Future(Nil)
        }
      case _ => Future(Nil)
    }
  }

}
