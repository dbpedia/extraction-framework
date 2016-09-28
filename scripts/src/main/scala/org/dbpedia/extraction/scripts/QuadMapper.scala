package org.dbpedia.extraction.scripts

import java.lang.StringBuilder
import java.net.{URLEncoder, URL, URI}
import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.util.StringUtils.formatCurrentTimestamp
import org.dbpedia.extraction.util.FileLike
import scala.Console.err
import org.dbpedia.extraction.util.IOUtils
import org.dbpedia.extraction.util.IOUtils.writer
import org.dbpedia.extraction.destinations.Destination
import org.dbpedia.extraction.destinations.WriterDestination
import org.dbpedia.extraction.destinations.formatters.{TerseBuilder, Formatter, TerseFormatter}
import org.dbpedia.extraction.destinations.formatters.UriPolicy.Policy

/**
 * Maps old quads/triples to new quads/triples.
 */
object QuadMapper {

  /**
   * @deprecated use one of the map functions below 
   */
  @Deprecated
  def mapQuads[T <% FileLike[T]](finder: DateFinder[T], input: String, output: String)(map: Quad => Traversable[Quad]): Unit = {
    // auto only makes sense on the first call to finder.find(), afterwards the date is set
    val destination = new WriterDestination(() => writer(finder.byName(output, auto = false)), new QuadMapperFormatter())
    mapQuads(finder.language.wikiCode, finder.byName(input, auto = false), destination, required = true)(map)
  }
  def mapQuads[T <% FileLike[T]](finder: DateFinder[T], input: String, output: String, auto: Boolean, required: Boolean)(map: Quad => Traversable[Quad]): Unit = {
    // auto only makes sense on the first call to finder.find(), afterwards the date is set
    val readFiles = finder.byName(input, auto = auto)
    val destination = new WriterDestination(() => writer(finder.byName(output, auto)), new QuadMapperFormatter())
    mapQuads(finder.language.wikiCode, readFiles, destination, required = required)(map)
  }
  def mapQuads[T <% FileLike[T]](finder: DateFinder[T], input: String, output: String, required: Boolean )(map: Quad => Traversable[Quad]): Unit = {
    // auto only makes sense on the first call to finder.find(), afterwards the date is set
    val destination = new WriterDestination(() => writer(finder.byName(output, auto = false)), new QuadMapperFormatter())
    mapQuads(finder.language.wikiCode, finder.byName(input, auto = false), destination, required)(map)
  }
  def mapQuads[T <% FileLike[T]](finder: DateFinder[T], input: String, output: String, required: Boolean, quads: Boolean, turtle: Boolean, policies: Array[Policy])(map: Quad => Traversable[Quad]): Unit = {
    mapQuads(finder.language.wikiCode, finder.byName(input, auto = false), finder.byName(output, auto = false), required, quads, turtle, policies)(map)
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
          writer.write(quadToString(quad))
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
    * @deprecated don't use it any more!
    */
  @Deprecated
  private def quadToString(quad: Quad): String = {
    val sb = new StringBuilder
    sb append '<' append quad.subject append "> <" append quad.predicate append "> "
    if (quad.datatype == null) {
      sb append '<' append quad.value append "> "
    }
    else {
      sb append '"' append quad.value append '"'
      if (quad.language != null) sb append '@' append quad.language append ' '
      else if (quad.datatype != "http://www.w3.org/2001/XMLSchema#string") sb append "^^<" append quad.datatype append "> "
    }
    if (quad.context != null) sb append '<' append quad.context append "> "
    sb append ".\n"
    sb.toString
  }

  /**
   */
  def mapQuads(tag: String, inFile: FileLike[_], outFile: FileLike[_], required: Boolean, quads: Boolean, turtle: Boolean, policies: Array[Policy] = null)(map: Quad => Traversable[Quad]): Unit = {
    err.println(tag+": writing "+outFile+" ...")
    val destination = new WriterDestination(() => writer(outFile), new QuadMapperFormatter(quads, turtle, policies))
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

  /**
    * TODO: do we really want to open and close the destination here? Users may want to map quads
    * from multiple input files to one destination. On the other hand, if the input file doesn't
    * exist, we probably shouldn't open the destination at all, so it's ok that it's happening in
    * this method after checking the input file.
    */
  def mapSortedQuads(tag: String, inFile: FileLike[_], destination: Destination, required: Boolean)(map: Traversable[Quad] => Traversable[Quad]): Unit = {

    if (! inFile.exists) {
      if (required) throw new IllegalArgumentException(tag+": file "+inFile+" does not exist")
      err.println(tag+": WARNING - file "+inFile+" does not exist")
      return
    }

    var mapCount = 0
    destination.open()
    try {
      QuadReader.readSortedQuads(tag, inFile) { old =>
        destination.write(map(old))
        mapCount += old.size
      }
    }
    finally destination.close()
    err.println(tag+": mapped "+mapCount+" quads")
  }

  class QuadMapperFormatter(quad: Boolean = true, turtle: Boolean = true, policies: Array[Policy]= null) extends Formatter {
    private val builder = new TerseBuilder(quad, turtle, policies)
    private var contextAdditions = Map[String, String]()

    def addContextAddition(paramName: String, paramValue: String): Unit ={
      val param = paramName.replaceAll("\\s", "").toLowerCase()
      val value = URLEncoder.encode(paramValue, "UTF-8")
        .replaceAll("\\+", "%20")
        .replaceAll("\\%21", "!")
        .replaceAll("\\%27", "'")
        .replaceAll("\\%28", "(")
        .replaceAll("\\%29", ")")
        .replaceAll("\\%7E", "~")
      contextAdditions += ( param -> paramValue)
    }

    override def header: String = "# started " + formatCurrentTimestamp + "\\n"

    override def footer: String = "# completed " + formatCurrentTimestamp + "\\n"

    override def render(quad: Quad): String = {
      builder.start(quad.context)
      builder.subjectUri(quad.subject)
      builder.predicateUri(quad.predicate)
      if (quad.datatype == null)
        builder.objectUri(quad.value)
      else if (quad.datatype == "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString")
        builder.plainLiteral(quad.value, quad.language)
      else
        builder.typedLiteral(quad.value, quad.datatype)

      var context = quad.context
      for(add <- contextAdditions)
        if(context.indexOf("#") > 0)
          context += "&" + add._1 + "=" + add._2
        else
          context += "#" + add._1 + "=" + add._2
      builder.end(context)
      builder.result()
    }
  }
}
