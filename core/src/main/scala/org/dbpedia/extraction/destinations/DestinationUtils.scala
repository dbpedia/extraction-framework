package org.dbpedia.extraction.destinations

import java.io.File

import org.dbpedia.extraction.config.provenance.{DBpediaDatasets, Dataset}
import org.dbpedia.extraction.destinations.formatters.{Formatter, TerseFormatter}
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Created by Chile on 9/29/2016.
  */
object DestinationUtils {
  def createDatasetDestination(finder: Finder[File], date: String, datasets: Seq[Dataset], formats: collection.Map[String, Formatter], append: Boolean) : Destination = {
    val destination = new ArrayBuffer[Destination]()
    for ((suffix, format) <- formats) {
      val datasetDestinations = new mutable.HashMap[Dataset, Destination]()
      for (dataset <- datasets) {
        finder.file(date, dataset.filenameEncoded +'.'+suffix) match{
          case Some(file) => datasetDestinations(dataset) = createWriterDestination(file, format)
          case None =>
        }
      }
      destination += new DatasetDestination(datasetDestinations)
    }
    new CompositeDestination(destination: _*)
  }

  def createDatasetDestination(finder: DateFinder[File], datasets: Seq[Dataset], formats: collection.Map[String, Formatter], append: Boolean = false) : Destination = {
    createDatasetDestination(finder, finder.date, datasets, formats, append)
  }

  def createDatasetDestination(finder: DateFinder[File], datasets: Seq[String], formats: collection.Map[String, Formatter]) : Destination = {
    createDatasetDestination(finder, finder.date, datasets.map(x => DBpediaDatasets.getDataset(x).getOrElse[Dataset](
      throw new IllegalArgumentException("A dataset named " + x + " is unknown."))), formats, append = false)
  }

  def createWriterDestination(file: FileLike[_], format: Formatter): WriterDestination ={
    new WriterDestination(() => IOUtils.writer(file), format)
  }

  def createWriterDestination(file: FileLike[_]): WriterDestination ={
    new WriterDestination(() => IOUtils.writer(file), new TerseFormatter(false, true))
  }
}
