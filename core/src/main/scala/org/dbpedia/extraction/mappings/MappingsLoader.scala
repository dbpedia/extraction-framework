package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.{OntologyClass, OntologyProperty}
import org.dbpedia.extraction.ontology.datatypes.Datatype

import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer

import java.util.logging.{Logger, Level}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.dataparser.StringParser
import java.lang.IllegalArgumentException
import org.dbpedia.extraction.util.Language

/**
 * Loads the mappings from the configuration and builds a MappingExtractor instance.
 * This should be replaced by a general loader later on, which loads the mapping objects based on the grammar (which can be defined using annotations)
 */
object MappingsLoader
{
    private val logger = Logger.getLogger(MappingsLoader.getClass.getName)
    
    def load(context : ExtractionContext) : (Map[String, TemplateMapping], List[TableMapping], Map[String, ConditionalMapping]) =
    {
        logger.info("Loadings mappings ("+context.language.wikiCode+")")

		val templateMappings = new HashMap[String, TemplateMapping]()
		val tableMappings = new ArrayBuffer[TableMapping]()
		val conditionalMappings = new HashMap[String, ConditionalMapping]()

        val parser = WikiParser()

		for ( page <- context.mappingsSource.map(parser);
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
            			if(!templateMappings.contains(name))
            			{
            			     val mapping = loadTemplateMapping(tnode, context)
            			     
            			     templateMappings(name) = mapping
            			}
            			else
            			{
            			    logger.log(Level.WARNING, "Duplicate template mapping for '" + name + "'")
            			}
                	}
                	case "TableMapping" =>
                	{
                	    tableMappings += new TableMapping( loadOntologyClass(tnode, "mapToClass", true, context),
                                                           loadOntologyClass(tnode, "correspondingClass", false, context),
                                                           loadOntologyProperty(tnode, "correspondingProperty", false, context),
                                                           loadTemplateProperty(tnode, "keywords"),
                                                           loadTemplateProperty(tnode, "header"),
                                                           loadPropertyMappings(tnode, "mappings", context),
                                                           context )
                	}
                	case "ConditionalMapping" =>
                	{
                	    conditionalMappings(name) = loadConditionalMapping(tnode, context)
                	}
                	case name => //Unknown mapping element
		        }
            }
            catch
            {
                case ex => ex.printStackTrace(); logger.warning("Couldn't load " + tnode.title + " on page " + page.title + ". Details: " + ex.getMessage);
            }
        }

        logger.info("Mappings loaded ("+context.language.wikiCode+")")

		(templateMappings.toMap, tableMappings.toList, conditionalMappings.toMap)
	}
    
    private def loadTemplateMapping(tnode : TemplateNode, context : ExtractionContext) =
    {
        new TemplateMapping( loadOntologyClass(tnode, "mapToClass", true, context),
                             loadOntologyClass(tnode, "correspondingClass", false, context),
                             loadOntologyProperty(tnode, "correspondingProperty", false, context),
                             loadPropertyMappings(tnode, "mappings", context),
                             context )
    }

    private def loadPropertyMappings(node : TemplateNode, propertyName : String, context : ExtractionContext) : List[PropertyMapping] =
    {
        var mappings = List[PropertyMapping]()

        for( mappingsNode <- node.property(propertyName);
             mappingNode <- mappingsNode.children if mappingNode.isInstanceOf[TemplateNode])
        {
            try
            {
                mappings ::= loadPropertyMapping(mappingNode.asInstanceOf[TemplateNode], context)
            }
            catch
            {
                case ex : Exception => logger.warning("Couldn't load property mapping on page " + node.root.title + ". Details: " + ex.getMessage)
            }
        }

	    mappings.reverse
	}

    private def loadPropertyMapping(tnode : TemplateNode, context : ExtractionContext) = tnode.title.decoded match
    {
        case "PropertyMapping" =>
        {
            new SimplePropertyMapping( loadTemplateProperty(tnode, "templateProperty"),
                                       loadOntologyProperty(tnode, "ontologyProperty", true, context),
                                       loadDatatype(tnode, "unit", false, context),
                                       loadLanguage(tnode, "language", false),
                                       loadDouble(tnode, "factor", false),
                                       context )
        }
        case "IntermediateNodeMapping" =>
        {
            new IntermediateNodeMapping( loadOntologyClass(tnode, "nodeClass", true, context),
                                         loadOntologyProperty(tnode, "correspondingProperty", true, context),
                                         loadPropertyMappings(tnode, "mappings", context),
                                         context )
        }
        case "DateIntervalMapping" =>
        {
            new DateIntervalMapping( loadTemplateProperty(tnode, "templateProperty"),
                                     loadOntologyProperty(tnode, "startDateOntologyProperty", true, context),
                                     loadOntologyProperty(tnode, "endDateOntologyProperty", true, context),
                                     context )
        }
        case "CombineDateMapping" =>
        {
            new CombineDateMapping( loadOntologyProperty(tnode, "ontologyProperty", true, context),
                                    loadTemplateProperty(tnode, "templateProperty1"),
                                    loadDatatype(tnode, "unit1", true, context),
                                    loadTemplateProperty(tnode, "templateProperty2"),
                                    loadDatatype(tnode, "unit2", true, context),
                                    loadTemplateProperty(tnode, "templateProperty3", false),
                                    loadDatatype(tnode, "unit3", false, context),
                                    context )
        }
        case "CalculateMapping" =>
        {
            new CalculateMapping( loadTemplateProperty(tnode, "templateProperty1"),
                                  loadTemplateProperty(tnode, "templateProperty2"),
                                  loadDatatype(tnode, "unit1", false, context),
                                  loadDatatype(tnode, "unit2", false, context),
                                  loadTemplateProperty(tnode, "operation"),
                                  loadOntologyProperty(tnode, "ontologyProperty", true, context),
                                  context )
        }
        case "GeocoordinatesMapping" =>
        {
            new GeoCoordinatesMapping( loadOntologyProperty(tnode, "ontologyProperty", false, context),
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
            new ConstantMapping( loadOntologyProperty(tnode, "ontologyProperty", true, context),
                                 loadTemplateProperty(tnode, "value", true),
                                 loadDatatype(tnode, "unit", false, context),
                                 context )
        }
        case title => throw new IllegalArgumentException("Unknown property type " + title + " on page " + tnode.root.title)
    }

    private def loadConditionalMapping(tnode : TemplateNode, context : ExtractionContext) =
    {
        val conditionMappings =
            for( casesProperty <- tnode.property("cases").toList;
                 conditionNode @ TemplateNode(_,_,_) <- casesProperty.children ) 
            yield loadConditionMapping(conditionNode, context)
        
        new ConditionalMapping( conditionMappings,
                                loadPropertyMappings(tnode, "defaultMappings", context),
                                context )
    }
    
    private def loadConditionMapping(tnode : TemplateNode, context : ExtractionContext) =
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
                              loadTemplateMapping(mapping, context),
                              context )
    }
	
	private def loadOntologyClass(node : TemplateNode, propertyName : String, required : Boolean, context : ExtractionContext) : OntologyClass =
	{
	    val className = loadTemplateProperty(node, propertyName, required)
	    
	    context.ontology.getClass(className) match
	    {
	        case Some(ontClass) => ontClass
            case _ =>
            {
                if(required)
                    throw new IllegalArgumentException("Ontology class " + className + " not found.")
                else
                    null
            }
	    }
	}
	
    private def loadOntologyProperty(node : TemplateNode, propertyName : String, required : Boolean, context : ExtractionContext) : OntologyProperty =
    {
        val ontPropertyName = loadTemplateProperty(node, propertyName, required)
        
        context.ontology.getProperty(ontPropertyName) match
        {
            case Some(ontProperty) => ontProperty
            case _ =>
            {
                if(required)
                    throw new IllegalArgumentException("Ontology property " + ontPropertyName + " not found.")
                else
                    null
            }
        }
    }
    
    private def loadDatatype(node : TemplateNode, propertyName : String, required : Boolean, context : ExtractionContext) : Datatype =
    {
        val datatypeName = loadTemplateProperty(node, propertyName, required)
        
        context.ontology.getDatatype(datatypeName) match
        {
            case Some(datatypeName) => datatypeName
            case _ =>
            {
                if(required)
                    throw new IllegalArgumentException("Datatype " + datatypeName + " not found.")
                else
                    null
            }
        } 
    }
	
	private def loadTemplateProperty(node : TemplateNode, propertyName : String, required : Boolean = true) : String = 
	{
        val value = node.property(propertyName).flatMap(propertyNode => StringParser.parse(propertyNode))

	    value match
	    {
	        case Some(str) => str
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
            case lang : String => Language.fromWikiCode(lang).getOrElse(throw new IllegalArgumentException("Language " + lang + " unknown"))
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
}
