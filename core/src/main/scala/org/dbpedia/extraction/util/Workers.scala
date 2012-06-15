package org.dbpedia.extraction.util

import java.util.concurrent.ArrayBlockingQueue
import Workers._

trait Worker[T <: AnyRef] {
  def init(): Unit
  def process(value: T): Unit
  def destroy(): Unit
}

object SimpleWorkers {
  
  /**
   * Convenience 'constructor'. Allows very concise syntax:
   * val workers = SimpleWorkers(threads, queueLength) { foo: Foo =>
   *   // do something with foo...
   * }
   */
  def apply[T <: AnyRef](threads: Int, queueLength: Int)(proc: T => Unit): Workers[T] = {
    new Workers[T](threads, queueLength, new Worker[T]() {
      def init() = {}
      def process(value: T) = proc(value)
      def destroy() = {}
    })
  }
  
  /**
   * Convenience 'constructor'. Allows very concise syntax:
   * val workers = SimpleWorkers(queueDepth) { foo: Foo =>
   *   // do something with foo...
   * }
   */
  def apply[T <: AnyRef](queueDepth: Int)(proc: T => Unit): Workers[T] = {
    apply(defaultThreads, defaultThreads * queueDepth)(proc)
  }
  
  /**
   * Convenience 'constructor'. Allows very concise syntax:
   * val workers = SimpleWorkers { foo: Foo =>
   *   // do something with foo...
   * }
   */
  def apply[T <: AnyRef](proc: T => Unit): Workers[T] = {
    apply(1)(proc)
  }
}
  
object ResourceWorkers {
  
  /**
   * Convenience 'constructor'. Allows very concise syntax:
   * val workers = ResourceWorkers(threads, queueLength) {
   *   new Worker[Foo] {
   *     def init() = { /* init... */ }
   *     def process(value: T) = { /* work... */ }
   *     def destroy() = { /* destroy... */ }
   *   }
   * }
   */
  def apply[T <: AnyRef](threads: Int, queueLength: Int)(factory: => Worker[T]): Workers[T] = {
    new Workers[T](threads, queueLength, factory)
  }
  
  /**
   * Convenience 'constructor'. Allows very concise syntax:
   * val workers = ResourceWorkers(queueDepth) {
   *   new Worker[Foo] {
   *     def init() = { /* init... */ }
   *     def process(value: T) = { /* work... */ }
   *     def destroy() = { /* destroy... */ }
   *   }
   * }
   */
  def apply[T <: AnyRef](queueDepth: Int)(factory: => Worker[T]): Workers[T] = {
    apply(defaultThreads, defaultThreads * queueDepth)(factory)
  }
  
  /**
   * Convenience 'constructor'. Allows very concise syntax:
   * val workers = ResourceWorkers {
   *   new Worker[Foo] {
   *     def init() = { /* init... */ }
   *     def process(value: T) = { /* work... */ }
   *     def destroy() = { /* destroy... */ }
   *   }
   * }
   */
  def apply[T <: AnyRef](factory: => Worker[T]): Workers[T] = {
    apply(1)(factory)
  }
}

object Workers {
  private[util] val sentinel = new Object()
  private[util] val defaultThreads = Runtime.getRuntime().availableProcessors()
}
  
class Workers[T <: AnyRef](threads: Int, queueLength: Int, factory: => Worker[T]) {
  
  def this(queueDepth: Int, factory: => Worker[T]) = this(defaultThreads, defaultThreads * queueDepth, factory)
  
  def this(factory: => Worker[T]) = this(1, factory)
  
  private val queue = new ArrayBlockingQueue[AnyRef](queueLength)
  
  private val workers =
  for (i <- 0 until threads) yield
  new Thread(getClass.getName+"-thread-"+i) {
    override def run(): Unit = {
      val worker = factory
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
