package models

import io.circe._
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

/**
  * Created by toidiu on 11/22/16.
  */
case class Meta(copies: MetaServer*)

case class MetaError(backend: String, error: String)

case class MetaDetail(path: Option[String] = None, bucket: Option[String] = None, key: Option[String] = None)


case class MetaServer(
                       bytes: Long
                       , mime: String
                       , uploadedAt: String
                       , backend: String
                       , detail: MetaDetail
                     )
