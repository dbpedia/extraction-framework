package org.dbpedia.extraction.scripts

import java.io.File

import org.dbpedia.extraction.config.provenance.Dataset
import org.dbpedia.extraction.destinations._
import org.dbpedia.extraction.destinations.formatters.TerseFormatter
import org.dbpedia.extraction.destinations.formatters.UriPolicy.Policy
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.IOUtils.writer
import org.dbpedia.extraction.util._
import org.dbpedia.iri.UriUtils

import scala.Console.err

/**
 * Maps old quads/triples to new quads/triples.
 */
class QuadMapper(file: FileLike[File] = null, preamble: String = null) extends QuadReader(file, preamble) {

  /**
   * @deprecated use one of the map functions below 
   */
  @Deprecated
  def mapQuads[T <% FileLike[T]](finder: DateFinder[T], input: String, output: String)(map: Quad => Traversable[Quad]): Unit = {
    // auto only makes sense on the first call to finder.find(), afterwards the date is set
    val destination = new WriterDestination(() => writer(finder.byName(output).get), new QuadMapperFormatter())
    mapQuads(finder.language, finder.byName(input).get, destination, required = true)(map)
  }
  def mapQuads[T <% FileLike[T]](finder: DateFinder[T], input: String, output: String, auto: Boolean, required: Boolean)(map: Quad => Traversable[Quad]): Unit = {
    // auto only makes sense on the first call to finder.find(), afterwards the date is set
    val readFiles = finder.byName(input, auto = auto).get
    val destination = new WriterDestination(() => writer(finder.byName(output, auto).get), new QuadMapperFormatter())
    mapQuads(finder.language, readFiles, destination, required = required)(map)
  }
  def mapQuads[T <% FileLike[T]](finder: DateFinder[T], input: String, output: String, required: Boolean )(map: Quad => Traversable[Quad]): Unit = {
    // auto only makes sense on the first call to finder.find(), afterwards the date is set
    val destination = new WriterDestination(() => writer(finder.byName(output).get), new QuadMapperFormatter())
    mapQuads(finder.language, finder.byName(input).get, destination, required)(map)
  }
  def mapQuads[T <% FileLike[T]](finder: DateFinder[T], input: String, output: String, required: Boolean, quads: Boolean, turtle: Boolean, policies: Array[Policy])(map: Quad => Traversable[Quad]): Unit = {
    mapQuads(finder.language, finder.byName(input).get, finder.byName(output).get, required, quads, turtle, policies)(map)
  }

  def mapQuads(language: Language, inFile: FileLike[_], outFile: FileLike[_], required: Boolean = true, closeWriter: Boolean = true)(map: Quad => Traversable[Quad]): Unit = {

    val destination = DestinationUtils.createWriterDestination(outFile)
    mapQuads(language, inFile, destination, required, closeWriter)(map)
  }

  /**
   */
  def mapQuads(language: Language, inFile: FileLike[_], outFile: FileLike[_], required: Boolean, quads: Boolean, turtle: Boolean, policies: Array[Policy])(map: Quad => Traversable[Quad]): Unit = {
    err.println(language.wikiCode+": writing "+outFile+" ...")
    val destination = new WriterDestination(() => writer(outFile), new QuadMapperFormatter(quads, turtle, policies))
    mapQuads(language, inFile, destination, required)(map)
  }

  def mapQuads(language: Language, inFile: FileLike[_], destination: Destination, required: Boolean) (map: Quad => Traversable[Quad]): Unit = {
    mapQuads(language, inFile, destination, required, closeWriter = true)(map)
  }

  private def getTransitiveDatasets(dest: Destination): Seq[Dataset] = {
    dest match{
      case d : CompositeDestination => d.destinations.flatMap(x => getTransitiveDatasets(x))
      case d : WrapperDestination => getTransitiveDatasets(d.destination)
      case d : DatasetDestination => d.destinations.keySet.toSeq
      case _ => Seq()
    }
  }

  /**
   * TODO: do we really want to open and close the destination here? Users may want to map quads
   * from multiple input files to one destination. On the other hand, if the input file doesn't
   * exist, we probably shouldn't open the destination at all, so it's ok that it's happening in
   * this method after checking the input file.
    * Chile: made closing optional, also WriteDestination can only open Writer one now
   */
  def mapQuads(language: Language, inFile: FileLike[_], destination: Destination, required: Boolean, closeWriter: Boolean)(map: Quad => Traversable[Quad]): Unit = {


    if (! inFile.exists) {
      if (required) throw new IllegalArgumentException(language.wikiCode+": file "+inFile+" does not exist")
      return
    }
    destination.open()
    this.getRecorder.initialize(language, "Mapping Quads", getTransitiveDatasets(destination))

    try {
      readQuads(language, inFile) { old =>
        destination.write(map(old))
      }
    }
    finally
      if(closeWriter)
        destination.close()
    //err.println(language.wikiCode+": mapped "+mapCount+" quads")
  }

  /**
    * TODO: do we really want to open and close the destination here? Users may want to map quads
    * from multiple input files to one destination. On the other hand, if the input file doesn't
    * exist, we probably shouldn't open the destination at all, so it's ok that it's happening in
    * this method after checking the input file.
    */
  def mapSortedQuads(language: Language, inFile: FileLike[_], destination: Destination, required: Boolean, closeWriter: Boolean = true)(map: Traversable[Quad] => Traversable[Quad]): Unit = {

    if (! inFile.exists) {
      if (required) throw new IllegalArgumentException(language.wikiCode+": file "+inFile+" does not exist")
      err.println(language.wikiCode+": WARNING - file "+inFile+" does not exist")
      return
    }

    var mapCount = 0
    destination.open()
    this.getRecorder.initialize(language, "Mapping Quads", getTransitiveDatasets(destination))

    try {
      readSortedQuads(language, inFile) { old =>
        val rs = map(old)
        destination.write(rs)
        mapCount += old.size
      }
    }
    finally
      if(closeWriter)
        destination.close()
    err.println(language.wikiCode+": mapped "+mapCount+" quads")
  }
}

class QuadMapperFormatter(quad: Boolean = true, turtle: Boolean = true, policies: Array[Policy]= null) extends TerseFormatter(quad, turtle, policies) {
  def this(formatter: TerseFormatter){
    this(formatter.quads, formatter.turtle, formatter.policies)
  }
  private var contextAdditions = Map[String, String]()

  def addContextAddition(paramName: String, paramValue: String): Unit = synchronized {
    val param = paramName.replaceAll("\\s", "").toLowerCase()
    contextAdditions += ( param -> UriUtils.encodeUriComponent(paramValue))
  }

  override def render(quad: Quad): String = synchronized {
    var context = Option(quad.context) match{
      case Some(c) if c.trim.nonEmpty => c.trim
      case None => null
      case _ => null
    }
    for(add <- contextAdditions if context != null)
      if(context.indexOf("#") > 0)
        context += "&" + add._1 + "=" + add._2
      else
        context += "#" + add._1 + "=" + add._2
    val value = org.dbpedia.extraction.util.TurtleUtils.unescapeTurtle(quad.value)  //TODO unescaping turtle escapes
    super.render(quad.copy(value=value,context=context))
  }
}