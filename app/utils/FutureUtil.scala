package utils

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

/**
  * Created by toidiu on 11/23/16.
  */
object FutureUtil {

  /**
    * "Select" off the first future to be satisfied.  Return this as a
    * result, with the remainder of the Futures as a sequence.
    *
    * @param fs a scala.collection.Seq
    */
  def first[A](fs: Seq[Future[A]])(implicit ec: ExecutionContext): Future[(Try[A], Seq[Future[A]])] = {
    @tailrec
    def stripe(p: Promise[(Try[A], Seq[Future[A]])],
               heads: Seq[Future[A]],
               elem: Future[A],
               tail: Seq[Future[A]]): Future[(Try[A], Seq[Future[A]])] = {
      elem onComplete { res => if (!p.isCompleted) p.trySuccess((res, heads ++ tail)) }
      if (tail.isEmpty) p.future
      else stripe(p, heads :+ elem, tail.head, tail.tail)
    }

    if (fs.isEmpty) Future.failed(new IllegalArgumentException("empty future list!"))
    else stripe(Promise(), fs.genericBuilder[Future[A]].result, fs.head, fs.tail)
  }
}
