package org.dbpedia.extraction.mappings

import java.io._

import org.dbpedia.extraction.config.ExtractionLogger
import org.dbpedia.extraction.dataparser.RedirectFinder
import org.dbpedia.extraction.sources.Source
import org.dbpedia.extraction.util.{IOUtils, Language}
import org.dbpedia.extraction.wikiparser._

import scala.collection.mutable

/**
 * Holds the redirects between wiki pages
 * At the moment, only redirects between Templates are considered
 */
// FIXME: this class is a hack. Resolving redirects is a central part of DBpedia and should be done
// right and more explicitly. This class is trying to do too much under the hood.
// FIXME: this class does basically the same thing as RedirectExtractor, just differently.
class Redirects()
{
    private val map = new mutable.HashMap[String, String]()

    def enterRedirect(source: String, target: String): Unit ={
        map.put(source, target)
    }

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
    private val logger = ExtractionLogger.getLogger(getClass, Language.None)

    /**
      * load from an existing map
      * @param map
      * @return
      */
    def fromMap(map : Map[String, String]): Redirects ={
        val reds = new Redirects
        map.foreach(e => reds.enterRedirect(e._1, e._2))
        reds
    }

    /**
     * Loads the redirects from a cache file.
     */
    def loadFromCache(cache : File) : Redirects =
    {
        logger.info("Loading redirects from cache file "+cache)
        Redirects.fromMap(IOUtils.loadSerializedObject[mutable.HashMap[String, String]](cache).toMap)
    }

    /**
      * Loads the redirects from a cache file.
      */
    def saveToCache(cache : File, red: Redirects) : Unit =
    {
        logger.info("Saving redirects to cache file "+cache)
        IOUtils.serializeToObjectFile(cache, red.map)
    }

    /**
     * Loads the redirects from a source.
     */
    def loadFromSource(source : Source, lang : Language) : Redirects =
    {
        logger.info("Loading redirects from source ("+lang.wikiCode+")")

        val redirectFinder = RedirectFinder.getRedirectFinder(lang)

        val res = source.flatMap(x => {
            redirectFinder.apply(x)
        })
        val redirects = Redirects.fromMap(res.map(x => x._1.decoded -> x._2.decoded).toMap)

        logger.info("Redirects loaded from source ("+lang.wikiCode+")")
        redirects
    }

}