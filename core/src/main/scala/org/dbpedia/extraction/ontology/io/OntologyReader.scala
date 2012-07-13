package org.dbpedia.extraction.ontology.io

import java.util.logging.Logger
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology._
import org.dbpedia.extraction.ontology.datatypes._
import org.dbpedia.extraction.util.RichString.wrapString
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.sources.Source

/**
 * Loads an ontology from configuration files using the DBpedia mapping language.
 */
class OntologyReader
{
    private val logger = Logger.getLogger(classOf[OntologyReader].getName)

    def read(source : Source) : Ontology =
    {
        logger.info("Loading ontology pages")

        read(source.map(WikiParser()))
    }

    /**
     *  Loads an ontology from configuration files using the DBpedia mapping language.
     *
     * @param source The source containing the ontology pages
     * @return Ontology The ontology
     */
    def read(pageNodeSource : Traversable[PageNode]) : Ontology =
    {
        logger.info("Loading ontology")

        val ontologyBuilder = new OntologyBuilder()

        ontologyBuilder.datatypes = OntologyDatatypes.load()

        ontologyBuilder.classes ::= new ClassBuilder("owl:Thing", Map(Language.Mappings -> "Thing"), Map(Language.Mappings -> "Base class of all ontology classes"), List(), Set())
        ontologyBuilder.classes ::= new ClassBuilder("rdf:Property", Map(Language.Mappings -> "Property"), Map(), List("owl:Thing"), Set())

        // TODO: range should be rdfs:Class
        ontologyBuilder.properties ::= new PropertyBuilder("rdf:type", Map(Language.Mappings -> "has type"), Map(), true, false, "owl:Thing", "owl:Thing", Set())
        ontologyBuilder.properties ::= new PropertyBuilder("rdfs:label", Map(Language.Mappings -> "has label"), Map(), false, false, "owl:Thing", "xsd:string", Set())
        ontologyBuilder.properties ::= new PropertyBuilder("rdfs:comment", Map(Language.Mappings -> "has comment"), Map(), false, false, "owl:Thing", "xsd:string", Set())

        for(page <- pageNodeSource)
        {
            load(ontologyBuilder, page)
        }

        val ontology = ontologyBuilder.build()
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
                val name = getName(page.title, _.capitalize(page.title.language.locale))

                ontologyBuilder.classes ::= loadClass(name, templateNode)

                for(specificProperty <- loadSpecificProperties(name, templateNode))
                {
                    ontologyBuilder.specializedProperties ::= specificProperty
                }
            }
            else if(templateName == OntologyReader.OBJECTPROPERTY_NAME || templateName == OntologyReader.DATATYPEPROPERTY_NAME)
            {
                val name = getName(page.title, _.uncapitalize(page.title.language.locale))

                for(property <- loadOntologyProperty(name, templateNode))
                {
                    ontologyBuilder.properties ::= property
                }
            }
            // TODO: read datatypes
        }
    }

    /**
     * Generates the name of an ontology class or property based on the article title.
     * A namespace like "Foaf:" is always converted to lowercase. The local name (the part
     * after the namespace prefix) is cleaned using the given function.
     *
     * @param name page title
     * @param clean used to process the local name
     * @return clean name, including lower-case namespace and clean local name
     * @throws IllegalArgumentException
     */
    private def getName(title : WikiTitle, clean: String => String): String = title.encoded.split("/|:", 2) match
    {
        case Array(name) => clean(name)
        case Array(namespace, name) => namespace.toLowerCase(title.language.locale) + ":" + clean(name)
        case _ => throw new IllegalArgumentException("Invalid name: " + title)
    }
    
    private def loadClass(name : String, node : TemplateNode) : ClassBuilder =
    {
        new ClassBuilder(name = name,
                         labels = readTemplatePropertiesByLanguage(node, "rdfs:label") ++ readPropertyTemplatesByLanguage(node, "label"),
                         comments = readTemplatePropertiesByLanguage(node, "rdfs:comment") ++ readPropertyTemplatesByLanguage(node, "comment"),
                         baseClassNames = readTemplatePropertyAsList(node, "rdfs:subClassOf"),
                         equivClassNames = readTemplatePropertyAsList(node, "owl:equivalentClass").toSet)
    }

    private def loadOntologyProperty(name : String, node : TemplateNode) : Option[PropertyBuilder] =
    {
        val isObjectProperty = node.title.encoded == OntologyReader.OBJECTPROPERTY_NAME

        val labels = readTemplatePropertiesByLanguage(node, "rdfs:label") ++ readPropertyTemplatesByLanguage(node, "label")
        val comments = readTemplatePropertiesByLanguage(node, "rdfs:comment") ++ readPropertyTemplatesByLanguage(node, "comment")

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

        //Equivalent Properties
        val equivProperties = readTemplatePropertyAsList(node, "owl:equivalentProperty").toSet

        Some(new PropertyBuilder(name, labels, comments, isObjectProperty, isFunctional, domain, range, equivProperties))
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

    private def readTemplatePropertiesByLanguage(node: TemplateNode, propertyName: String) : Map[Language, String]=
    {
      val prefix = propertyName+'@'
      node.children.filter(_.key.startsWith(prefix)).flatMap
      { property =>
        property.key.split("@", 2) match
        {
          case Array(prefix, langCode) => Language.get(langCode) match
          {
            case Some(language) => property.retrieveText match
            {
              case Some(text) if ! text.trim.isEmpty => Some(language -> text.trim)
              case _ => None
            }
            case _ =>
            {
              logger.warning(node.root.title + " - Language code '" + langCode + "' is not supported. Ignoring corresponding " + propertyName)
              None
            }
          }
          case _ =>
          {
            logger.warning(node.root.title + " - Property '" + property.key + "' could not be parsed. Ignoring corresponding " + propertyName)
            None
          }
        }
      }.toMap
    }
    
    /**
     * TODO: this seems to work, but I find it unreadable and ugly. Maybe more procedural, 
     * less Scala-ish code would be easier to read and actually simpler?
     */
    private def readPropertyTemplatesByLanguage(node: TemplateNode, templateName: String) : Map[Language, String] =
    {
      val propertyName = templateName + 's' // label -> labels
      node.children.filter(_.key.equals(propertyName)).flatMap { property =>
        for (child <- property.children) yield child match {
          case template @ TemplateNode(title, _, _) if title.decoded equalsIgnoreCase templateName => {
            readPropertyTemplate(template)
          }
          case TextNode(text, _) if text.trim.isEmpty => {
            None // ignore space between templates
          }
          case _ => {
            logger.warning(node.root.title+" - Ignoring invalid node '"+child.toWikiText+"' in value of property '"+propertyName+"'.")
            None
          }
        }
      }.flatten.toMap
    }

    /**
     * TODO: this seems to work, but I find it unreadable and ugly. Maybe more procedural, 
     * less Scala-ish code would be easier to read and actually simpler?
     */
    private def readPropertyTemplate(template: TemplateNode) : Option[(Language, String)] = {
      template.children match {
        case List(lang, text) if lang.key == "1" && text.key == "2" => lang.retrieveText match {
          case Some(langCode) if ! langCode.trim.isEmpty => Language.get(langCode.trim) match {
            case Some(language) => { 
              val content = text.children.map(_.toPlainText).mkString.trim
              if (content.nonEmpty) return Some(language -> content)
              // no text content
            }
            case _ => // bad language
          }
          case _ => // no language
        }
        case _ => // bad children
      }
      logger.warning(template.root.title+" - Ignoring invalid template "+template.toWikiText)
      None
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

            new Ontology( classes.flatMap(_.build(classMap)).map(c => (c.name, c)).toMap,
                          properties.flatMap(_.build(classMap, typeMap)).map(p => (p.name, p)).toMap,
                          datatypes.map(t => (t.name, t)).toMap,
                          specializedProperties.flatMap(_.build(classMap, propertyMap, typeMap)).toMap )
        }
    }

    private class ClassBuilder(val name : String, val labels : Map[Language, String], val comments : Map[Language, String],
                               var baseClassNames : List[String], val equivClassNames : Set[String])
    {
        require(name != null, "name != null")
        require(labels != null, "labels != null")
        require(comments != null, "comments != null")
        if (name != "owl:Thing" && baseClassNames.isEmpty) baseClassNames = List("owl:Thing")
        require(equivClassNames != null, "equivClassNames != null")

        /** Caches the class, which has been build by this builder. */
        var generatedClass : Option[OntologyClass] = None

        /** Remembers if build has already been called on this object */
        private var buildCalled = false

        def build(classMap : Map[String, ClassBuilder]) : Option[OntologyClass] =
        {
            if(!buildCalled)
            {
                 //TODO check for cycles to avoid infinite recursion
                val baseClasses = baseClassNames.map{ baseClassName => classMap.get(baseClassName) match
                {
                    case Some(baseClassBuilder) => baseClassBuilder.build(classMap)
                    case None if ! RdfNamespace.validate(baseClassName) =>
                    {
                        logger.config("base class '"+baseClassName+"' of class '"+name+"' was not found, but for its namespace this was expected")
                        Some(new OntologyClass(baseClassName, Map(), Map(), List(), Set()))
                    }
                    case None =>
                    {
                        logger.warning("base class '"+baseClassName+"' of class '"+name+"' not found")
                        None
                    }
                }}.flatten

                val equivClasses = equivClassNames.map{ equivClassName => classMap.get(equivClassName) match
                {
                    case Some(equivClassBuilder) => equivClassBuilder.build(classMap)
                    case None if ! RdfNamespace.validate(equivClassName) =>
                    {
                        logger.config("equivalent class '"+equivClassName+"' of class '"+name+"' was not found, but for its namespace this was expected")
                        Some(new OntologyClass(equivClassName, Map(), Map(), List(), Set()))
                    }
                    case None =>
                    {
                        logger.warning("equivalent class '"+equivClassName+"' of class '"+name+"' not found")
                        None
                    }
                }}.flatten

                name match
                {
                    case "owl:Thing" => generatedClass = Some(new OntologyClass(name, labels, comments, List(), equivClasses))
                    case _ => generatedClass = Some(new OntologyClass(name, labels, comments, baseClasses, equivClasses))
                }

                buildCalled = true

            }

            generatedClass
        }
    }

    private class PropertyBuilder(val name : String, val labels : Map[Language, String], val comments : Map[Language, String],
                                  val isObjectProperty : Boolean, val isFunctional : Boolean, val domain : String, val range : String,
                                  val equivPropertyNames : Set[String])
    {
        require(name != null, "name != null")
        require(labels != null, "labels != null")
        require(comments != null, "comments != null")
        require(domain != null, "domain != null")
        require(range != null, "range != null")
        require(equivPropertyNames != null, "equivPropertyNames != null")

        /** Caches the property, which has been build by this builder. */
        var generatedProperty : Option[OntologyProperty] = None

        def build(classMap : Map[String, ClassBuilder], typeMap : Map[String, Datatype]) : Option[OntologyProperty] =
        {
            val domainClass = classMap.get(domain) match
            {
                case Some(domainClassBuilder) => domainClassBuilder.generatedClass match
                {
                    case Some(cls) => cls
                    case None => logger.warning("domain '"+domain+"' of property '"+name+"' could not be loaded"); return None
                }
                // TODO: do we want this? Maybe we should disallow external domain types.
                case None if ! RdfNamespace.validate(domain) =>
                {
                    logger.config("domain '"+domain+"' of property '"+name+"' was not found, but for its namespace this was expected")
                    new OntologyClass(domain, Map(), Map(), List(), Set())
                }
                case None => logger.warning("domain '"+domain+"' of property '"+name+"' not found"); return None
            }

            var equivProperties = Set.empty[OntologyProperty]
            for (name <- equivPropertyNames) {
              // FIXME: handle equivalent properties in namespaces that we validate
              if (RdfNamespace.validate(name)) logger.warning("Cannot use equivalent property '"+name+"'")
              else equivProperties += new OntologyProperty(name, Map(), Map(), null, null, false, Set())
            }

            if(isObjectProperty)
            {
                val rangeClass = classMap.get(range) match
                {
                    case Some(rangeClassBuilder) => rangeClassBuilder.generatedClass match
                    {
                        case Some(clazz) => clazz
                        case None => logger.warning("range '"+range+"' of property '"+name+"' could not be loaded"); return None
                    }
                    // TODO: do we want this? Maybe we should disallow external range types.
                    case None if ! RdfNamespace.validate(range) =>
                    {
                        logger.config("range '"+range+"' of property '"+name+"' was not found, but for its namespace this was expected")
                        new OntologyClass(range, Map(), Map(), List(), Set())
                    }
                    case None => logger.warning("range '"+range+"' of property '"+name+"' not found"); return None
                }

                generatedProperty = Some(new OntologyObjectProperty(name, labels, comments, domainClass, rangeClass, isFunctional, equivProperties))
            }
            else
            {
                val rangeType = typeMap.get(range) match
                {
                    case Some(datatype) => datatype
                    case None => logger.warning("range '"+range+"' of property '"+name+"' not found"); return None
                }

                generatedProperty = Some(new OntologyDatatypeProperty(name, labels, comments, domainClass, rangeType, isFunctional, equivProperties))
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
                    case Some(clazz) => clazz
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
}
