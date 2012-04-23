package org.dbpedia.extraction.util

import java.io.{InputStream,OutputStream}

object IOUtils {

  /**
   * Copy all bytes from input to output. Don't close any stream.
   */
  def copy(in : InputStream, out : OutputStream) : Unit = {
    val buf = new Array[Byte](1 << 20) // 1 MB
    while (true)
    {
      val read = in.read(buf)
      if (read == -1)
      {
        out.flush
        return
      }
      out.write(buf, 0, read)
    }
  }

}