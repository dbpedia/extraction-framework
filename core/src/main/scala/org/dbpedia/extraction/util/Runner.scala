package org.dbpedia.extraction.util

import java.util.concurrent.{ExecutorService,TimeUnit,ArrayBlockingQueue,ThreadPoolExecutor}
import java.util.logging.{Logger,Level}

/**
 * A thin wrapper around ExecutorService / ThreadPoolExecutor.
 */
class Runner(executor: ExecutorService) {
  
  /**
   * Starts a fixed size thread pool. If all slave threads are busy, the master does some work 
   * as well. This seems to be the simplest way to implement something like this in JDK 6. 
   * With other solutions, we either need busy waiting or some complex locking and waiting.
   * @param threads number of threads in pool
   * @param queueLength length of task queue
   */
  def this(threads: Int, queueLength: Int) = 
  this(
    new ThreadPoolExecutor(
      threads,
      threads,
      0L,
      TimeUnit.MILLISECONDS,
      new ArrayBlockingQueue[Runnable](queueLength),
      new ThreadPoolExecutor.CallerRunsPolicy()
    )
  )
  
  /**
   * Starts a fixed size thread pool with one thread per logical CPU and the given queue length
   * per thread.
   * @param queueDepth will be multiplied by number of threads to choose task queue length
   */
  def this(queueDepth: Int) =
  this(
    Runtime.getRuntime.availableProcessors,
    Runtime.getRuntime.availableProcessors * queueDepth
  )
  
  private val logger = Logger.getLogger(getClass.getName)
  
  /**
   * Execute the given task.
   */
  def run(task: => Unit): Unit = {
    executor.execute(new Runnable() { def run() { task } } )
  }

  /**
   * Shut down the executor and wait until all tasks have finished. If necessary, wait forever.
   * TODO: this may not be optimal for all use cases.
   * It would be nice to re-use the pool across instances, but shutdown seems 
   * to be the only way to wait for all currently working threads to finish. 
   */
  def shutdown(): Unit = {
    executor.shutdown()
    while (! executor.awaitTermination(1L, TimeUnit.MINUTES)) {
      logger.log(Level.SEVERE, "execution did not terminate - waiting one more minute")
    }
  }
  
}