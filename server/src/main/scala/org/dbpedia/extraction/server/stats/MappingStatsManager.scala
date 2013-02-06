package org.dbpedia.extraction.server.stats

import java.io.File
import java.util.logging.Logger
import scala.io.Source
import org.dbpedia.extraction.mappings.Mappings
import org.dbpedia.extraction.util.StringUtils.prettyMillis
import org.dbpedia.extraction.util.Language

class MappingStatsManager(statsDir : File, language: Language)
extends MappingStatsConfig(statsDir, language)
{
    private val logger = Logger.getLogger(getClass.getName)

    val ignoreList = new IgnoreList(new File(statsDir, "ignorelist_"+language.wikiCode+".txt"), updateStats)

    val percentageFile = new File(statsDir, "percentage." + language.wikiCode)

    val wikiStats = loadStats
    
    @volatile var holder: MappingStatsHolder = null
    
    /**
     * Called by ignore list when it changes. TODO: the relations between all these holders, managers etc
     * are becoming too complex. For example, the server must call updateAll on the extractor manager
     * to make sure that the mappings are set in this stats manager. If that doesn't happen, we'll
     * get a NullPointerException here.
     */
    def updateStats(): Unit = updateStats(holder.mappings)

    /**
     * Called when ignore list changes, and by extractor manager when mappings or ontology
     * change.
     */
    def updateStats(mappings: Mappings): Unit = {
      holder = MappingStatsHolder(wikiStats, mappings, ignoreList)
    }

    private def loadStats(): WikipediaStats =
    {
        val millis = System.currentTimeMillis
        logger.info("Loading "+language.wikiCode+" wiki statistics from " + mappingStatsFile)
        val source = Source.fromFile(mappingStatsFile, "UTF-8")
        val wikiStats = try new WikipediaStatsReader(source.getLines).read() finally source.close
        logger.info("Loaded "+language.wikiCode+" wiki statistics from " + mappingStatsFile+" in "+prettyMillis(System.currentTimeMillis - millis))
        wikiStats
    }
}
