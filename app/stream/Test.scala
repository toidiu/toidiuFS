

//package stream
//
//import akka.stream.FlowShape
//import akka.stream.scaladsl.GraphDSL.Implicits._
//import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Sink, Source}
//import akka.util.ByteString
//import fileUtils.s3.S3Service
//import play.api.libs.streams.Accumulator
//
//import scala.concurrent.Future
//
///**
//  * Created by toidiu on 11/22/16.
//  */
//object Test {
//
//
//  def test[A](meta: ByteString, key: String, stream: Source[ByteString, A]) = {
//
//    val sink = Sink.ignore
//
//    val graph = GraphDSL.create() { implicit builder =>
//      //      val input = builder.add(Flow[ByteString, _])
//
//
//      val B = builder.add(Broadcast[ByteString](2))
//      val bufferSize = 100
//      val overflowStrategy = akka.stream.OverflowStrategy.dropHead
//
//      val s3 = builder.add(
//        Flow[ByteString]
//          .map {
//            Accumulator[ByteString, Source[ByteString, _]]()//().map(Right.apply)
//          }
//          .map(d => S3Service.postFile(meta, key, d))
//      )
//
//      //      val db = builder.add(
//      //        Flow[ByteString, _]
//      //          .map(Accumulator.source[ByteString].map(Right.apply))
//      ////          .map ( d => S3Service.postFile(meta, key, d))
//      //          .map(d => DbxService.postFile(meta, key, d))
//      //      )
//
//      val done = builder.add(Merge[Future[Either[_, Boolean]]](2))
//
//      B ~> s3 ~> done
//      //      B ~> db ~> done
//
//      FlowShape(B.in, done.out)
//    }
//
//
//    stream
//      .via(graph)
//      .runWith(sink)
//    //      .onComplete { a =>
//    //        a
//    //        logger.info("Done!")
//    //        shutdown()
//    //      }
//  }
//
//
//}
