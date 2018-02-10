package org.dbpedia.extraction.config

import java.io.File

import org.dbpedia.extraction.config.provenance.QuadProvenanceRecord
import org.dbpedia.extraction.destinations.ProvenanceDestination
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util._
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.iri.IRI

import scala.collection.mutable

private[config] class ProvenanceRecordManager(val baseDir: RichFile, val wikiName: String) extends AutoCloseable {

  val finders = new mutable.HashMap[Language, DateFinder[File]]()
  val fileMap = new mutable.HashMap[(Language, IRI), ProvenanceDestination]()

  def getDestination(lang: Language, dataset: IRI): ProvenanceDestination ={
    fileMap.get((lang, dataset)) match{
      case Some(d) => d
      case None =>
        val finder = finders.get(lang) match{
          case Some(f) => f
          case None =>
            val f = new DateFinder[File](baseDir.getFile, lang, wikiName)
            finders.put(lang, f)
            f
        }
        val provDir = finder.provenanceDir(finder.date)
        if(!provDir.exists())
          provDir.mkdir()
        val fileName = dataset.getPath.substring(dataset.getPath.lastIndexOf("/")+1).replace("_", "-")
        val prefix = lang.wikiCode + wikiName + "-" + finder.date + "-"
        val destFile: RichFile = new File(provDir, prefix + fileName + "-prov.json.bz2")
        destFile.getFile.createNewFile()
        val dest = new ProvenanceDestination(() => IOUtils.writer(destFile))
        dest.open()
        fileMap.put((lang, dataset), dest)
        dest
    }
  }

  def ingestQuad(quads: Seq[Quad]): Unit ={
    quads.flatMap(q => q.getProvenanceRecord).foreach(prov =>{
      getDestination(prov.metadata.language, prov.metadata.datasetIri).writeProvenance(Seq(prov))
    })
  }

  def ingestRecord(records: Seq[QuadProvenanceRecord]): Unit ={
    records.foreach(prov =>{
      getDestination(prov.metadata.language, prov.metadata.datasetIri).writeProvenance(Seq(prov))
    })
  }

  override def close() = fileMap.values.foreach(d => d.close())
}
