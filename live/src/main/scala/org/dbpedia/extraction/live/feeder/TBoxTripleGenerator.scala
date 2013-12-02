package org.dbpedia.extraction.live.feeder

import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology._
import org.dbpedia.extraction.sources.Source
import com.hp.hpl.jena.shared.PrefixMapping
import org.slf4j.LoggerFactory
import collection.immutable.Traversable;
import com.hp.hpl.jena.vocabulary.{RDF, RDFS, OWL}
import collection.mutable.{Set, Map, MultiMap, HashMap}
import com.hp.hpl.jena.rdf.model.{ModelFactory, ResourceFactory, Resource, Model}
import org.dbpedia.extraction.ontology.io.OntologyReader

/**
 * This extractor extracts all properties from all infoboxes.
 * Extracted information is represented using properties in the http://dbpedia.org/property/ namespace.
 * The names of the these properties directly reflect the name of the Wikipedia infobox property.
 * Property names are not cleaned or merged.
 * Property types are not part of a subsumption hierarchy and there is no consistent ontology for the infobox dataset.
 * The infobox extractor performs only a minimal amount of property value clean-up, e.g., by converting a value like “June 2009” to the XML Schema format “2009–06”.
 * You should therefore use the infobox dataset only if your application requires complete coverage of all Wikipeda properties and you are prepared to accept relatively noisy data.
 */
/**
 * This is the actual extractor (PropDefExtractor is just the handler)
 *
 * @author raven
 *
 */
/*
object TBoxExtractor2 {
  val OBJECT_PROPERTY = "ObjectProperty"
  val DATATYPE_PROPERTY = "DatatypeProperty"
  val CLASS = "Class"
}
*/

object KeyInfo {
  def apply(key: String): KeyInfo = {
    val parts = key.split("@", 2)

    return new KeyInfo(parts.lift(0).get, parts.lift(1).getOrElse("en"))
  }
}

class KeyInfo(val name: String, val language: String) {}

class TBoxTripleGenerator() {
  def toJava[K, V](multiMap : MultiMap[K, V]): org.apache.commons.collections15.MultiMap[K, V] = {
    val result = new org.apache.commons.collections15.multimap.MultiHashMap[K, V]()

    multiMap.foreach {case (k, vs) => vs.foreach(result.put(k, _))}
    /*
    for((k: K, v: V) <- multiMap) {
      v.foreach(result.put(k, _))
    }*/

    return result
  }



  //import TBoxExtractor2.logger
  val logger = LoggerFactory.getLogger(classOf[TBoxTripleGenerator])


  var rdfType = ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")

  /*
  val CLASSTEMPLATE_NAME = "Class"
  val OBJECTPROPERTY_NAME = "ObjectProperty"
  val DATATYPEPROPERTY_NAME = "DatatypeProperty"
  val SPECIFICPROPERTY_NAME = "SpecificProperty"
*/

  var exprPrefixRef = new StringReference()
  private var prefixResolver: PrefixMapping = null // = context.prefixResolver;

  // NOTE: This extractor uri is NOT THIS CLASS' name!!!
  /*
   * private static final Resource extractorUri = Resource .create(DBM +
   * PropertyDefinitionExtractor.class .getSimpleName());
   */

  // mapping for class definition attributes to triple generators
  private val classToGenerator = new HashMap[String, ITripleGenerator]()

  // Default values for class (will be used if the parameter is not present)
  private val classDefaults = new HashMap[String, ITripleGenerator]

  private val propertyToGenerator = new HashMap[String, ITripleGenerator]

  private val objectDefaults = new HashMap[String, ITripleGenerator]()

  private val dataToGenerator = new HashMap[String, ITripleGenerator]()

  private val dataDefaults = new HashMap[String, ITripleGenerator]()


  def this(prefixResolver: PrefixMapping) = {
    this ()

    this.prefixResolver = prefixResolver //context.prefixResolver;

    val literalTripleGenerator = new LiteralTripleGenerator()

    val labelsTripleGenerator = new LabelsTripleGenerator()
    val commentsTripleGenerator = new CommentsTripleGenerator()

    val mosListTripleGenerator = new ListTripleGenerator(",",
      new MosTripleGenerator(exprPrefixRef, prefixResolver))

    val fallbackSubClassGenerator = new StaticTripleGenerator(RDFS.subClassOf, OWL.Thing)

    /*
      * ITripleGenerator subClassTripleGenerator = new
      * AlternativeTripleGenerator( mosListTripleGenerator,
      * fallbackSubClassGenerator);
      */

    classToGenerator.put("labels", labelsTripleGenerator)
    classToGenerator.put("comments", commentsTripleGenerator)
    classToGenerator.put("rdfs:label", literalTripleGenerator);
    classToGenerator.put("rdfs:comment", literalTripleGenerator);
    classToGenerator.put("owl:equivalentClass", mosListTripleGenerator);
    classToGenerator.put("owl:disjointWith", mosListTripleGenerator);
    classToGenerator.put("rdfs:seeAlso", mosListTripleGenerator);
    classToGenerator.put("rdfs:subClassOf", mosListTripleGenerator); // )subClassTripleGenerator);

    classDefaults.put("rdfs:subClassOf", fallbackSubClassGenerator);

    propertyToGenerator.put("labels", labelsTripleGenerator)
    propertyToGenerator.put("comments", commentsTripleGenerator)
    propertyToGenerator.put("rdfs:label", literalTripleGenerator);
    propertyToGenerator.put("rdfs:comment", literalTripleGenerator);
    propertyToGenerator.put("owl:equivalentProperty",
      mosListTripleGenerator);
    propertyToGenerator.put("rdfs:seeAlso", mosListTripleGenerator);
    propertyToGenerator.put("rdfs:subPropertyOf", mosListTripleGenerator);
    propertyToGenerator.put("rdfs:domain", mosListTripleGenerator);
    propertyToGenerator.put("rdfs:range", mosListTripleGenerator);
    propertyToGenerator.put("rdf:type", mosListTripleGenerator);

    dataToGenerator.put("labels", labelsTripleGenerator);
    dataToGenerator.put("comments", commentsTripleGenerator);
    dataToGenerator.put("rdfs:label", literalTripleGenerator);
    dataToGenerator.put("rdfs:comment", literalTripleGenerator);
    dataToGenerator.put("owl:equivalentProperty", mosListTripleGenerator);
    dataToGenerator.put("rdfs:seeAlso", mosListTripleGenerator);
    dataToGenerator.put("rdfs:subPropertyOf", mosListTripleGenerator);
    dataToGenerator.put("rdfs:domain", mosListTripleGenerator);
    dataToGenerator.put("rdfs:range", mosListTripleGenerator);
    dataToGenerator.put("rdf:type", mosListTripleGenerator);
  }

  def readJava(source: Source): org.apache.commons.collections15.MultiMap[Resource, Model] = {
    return toJava(read(source))
  }

  def read(source: Source): MultiMap[Resource, Model]  = {
    logger.info("Loading ontology pages")

    return read(source.map(WikiParser.getInstance()))
  }



  /**
   *  Loads an ontology from configuration files using the DBpedia mapping language.
   *
   * @param source The source containing the ontology pages
   * @return Ontology The ontology
   */
  def read(pageNodeSource: Traversable[PageNode]): MultiMap[Resource, Model] = {
    logger.info("Loading ontology")

    for (pageNode <- pageNodeSource) {
      return load(ResourceFactory.createResource("http://example.org"), pageNode)
    }

    logger.info("Ontology loaded")


    return new HashMap[Resource, Set[Model]] with MultiMap[Resource, Model]
    //return null //result
  }



  /**
   * Loads all classes and properties from a page.
   *
   * @param ontology The OntologyBuilder instance
   * @param pageNode The page node of the configuration page
   */
   def load(rootId: Resource, page: PageNode): MultiMap[Resource, Model] = {

    //val root = ResourceFactory.createResource(rootId)

    val map = process(rootId, page)


    return map


    //return ModelFactory.createDefaultModel()
    /*
    for (node <- page.children if node.isInstanceOf[TemplateNode]) {
      val templateNode = node.asInstanceOf[TemplateNode]
      val templateName = templateNode.title.encoded

      logger.info(templateName)

      if (templateName == OntologyReader.CLASSTEMPLATE_NAME) {
        val name = OntologyReader.getClassName(page.title)

        processTemplate(result, name, page, templateNode)

        /*
        for (specificProperty <- loadSpecificProperties(name, templateNode)) {
          ontologyBuilder.specializedProperties ::= specificProperty
        }*/
      }
      else if (templateName == OntologyReader.OBJECTPROPERTY_NAME || templateName == OntologyReader.DATATYPEPROPERTY_NAME) {
        val name = OntologyReader.getPropertyName(page.title)

        loadOntologyProperty(result, name, templateNode)
      }
    }

    return result
    */
  }

  /*
  private def getOrCreateModel(map: Map[Resource, Set[Model]], key: Resource) : M = {

  }*/

  def process(root: Resource, pageNode: PageNode): MultiMap[Resource, Model] = {
    val result = new HashMap[Resource, Set[Model]] with MultiMap[Resource, Model]


    for (node <- pageNode.children if node.isInstanceOf[TemplateNode]) {
      val templateNode = node.asInstanceOf[TemplateNode]
      val templateName = templateNode.title.encoded

      //logger.info(templateName)

      if (templateName == OntologyReader.CLASSTEMPLATE_NAME) {
//        val name = OntologyReader.getClassName(pageNode.title)
        //processClass(result, root, page, templateNode)
        processX(result, root, templateNode, classToGenerator, classDefaults, OWL.Class)


        /*
        for (specificProperty <- loadSpecificProperties(name, templateNode)) {
          ontologyBuilder.specializedProperties ::= specificProperty
        }*/
      }
      else if(templateName == OntologyReader.OBJECTPROPERTY_NAME) {
//        val name = OntologyReader.getPropertyName(pageNode.title)

        processX(result, root, templateNode, propertyToGenerator, objectDefaults, OWL.ObjectProperty)


      } else if(templateName == OntologyReader.DATATYPEPROPERTY_NAME) {
//        val name = OntologyReader.getPropertyName(pageNode.title)

        processX(result, root, templateNode, dataToGenerator, dataDefaults, OWL.DatatypeProperty)
      }
    }

    return result
  }


  private def processX(result: MultiMap[Resource, Model], root: Resource, templateNode: TemplateNode, xToGenerator: Map[String, ITripleGenerator], xToDefault: Map[String, ITripleGenerator], clazz: Resource): MultiMap[Resource, Model] = {
    processTemplate(result, root, templateNode, xToGenerator, xToDefault)
    

    val tmp = ModelFactory.createDefaultModel()
    tmp.add(root, rdfType, clazz)
    result.addBinding(rdfType, tmp)

    return result
  }


  private def loadClass(result: Model, name: String, page: PageNode, node: TemplateNode): Model = {

    /*
    node.children.foreach(child => {

      val keyInfo = KeyInfo.apply(child.key);


      //logger.info("Key: " + keyInfo.name + " --- " + keyInfo.language)
      //val wtf =classToGenerator.get(keyInfo.name);
      //println("WTF: " + wtf + " " + classToGenerator)

      classToGenerator.get(keyInfo.name) match {
        case None => None
        case Some(generator) => {

          val text = child.children.map(_.toWikiText).mkString("")
          logger.info("kv: " + child.key + " ---" + text)

          val s = OntologyReader.getClassName(page.title)

          val subject = ResourceFactory.createResource(s);

          val p = prefixResolver.expandPrefix(keyInfo.name);
          val property = ResourceFactory.createProperty(p);

          println(s + " - " + p + " - " + text + " - " + keyInfo.language)

          generator.generate(result, subject, property, text, keyInfo.language)
        }
      }

    });
    */
    return result
  }


  def processTemplate(
                       result: MultiMap[Resource, Model],
                       subject: Resource,
                       templateNode: TemplateNode,
                       // TripleSetGroup result,
                       map: Map[String, ITripleGenerator],
                       defaults: Map[String, ITripleGenerator]): MultiMap[Resource, Model] = {


    //val result = new Map[Resource, List[Model]] with MultiMap[Resource, Model]

    // Those parameters for which triples have been generated
    val seenParameters = Set[String]();

    templateNode.children.foreach(child => {

      val keyInfo = KeyInfo.apply(child.key);


      //logger.info("Key: " + keyInfo.name + " --- " + keyInfo.language)
      //val wtf =classToGenerator.get(keyInfo.name);
      //println("WTF: " + wtf + " " + classToGenerator)

      map.get(keyInfo.name) match {
        case Some(generator) => {

          val text = child.children.map(_.toWikiText).mkString("")
          logger.info("kv: " + child.key + " ---" + text)

          val p = prefixResolver.expandPrefix(keyInfo.name);
          val property = ResourceFactory.createProperty(p);

          //println(s + " - " + p + " - " + text + " - " + keyInfo.language)

          var model = ModelFactory.createDefaultModel()
          generator.generate(model, subject, property, text, keyInfo.language)

          if(!model.isEmpty) {
            seenParameters.add(keyInfo.name)
          }

          result.addBinding(property, model)

        }
        case _ =>
      }

    });


    for((key, generator) <- defaults) {
      if (!seenParameters.contains(key)) {

        val keyInfo = KeyInfo.apply(key)

        val p = prefixResolver.expandPrefix(keyInfo.name);
        val property = ResourceFactory.createProperty(p);

        //println(s + " - " + p + " - " + text + " - " + keyInfo.language)

        val model = ModelFactory.createDefaultModel()
        generator.generate(model, subject, null, null, null)

        result.addBinding(property, model)
      }
    }

    return result
  }
}