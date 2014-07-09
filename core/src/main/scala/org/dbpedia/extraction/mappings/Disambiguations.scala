package org.dbpedia.extraction.mappings

import java.util.logging.{Level, Logger}
import java.io._
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.util.RichReader._
import collection.mutable.{HashSet}

/**
 */
class Disambiguations(val set : Set[Long])
{
  /**
   */
  def isDisambiguation(id : Long) : Boolean =
  {
     set.contains(id)
  }
}

/**
 */
object Disambiguations
{
  private val logger = Logger.getLogger(classOf[Disambiguations].getName)

  /**
   * return an empty Disambiguations object
   */
  def empty() : Disambiguations = new Disambiguations(Set.empty.asInstanceOf[Set[Long]])

  /**
   */
  def load(reader : () => Reader, cache : File, lang : Language) : Disambiguations =
  {
    try
    {
      return loadFromCache(cache)
    }
    catch
      {
        case ex : Exception => logger.log(Level.INFO, "Will extract disambiguations from source for "+lang.wikiCode+" wiki, could not load cache file '"+cache+"': "+ex)
      }

    val disambiguations = loadFromFile(reader, lang)

    val dir = cache.getParentFile
    if (! dir.exists && ! dir.mkdirs) throw new IOException("cache dir ["+dir+"] does not exist and cannot be created")
    val outputStream = new ObjectOutputStream(new FileOutputStream(cache))
    try
    {
      outputStream.writeObject(disambiguations.set)
    }
    finally
    {
      outputStream.close()
    }
    logger.info(disambiguations.set.size + " disambiguations written to cache file " + cache)

    disambiguations
  }

  /**
   */
  private def loadFromCache(cache : File) : Disambiguations =
  {
    logger.info("Loading disambiguations from cache file " + cache)
    val inputStream = new ObjectInputStream(new FileInputStream(cache))
    try
    {
      val disambiguations = new Disambiguations(inputStream.readObject().asInstanceOf[Set[Long]])

      logger.info(disambiguations.set.size + " disambiguations loaded from cache file " + cache)
      disambiguations
    }
    finally
    {
      inputStream.close()
    }
  }

  /**
   */
  def loadFromFile(reader : () => Reader, lang : Language) : Disambiguations =
  {
    logger.info("Loading disambiguations from source (" + lang.wikiCode + ")")

    val disambiguationsFinder = new DisambiguationsFinder(lang)

    val disambiguations = new Disambiguations(disambiguationsFinder(reader))

    logger.info("Disambiguations loaded from source ("+lang.wikiCode+")")
    disambiguations
  }

  private class DisambiguationsFinder(lang : Language)
  {
    // (264534,'disambiguation','')
    val regex = """\((\d+),'disambiguation',''\)""".r

    def apply(reader : () => Reader): Set[Long]=
    {
      val d = HashSet[Long]()
      val r = reader()
      r.foreach { line =>
        d ++= regex.findAllIn(line).matchData.map { m => m.group(1).toLong }.toSet
      }
      r.close()
      d.toSet
    }
  }
}
