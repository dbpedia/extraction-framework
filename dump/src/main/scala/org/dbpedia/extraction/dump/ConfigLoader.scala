package org.dbpedia.extraction.dump

import _root_.org.dbpedia.extraction.destinations.formatters.{NTriplesFormatter, NQuadsFormatter}
import _root_.org.dbpedia.extraction.destinations.{FileDestination, CompositeDestination}
import _root_.org.dbpedia.extraction.mappings._
import java.net.URL
import _root_.org.dbpedia.extraction.wikiparser.WikiTitle
import collection.immutable.ListMap
import java.util.Properties
import java.io.{FileReader, File}
import _root_.org.dbpedia.extraction.util.StringUtils._
import _root_.org.dbpedia.extraction.util.Language
import _root_.org.dbpedia.extraction.sources.{WikiSource, MemorySource, XMLSource}

/**
 * Loads the dump extraction configuration.
 */
object ConfigLoader
{
    /**
     * Loads the configuration and creates extraction jobs for all configured languages.
     *
     * @param configFile The configuration file
     * @return Non-strict Traversable over all configured extraction jobs i.e. an extractions job will not be created until it is explicitly requested.
     */
    def load(configFile : File) : Traversable[ExtractionJob] =
    {
        //Load properties
        val properties = new Properties()
        properties.load(new FileReader(configFile))

        //Update dumps (if configured to do so)
        updateDumps(properties)

        //Load configuration
        val config = new Config(properties)

        //Create a non-strict view of the extraction jobs
        config.extractors.keySet.view.map(createExtractionJob(config))
    }

    //TODO load properties only once
    def updateDumps(config : Properties)
    {
        //Load property dumpDir
        if(config.getProperty("dumpDir") == null) throw new IllegalArgumentException("Property 'dumpDir' not defined.")
        val dumpDir = new File(config.getProperty("dumpDir"))

        //Load languages
        if(config.getProperty("languages") == null) throw new IllegalArgumentException("Property 'languages' not defined.")
        val languages = config.getProperty("languages").split("\\s+").map(_.trim).toList

        //Load property updateDumps
        val update = Option(config.getProperty("updateDumps")).getOrElse(return false).trim.toLowerCase match
        {
            case BooleanLiteral(b) => b
            case _ => throw new IllegalArgumentException("Invalid value for property 'updateDumps'")
        }

        //Update dumps
        if(update) Download.download(dumpDir, languages)
    }

    /**
     * Creates ab extraction job for a specific language.
     */
    private def createExtractionJob(config : Config)(language : Language) : ExtractionJob =
    {
        /** Mappings source */
        val mappingsSource = WikiTitle.Namespace.mappingNamespace(language) match
        {
            case Some(namespace) => WikiSource.fromNamespaces(namespaces = Set(namespace),
                                                              url = new URL("http://mappings.dbpedia.org/api.php"),
                                                              language = Language.Default)
            case None => new MemorySource()
        }

        //Articles source
        val articlesSource = XMLSource.fromFile(config.getDumpFile(language.wikiCode),
            title => title.namespace == WikiTitle.Namespace.Main || title.namespace == WikiTitle.Namespace.File ||
                    title.namespace == WikiTitle.Namespace.Category || title.namespace == WikiTitle.Namespace.Template)

        //Extractor
        val extractor = Extractor.load(config.ontologySource, mappingsSource, config.commonsSource, articlesSource, config.extractors(language), language)

        //Destination
        val tripleDestination = new FileDestination(new NTriplesFormatter(), config.outputDir, dataset => language.filePrefix + "/" + dataset.name + "_" + language.filePrefix + ".nt")
        val quadDestination = new FileDestination(new NQuadsFormatter(), config.outputDir, dataset => language.filePrefix + "/" + dataset.name + "_" + language.filePrefix + ".nq")
        val destination = new CompositeDestination(tripleDestination, quadDestination)

        new ExtractionJob(extractor, articlesSource, destination, "Extraction Job for " + language.wikiCode + " Wikipedia")
    }

    private class Config(config : Properties)
    {
        /** Dump directory */
        if(config.getProperty("dumpDir") == null) throw new IllegalArgumentException("Property 'dumpDir' not defined.")
        val dumpDir = new File(config.getProperty("dumpDir"))

        /** Output directory */
        if(config.getProperty("outputDir") == null) throw new IllegalArgumentException("Property 'outputDir' not defined.")
        val outputDir = new File(config.getProperty("outputDir"))

        /** Languages */
        if(config.getProperty("languages") == null) throw new IllegalArgumentException("Property 'languages' not defined.")
        private val languages = config.getProperty("languages").split("\\s+").map(_.trim).toList
                        .map(code => Language.fromWikiCode(code).getOrElse(throw new IllegalArgumentException("Invalid language: '" + code + "'")))

        /** Extractor classes */
        val extractors = loadExtractorClasses()

        /** Ontology source */
        val ontologySource = WikiSource.fromNamespaces(namespaces = Set(WikiTitle.Namespace.OntologyClass, WikiTitle.Namespace.OntologyProperty),
                                                       url = new URL("http://mappings.dbpedia.org/api.php"),
                                                       language = Language.Default )

        /** Commons source */
        val commonsSource = XMLSource.fromFile(getDumpFile("commons"), _.namespace == WikiTitle.Namespace.File)

        /**
         * Retrieves the dump stream for a specific language edition.
         */
        def getDumpFile(wikiPrefix : String) : File =
        {
            val wikiDir = new File(dumpDir + "/" + wikiPrefix)
            if(!wikiDir.exists) throw new Exception("Dump directory not found: " + wikiDir)

            //Find most recent dump date
            val date = wikiDir.list()
                       .filter(_.matches("\\d{8}"))
                       .sortWith(_.toInt > _.toInt)
                       .headOption.getOrElse(throw new Exception("No dump found for Wiki: " + wikiPrefix))

            val articlesDump = new File(wikiDir + "/" + date + "/" + wikiPrefix.replace('-', '_') + "wiki-" + date + "-pages-articles.xml")
            if(!articlesDump.exists) throw new Exception("Dump not found: " + articlesDump)

            articlesDump
        }

        /**
         *  Loads the extractors classes from the configuration.
         *
         * @return A Map which contains the extractor classes for each language
         */
        private def loadExtractorClasses() : Map[Language, List[Class[Extractor]]] =
        {
            //Load extractor classes
            if(config.getProperty("extractors") == null) throw new IllegalArgumentException("Property 'extractors' not defined.")
            val stdExtractors = loadExtractorConfig(config.getProperty("extractors"))

            //Create extractor map
            var extractors = ListMap[Language, List[Class[Extractor]]]()
            for(language <- languages) extractors += ((language, stdExtractors))

            //Load language specific extractors
            val LanguageExtractor = "extractors\\.(.*)".r

            for(LanguageExtractor(code) <- config.stringPropertyNames.toArray;
                language = Language.fromISOCode(code).getOrElse(throw new IllegalArgumentException("Invalid language: " + code));
                if extractors.contains(language))
            {
                extractors += ((language, stdExtractors ::: loadExtractorConfig(config.getProperty("extractors." + code))))
            }

            extractors
        }

        /**
         * Parses a enumeration of extractor classes.
         */
        private def loadExtractorConfig(configStr : String) : List[Class[Extractor]] =
        {
            configStr.split("\\s+").map(_.trim).toList
            .map(className => ClassLoader.getSystemClassLoader().loadClass(className))
            .map(_.asInstanceOf[Class[Extractor]])
        }
    }
}