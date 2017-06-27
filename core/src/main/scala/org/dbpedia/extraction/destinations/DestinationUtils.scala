package org.dbpedia.extraction.destinations

import java.io.File

import org.dbpedia.extraction.config.provenance.Dataset
import org.dbpedia.extraction.destinations.formatters.Formatter
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util.{DateFinder, Finder, IOUtils}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Created by Chile on 9/29/2016.
  */
object DestinationUtils {
  def createDestination(finder: Finder[File], date: String, datasets: Seq[String], formats: collection.Map[String, Formatter], append: Boolean) : Destination = {
    val destination = new ArrayBuffer[Destination]()
    for ((suffix, format) <- formats) {
      val datasetDestinations = new mutable.HashMap[String, Destination]()
      for (dataset <- datasets) {
        finder.file(date, dataset.replace("_", "-") +'.'+suffix) match{
          case Some(file) => datasetDestinations(dataset) = new WriterDestination(() => IOUtils.writer(file), format)
          case None =>
        }
      }
      destination += new DatasetDestination(datasetDestinations)
    }
    new CompositeDestination(destination: _*)
  }

  def createDestination(finder: DateFinder[File], datasets: Seq[Dataset], formats: collection.Map[String, Formatter], append: Boolean = false) : Destination = {
    createDestination(finder.finder, finder.date, datasets.map(_.encoded), formats, append)
  }

  def createDestination(finder: DateFinder[File], datasets: Seq[String], formats: collection.Map[String, Formatter]) : Destination = {
    createDestination(finder.finder, finder.date, datasets, formats, append = false)
  }

  def getDatasets(obj: Any) = {

    val fields = obj.getClass.getDeclaredFields
    fields.foreach(_.setAccessible(true))

    val map = new mutable.HashMap[String, Dataset]()
    for (field <- fields) yield
      field.get(obj) match {
        case set: Dataset => map(set.encoded)= set
        case _ =>
      }
    map.toMap
  }
}
