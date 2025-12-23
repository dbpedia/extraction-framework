package org.dbpedia.extraction.server

import java.io.File
import java.net.URL

/**
 * @param pagesUrl The URL where the pages of the Mappings Wiki are located
 * @param apiUrl The URL of the MediaWiki API of the Mappings Wiki 
 * @param statsDir Directory of statistics and ignore list files.
 * @param ontologyFile Ontology in xml dump format. If file is null or does not exist, 
 * load ontology from server.
 * @param mappingsDir Directory containing mappings in xml dump format. If directory is null or 
 * does not exist, load mappings from server. 
 */
class Paths(val pagesUrl: URL, val apiUrl: URL, val statsDir: File, val ontologyFile: File, val mappingsDir: File)
