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
   * val workers = SimpleWorkers(loadFactor, queueDepth) { foo: Foo =>
   *   // do something with foo...
   * }
   */
  def apply[T <: AnyRef](loadFactor: Double, queueDepth: Double)(proc: T => Unit): Workers[T] = {
    apply((defaultThreads * loadFactor).toInt, (defaultThreads * loadFactor * queueDepth).toInt)(proc)
  }
  
  /**
   * Convenience 'constructor'. Allows very concise syntax:
   * val workers = SimpleWorkers { foo: Foo =>
   *   // do something with foo...
   * }
   */
  def apply[T <: AnyRef](proc: T => Unit): Workers[T] = {
    apply(defaultThreads, defaultThreads)(proc)
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
   * val workers = ResourceWorkers(loadFactor, queueDepth) {
   *   new Worker[Foo] {
   *     def init() = { /* init... */ }
   *     def process(value: T) = { /* work... */ }
   *     def destroy() = { /* destroy... */ }
   *   }
   * }
   */
  def apply[T <: AnyRef](loadFactor: Double, queueDepth: Double)(factory: => Worker[T]): Workers[T] = {
    apply((defaultThreads * loadFactor).toInt, (defaultThreads * loadFactor * queueDepth).toInt)(factory)
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
    apply(defaultThreads, defaultThreads)(factory)
  }
}

/**
 * Constants for workers.
 */
object Workers {
  
  /**
   * Sentinel object that signals to the threads that the queue is empty.
   */
  private[util] val sentinel = new Object()
  
  /**
   * By default, use one thread per logical processor.
   */
  private[util] val defaultThreads = Runtime.getRuntime().availableProcessors()
}
  
/**
 * A simple fixed size thread-pool.
 * 
 * TODO: If a worker thread dies because of an uncaught exception, it just goes away and we
 * may not fully use all CPUs. Maybe we should start a new worker thread? Or use a thread pool
 * who does that for us? On the other hand - what about worker.init() and worker.destroy()?
 * We probably don't want to call them twice. No, I guess it's better to let the thread die.
 * Users can always catch Throwable in their implementation of Worker.process().
 * 
 * FIXME: If all worker threads die because of uncaught exceptions, the master thread will
 * probably still add tasks to the queue and block forever. When a worker thread dies, it should
 * count down the number of live threads and if none are left interrupt the master thread if it is 
 * blocking in process(). But what if there are multiple master threads? Ough. We need more ways to
 * communicate between masters and workers...
 *  
 * @param threads number of threads in pool
 * @param queueLength max length of work queue
 * @param factory called during initialization of this class to create a worker for each thread
 */
class Workers[T <: AnyRef](threads: Int, queueLength: Int, factory: => Worker[T]) {
  
  private val queue = new ArrayBlockingQueue[AnyRef](queueLength)
  
  private val workers =
  for (i <- 0 until threads) yield
  new Thread() {
    val worker = factory
    override def run(): Unit = {
      worker.init()
      try {
        while(true) {
          val value = queue.take()
          // if we find the sentinel, we're done
          if (value eq sentinel) return
          worker.process(value.asInstanceOf[T])
        }
      } finally {
        worker.destroy()
      }
    }
  }
  
  /**
   * Start all threads. Each thread will initialize its worker.
   */
  final def start(): Unit = {
    for (worker <- workers) worker.start()
  }
  
  /**
   * Add a value to the queue. A thread will take the value and let its worker process it.
   * If queue is full and all threads are busy, wait until a thread becomes available.
   */
  final def process(value: T): Unit = {
    if (value == null) throw new NullPointerException("value")
    queue.put(value)
  }
  
  /**
   * Stop all threads and wait for them to finish. Each thread will destroy its worker.
   */
  final def stop(): Unit = {
    // enqueue one sentinel per thread - each thread removes one
    for (worker <- workers) queue.put(sentinel)
    // wait for the threads to find the sentinels and finish
    for (worker <- workers) worker.join()
  }
    
}
