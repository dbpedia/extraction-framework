package org.dbpedia.extraction.config

import java.io.File

import org.dbpedia.extraction.config.provenance.QuadProvenanceRecord
import org.dbpedia.extraction.destinations.ProvenanceDestination
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.{IOUtils, Language, RichFile}
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.iri.IRI

import scala.collection.mutable

private[config] class ProvenanceRecordManager(val recordDir: RichFile) extends AutoCloseable {

  val dirMap = new mutable.HashMap[Language, RichFile]()
  val fileMap = new mutable.HashMap[(Language, IRI), ProvenanceDestination]()

  def getDestination(lang: Language, dataset: IRI): ProvenanceDestination ={
    fileMap.get((lang, dataset)) match{
      case Some(d) => d
      case None =>
        val dir = dirMap.get(lang) match{
          case Some(d) => d
          case None =>
            val newDir = new File(recordDir.getFile, lang.wikiCode)
            if(!newDir.exists())
              newDir.mkdir()
            dirMap.put(lang, newDir)
            new RichFile(newDir)
        }
        val fileName = dataset.getPath.substring(dataset.getPath.lastIndexOf("/")+1)
        val destFile: RichFile = new File(dir.getFile, fileName + "_prov.json.bz2")
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
