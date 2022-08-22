package org.dbpedia.extraction.dump

import java.io.File

import org.dbpedia.extraction.dump.tags.DownloadTestTag
import org.dbpedia.extraction.util.MappingsDownloader.apiUrl
import org.dbpedia.extraction.util.{Language, OntologyDownloader, WikiDownloader}
import org.dbpedia.extraction.wikiparser.Namespace
import org.scalatest.{DoNotDiscover, FunSuite}

@DoNotDiscover
class DownloadTest extends FunSuite {

  test("download ontology", DownloadTestTag) {
    val dumpFile = new File("../ontology.xml")
    val owlFile = new File("../ontology.owl")
    val version = "1.0"
    org.dbpedia.extraction.util.OntologyDownloader.download(dumpFile)
    val ontology = OntologyDownloader.load(dumpFile)
    org.dbpedia.extraction.util.OntologyDownloader.save(ontology, version, owlFile)
  }

  test("download mappings", DownloadTestTag) {
    val dir = new File("../mappings")
    // don't use mkdirs, that often masks mistakes.
    require(dir.isDirectory || dir.mkdir, "directory ["+dir+"] does not exist and cannot be created")
    Namespace.mappings.values.par.foreach { namespace =>
      val file = new File(dir, namespace.name(Language.Mappings).replace(' ','_')+".xml")
      val nanos = System.nanoTime
      println("downloading mappings from "+apiUrl+" to "+file)
      new WikiDownloader(apiUrl).download(file, namespace)
      println("downloaded mappings from "+apiUrl+" to "+file+" in "+((System.nanoTime - nanos) / 1000000000F)+" seconds")
    }
  }
}
