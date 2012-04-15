package org.dbpedia.extraction.server.util

import java.io.File

import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.impl.wikipedia.Namespaces
import org.dbpedia.extraction.wikiparser.Namespace

class MappingStatsConfig(statsDir : File, language: Language)
{
    final val mappingStatsFile = new File(statsDir, "mappingstats_" + language.filePrefix + ".txt")
    final val templateNamespacePrefix = Namespace.Template.getName(language) + ":"
}
