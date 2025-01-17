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
package kafka.javaapi

import java.nio.ByteBuffer
import org.apache.log4j.Logger
import kafka.serializer.Encoder
import kafka.producer.{ProducerConfig, ProducerPool}
import kafka.producer.async.{AsyncProducerConfig, QueueItem}

private[javaapi] object Implicits {
  private val logger = Logger.getLogger(getClass())

  implicit def javaMessageSetToScalaMessageSet(messageSet: kafka.javaapi.message.ByteBufferMessageSet):
     kafka.message.ByteBufferMessageSet = messageSet.underlying

  implicit def scalaMessageSetToJavaMessageSet(messageSet: kafka.message.ByteBufferMessageSet):
     kafka.javaapi.message.ByteBufferMessageSet = {
    new kafka.javaapi.message.ByteBufferMessageSet(messageSet.getBuffer, messageSet.getInitialOffset,
                                                   messageSet.getErrorCode)
  }

  implicit def toJavaSyncProducer(producer: kafka.producer.SyncProducer): kafka.javaapi.producer.SyncProducer = {
    if(logger.isDebugEnabled)
      logger.debug("Implicit instantiation of Java Sync Producer")
    new kafka.javaapi.producer.SyncProducer(producer)
  }

  implicit def toSyncProducer(producer: kafka.javaapi.producer.SyncProducer): kafka.producer.SyncProducer = {
    if(logger.isDebugEnabled)
      logger.debug("Implicit instantiation of Sync Producer")
    producer.underlying
  }

  implicit def toScalaEventHandler[T](eventHandler: kafka.javaapi.producer.async.EventHandler[T])
       : kafka.producer.async.EventHandler[T] = {
    new kafka.producer.async.EventHandler[T] {
      override def init(props: java.util.Properties) { eventHandler.init(props) }
      override def handle(events: Seq[QueueItem[T]], producer: kafka.producer.SyncProducer, encoder: Encoder[T]) {
        import collection.JavaConversions._
        eventHandler.handle(seqAsJavaList(events), producer, encoder)
      }
      override def close { eventHandler.close }
    }
  }

  implicit def toJavaEventHandler[T](eventHandler: kafka.producer.async.EventHandler[T])
    : kafka.javaapi.producer.async.EventHandler[T] = {
    new kafka.javaapi.producer.async.EventHandler[T] {
      override def init(props: java.util.Properties) { eventHandler.init(props) }
      override def handle(events: java.util.List[QueueItem[T]], producer: kafka.javaapi.producer.SyncProducer,
                          encoder: Encoder[T]) {
        import collection.JavaConversions._
        eventHandler.handle(asScalaBuffer(events), producer, encoder)
      }
      override def close { eventHandler.close }
    }
  }

  implicit def toScalaCbkHandler[T](cbkHandler: kafka.javaapi.producer.async.CallbackHandler[T])
      : kafka.producer.async.CallbackHandler[T] = {
    new kafka.producer.async.CallbackHandler[T] {
      import collection.JavaConversions._
      override def init(props: java.util.Properties) { cbkHandler.init(props)}
      override def beforeEnqueue(data: QueueItem[T] = null.asInstanceOf[QueueItem[T]]): QueueItem[T] = {
        cbkHandler.beforeEnqueue(data)
      }
      override def afterEnqueue(data: QueueItem[T] = null.asInstanceOf[QueueItem[T]], added: Boolean) {
        cbkHandler.afterEnqueue(data, added)
      }
      override def afterDequeuingExistingData(data: QueueItem[T] = null): scala.collection.mutable.Seq[QueueItem[T]] = {
        cbkHandler.afterDequeuingExistingData(data)
      }
      override def beforeSendingData(data: Seq[QueueItem[T]] = null): scala.collection.mutable.Seq[QueueItem[T]] = {
        bufferAsJavaList(cbkHandler.beforeSendingData(seqAsJavaList(data)))
      }
      override def lastBatchBeforeClose: scala.collection.mutable.Seq[QueueItem[T]] = {
        asScalaBuffer(cbkHandler.lastBatchBeforeClose)
      }
      override def close { cbkHandler.close }
    }
  }

  implicit def toJavaCbkHandler[T](cbkHandler: kafka.producer.async.CallbackHandler[T])
      : kafka.javaapi.producer.async.CallbackHandler[T] = {
    new kafka.javaapi.producer.async.CallbackHandler[T] {
      import collection.JavaConversions._
      override def init(props: java.util.Properties) { cbkHandler.init(props)}
      override def beforeEnqueue(data: QueueItem[T] = null.asInstanceOf[QueueItem[T]]): QueueItem[T] = {
        cbkHandler.beforeEnqueue(data)
      }
      override def afterEnqueue(data: QueueItem[T] = null.asInstanceOf[QueueItem[T]], added: Boolean) {
        cbkHandler.afterEnqueue(data, added)
      }
      override def afterDequeuingExistingData(data: QueueItem[T] = null)
      : java.util.List[QueueItem[T]] = {
        mutableSeqAsJavaList(cbkHandler.afterDequeuingExistingData(data))
      }
      override def beforeSendingData(data: java.util.List[QueueItem[T]] = null)
      : java.util.List[QueueItem[T]] = {
        asScalaBuffer(cbkHandler.beforeSendingData(asScalaBuffer(data)))
      }
      override def lastBatchBeforeClose: java.util.List[QueueItem[T]] = {
        mutableSeqAsJavaList(cbkHandler.lastBatchBeforeClose)
      }
      override def close { cbkHandler.close }
    }
  }

  implicit def toMultiFetchResponse(response: kafka.javaapi.MultiFetchResponse): kafka.api.MultiFetchResponse =
    response.underlying

  implicit def toJavaMultiFetchResponse(response: kafka.api.MultiFetchResponse): kafka.javaapi.MultiFetchResponse =
    new kafka.javaapi.MultiFetchResponse(response.buffer, response.numSets, response.offsets)
}
