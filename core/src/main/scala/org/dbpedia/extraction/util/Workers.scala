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

abstract class SimpleWorkers[T <: AnyRef](threads: Int, queueLength: Int)
extends Workers[T](threads, queueLength) {
  
  def this(queueDepth: Int) = this(defaultThreads, defaultThreads * queueDepth)
  
  def this() = this(1)
  
  override def worker() = new Worker[T]() {
    def process(value: T) = SimpleWorkers.this.process(value)
  }
  
  def process(value: T) 
}
  
abstract class Workers[T <: AnyRef](threads: Int, queueLength: Int) {
  
  def this(queueDepth: Int) = this(defaultThreads, defaultThreads * queueDepth)
  
  def this() = this(1)
  
  private val queue = new ArrayBlockingQueue[AnyRef](queueLength)
  
  private val workers =
  for (i <- 0 until threads) yield
  new Thread(getClass.getName+"-thread-"+i) {
    override def run(): Unit = {
      val worker = Workers.this.worker()
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
  
  def start(): Unit = {
    for (worker <- workers) worker.start()
  }
  
  def process(value: T): Unit = {
    if (value == null) throw new NullPointerException("value")
    queue.put(value)
  }
  
  def stop(): Unit = {
    for (worker <- workers) queue.put(sentinel)
    for (worker <- workers) worker.join()
  }
    
}
