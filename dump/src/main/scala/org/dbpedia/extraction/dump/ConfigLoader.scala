package org.dbpedia.extraction.dump

import _root_.org.dbpedia.extraction.destinations.formatters.{NTriplesFormatter, NQuadsFormatter}
import _root_.org.dbpedia.extraction.destinations.{FileDestination, CompositeDestination}
import _root_.org.dbpedia.extraction.mappings._
import collection.immutable.ListMap
import java.util.Properties
import java.io.{FileReader, File}
import _root_.org.dbpedia.extraction.util.StringUtils._
import _root_.org.dbpedia.extraction.util.Language
import java.net.URL
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.sources.{MemorySource, Source, XMLSource, WikiSource}
import org.dbpedia.extraction.wikiparser.{WikiParser, PageNode, WikiTitle}

/**
 * Loads the dump extraction configuration.
 */
object ConfigLoader
{
    private var config : Config = null

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

        //Load configuration
        config = new Config(properties)

        //Update dumps (if configured to do so)
        if(config.update) Download.download(config.dumpDir, config.languages.map(_.wikiCode))

        //Create a non-strict view of the extraction jobs
        config.extractors.keySet.view.map(createExtractionJob)
    }

    private class Config(config : Properties)
    {
        /** Dump directory */
        if(config.getProperty("dumpDir") == null) throw new IllegalArgumentException("Property 'dumpDir' not defined.")
        val dumpDir = new File(config.getProperty("dumpDir"))

        /** Output directory */
        if(config.getProperty("outputDir") == null) throw new IllegalArgumentException("Property 'outputDir' not defined.")
        val outputDir = new File(config.getProperty("outputDir"))

        //** Update dumps boolean */
        val update = Option(config.getProperty("updateDumps")).getOrElse("false").trim.toLowerCase match
        {
            case BooleanLiteral(b) => b
            case _ => throw new IllegalArgumentException("Invalid value for property 'updateDumps'")
        }

        /** Languages */
        if(config.getProperty("languages") == null) throw new IllegalArgumentException("Property 'languages' not defined.")
        val languages = config.getProperty("languages").split("\\s+").map(_.trim).toList
                        .map(code => Language.fromWikiCode(code).getOrElse(throw new IllegalArgumentException("Invalid language: '" + code + "'")))

        /** Extractor classes */
        val extractors = loadExtractorClasses()

        /**
         * Loads the extractors classes from the configuration.
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
            val LanguageExtractor = """extractors\.(.*)""".r

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
            .map(className => ClassLoader.getSystemClassLoader.loadClass(className))
            .map(_.asInstanceOf[Class[Extractor]])
        }
    }


    /**
     * Creates ab extraction job for a specific language.
     */
    private def createExtractionJob(lang : Language) : ExtractionJob =
    {
        //Extraction Context
        val context : DumpExtractionContext = extractionContext(lang)

        //Extractors
        val extractors = config.extractors(lang)
        val compositeExtractor = Extractor.load(extractors, context)

        //Destination
        val tripleDestination = new FileDestination(new NTriplesFormatter(), config.outputDir, dataset => lang.filePrefix + "/" + dataset.name + "_" + lang.filePrefix + ".nt")
        val quadDestination = new FileDestination(new NQuadsFormatter(), config.outputDir, dataset => lang.filePrefix + "/" + dataset.name + "_" + lang.filePrefix + ".nq")
        val destination = new CompositeDestination(tripleDestination, quadDestination)

        val jobLabel = "Extraction Job for " + lang.wikiCode + " Wikipedia with " + extractors.size + " extractors"
        new ExtractionJob(compositeExtractor, context.articlesSource, destination, jobLabel)
    }

    private val parser = WikiParser()
    /**
     * Make an object that will be injected into the extractors.
     */
    private def extractionContext(lang : Language) : DumpExtractionContext = new DumpExtractionContext
    {
        def ontology : Ontology = _ontology

        def commonsSource : Source = _commonsSource

        def language : Language = lang

        private lazy val _mappingPageSource =
        {
            WikiTitle.Namespace.mappingNamespace(language) match
            {
                case Some(namespace) => WikiSource.fromNamespaces(namespaces = Set(namespace),
                                                                  url = new URL("http://mappings.dbpedia.org/api.php"),
                                                                  language = Language.Default).map(parser)
                case None => new MemorySource().map(parser)
            }
        }
        def mappingPageSource : Traversable[PageNode] = _mappingPageSource

        private lazy val _mappings =
        {
            MappingsLoader.load(this)
        }
        def mappings : Mappings = _mappings

        private lazy val _articlesSource =
        {
            XMLSource.fromFile(getDumpFile(config.dumpDir, language.wikiCode),
                title => title.namespace == WikiTitle.Namespace.Main || title.namespace == WikiTitle.Namespace.File ||
                         title.namespace == WikiTitle.Namespace.Category || title.namespace == WikiTitle.Namespace.Template)
        }
        def articlesSource : Source = _articlesSource

        private lazy val _redirects =
        {
            Redirects.load(articlesSource, language)
        }
        def redirects : Redirects = _redirects
    }

    //language-independent val
    private lazy val _ontology =
    {
        val ontologySource = WikiSource.fromNamespaces(namespaces = Set(WikiTitle.Namespace.OntologyClass, WikiTitle.Namespace.OntologyProperty),
                                                       url = new URL("http://mappings.dbpedia.org/api.php"),
                                                       language = Language.Default )
        new OntologyReader().read(ontologySource)
    }

    //language-independent val
    private lazy val _commonsSource =
    {
        XMLSource.fromFile(getDumpFile(config.dumpDir, "commons"), _.namespace == WikiTitle.Namespace.File)
    }

    /**
     * Retrieves the dump stream for a specific language edition.
     */
    private def getDumpFile(dumpDir : File, wikiPrefix : String) : File =    //wikiPrefix is language prefix (and can be 'commons')
    {
        val wikiDir = new File(dumpDir + "/" + wikiPrefix)
        if(!wikiDir.isDirectory) throw new Exception("Dump directory not found: " + wikiDir)

        //Find most recent dump date
        val date = wikiDir.list()
                   .filter(_.matches("\\d{8}"))
                   .sortWith(_.toInt > _.toInt)
                   .headOption.getOrElse(throw new Exception("No dump found for Wiki: " + wikiPrefix))

        val articlesDump = new File(wikiDir + "/" + date + "/" + wikiPrefix.replace('-', '_') + "wiki-" + date + "-pages-articles.xml")
        if(!articlesDump.isFile) throw new Exception("Dump not found: " + articlesDump)

        articlesDump
    }
}