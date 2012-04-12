package org.dbpedia.extraction.dump.download

import java.io.IOException

trait RetryBlock[R] {
  def log(logger: (Int, Int, Exception) => Unit) : R
}

class Retry(retryMax : Int, retryMillis : Int) {
  def apply[R](op: => R) = new RetryBlock[R] {
    
    def log(logger: (Int, Int, Exception) => Unit) : R = {
      
      var attempt = 0
      
      while (true) {
        try {
          return op
        } catch {
          case ex : Exception =>
            attempt += 1
            logger(attempt, retryMax, ex)
            if (attempt >= retryMax) throw ex
            Thread.sleep(retryMillis)
        }
      }
      
      throw new Exception("can't get here, but the Scala compiler doesn't know")
    }
  }
}