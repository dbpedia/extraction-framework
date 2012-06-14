package org.dbpedia.extraction.util

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
  
  def apply[T <: AnyRef](threads: Int, queueLength: Int)(proc: T => Unit): Workers[T] = {
    new Workers[T](threads, queueLength) {
      override def doProcess(value: T) = proc(value)
    }
  }
  
  def apply[T <: AnyRef](queueDepth: Int)(proc: T => Unit): Workers[T] = {
    apply(defaultThreads, defaultThreads * queueDepth)(proc)
  }
  
  def apply[T <: AnyRef](proc: T => Unit): Workers[T] = {
    apply(1)(proc)
  }
}

class Workers[T <: AnyRef](threads: Int, queueLength: Int) {
  
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
  
  protected def worker() = new Worker[T]() {
    def process(value: T) = Workers.this.doProcess(value)
  }
  
  protected def doProcess(value: T) = {}
  
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
