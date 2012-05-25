package org.dbpedia.extraction.destinations

import java.io.{File,FileOutputStream,IOException}

/**
 * Handles a marker file that signals that the extraction is either running ('start mode')
 * or finished ('end mode').
 * 
 * In 'start mode', the file is created before the extraction starts (it must not already exist)
 * and deleted after the extraction ends.
 * 
 * In 'end mode', the file is deleted before the extraction starts (if it already exists) 
 * and re-created after the extraction ends.
 * 
 * @param file marker file
 * @param start 'start mode' if true, 'end mode' if false. 
 */
class MarkerDestination(destination: Destination, file: File, start: Boolean)
extends Destination
{
  override def open(): Unit = {
    if (start) create() else delete()
    destination.open()
  }

  override def write(graph: Seq[Quad]): Unit = {
    destination.write(graph)
  } 

  override def close(): Unit = {
    destination.close()
    if (! start) create() else delete()
  }
  
  private def create(): Unit = {
    if (file.exists()) throw new IOException("file '"+file+"' already exists")
    new FileOutputStream(file).close()
  }
  
  private def delete(): Unit = {
    if (! file.exists()) return
    if (! file.delete()) throw new IOException("failed to delete file '"+file+"'")
  }
  
}
