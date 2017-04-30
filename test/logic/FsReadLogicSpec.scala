package logic

import logic.FsReadFileLogic.filterMostUpdatedFs
import models.{MetaDetail, MetaServer}
import org.specs2._
import replicas.{FileService, MockService1, MockService2, MockService3}
import utils.TimeUtils._

/**
  * Created by toidiu on 11/28/16.
  */
class FsReadLogicSpec extends Specification {
  def is =
    s2"""

    filterMostUpdated should
      return server with latest upload time $s1
      return server with latest upload time $s2
      return server list with latest upload time $s3
      return server list with latest upload time $s4

  """


  def s1 = {
    val now = getZoneTimeNow

    val ms1 = (MockService1, MetaServer(1L, "mime", zoneTimeAsString(now), "backend", MetaDetail()))
    val ms2 = (MockService2, MetaServer(1L, "mime", zoneTimeAsString(now.plusSeconds(1)), "backend", MetaDetail()))

    val list: List[(FileService, MetaServer)] = List(ms1)
    val nxt: (FileService, MetaServer) = ms2

    val fil = filterMostUpdatedFs(list, nxt)
    fil mustEqual List(ms2)
  }

  def s2 = {
    val now = getZoneTimeNow

    val ms1 = (MockService1, MetaServer(1L, "mime", zoneTimeAsString(now.plusSeconds(1)), "backend", MetaDetail()))
    val ms2 = (MockService2, MetaServer(1L, "mime", zoneTimeAsString(now), "backend", MetaDetail()))

    val list: List[(FileService, MetaServer)] = List(ms1)
    val nxt: (FileService, MetaServer) = ms2

    val fil = filterMostUpdatedFs(list, nxt)
    fil mustEqual List(ms1)
  }


  def s3 = {
    val now = getZoneTimeNow
    val laterTime: String = zoneTimeAsString(now.plusSeconds(5))

    val ms1 = (MockService1, MetaServer(1L, "mime", laterTime, "backend", MetaDetail()))
    val ms2 = (MockService2, MetaServer(1L, "mime", laterTime, "backend", MetaDetail()))
    val ms3 = (MockService3, MetaServer(1L, "mime", zoneTimeAsString(now), "backend", MetaDetail()))

    val list: List[(FileService, MetaServer)] = List(ms1, ms2)
    val nxt: (FileService, MetaServer) = ms3

    val fil = filterMostUpdatedFs(list, nxt)
    fil mustEqual List(ms1, ms2)
  }

  def s4 = {
    val now = getZoneTimeNow
    val muchLaterTime = zoneTimeAsString(now.plusSeconds(5))
    val laterTime = zoneTimeAsString(now.plusSeconds(2))

    val ms1 = (MockService1, MetaServer(1L, "mime", zoneTimeAsString(now), "backend", MetaDetail()))
    val ms2 = (MockService2, MetaServer(1L, "mime", muchLaterTime, "backend", MetaDetail()))
    val ms3 = (MockService3, MetaServer(1L, "mime", muchLaterTime, "backend", MetaDetail()))
    val ms4 = (MockService3, MetaServer(1L, "mime", laterTime, "backend", MetaDetail()))

    val list: List[(FileService, MetaServer)] = List(ms1, ms2, ms3, ms4)

    val fil = list.foldLeft(Nil: List[(FileService, MetaServer)])(filterMostUpdatedFs)

    fil mustEqual List(ms3, ms2)
  }


}
