package org.dbpedia.extraction.util

import java.io.Closeable
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ArrayBlockingQueue, ConcurrentHashMap}

import org.dbpedia.extraction.util.Workers._

import scala.Console._
import scala.collection.convert.decorateAsScala._
import scala.language.reflectiveCalls


/**
  * Provides the overall state of a worker
  */
object WorkerState extends Enumeration {
  val
  declared,             //worker was declared but was not yet initialized (started)
  initialized,          //worker is ready to work after its initialization (started)
  destroyed = Value     //worker has finished all work after it was told to do so (destroyed)
}

trait Worker[T <: AnyRef] {
  def init(): Unit
  def process(value: T): Unit
  def destroy(): Unit
  def getState: WorkerState.Value
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
      private var state : WorkerState.Value = WorkerState.declared
      def init(): Unit = {state = WorkerState.initialized}
      def process(value: T): Unit = proc(value)
      def destroy(): Unit = {state = WorkerState.destroyed}
      def getState: _root_.org.dbpedia.extraction.util.WorkerState.Value = state
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
  private[util] val defaultThreads = Runtime.getRuntime.availableProcessors()


  def work[T <: AnyRef](args : List[T])(proc : T => Unit) : Unit = {
    val worker = SimpleWorkers(proc)
    Workers.work[T](worker, args, null)
  }

  def work[T <: AnyRef](worker: Workers[T], args : Seq[T], showProgress: String = null) : Unit = {
    val percent = new AtomicInteger()
    try {
      worker.start()
      val startStamp = System.currentTimeMillis
      for(arg <- args.indices)
      {
        worker.process(args(arg))
        if(showProgress != null) {
          val o = percent.get().toFloat
          val n = percent.incrementAndGet().toFloat
          if ((o * 100f / args.length.toFloat).toInt < (n * 100f / args.length.toFloat).toInt)
            err.println(StringUtils.formatCurrentTimestamp + ": " + showProgress + " at: " + (n * 100f / args.length.toFloat).toInt + "% after " + (System.currentTimeMillis - startStamp) / 1000 + " seconds.")
        }
      }
    }
    finally {
      worker.close()
      if(showProgress != null)
        err.println(StringUtils.formatCurrentTimestamp + ": " + showProgress + " is finished. Processed " + percent.get() + " instances of " + (if(args.nonEmpty) args.head.getClass.getName else "nothing"))
    }
  }

  def workInParallel[T <: AnyRef](workers: Traversable[Workers[T]], args : Seq[T]) : Unit = {
    try {
      for(worker <- workers) {
        worker.start()
        for (arg <- args)
          worker.process(arg)
      }
    }
    finally {
      for(worker <- workers)
        worker.close()
    }
  }
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
 * @param availThreads number of threads in pool
 * @param queueLength max length of work queue
 * @param factory called during initialization of this class to create a worker for each thread
 */
class Workers[T <: AnyRef](availThreads: Int, queueLength: Int, factory: => Worker[T]) extends Closeable {

  /**
    * Provides the state of a package of work
    */
  object WorkerObjectState extends Enumeration {
    val
    queued,       //this package of work is queued (and therefor soon to be processed)
    inProcess,    //this package of work is already being processed atm.
    done = Value  //this package of work was already processed
  }

  private val queue = new ArrayBlockingQueue[AnyRef](queueLength)

  private val processLog = new ConcurrentHashMap[Int, WorkerObjectState.Value]().asScala
  private val queueDependency = new ConcurrentHashMap[Int, Int]().asScala

  private val threads =
  for (i <- 0 until availThreads) yield
    new Thread() {
      val worker: Worker[T] = factory
      override def run(): Unit = {
        try {
          if(worker.getState != WorkerState.initialized)
            throw new IllegalStateException("A worker was tasked with work while not being in the 'initialized' state: " + worker.getState)

          while(true) {
            val value = queue.take()
            queueDependency.get(value.hashCode()) match{
              case Some(h) => processLog.get(h) match{
                case Some(x) => queue.put(value) // dependency is queued or in progress -> ergo put it back in the queue
                case None =>
              }
              case None =>
            }
            processLog(value.hashCode()) = WorkerObjectState.inProcess

            // if we find the sentinel, we're done
            if (value eq sentinel)
              return

            worker.process(value.asInstanceOf[T])

            //will  not longer save WorkerObjectState.done since this constitutes a memory leak. Instead, we assume that its done when not available
            processLog.remove(value.hashCode())
          }
        } finally {
        }
      }
    }
  
  /**
   * Start all threads. Each thread will initialize its worker.
   */
  final def start(): Unit = {
    for (thread <- threads) {
      if(thread.worker.getState == WorkerState.declared){
        thread.worker.init()
      }
      if(thread.getState != Thread.State.NEW)
        thread.resume()                         //TODO replace this with something more scala like
      else
        thread.start()
    }
  }

  final def process(value: T, dependentOn: T): Unit = process(value, dependentOn.hashCode())
  /**
   * Add a value to the queue. A thread will take the value and let its worker process it.
   * If queue is full and all threads are busy, wait until a thread becomes available.
   */
  final def process(value: T, dependentOn: Int = -1): Unit = {
    if (value == null) throw new NullPointerException("value")
    if(dependentOn >= 0)
      queueDependency(value.hashCode()) = dependentOn
    queue.put(value)
    processLog(value.hashCode()) = WorkerObjectState.queued
  }
  
  /**
   * Stop all threads and wait for them to finish. Each thread will destroy its worker.
   */
  final def stop(): Unit = {
    // enqueue one sentinel per thread - each thread removes one
    for (thread <- threads)
      queue.put(sentinel)
    // wait for the threads to find the sentinels and finish
    for (thread <- threads) {
      thread.join()
      thread.worker.destroy()
    }
  }

  /**
    * check the state of a queued Worker object
    * @param hash - hashcode of queued object
    * @return WorkerState
    */
  def checkWorkerProcess(hash: Int) = Option(processLog(hash))

  override def close(): Unit = stop()
}
