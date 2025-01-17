/*
 * Copyright 2010 LinkedIn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package kafka.javaapi.message

import java.nio.ByteBuffer
import kafka.common.ErrorMapping
import org.apache.log4j.Logger
import kafka.message._

class ByteBufferMessageSet(private val buffer: ByteBuffer,
                           private val initialOffset: Long = 0L,
                           private val errorCode: Int = ErrorMapping.NoError) extends MessageSet {
  private val logger = Logger.getLogger(getClass())
  val underlying: kafka.message.ByteBufferMessageSet = new kafka.message.ByteBufferMessageSet(buffer,
                                                                                              initialOffset,
                                                                                              errorCode)
  def this(buffer: ByteBuffer) = this(buffer, 0L, ErrorMapping.NoError)

  def this(compressionCodec: CompressionCodec, messages: java.util.List[Message]) {
    this(compressionCodec match {
      case NoCompressionCodec =>
        val buffer = ByteBuffer.allocate(MessageSet.messageSetSize(messages))
        val messageIterator = messages.iterator
        while(messageIterator.hasNext) {
          val message = messageIterator.next
          message.serializeTo(buffer)
        }
        buffer.rewind
        buffer
      case _ =>
        import scala.collection.JavaConversions._
        val message = CompressionUtils.compress(asScalaBuffer(messages), compressionCodec)
        val buffer = ByteBuffer.allocate(message.serializedSize)
        message.serializeTo(buffer)
        buffer.rewind
        buffer
    }, 0L, ErrorMapping.NoError)
  }

  def this(messages: java.util.List[Message]) {
    this(NoCompressionCodec, messages)
  }

  def validBytes: Long = underlying.validBytes

  def serialized():ByteBuffer = underlying.serialized

  def getInitialOffset = initialOffset

  def getBuffer = buffer

  def getErrorCode = errorCode

  override def iterator: java.util.Iterator[MessageAndOffset] = new java.util.Iterator[MessageAndOffset] {
    val underlyingIterator = underlying.iterator
    override def hasNext(): Boolean = {
      underlyingIterator.hasNext
    }

    override def next(): MessageAndOffset = {
      underlyingIterator.next
    }

    override def remove = throw new UnsupportedOperationException("remove API on MessageSet is not supported")
  }

  override def toString: String = underlying.toString

  def sizeInBytes: Long = underlying.sizeInBytes

  override def equals(other: Any): Boolean = {
    other match {
      case that: ByteBufferMessageSet =>
        (that canEqual this) && errorCode == that.errorCode && buffer.equals(that.buffer) && initialOffset == that.initialOffset
      case _ => false
    }
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[ByteBufferMessageSet]

  override def hashCode: Int = 31 * (17 + errorCode) + buffer.hashCode + initialOffset.hashCode

}
