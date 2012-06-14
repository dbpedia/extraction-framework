package org.dbpedia.extraction.util

import scala.collection.mutable.ArrayBuffer
import java.util.concurrent.ArrayBlockingQueue
import Workers._

trait Worker[T <: AnyRef] {
  def init(): Unit = {}
  def process(value: T): Unit
  def destroy(): Unit = {}
}

object Workers {
  private[util] val sentinel = new Object()
  private[util] val defaultThreads = Runtime.getRuntime().availableProcessors()
}

abstract class AbstractWorkers[T <: AnyRef](threads: Int, queueLength: Int) {
  
  def this(queueDepth: Int) = this(defaultThreads, defaultThreads * queueDepth)
  
  def this() = this(1)
  
  protected val queue = new ArrayBlockingQueue[AnyRef](queueLength)
  
  protected val workers: Seq[Thread]
  
  final def start(): Unit = {
    for (worker <- workers) worker.start()
  }
  
  final def process(value: T): Unit = {
    if (value == null) throw new NullPointerException("value")
    queue.put(value)
  }
  
  final def stop(): Unit = {
    for (worker <- workers) queue.put(sentinel)
    for (worker <- workers) worker.join()
  }
}
  
abstract class SimpleWorkers[T <: AnyRef](threads: Int, queueLength: Int)
extends AbstractWorkers[T](threads, queueLength) {
  
  def this(queueDepth: Int) = this(defaultThreads, defaultThreads * queueDepth)
  
  def this() = this(1)
  
  protected val workers =
  for (i <- 0 until threads) yield
  new Thread(getClass.getName+"-thread-"+i) {
    override def run(): Unit = {
      while(true) {
        val value = queue.take()
        if (value eq sentinel) return
        doProcess(value.asInstanceOf[T])
      }
    }
  }
  
  def doProcess(value: T) 
}
  
abstract class ResourceWorkers[T <: AnyRef](threads: Int, queueLength: Int)
extends AbstractWorkers[T](threads, queueLength) {
  
  def this(queueDepth: Int) = this(defaultThreads, defaultThreads * queueDepth)
  
  def this() = this(1)
  
  protected val workers =
  for (i <- 0 until threads) yield
  new Thread(getClass.getName+"-thread-"+i) {
    override def run(): Unit = {
      val worker = ResourceWorkers.this.worker()
      worker.init()
      try {
        while(true) {
          val value = queue.take()
          if (value eq sentinel) return
          worker.process(value.asInstanceOf[T])
        }
      } finally {
        worker.destroy()
      }
    }
  }
  
  def worker(): Worker[T]
}
