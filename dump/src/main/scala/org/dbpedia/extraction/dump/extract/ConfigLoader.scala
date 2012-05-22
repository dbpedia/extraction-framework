package org.dbpedia.extraction.dump.extract

import org.dbpedia.extraction.destinations.formatters.{Formatter,TerseFormatter,TriXFormatter}
import org.dbpedia.extraction.destinations.formatters.UriPolicy._
import org.dbpedia.extraction.destinations.{Destination,FileDestination,CompositeDestination}
import org.dbpedia.extraction.mappings._
import scala.collection.immutable.ListMap
import scala.collection.mutable.{HashMap,ArrayBuffer,HashSet}
import java.util.Properties
import java.io.{FileInputStream, InputStreamReader, File}
import org.dbpedia.extraction.util.StringUtils._
import org.dbpedia.extraction.util.{Language,Finder}
import org.dbpedia.extraction.util.RichFile.toRichFile
import java.net.{URI,URL}
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.sources.{MemorySource, Source, XMLSource, WikiSource}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.destinations.Dataset
import org.dbpedia.extraction.dump.download.Download
import scala.collection.JavaConversions.asScalaSet // implicit
import org.dbpedia.extraction.destinations.MarkerDestination

/**
 * Loads the dump extraction configuration.
 * 
 * TODO: clean up. The relations between the objects, classes and methods have become a bit chaotic.
 * There is no clean separation of concerns.
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
        properties.load(new InputStreamReader(new FileInputStream(configFile), "UTF-8"))

        //Load configuration
        config = new Config(properties)

        //Create a non-strict view of the extraction jobs
        // TODO: why non-strict?
        config.extractorClasses.keySet.view.map(createExtractionJob)
    }
    
    private var ontologyFile : File = null

    private var mappingsDir : File = null
    
    private var formats: List[String] = null
    
    private var requireComplete = false

    private class Config(config : Properties)
    {
        // TODO: rewrite this, similar to download stuff:
        // - Don't use java.util.Properties, allow multiple values for one key
        // - Resolve config file names and load them as well
        // - Use pattern matching to parse arguments
        // - allow multiple config files, given on command line
      
        /** Dump directory */
        val dumpDir = getFile("dir")
        if (dumpDir == null) throw error("property 'dir' not defined.")
        if (! dumpDir.exists) throw error("dir "+dumpDir+" does not exist")
        
        if(config.getProperty("require-download-complete") != null)
          requireComplete = config.getProperty("require-download-complete").toBoolean

        /** Local ontology file, downloaded for speed and reproducibility */
        ontologyFile = getFile("ontology")

        /** Local mappings files, downloaded for speed and reproducibility */
        mappingsDir = getFile("mappings")
        
        /**
         * Order is important here: First convert IRI to URI, then append '_' if necessary,
         * then convert specific domain to generic domain. The second step must happen 
         * after URI conversion (because a URI may need an underscore where a IRI doesn't), 
         * and before the third step (because we need the specific domain to decide which 
         * URIs should be made xml-safe).
         * 
         * In each tuple, the key is the policy name. The value is a policy-factory that
         * takes a predicate. The predicate decides for which DBpedia URIs a policy
         * should be applied. We pass a predicate to the policy-factory to get the policy.
         */
        private val policyFactories = Seq[(String, Predicate => Policy)] (
          "uris" -> uris,
          "xml-safe" -> xmlSafe,
          "generic" -> generic
        )

        /**
         * Parses a list of languages like "en,fr" or "*" or even "en,*,fr"
         */
        private def parsePredicate(languages: String): Predicate = {
          
          val codes = split(languages, ',').toSet
          val domains = codes.map { code => if (code == "*") "*" else Language(code).dbpediaDomain }
          
          if (domains("*")) { uri => uri.getHost.equals("dbpedia.org") || uri.getHost.endsWith(".dbpedia.org") }
          else { uri => domains(uri.getHost) }
        }
        
        /**
         * Parses a policy line like "uri-policy.main=uris:en,fr; generic:en"
         */
        private def parsePolicy(key: String): Policy = {
          
          val predicates = new HashMap[String, Predicate]()
          
          // parse all predicates
          for (policy <- splitValue(key, ';')) {
            split(policy, ':') match {
              case List(key, languages) => {
                require(! predicates.contains(key), "duplicate policy '"+key+"'")
                predicates(key) = parsePredicate(languages)
              }
              case _ => throw error("invalid format: '"+policy+"'")
            }
          }
          
          require(predicates.nonEmpty, "found no URI policies")
          
          val policies = new ArrayBuffer[Policy]()
          
          // go through known policies in correct order, get predicate, and create policy
          for ((key, factory) <- policyFactories; predicate <- predicates.remove(key)) {
            policies += factory(predicate)
          }
          
          require(predicates.isEmpty, "unknown URI policies "+predicates.keys.mkString("'","','","'"))
          
          // The resulting policy is the concatenation of all policies.
          (iri, pos) => {
            var result = iri
            for (policy <- policies) result = policy(result, pos)
            result
          }
        }
        
        // parse all URI policy lines
        val policies = new HashMap[String, Policy]()
        for (key <- config.stringPropertyNames) {
          if (key.startsWith("uri-policy")) {
            try policies(key) = parsePolicy(key)
            catch { case e: Exception => throw error("invalid URI policy: '"+key+"="+config.getProperty(key)+"'", e) }
          }
        }
        
        private val formatters = Map[String, Policy => Formatter] (
          "trix-triples" -> { new TriXFormatter(false, _) },
          "trix-quads" -> { new TriXFormatter(true, _) },
          "turtle-triples" -> { new TerseFormatter(false, true, _) },
          "turtle-quads" -> { new TerseFormatter(true, true, _) },
          "n-triples" -> { new TerseFormatter(false, false, _) },
          "n-quads" -> { new TerseFormatter(true, false, _) }
        )

        // parse all format lines
        val formats = new HashMap[String, Formatter]()
        for (key <- config.stringPropertyNames) {
          if (key.startsWith("format.")) {
            
            val suffix = key.substring("format.".length)
            
            val settings = splitValue(key, ';')
            require(settings.length == 1 || settings.length == 2, "key '"+key+"' must have one or two values separated by ';' - file format and optional uri policy")
            
            val formatter = formatters.getOrElse(settings(0), throw error("first value for key '"+key+"' is '"+settings(0)+"' but must be one of "+formatters.keys.toSeq.sorted.mkString("'","','","'")))
            
            val policy =
              if (settings.length == 1) identity
              else policies.getOrElse(settings(1), throw error("second value for key '"+key+"' is '"+settings(1)+"' but must be a configured uri-policy, i.e. one of "+policies.keys.mkString("'","','","'")))
            
            formats(suffix) = formatter.apply(policy)
          }
        }

        /** Languages */
        // TODO: add special parameters, similar to download:
        // extract=10000-:InfoboxExtractor,PageIdExtractor means all languages with at least 10000 articles
        // extract=mapped:MappingExtractor means all languages with a mapping namespace
        var languages = splitValue("languages", ',').map(Language)
        if (languages.isEmpty) languages = Namespace.mappings.keySet.toList
        languages = languages.sorted(Language.wikiCodeOrdering)

        val extractorClasses = loadExtractorClasses()
        
        private def getFile(key: String): File = {
          val value = config.getProperty(key)
          if (value == null) null else new File(value)
        }
        
        private def splitValue(key: String, sep: Char): List[String] = {
          val values = config.getProperty(key)
          if (values == null) List.empty
          else split(values, sep)
        }

        private def split(value: String, sep: Char): List[String] = {
          value.split("["+sep+"\\s]+", -1).map(_.trim).filter(_.nonEmpty).toList
        }

        /**
         * Loads the extractors classes from the configuration.
         *
         * @return A Map which contains the extractor classes for each language
         */
        private def loadExtractorClasses() : Map[Language, List[Class[_ <: Extractor]]] =
        {
            //Load extractor classes
            if(config.getProperty("extractors") == null) throw error("Property 'extractors' not defined.")
            val stdExtractors = splitValue("extractors", ',').map(loadExtractorClass)

            //Create extractor map
            var extractors = ListMap[Language, List[Class[_ <: Extractor]]]()
            for(language <- languages) extractors += ((language, stdExtractors))

            //Load language specific extractors
            val LanguageExtractor = """extractors\.(.*)""".r

            for(LanguageExtractor(code) <- config.stringPropertyNames.toArray)
            {
                val language = Language(code)
                if (extractors.contains(language))
                {
                    extractors += language -> (stdExtractors ::: splitValue("extractors."+code, ',').map(loadExtractorClass))
                }
            }

            extractors
        }

        private def loadExtractorClass(name: String): Class[_ <: Extractor] = {
          val className = if (! name.contains(".")) classOf[Extractor].getPackage.getName+'.'+name else name
          // TODO: class loader of Extractor.class is probably wrong for some users.
          classOf[Extractor].getClassLoader.loadClass(className).asSubclass(classOf[Extractor])
        }
    }


    private val parser = WikiParser()
    
    /**
     * Creates ab extraction job for a specific language.
     */
    private def createExtractionJob(lang : Language) : ExtractionJob =
    {
        val finder = new Finder[File](config.dumpDir, lang)

        val date = latestDate(finder)
        
        //Extraction Context
        val context = new DumpExtractionContext
        {
            def ontology : Ontology = _ontology
    
            def commonsSource : Source = _commonsSource
    
            def language : Language = lang
    
            private lazy val _mappingPageSource =
            {
                val namespace = Namespace.mappings(language)
                
                if (mappingsDir != null && mappingsDir.isDirectory)
                {
                    val file = new File(mappingsDir, namespace.getName(Language.Mappings).replace(' ','_')+".xml")
                    XMLSource.fromFile(file, Language.Mappings).map(parser)
                }
                else
                {
                    val namespaces = Set(namespace)
                    val url = new URL(Language.Mappings.apiUri)
                    WikiSource.fromNamespaces(namespaces,url,Language.Mappings).map(parser)
                }
            }
            
            def mappingPageSource : Traversable[PageNode] = _mappingPageSource
    
            private lazy val _mappings =
            {
                MappingsLoader.load(this)
            }
            def mappings : Mappings = _mappings
    
            private val _articlesSource =
            {
                XMLSource.fromFile(finder.file(date, "pages-articles.xml"), language,                    
                    title => title.namespace == Namespace.Main || title.namespace == Namespace.File ||
                             title.namespace == Namespace.Category || title.namespace == Namespace.Template)
            }
            
            def articlesSource : Source = _articlesSource
    
            private val _redirects =
            {
              val cache = finder.file(date, "template-redirects.obj")
              Redirects.load(articlesSource, cache, language)
            }
            
            def redirects : Redirects = _redirects
        }

        //Extractors
        val extractorClasses = config.extractorClasses(lang)
        val extractor = new RootExtractor(CompositeExtractor.load(extractorClasses, context))
        
        /**
         * Get target file path in config.dumpDir. Note that this function should be fast and not 
         * access the file system - it is called not only in this class, but later during the 
         * extraction process for each dataset.
         */
        def targetFile(suffix : String)(dataset: Dataset) =
          finder.file(date, dataset.fileName+'.'+suffix)

        var destinations = new ArrayBuffer[Destination]()
        for ((suffix, format) <- config.formats) { 
          destinations += new FileDestination(format, targetFile(suffix)) 
        }
        
        destinations += new MarkerDestination(finder.file(date, Extraction.Complete), false, true)
        
        val jobLabel = "extraction job "+lang.wikiCode+" with "+extractorClasses.size+" extractors"
        new ExtractionJob(extractor, context.articlesSource, new CompositeDestination(destinations.toSeq: _*), jobLabel)
    }

    //language-independent val
    private lazy val _ontology =
    {
        val ontologySource = if (ontologyFile != null && ontologyFile.isFile) 
        {
          XMLSource.fromFile(ontologyFile, Language.Mappings)
        } 
        else 
        {
          val namespaces = Set(Namespace.OntologyClass, Namespace.OntologyProperty)
          val url = new URL(Language.Mappings.apiUri)
          val language = Language.Mappings
          WikiSource.fromNamespaces(namespaces, url, language)
        }
      
        new OntologyReader().read(ontologySource)
    }

    //language-independent val
    private lazy val _commonsSource =
    {
      val finder = new Finder[File](config.dumpDir, Language("commons"))
      val date = latestDate(finder)
      val file = finder.file(date, "pages-articles.xml")
      XMLSource.fromFile(file, Language.Commons, _.namespace == Namespace.File)
    }
    
    private def latestDate(finder: Finder[_]): String = {
      val fileName = if (requireComplete) Download.Complete else "pages-articles.xml"
      val dates = finder.dates(fileName)
      if (dates.isEmpty) throw error("found no directory with file '"+finder.wikiName+"-[YYYYMMDD]-"+fileName+"'")
      dates.last
    }
    
    private def error(message: String, cause: Throwable = null): IllegalArgumentException = {
      new IllegalArgumentException(message, cause)
    }
    
}