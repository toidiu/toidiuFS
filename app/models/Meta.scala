package models

import java.time.format.DateTimeFormatter
import java.time.{ZoneOffset, ZonedDateTime}

import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

/**
  * Created by toidiu on 11/22/16.
  */
case class Meta(copies: (DbxMeta, S3Meta))

case class DbxDteail(path: String)

case class DbxMeta(bytes: Long, mime: String, uploadedAt: String, backend: String = "dropbox", detail: DbxDteail) {
  require(backend.equals("dropbox"))
}

case class S3Dteail(bucket: String, key: String)

case class S3Meta(bytes: Long, mime: String, uploadedAt: String, backend: String = "s3", detail: S3Dteail) {
  require(backend.equals("s3"))
}
