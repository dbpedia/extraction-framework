package org.dbpedia.extraction.mappings

import java.io._
import java.util.logging.{Level, Logger}

import org.dbpedia.extraction.dataparser.RedirectFinder
import org.dbpedia.extraction.sources.Source
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser._

import scala.collection.mutable

/**
 * Holds the redirects between wiki pages
 * At the moment, only redirects between Templates are considered
 *
 * @param map Redirect map. Contains decoded template titles.
 */
// FIXME: this class is a hack. Resolving redirects is a central part of DBpedia and should be done
// right and more explicitly. This class is trying to do too much under the hood.
// FIXME: this class does basically the same thing as RedirectExtractor, just differently.
//TODO make map private?
//TODO language dependent
class Redirects(val map : Map[String, String])
{
    /**
     * Resolves a redirect.
     *
     * @param title The title of the page
     * @return If this page is a redirect, the destination of the redirect.
     * If this page is not a redirect, the page itself.
     */
    def resolve(title : WikiTitle) : WikiTitle =
    {
        // if there is no redirect for given title, just return same object
        if (! map.contains(title.decoded)) return title
        
        //Remember already visited pages to avoid cycles
        val visited = new mutable.HashSet[String]()

        //Follows redirects
        var currentTitle = title.decoded
        while(!visited.contains(currentTitle))
        {
            visited.add(currentTitle)
            map.get(currentTitle) match
            {
                case Some(destinationTitle) => currentTitle = destinationTitle
                case None => return new WikiTitle(currentTitle, Namespace.Template, title.language)
            }
        }

        //Detected a cycle
        title
    }

    /**
     * TODO: comment. What does this method do?
     */
    def resolveMap[T](mappings : Map[String, T]) : Map[String, T] =
    {
        val resolvedMappings = new mutable.HashMap[String, T]()

        for((source, destination) <- map if !mappings.contains(source))
        {
            //Remember already visited pages to avoid cycles
            val visited = new mutable.HashSet[String]()
            visited.add(source)

            //Compute transitive hull
            var lastDestination = source
            var currentDestination = destination
            while(currentDestination != null && !visited.contains(currentDestination))
            {
                 visited.add(currentDestination)
                 lastDestination = currentDestination
                 currentDestination = map.get(currentDestination).orNull
            }

            //Add to redirect map
            for(destinationT <- mappings.get(lastDestination))
            {
                resolvedMappings(source) = destinationT
            }
        }

        for ((source, destination) <- map if !mappings.contains(destination)) {
          if (mappings.contains(source)) {
            resolvedMappings(destination) = mappings(source)
          }
        }

        mappings ++ resolvedMappings
    }
}

/**
 * Loads redirects from a cache file or source of Wiki pages.
 * At the moment, only redirects between Templates are considered
 */
object Redirects
{
    private val logger = Logger.getLogger(classOf[Redirects].getName)

    /**
     * Tries to load the redirects from a cache file.
     * If not successful, loads the redirects from a source.
     * Updates the cache after loading the redirects from the source.
     */
    def load(source : Source, cache : File, lang : Language) : Redirects =
    {
        //Try to load redirects from the cache
        try
        {
           return loadFromCache(cache)
        }
        catch
        {
            case ex : Exception => logger.log(Level.INFO, "Will extract redirects from source for "+lang.wikiCode+" wiki, could not load cache file '"+cache+"': "+ex)
        }

        //Load redirects from source
        val redirects = loadFromSource(source, lang)
        
        val dir = cache.getParentFile
        if (! dir.exists && ! dir.mkdirs) throw new IOException("cache dir ["+dir+"] does not exist and cannot be created")
        val outputStream = new ObjectOutputStream(new FileOutputStream(cache))
        try
        {
            outputStream.writeObject(redirects.map)
        }
        finally
        {
            outputStream.close()
        }
        logger.info(redirects.map.size + " redirects written to cache file "+cache)

        redirects
    }

    /**
     * Loads the redirects from a cache file.
     */
    private def loadFromCache(cache : File) : Redirects =
    {
        logger.info("Loading redirects from cache file "+cache)
        val inputStream = new ObjectInputStream(new FileInputStream(cache))
        try
        {
            val redirects = new Redirects(inputStream.readObject().asInstanceOf[Map[String, String]])

            logger.info(redirects.map.size + " redirects loaded from cache file "+cache)
            redirects
        }
        finally
        {
            inputStream.close()
        }
    }

    /**
     * Loads the redirects from a source.
     */
    def loadFromSource(source : Source, lang : Language) : Redirects =
    {
        logger.info("Loading redirects from source ("+lang.wikiCode+")")

        val redirectFinder = RedirectFinder.getRedirectFinder(lang)

        val redirects = new Redirects(source.map(redirectFinder).flatten
          .map(x => x._1.decoded -> x._2.decoded).toMap)

        logger.info("Redirects loaded from source ("+lang.wikiCode+")")
        redirects
    }

}