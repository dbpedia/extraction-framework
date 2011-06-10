package org.dbpedia.extraction.ontology.io

import java.util.logging.{Logger}
import org.dbpedia.extraction.sources.Source
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology._
import org.dbpedia.extraction.ontology.datatypes._
import org.dbpedia.extraction.util.StringUtils._
import java.util.Locale

/**
 * Loads an ontology from configuration files using the DBpedia mapping language.
 */
class OntologyReader
{
    private val logger = Logger.getLogger(classOf[OntologyReader].getName)

    /**
     * Set of supported language codes.
     * These languages codes may be used for language annotations e.g. label@en
     * At the moment, all ISO 639-1 codes are supported.
     */
    private val supportedLanguageCodes = Locale.getISOLanguages.toSet

    /**
     *  Loads an ontology from configuration files using the DBpedia mapping language.
     *
     * @param source The source containing the ontology pages
     * @return Ontology The ontology
     */
	def read(source : Source) : Ontology =
    {
        logger.info("Loading ontology")

        val ontologyBuilder = new OntologyBuilder()

        ontologyBuilder.datatypes = OntologyDatatypes.load()

        ontologyBuilder.classes ::= new ClassBuilder("owl:Thing", Map("en" -> "Thing"), Map("en" -> "Base class of all ontology classes"), null, Set())
        ontologyBuilder.classes ::= new ClassBuilder("rdf:Property", Map("en" -> "Property"), Map(), "owl:Thing", Set())

        // TODO: range should be rdfs:Class
        ontologyBuilder.properties ::= new PropertyBuilder("rdf:type", Map("en" -> "has type"), Map(), true, false, "owl:Thing", "owl:Thing")
        ontologyBuilder.properties ::= new PropertyBuilder("rdfs:label", Map("en" -> "has label"), Map(), false, false, "owl:Thing", "xsd:string")
        ontologyBuilder.properties ::= new PropertyBuilder("rdfs:comment", Map("en" -> "has comment"), Map(), false, false, "owl:Thing", "xsd:string")

        for(page <- source.map(WikiParser()))
            load(ontologyBuilder, page)

        val ontology = ontologyBuilder.build
        logger.info("Ontology loaded")
        ontology
    }

	/**
     * Loads all classes and properties from a page.
     *
     * @param ontology The OntologyBuilder instance
     * @param pageNode The page node of the configuration page
     */
    private def load(ontologyBuilder : OntologyBuilder, page : PageNode)
    {
        for(node <- page.children if node.isInstanceOf[TemplateNode])
        {
            val templateNode = node.asInstanceOf[TemplateNode]
            val templateName = templateNode.title.encoded

            if(templateName == OntologyReader.CLASSTEMPLATE_NAME)
            {
                val name = OntologyReader.getClassName(page.title)

                ontologyBuilder.classes ::= loadClass(name, templateNode)

                for(specificProperty <- loadSpecificProperties(name, templateNode))
                {
                    ontologyBuilder.specializedProperties ::= specificProperty
                }
            }
            else if(templateName == OntologyReader.OBJECTPROPERTY_NAME || templateName == OntologyReader.DATATYPEPROPERTY_NAME)
            {
                val name = OntologyReader.getPropertyName(page.title)

                for(property <- loadOntologyProperty(name, templateNode))
                {
                    ontologyBuilder.properties ::= property
                }
            }
        }
    }

    private def loadClass(name : String, node : TemplateNode) : ClassBuilder =
    {
        new ClassBuilder(name = name,
                         labels = readTemplatePropertiesByLanguage(node, "rdfs:label"),
                         comments = readTemplatePropertiesByLanguage(node, "rdfs:comment"),
                         superClassName = readTemplateProperty(node, "rdfs:subClassOf").getOrElse("owl:Thing"),
                         equivalentClassNames = readTemplatePropertyAsList(node, "owl:equivalentClass").toSet)
    }

    private def loadOntologyProperty(name : String, node : TemplateNode) : Option[PropertyBuilder] =
    {
        val isObjectProperty = node.title.encoded == OntologyReader.OBJECTPROPERTY_NAME

        val labels = readTemplatePropertiesByLanguage(node, "rdfs:label")
        val comments = readTemplatePropertiesByLanguage(node, "rdfs:comment")

        //Type
        val isFunctional = readTemplateProperty(node, "rdf:type") match
        {
            case Some(text) if text == "owl:FunctionalProperty" => true
            case Some(text) =>
            {
                logger.warning(node.root.title + " - Found property with an invalid type")
                false
            }
            case None => false
        }

        //Domain
        val domain = readTemplateProperty(node, "rdfs:domain") match
        {
            case Some(domainClassName) => domainClassName
            case None => "owl:Thing"
        }

        //Range
        val range = readTemplateProperty(node, "rdfs:range") match
        {
            case Some(rangeClassName) => rangeClassName
            case None =>
            {
                if(isObjectProperty)
                {
                    "owl:Thing"
                }
                else
                {
                    logger.warning(node.root.title + " - Cannot load datatype property " + name + " because it does not define its range")
                    return None
                }
            }
        }

        Some(new PropertyBuilder(name, labels, comments, isObjectProperty, isFunctional, domain, range))
    }

    private def loadSpecificProperties(name : String, node : TemplateNode) : List[SpecificPropertyBuilder] =
    {
        for(PropertyNode(_, children, _) <- node.property("specificProperties").toList;
            templateNode @ TemplateNode(title, _, _) <- children if title.decoded == OntologyReader.SPECIFICPROPERTY_NAME;
            specificProperty <- loadSpecificProperty(name, templateNode))
            yield specificProperty
    }

    private def loadSpecificProperty(className : String, node : TemplateNode) : Option[SpecificPropertyBuilder] =
    {
        val property = readTemplateProperty(node, "ontologyProperty") match
        {
            case Some(text) => text
            case None =>
            {
                logger.warning(node.root.title + " - SpecificProperty on " + className +" does not define a base property")
                return None
            }
        }

        val unit = readTemplateProperty(node, "unit") match
        {
            case Some(text) => text
            case None =>
            {
                logger.warning(node.root.title + " - SpecificProperty on " + className +" does not define a unit")
                return None
            }
        }

        new Some(new SpecificPropertyBuilder(className, property, unit))
    }

	private def readTemplateProperty(node : TemplateNode, propertyName : String) : Option[String] =
	{
	    node.property(propertyName) match
	    {
	        case Some(PropertyNode(_, TextNode(text, _) :: Nil, _)) if !text.trim.isEmpty => Some(text.trim)
	        case _ => None
	    }
	}

    private def readTemplatePropertyAsList(node : TemplateNode, propertyName : String) : List[String] =
    {
        for(text <- readTemplateProperty(node, propertyName).toList;
            elem <- text.split(","))
            yield elem.trim
    }

    private def readTemplatePropertiesByLanguage(node : TemplateNode, propertyName : String) : Map[String, String]=
    {
        node.children.filter(_.key.startsWith(propertyName)).flatMap
        { property =>

            val languageCode = property.key.split("@", 2).lift(1).getOrElse("en")
            if(!supportedLanguageCodes.contains(languageCode))
            {
                logger.warning(node.root.title + " - Language code '" + languageCode + "' is not supported. Ignoring corresponding " + propertyName)
                None
            }
            else
            {
                property.retrieveText match
                {
                    case Some(text) if !text.trim.isEmpty => Some(languageCode -> text.trim)
                    case _ => None
                }
            }
        }.toMap
    }

    private class OntologyBuilder
    {
        var classes = List[ClassBuilder]()
        var properties = List[PropertyBuilder]()
        var datatypes = List[Datatype]()
        var specializedProperties = List[SpecificPropertyBuilder]()

        def build() : Ontology  =
        {
            val classMap = classes.map( clazz => (clazz.name, clazz) ).toMap
            val propertyMap = properties.map( property => (property.name, property) ).toMap
            val typeMap = datatypes.map( datatype => (datatype.name, datatype) ).toMap

            new Ontology( classes.flatMap(_.build(classMap)),
                          properties.flatMap(_.build(classMap, typeMap)),
                          datatypes,
                          specializedProperties.flatMap(_.build(classMap, propertyMap, typeMap)).toMap )
        }
    }

    private class ClassBuilder(val name : String, val labels : Map[String, String], val comments : Map[String, String],
                               val superClassName : String, val equivalentClassNames : Set[String])
    {
        require(name != null, "name != null")
        require(labels != null, "labels != null")
        require(comments != null, "comments != null")
        require(name == "owl:Thing" || superClassName != null, "superClassName != null")
        require(equivalentClassNames != null, "equivalentClassNames != null")

        /** Caches the class, which has been build by this builder. */
        var generatedClass : Option[OntologyClass] = None

        /** Remembers if build has already been called on this object */
        private var buildCalled = false

        def build(classMap : Map[String, ClassBuilder]) : Option[OntologyClass] =
        {
            if(!buildCalled)
            {
                 //TODO check for cycles to avoid infinite recursion

                val superClass =
                    if(name == "owl:Thing")
                    {
                        None
                    }
                    else
                    {
                        classMap.get(superClassName) match
                        {
                            case Some(superClassBuilder) => superClassBuilder.build(classMap)
                            case None =>
                            {
                                logger.warning("Super class of " + name + " (" + superClassName + ") does not exist")
                                None
                            }
                        }
                    }

                val equivalentClasses = for(equivalentClassName <- equivalentClassNames) yield classMap.get(equivalentClassName) match
                {
                    case Some(equivalentClassBuilder) => equivalentClassBuilder.build(classMap)
                    case None if OntologyNamespaces.nonValidatedNamespaces.exists(equivalentClassName.startsWith(_)) =>
                    {
                        logger.config("Equivalent class " + equivalentClassName + " of class " + name + " was not found but its namespace is an exception")
                        None
                    }
                    case None =>
                    {
                        logger.warning("Equivalent class of " + name + " (" + equivalentClassName + ") does not exist")
                        None
                    }
                }

                generatedClass = superClass match
                {
                    case Some(superClass) => Some(new OntologyClass(name, labels, comments, superClass, equivalentClasses.flatten))
                    case None if name == "owl:Thing" => Some(new OntologyClass(name, labels, comments, null, equivalentClasses.flatten))
                    case None => None
                }

                buildCalled = true
            }

            generatedClass
        }
    }

    private class PropertyBuilder(val name : String, val labels : Map[String, String], val comments : Map[String, String],
                                  val isObjectProperty : Boolean, val isFunctional : Boolean, val domain : String, val range : String )
    {
        require(name != null, "name != null")
        require(labels != null, "labels != null")
        require(comments != null, "comments != null")
        require(domain != null, "domain != null")
        require(range != null, "range != null")

        /** Caches the property, which has been build by this builder. */
        var generatedProperty : Option[OntologyProperty] = None

        def build(classMap : Map[String, ClassBuilder], typeMap : Map[String, Datatype]) : Option[OntologyProperty] =
        {
            val domainClass = classMap.get(domain) match
            {
                case Some(domainClassBuilder) => domainClassBuilder.generatedClass match
                {
                    case Some(domainClass) => domainClass
                    case None => logger.warning("Domain of property " + name + " (" + domain + ") couldn't be loaded"); return None
                }
                case None => logger.warning("Domain of property " + name + " (" + domain + ") does not exist"); return None
            }

            if(isObjectProperty)
            {
                val rangeClass = classMap.get(range) match
                {
                    case Some(rangeClassBuilder) => rangeClassBuilder.generatedClass match
                    {
                        case Some(rangeClass) => rangeClass
                        case None => logger.warning("Range of property '" + name + "' (" + range + ") couldn't be loaded"); return None
                    }
                    case None => logger.warning("Range of property '" + name + "' (" + range + ") does not exist"); return None
                }

                generatedProperty = Some(new OntologyObjectProperty(name, labels, comments, domainClass, rangeClass, isFunctional))
            }
            else
            {
                val rangeType = typeMap.get(range) match
                {
                    case Some(datatype) => datatype
                    case None => logger.warning("Range of property '" + name + "' (" + range + ") does not exist"); return None
                }

                generatedProperty =  Some(new OntologyDatatypeProperty(name, labels, comments, domainClass, rangeType, isFunctional))
            }

            generatedProperty
        }
    }

    private class SpecificPropertyBuilder(val className : String, val propertyName : String, val datatypeName : String)
    {
        require(className != null, "className != null")
        require(propertyName != null, "propertyName != null")
        require(datatypeName != null, "datatypeName != null")

        def build( classMap : Map[String, ClassBuilder],
                   propertyMap : Map[String, PropertyBuilder],
                   typeMap : Map[String, Datatype] ) : Option[((OntologyClass, OntologyProperty), UnitDatatype)] =
        {
            //Load the domain class of the property
            val domainClass = classMap.get(className) match
            {
                case Some(domainClassBuilder) => domainClassBuilder.generatedClass match
                {
                    case Some(domainClass) => domainClass
                    case None => logger.warning("Cannot specialize property on class '" + className + "', since the class failed to load"); return None
                }
                case None => logger.warning("Cannot specialize property on class '" + className + "', since the class has not been found"); return None
            }

            //Load the base property
            val baseProperty = propertyMap.get(propertyName) match
            {
                case Some(propertyBuilder) => propertyBuilder.generatedProperty match
                {
                    case Some(property) => property
                    case None => logger.warning("Cannot specialize property '" + propertyName + "' on class '" + className + "', since the property failed to load"); return None
                }
                case None => logger.warning("Cannot specialize property '" + propertyName + "' on class '" + className + "', since the property has not been found"); return None
            }

            //Load the specialized range of the property
            val specializedRange = typeMap.get(datatypeName) match
            {
                case Some(datatype) => datatype
                case None => logger.warning("Cannot specialize property " + propertyName + " on class " + className + ", " +
                        "since the range '" + datatypeName + "' has not been found"); return None
            }

            //Check if the range of the base property is a dimension
            if(!baseProperty.range.isInstanceOf[DimensionDatatype])
            {
                logger.warning("Cannot specialize property " + propertyName + " on class " + className + ", " +
                        "since the range of the base property '" + baseProperty.range + "' is not a dimension")
                return None
            }

            //Check if the range of the specialized property is a unit
            if(!specializedRange.isInstanceOf[UnitDatatype])
            {
                logger.warning("Cannot specialize property " + propertyName + " on class " + className + ", " +
                        "since the range '" + specializedRange + "' is not a unit")
                return None
            }

            //Check if the range of the specialized property is in the dimension of the base property range
            if(specializedRange.asInstanceOf[UnitDatatype].dimension != baseProperty.range)
            {
                logger.warning("Cannot specialize property " + propertyName + " on class " + className + ", " +
                        "since the range of the base property has another dimension")
                return None
            }

            Some((domainClass, baseProperty), specializedRange.asInstanceOf[UnitDatatype])
        }
    }
}

private object OntologyReader
{
    val CLASSTEMPLATE_NAME = "Class"
    val OBJECTPROPERTY_NAME = "ObjectProperty"
    val DATATYPEPROPERTY_NAME = "DatatypeProperty"
    val SPECIFICPROPERTY_NAME = "SpecificProperty"

    /**
     * Generates the name of an ontology class based on the article title e.g. 'Foaf/Person' becomes 'foaf:Person'.
     *
     * @param name page title
     * @return class name
     * @throws IllegalArgumentException
     */
    private def getClassName(title : WikiTitle) : String = title.encoded.split("/|:", 2) match
    {
        case Array(name) => name.capitalizeLocale(title.language.locale)
        case Array(namespace, name) => namespace.toLowerCase(title.language.locale) + ":" + name.capitalizeLocale(title.language.locale)
        case _ => throw new IllegalArgumentException("Invalid name: " + title)
    }

    /**
     * Generates the name of an ontology property based on the article title Foaf/name' becomes 'foaf:name'.
     *
     * @param name page title
     * @return property name
     * @throws IllegalArgumentException
     */
    private def getPropertyName(title : WikiTitle) : String = title.encoded.split("/|:", 2) match
    {
        case Array(name) => name.uncapitalize(title.language.locale)
        case Array(namespace, name) => namespace.toLowerCase(title.language.locale) + ":" + name.uncapitalize(title.language.locale)
        case _ => throw new IllegalArgumentException("Invalid name: " + title)
    }
}
