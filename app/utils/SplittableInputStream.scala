package utils

/**
  * Created by toidiu on 11/22/16.
  */
//package javaUtils

import java.io.{IOException, InputStream}
import java.util.Collections

object SplittableInputStream {

  // Almost an input stream: The read-method takes an id.
  private object MultiplexedSource {
    private val MIN_BUF: Int = 4096
  }

  private[utils] class MultiplexedSource(// Underlying source
                                             var source: InputStream) {
    // Read positions of each SplittableInputStream
    private val readPositions: java.util.List[Integer] = new java.util.ArrayList[Integer]
    // Data to be read by the SplittableInputStreams
    private var buffer: Array[Int] = new Array[Int](MultiplexedSource.MIN_BUF)
    // Last valid position in buffer
    private var writePosition: Int = 0

    // Add a multiplexed reader. Return new reader id.
    private[utils] def addSource(splitId: Int): Int = {
      readPositions.add(if (splitId == -1) 0
      else readPositions.get(splitId))
      readPositions.size - 1
    }

    // Make room for more data (and drop data that has been read by
    // all readers)
//    private def readjustBuffer() {
//      val from: Int = Collections.min(readPositions)
//      val to: Int = Collections.max(readPositions)
//      val newLength: Int = Math.max((to - from) * 2, MultiplexedSource.MIN_BUF)
//      val newBuf: Array[Int] = new Array[Int](newLength)
//      System.arraycopy(buffer, from, newBuf, 0, to - from)
//      var i: Int = 0
//
//      while (i < readPositions.size)
//        readPositions.set(i, readPositions.get(i) - from) {
//          i += 1
//          i - 1
//        }
//      writePosition -= from
//      buffer = newBuf
//    }

    private def readjustBuffer() {
      val from = Collections.min(readPositions);
      val to = Collections.max(readPositions);
      val newLength = Math.max((to - from) * 2, MultiplexedSource.MIN_BUF);
      val newBuf: Array[Int] = new Array[Int](newLength)
      System.arraycopy(buffer, from, newBuf, 0, to - from);

      for(i <- 0 until readPositions.size()){
//      }
//      for (int i = 0; i < readPositions.size(); i++){
      readPositions.set(i, readPositions.get(i) - from)
      }

      writePosition -= from;
      buffer = newBuf;
    }

    // Read and advance position for given reader
    @throws[IOException]
    def read(readerId: Int): Int = {
      // Enough data in buffer?
      if (readPositions.get(readerId) >= writePosition) {
        readjustBuffer()
        buffer({
          writePosition += 1;
          writePosition - 1
        }) = source.read
      }
      val pos: Int = readPositions.get(readerId)
      val b: Int = buffer(pos)
      if (b != -1) readPositions.set(readerId, pos + 1)
      b
    }
  }

}

class SplittableInputStream extends InputStream {
  // Non-root fields
  private var multiSource: SplittableInputStream.MultiplexedSource = null
  private var myId: Int = 0

  // Public constructor: Used for first SplittableInputStream
  def this(source: InputStream) {
    this()
    multiSource = new SplittableInputStream.MultiplexedSource(source)
    myId = multiSource.addSource(-1)
  }

  // Private constructor: Used in split()
  def this(multiSource: SplittableInputStream.MultiplexedSource, splitId: Int) {
    this()
    this.multiSource = multiSource
    myId = multiSource.addSource(splitId)
  }

  // Returns a new InputStream that will read bytes from this position
  // onwards.
  def split: SplittableInputStream = new SplittableInputStream(multiSource, myId)

  @throws[IOException]
  def read: Int = multiSource.read(myId)
}

