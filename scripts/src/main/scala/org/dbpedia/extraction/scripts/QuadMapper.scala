package org.dbpedia.extraction.scripts

import java.lang.StringBuilder
import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.util.StringUtils.formatCurrentTimestamp
import org.dbpedia.extraction.util.FileLike
import scala.Console.err
import org.dbpedia.extraction.util.IOUtils
import org.dbpedia.extraction.util.IOUtils.writer
import java.io.File
import org.dbpedia.extraction.destinations.Destination
import org.dbpedia.extraction.destinations.WriterDestination
import org.dbpedia.extraction.destinations.formatters.TerseFormatter
import org.dbpedia.extraction.destinations.formatters.UriPolicy.Policy

/**
 * Maps old quads/triples to new quads/triples.
 */
object QuadMapper {
  
  /**
   * @deprecated use one of the map functions below 
   */
  @Deprecated
  def mapQuads[T <% FileLike[T]](finder: DateFinder[T], input: String, output: String, auto: Boolean = false, required: Boolean = true)(map: Quad => Traversable[Quad]): Unit = {
    // auto only makes sense on the first call to finder.find(), afterwards the date is set
    mapQuads(finder.language.wikiCode, finder.find(input, auto), finder.find(output), required)(map)
  }
    
  /**
   * @deprecated use one of the map functions below 
   */
  @Deprecated
  def mapQuads(tag: String, inFile: FileLike[_], outFile: FileLike[_], required: Boolean)(map: Quad => Traversable[Quad]): Unit = {
    
    if (! inFile.exists) {
      if (required) throw new IllegalArgumentException(tag+": file "+inFile+" does not exist")
      err.println(tag+": WARNING - file "+inFile+" does not exist")
      return
    }

    err.println(tag+": writing "+outFile+" ...")
    var mapCount = 0
    val writer = IOUtils.writer(outFile)
    try {
      // copied from org.dbpedia.extraction.destinations.formatters.TerseFormatter.footer
      writer.write("# started "+formatCurrentTimestamp+"\n")
      QuadReader.readQuads(tag, inFile) { old =>
        for (quad <- map(old)) {
          val sb = new StringBuilder
          sb append '<' append quad.subject append "> <" append quad.predicate append "> "
          if (quad.datatype == null) {
            sb append '<' append quad.value append "> "
          }
          else {
            sb  append '"' append quad.value append '"'
            if (quad.language != null) sb append '@' append quad.language append ' '
            else if (quad.datatype != "http://www.w3.org/2001/XMLSchema#string") sb append "^^<" append quad.datatype append "> "
          }
          if (quad.context != null) sb append '<' append quad.context append "> "
          sb append ".\n"
          writer.write(sb.toString)
          
          mapCount += 1
        }
      }
      // copied from org.dbpedia.extraction.destinations.formatters.TerseFormatter.header
      writer.write("# completed "+formatCurrentTimestamp+"\n")
    }
    finally writer.close()
    err.println(tag+": mapped "+mapCount+" quads")
  }
  
  /**
   */
  def mapQuads(tag: String, inFile: FileLike[_], outFile: FileLike[_], required: Boolean, quads: Boolean, turtle: Boolean, policies: Array[Policy] = null)(map: Quad => Traversable[Quad]): Unit = {
    err.println(tag+": writing "+outFile+" ...")
    val destination = new WriterDestination(() => writer(outFile), new TerseFormatter(quads, turtle, policies))
    mapQuads(tag, inFile, destination, required)(map)
  }
  
  /**
   * TODO: do we really want to open and close the destination here? Users may want to map quads
   * from multiple input files to one destination. On the other hand, if the input file doesn't
   * exist, we probably shouldn't open the destination at all, so it's ok that it's happening in
   * this method after checking the input file.
   */
  def mapQuads(tag: String, inFile: FileLike[_], destination: Destination, required: Boolean)(map: Quad => Traversable[Quad]): Unit = {
    
    if (! inFile.exists) {
      if (required) throw new IllegalArgumentException(tag+": file "+inFile+" does not exist")
      err.println(tag+": WARNING - file "+inFile+" does not exist")
      return
    }

    var mapCount = 0
    destination.open()
    try {
      QuadReader.readQuads(tag, inFile) { old =>
        destination.write(map(old))
        mapCount += 1
      }
    }
    finally destination.close()
    err.println(tag+": mapped "+mapCount+" quads")
  }
  
}