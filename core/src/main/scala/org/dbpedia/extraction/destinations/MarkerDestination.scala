package org.dbpedia.extraction.destinations

import java.io.{File,FileOutputStream}

class MarkerDestination(file: File, private var start: Boolean, private var finish: Boolean)
extends Destination {
  
  override def write(graph : Seq[Quad]): Unit = {
    if (start) {
      touch()
      start = false
    }
  }

  override def close() : Unit = {
    if (finish) {
      touch()
      finish = false
    }
  }
  
  private def touch(): Unit = {
    new FileOutputStream(file).close() 
  }
  
}
