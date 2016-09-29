package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.util.{DateFinder, RichFile, Finder, IOUtils}
import org.dbpedia.extraction.util.ConfigUtils.parseLanguages
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.wikiparser.Namespace
import org.apache.commons.lang.StringEscapeUtils
import java.io.File
import scala.Long


/**
 * Generates an SQL import file that contains all cache items
 *
 * Example calls:
 * ../run UnmodifiedFeederCacheGenerator /data/dbpedia .nt.gz 2013-02-01 en
 */

object UnmodifiedFeederCacheGenerator {

  def main(args: Array[String]): Unit = {

    require(args != null && args.length >= 4,
      "need at least four args: " +
        /*0*/ "base dir, " +
        /*1*/ "mapping file suffix (e.g. '.nt.gz', '.ttl', '.ttl.bz2'), " +
        /*2*/ "timestamp to use for cache (e.g. '2013-02-27' ), "  +
        /*3*/ "languages (e.g. 'en,fr')" )


    val baseDir = new File(args(0))

    // Suffix of mapping files, for example ".nt", ".ttl.gz", ".nt.bz2" and so on.
    // This script works with .nt, .ttl, .nq or .tql files, using IRIs or URIs.
    val suffix = args(1)
    require(suffix.nonEmpty, "no mapping file suffix")

    val timestamp = args(2)
    require(timestamp.nonEmpty, "no timestamp")

    val languages = parseLanguages(baseDir, args.drop(3))
    require(languages.nonEmpty, "no languages")

    for (language <- languages) {

      val finder = new DateFinder(baseDir, language)
      val writer = IOUtils.writer(new File(language.wikiCode + "-cache_generate.sql"))

      try {
        QuadReader.readQuads(finder, "page-ids" + suffix, auto = true) {
          quad =>
            val pageID = quad.value.toInt
            var pageTitle = StringEscapeUtils.escapeSql(quad.subject.split("dbpedia.org/resource/", 2)(1))
            if (pageTitle.contains(':')) {
              val splitPageTitle = pageTitle.split(":", 2)
              val namespace = Namespace.get(language, splitPageTitle(0).replace("_", " "))
              if (namespace.isDefined) {
                pageTitle = splitPageTitle(1)
              }
            }
            if (pageID > 0) {
              writer.write("INSERT INTO DBPEDIALIVE_CACHE(pageID, title, updated) VALUES(" + pageID + ", '" + pageTitle + "', '" + timestamp + "') ; \n")
            }

        }
      }
      finally writer.close


    }
  }
}