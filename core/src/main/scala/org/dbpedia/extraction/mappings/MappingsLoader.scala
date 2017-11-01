package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.datatypes.Datatype

import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer
import java.util.logging.{Level, LogRecord, Logger}

import org.dbpedia.extraction.config.ExtractionRecorder
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.dataparser.StringParser
import org.dbpedia.extraction.ontology.{Ontology, OntologyClass, OntologyProperty}
import org.dbpedia.extraction.util.Language

import scala.language.reflectiveCalls
import scala.reflect.ClassTag

/**
 * Loads the mappings from the configuration and builds a MappingExtractor instance.
 * This should be replaced by a general loader later on, which loads the mapping objects based on the grammar (which can be defined using annotations)
 */
object MappingsLoader
{
    // Note: we pass the encoded page title as log parameter so we can render a link on the server
    private val logger = Logger.getLogger(MappingsLoader.getClass.getName)
    
    def load( context : {
                 def ontology : Ontology
                 def language : Language
                 def redirects : Redirects
                 def mappingPageSource : Traversable[WikiPage]
                 def recorder[T: ClassTag] : ExtractionRecorder[T]
    } ) : Mappings =
    {
        logger.info("Loading mappings ("+context.language.wikiCode+")")

        val classMappings = new HashMap[String, Extractor[TemplateNode]]()
        val tableMappings = new ArrayBuffer[TableMapping]()
        val parser = WikiParser.getInstance()

        for (page <- context.mappingPageSource.flatMap(parser.apply(_, context.redirects));
             node <- page.children if node.isInstanceOf[TemplateNode] )
        {
            val tnode = node.asInstanceOf[TemplateNode]
            val name = page.title.decoded

            try
            {
                tnode.title.decoded match
                {
                    case "TemplateMapping" =>
                    {
                        if(! classMappings.contains(name))
                        {
                             classMappings(name) = loadTemplateMapping(tnode, name, context)
                        }
                        else
                        {
                            logger.log(Level.WARNING, "Duplicate template mapping for '" + name + "' on page " + page.title.decodedWithNamespace + ".", page.title.encodedWithNamespace)
                        }
                    }
                    case "TableMapping" =>
                    {
                        tableMappings += new TableMapping( loadOntologyClass(tnode, "mapToClass", true, context.ontology),
                                                           loadOntologyClass(tnode, "correspondingClass", false, context.ontology),
                                                           loadOntologyProperty(tnode, "correspondingProperty", false, context.ontology),
                                                           loadTemplateProperty(tnode, "keywords"),
                                                           loadTemplateProperty(tnode, "header"),
                                                           loadPropertyMappings(tnode, "mappings", name, context),
                                                           context )
                    }
                    case "ConditionalMapping" =>
                    {
                        if(! classMappings.contains(name))
                        {
                             classMappings(name) = loadConditionalMapping(tnode, name, context)
                        }
                        else
                        {
                            logger.log(Level.WARNING, "Duplicate template mapping for '" + name + "' on page " + page.title.decodedWithNamespace + ".", page.title.encodedWithNamespace)
                        }
                    }
                    case _ => throw new IllegalArgumentException("Unknown mapping element "+tnode.title.decoded)
                }
            }
            catch
            {
                case ex : Throwable => log(Level.WARNING, "Couldn't load " + tnode.title.decoded + " on page " + page.title.decodedWithNamespace + ". Details: " + ex.getMessage, Array(page.title.encodedWithNamespace), ex)
            }
        }

        logger.info("Mappings loaded ("+context.language.wikiCode+")")

        new Mappings(classMappings.toMap, tableMappings.toList)
    }

    private def loadTemplateMapping(tnode : TemplateNode, topLevelTemplate: String, context : {
          def ontology : Ontology
          def redirects : Redirects
          def language : Language
          def recorder[T: ClassTag] : ExtractionRecorder[T]
    } ) =
    {
        new TemplateMapping( loadOntologyClass(tnode, "mapToClass", true, context.ontology),
                             loadOntologyClass(tnode, "correspondingClass", false, context.ontology),
                             loadOntologyProperty(tnode, "correspondingProperty", false, context.ontology),
                             loadPropertyMappings(tnode, "mappings", topLevelTemplate, context),
                             context )
    }

    private def loadPropertyMappings(node : TemplateNode, propertyName : String, topLevelTemplate: String, context : {
                  def ontology : Ontology
                  def redirects : Redirects
                  def language : Language
                  def recorder[T: ClassTag] : ExtractionRecorder[T]
    } ) : List[PropertyMapping] =
    {
        var mappings = List[PropertyMapping]()

        for( mappingsNode <- node.property(propertyName);
             mappingNode <- mappingsNode.children if mappingNode.isInstanceOf[TemplateNode])
        {
            try
            {
                mappings ::= loadPropertyMapping(mappingNode.asInstanceOf[TemplateNode], topLevelTemplate, context)
            }
            catch
            {
                case ex : Exception => log(Level.WARNING, "Couldn't load property mapping on page " + node.root.title.decodedWithNamespace + ". Details: " + ex.getMessage, Array(node.root.title.encodedWithNamespace), ex)
            }
        }

        mappings.reverse
    }

    private def loadPropertyMapping(tnode : TemplateNode, topLevelTemplate: String, context : {
                                      def ontology : Ontology
                                      def redirects : Redirects
                                      def language : Language
                                      def recorder[T: ClassTag] : ExtractionRecorder[T]}
                                   ) =
      tnode.title.decoded match
    {
        case "PropertyMapping" =>
        {
            new SimplePropertyMapping( topLevelTemplate,
                                       loadTemplateProperty(tnode, "templateProperty"),
                                       loadOntologyProperty(tnode, "ontologyProperty", true, context.ontology),
                                       loadTemplateProperty(tnode, "select", false),
                                       loadTemplateProperty(tnode, "prefix", false),
                                       loadTemplateProperty(tnode, "suffix", false),
                                       loadTemplateProperty(tnode, "transform", false),
                                       loadDatatype(tnode, "unit", false, context.ontology),
                                       loadLanguage(tnode, "language", false),
                                       loadDouble(tnode, "factor", false),
                                       context )
        }
        case "IntermediateNodeMapping" =>
        {
            new IntermediateNodeMapping( loadOntologyClass(tnode, "nodeClass", true, context.ontology),
                                         loadOntologyProperty(tnode, "correspondingProperty", true, context.ontology),
                                         loadPropertyMappings(tnode, "mappings", topLevelTemplate, context),
                                         context )
        }
        case "DateIntervalMapping" =>
        {
            new DateIntervalMapping( loadTemplateProperty(tnode, "templateProperty"),
                                     loadOntologyProperty(tnode, "startDateOntologyProperty", true, context.ontology),
                                     loadOntologyProperty(tnode, "endDateOntologyProperty", true, context.ontology),
                                     context )
        }
        case "CombineDateMapping" =>
        {
					// TODO: change the syntax on the mappings wiki to allow an arbitrary number of template properties.
          val templateProperties = new HashMap[String, Datatype]()
          val templateProperty1 = loadTemplateProperty(tnode, "templateProperty1", true)
          templateProperties(templateProperty1) = loadDatatype(tnode, "unit1", true, context.ontology)
          val templateProperty2 = loadTemplateProperty(tnode, "templateProperty2", true)
          templateProperties(templateProperty2) = loadDatatype(tnode, "unit2", true, context.ontology)
          val templateProperty3 = loadTemplateProperty(tnode, "templateProperty3", false)
          if (templateProperty3 != null) templateProperties(templateProperty3) = loadDatatype(tnode, "unit3", true, context.ontology)
          new CombineDateMapping(loadOntologyProperty(tnode, "ontologyProperty", true, context.ontology), templateProperties, context)
        }
        case "CalculateMapping" =>
        {
            new CalculateMapping( loadTemplateProperty(tnode, "templateProperty1"),
                                  loadTemplateProperty(tnode, "templateProperty2"),
                                  loadDatatype(tnode, "unit1", false, context.ontology),
                                  loadDatatype(tnode, "unit2", false, context.ontology),
                                  loadTemplateProperty(tnode, "operation"),
                                  loadOntologyProperty(tnode, "ontologyProperty", true, context.ontology),
                                  context )
        }
        case "GeocoordinatesMapping" =>
        {
            new GeoCoordinatesMapping( loadOntologyProperty(tnode, "ontologyProperty", false, context.ontology),
                                       loadTemplateProperty(tnode, "coordinates", false),
                                       loadTemplateProperty(tnode, "latitude", false),
                                       loadTemplateProperty(tnode, "longitude", false),
                                       loadTemplateProperty(tnode, "longitudeDegrees", false),
                                       loadTemplateProperty(tnode, "longitudeMinutes", false),
                                       loadTemplateProperty(tnode, "longitudeSeconds", false),
                                       loadTemplateProperty(tnode, "longitudeDirection", false),
                                       loadTemplateProperty(tnode, "latitudeDegrees", false),
                                       loadTemplateProperty(tnode, "latitudeMinutes", false),
                                       loadTemplateProperty(tnode, "latitudeSeconds", false),
                                       loadTemplateProperty(tnode, "latitudeDirection", false),
                                       context )
        }
        case "ConstantMapping" =>
        {
            new ConstantMapping( loadOntologyProperty(tnode, "ontologyProperty", true, context.ontology),
                                 loadTemplateProperty(tnode, "value", true),
                                 loadDatatype(tnode, "unit", false, context.ontology),
                                 context )
        }
        case "CombineSimpleMapping" =>
        {
          new CombineSimpleMapping( loadOntologyProperty(tnode, "ontologyProperty", true, context.ontology),
            tnode.keySet.filter(x => x.startsWith("templateProperty")).map(x => loadTemplateProperty(tnode, x, true)),
            Option(loadTemplateProperty(tnode, "delineator", false)).getOrElse(""),
            context )
        }
        case title => throw new IllegalArgumentException("Unknown property type " + title + " on page " + tnode.root.title)
    }
    
    private def loadConditionalMapping(tnode : TemplateNode, topLevelTemplate: String,  context : {
         def ontology : Ontology
         def redirects : Redirects
         def language : Language
          def recorder[T: ClassTag] : ExtractionRecorder[T]
    } ) =
    {
        val conditionMappings =
            for( casesProperty <- tnode.property("cases").toList;
                 conditionNode @ TemplateNode(_,_,_,_) <- casesProperty.children )
            yield loadConditionMapping(conditionNode, topLevelTemplate, context)
        
        new ConditionalMapping(conditionMappings, loadPropertyMappings(tnode, "defaultMappings", topLevelTemplate, context))
    }
    
    private def loadConditionMapping(tnode : TemplateNode, topLevelTemplate: String, context : {
      def ontology : Ontology
      def redirects : Redirects
      def language : Language
      def recorder[T: ClassTag] : ExtractionRecorder[T]
    } ) =
    {
        //Search for the template mapping in the first template node of the mapping property
        val mapping = tnode.property("mapping").flatMap(mappingNode =>
                      mappingNode.children.filter(childNode =>
                      childNode.isInstanceOf[TemplateNode]).headOption)
                      .getOrElse(throw new IllegalArgumentException("Condition does not define a mapping"))
                      .asInstanceOf[TemplateNode]

        new ConditionMapping( loadTemplateProperty(tnode, "templateProperty", false),
                              loadTemplateProperty(tnode, "operator", false),
                              loadTemplateProperty(tnode, "value", false),
                              // TODO: allow nested ConditionalMapping - don't use loadTemplateMapping() 
                              // here, move the switch block in load() to its own method and use that
                              loadTemplateMapping(mapping, topLevelTemplate, context))
    }
    
    private def loadOntologyClass(node : TemplateNode, propertyName : String, required : Boolean, ontology : Ontology) : OntologyClass = {
      loadOntologyValue(node, propertyName, ontology.classes, required, "Ontology class not found: ")
    }
    
  private def loadOntologyProperty(node : TemplateNode, propertyName : String, required : Boolean, ontology : Ontology) : OntologyProperty = {
    loadOntologyValue(node, propertyName, ontology.properties, required, "Ontology property not found: ")
  }
  
  private def loadDatatype(node : TemplateNode, propertyName : String, required : Boolean, ontology : Ontology) : Datatype = {
    loadOntologyValue(node, propertyName, ontology.datatypes, required, "Datatype not found: ")
  }
  
  private def loadOntologyValue[T](node : TemplateNode, propertyName : String, map: Map[String, T], required: Boolean, msg: String) : T = {
    val name = loadTemplateProperty(node, propertyName, required)
    map.getOrElse(name, if (! required) null.asInstanceOf[T] else throw new IllegalArgumentException(msg+name))
  }
      
    
    private def loadTemplateProperty(node : TemplateNode, propertyName : String, required : Boolean = true) : String = 
    {
        val value = node.property(propertyName).flatMap(propertyNode => StringParser.parseWithProvenance(propertyNode))

        value match
        {
            case Some(str) => str.value
            case None =>
            {
                if(required)
                    throw new IllegalArgumentException("Missing property '" + propertyName + "' in template '" + node.title.decoded + "' starting on line " + node.line + ".")
                else
                    null
            }
        }
    }

    private def loadLanguage(node : TemplateNode, propertyName : String, required : Boolean) : Language =
    {
        loadTemplateProperty(node, propertyName, required) match
        {
            case lang : String => Language(lang)
            case null => null
        }
    }

    private def loadDouble(node : TemplateNode, propertyName : String, required : Boolean) : Double =
    {
        try
        {
            loadTemplateProperty(node, propertyName, required) match
            {
                case factor : String => factor.toDouble
                case null => 1
            }
        }
        catch
        {
            case e : NumberFormatException => throw new IllegalArgumentException("Invalid value for " + propertyName + ". Must be double.")
        }
    }
    
    /**
     * java.util.logging is so stupid. It doesn't even have methods that allow passing params AND
     * exceptions. We have to write our own method. Copy & paste from several Logger.log() methods
     * PLUS the private Logger.doLog() method that sets the logger name. To really emulate the
     * behavior of Logger.log(), we would also have to copy the handling of the resource bundle
     * from  doLog()... luckily, we don't care about internationalization.
     */
    private def log(level: Level, msg: String, params: Array[Object], thrown: Throwable): Unit =
    {
      if (! logger.isLoggable(level)) return
      val lr = new LogRecord(level, msg)
      lr.setLoggerName(logger.getName())
      lr.setParameters(params)
      lr.setThrown(thrown)
      logger.log(lr)
    }
}
