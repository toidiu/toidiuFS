package models

import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

/**
  * Created by toidiu on 11/22/16.
  */
case class Meta(copies: (DbxMeta, S3Meta))

case class MetaError(backend: String, error: String)

case class MetaDetail(path: Option[String] = None, bucket: Option[String] = None, key: Option[String] = None)

//object bla {
//  implicit val encodeFoo: Encoder[MetaDetail] = new Encoder[MetaDetail] {
//    final def apply(detail: MetaDetail): Json = {
//      val map = Map("", Json.fromString(""))
//      detail.
//       JsonObject.fromMap(map).asJson
//    }
//  }
//}

//region Dbx-=-=-=-=-=-=-=-==-==-==-==-=-=-=-=-=-=-

sealed trait MetaServer {
  val bytes: Long
  val mime: String
  val uploadedAt: String
  val backend: String
  val detail: MetaDetail
}

case class DbxMeta(bytes: Long, mime: String, uploadedAt: String, backend: String = "dropbox", detail: MetaDetail) extends MetaServer {
  require(backend.equals("dropbox"))
}

case class S3Meta(bytes: Long, mime: String, uploadedAt: String, backend: String = "s3", detail: MetaDetail) extends MetaServer {
  require(backend.equals("s3"))
}
