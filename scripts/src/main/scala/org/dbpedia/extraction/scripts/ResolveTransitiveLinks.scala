package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.config.ConfigUtils.parseLanguages
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util._

import scala.collection.mutable.LinkedHashMap
import java.io.File
import java.util.concurrent.ConcurrentHashMap

import org.dbpedia.extraction.config.Config
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.destinations.{DestinationUtils, WriterDestination}
import org.dbpedia.extraction.transform.QuadBuilder

import scala.Console.err
import scala.util.{Failure, Success, Try}
import scala.collection.convert.decorateAsScala._

/**
 * Replace triples in a dataset by their transitive closure.
 * All triples must use the same property. Cycles are removed.
 *
 * Example call:
 * ../run ResolveTransitiveLinks /data/dbpedia redirects transitive-redirects .nt.gz 10000-
 */
object ResolveTransitiveLinks {
  
  def main(args: Array[String]) {
    
    require(args != null && args.length >= 5, 
      "need at least five args: " +
      /*0*/ "base dir, " +
      /*1*/ "input file part (e.g. 'redirects'), " +
      /*2*/ "output file part (e.g. 'transitive-redirects'), " +
      /*3*/ "triples file suffix (e.g. '.nt.gz'), " +
      /*4*/ "languages or article count ranges (e.g. 'en,fr' or '10000-')" +
      /*5*/ "wikidata interlanguage links (optional: e.g. interlanguage-links) - if provided, transitive redirect is omitted if a wikidata uri exists for a redirect page" +
      /*6*/ "log dir (optional) - if provided, log output files are created ther for each language")
    
    val baseDir = new File(args(0))
    
    val input = args(1)
    require(input.nonEmpty, "no input dataset name")
    
    val output = args(2)
    require(output.nonEmpty, "no output dataset name")
    require(output != input, "output dataset name must different from input dataset name ")
    
    // Suffix of DBpedia files, for example ".nt", ".ttl.gz", ".nt.bz2" and so on.
    // This script works with .nt or .ttl files, using IRIs or URIs.
    // Does NOT work with .nq or .tql files. (Preserving the context wouldn't make sense.)
    val suffix = args(3)
    require(suffix.nonEmpty, "no file suffix")

    // Use all remaining args as keys or comma or whitespace separated lists of keys
    val languages = parseLanguages(baseDir, args(4).split(","))
    require(languages.nonEmpty, "no languages")

    val wikidataSamaAs = Try{args(5)}

    val logDir = Try{new File(args(6))}

    Workers.work(SimpleWorkers(1.5, 1.0) { language: Language =>

      val finder = new DateFinder(baseDir, language)
      finder.byName("redirects" + suffix, auto = true)

      // use LinkedHashMap to preserve order
      val map = new LinkedHashMap[String, String]()

      val logfile = logDir match {
        case Success(s) => new RichFile(new File(s, "resolveTransitiveLinks" + "_" + language.wikiCode + ".log"))
        case Failure(f) => null
      }


      val outputFile = finder.byName(output + suffix).get
      err.println(language.wikiCode+": writing "+outputFile+" ...")
      val suff = if(suffix.startsWith(".")) suffix.substring(1) else suffix
      val destination = DestinationUtils.createWriterDestination(outputFile, Config.universalConfig.formats.getOrElse(suff, throw new IllegalArgumentException("no formatter found for suffix: " + suffix)))

      val wikidatamap = new ConcurrentHashMap[String, Int]().asScala
      wikidataSamaAs match {
        case Success(w) => new QuadMapper(logfile).readQuads(finder, w + suffix) { quad =>
          if (quad.predicate != "http://www.w3.org/2002/07/owl#sameAs")
            throw new IllegalArgumentException("none sameAs predicate found in wikidata sameAs file")
          if(quad.value.startsWith(Language.Wikidata.dbpediaUri))
            wikidatamap.put(quad.subject, 0)
        }
        case Failure(e) =>
      }

      var count = 0
      var predicate: String = null
      new QuadMapper(logfile).readQuads(finder, input + suffix) { quad =>
        wikidatamap.get(quad.subject) match {
          case Some(s) =>
            count = count +1  //do nothing since we dont want to redirect if a wikidata uri exists
          case None => {
            if (quad.datatype != null) throw new IllegalArgumentException("expected object uri, found object literal: " + quad)
            if (predicate == null) predicate = quad.predicate
            else if (predicate != quad.predicate) throw new IllegalArgumentException("expected predicate " + predicate + ", found " + quad.predicate + ": " + quad)
            map(quad.subject) = quad.value
          }
        }
      }
      err.println(language.wikiCode + ": " + count + " redirects were suppressed since they have a wikidata uri")

      val qb = QuadBuilder.stringPredicate(language, DBpediaDatasets.RedirectsTransitive, predicate)

      err.println("resolving "+map.size+" links...")
      val cycles = new TransitiveClosure(map).resolve()
      err.println("found "+cycles.size+" cycles:")
      for (cycle <- cycles.sortBy(- _.size)) {
        err.println("length "+cycle.size+": ["+cycle.mkString(" ")+"]")
      }

      try {
        destination.open()
        for ((subjUri, objUri) <- map) {
          qb.setSubject(subjUri)
          qb.setValue(objUri)
          //TODO qb.setSourceUri() and provenance??
          destination.write(Seq(qb.getQuad))
        }
      }
      finally destination.close
    }, languages.sortBy(x => x.pages).reverse)
  }
}
