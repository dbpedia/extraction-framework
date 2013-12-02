package org.dbpedia.extraction.live.feeder

import java.lang.String
import org.apache.log4j.Logger
import java.net.URI
import com.hp.hpl.jena.rdf.model.{Resource, ResourceFactory, ModelFactory, Model}
import org.dbpedia.extraction.sources.Source
import org.dbpedia.extraction.wikiparser._
import collection.mutable.MultiMap
import com.hp.hpl.jena.shared.PrefixMapping
import java.util.GregorianCalendar
import com.hp.hpl.jena.vocabulary.DCTerms
import org.dbpedia.extraction.live.util.StringUtil

/**
 * @author Claus Stadler
 *
 * Date: 9/16/11
 * Time: 4:13 PM
 */
class TBoxExtractor2(val prefixMapping: PrefixMapping, val baseUri: String, val rootPrefix: String, val innerPrefix: String, val destination: TBoxTripleDestination) {

  def tripleGenerator = new TBoxTripleGenerator(prefixMapping)

  private final val logger: Logger = Logger.getLogger(classOf[TBoxExtractor2])
  private final val ONTOLOGY_PROPERTY: String = "OntologyProperty"
  private final val ONTOLOGY_CLASS: String = "OntologyClass"

  // private IGroupTripleManager sys;
  private var maxNumTriples: Int = 500
  //private var destination: TBoxTripleDestination = new TBoxTripleDestination(queryfactory, dataGraphName, metaGraphName, reifierPrefix)
  // private String innerPrefix;
  // private IPrefixResolver prefixResolver;
  //private var baseUri: String = null
  final val extractorUri: URI = URI.create(MyVocabulary.NS + classOf[TBoxExtractor2].getSimpleName)


  def getRootName(title: WikiTitle) : String = {
    var rootName = "error";

    val parts = title.encoded.split(":", 2);

    val prefix = if(parts.length == 1) "" else StringUtil.lcFirst(parts(0)) + ":"
    var suffix = if(parts.length == 1)  parts(0) else parts(1);


    if(title.namespace == Namespace.OntologyClass) {
      suffix = StringUtil.ucFirst(suffix);



    }
    else if(title.namespace == Namespace.OntologyProperty) {
      suffix = StringUtil.lcFirst(suffix);

      /*
      if(title.getShortTitle().equalsIgnoreCase("City")) {
        System.out.println("City");
      }
      */
    }
    else {
      //logger.error("Unexpected title: " + title);
      return null;
    }

    rootName = prefix + suffix;

    return rootName;
  }


  def countTriples(data: MultiMap[Resource, Model]): Int = {
    return data.values.map(_.size).sum
  }

  def handle(source: Source): Unit = {
    handle(source.map(WikiParser.getInstance()))
  }

  def handle(pageNodeSource: Traversable[PageNode]): Unit = {
    for (pageNode <- pageNodeSource) {
      handle(pageNode)
    }
  }



  def handle(pageNode: PageNode): Unit = {//: MultiMap[Resource, Model] = {
    // TODO Deal with subpages
    val rootName = getRootName(pageNode.title);

    if(rootName == null) {
      return
    }

    //val rootId = ResourceFactory.createResource(rootPrefix + rootName);
    val rootId = ResourceFactory.createResource(prefixMapping.expandPrefix(rootName))

    if(!rootId.toString.startsWith("http://")) {
      logger.warn("Skipping resource " + rootId + " because it does not start with http://");
      return;
    }


    //tripleGenerator.setExprPrefix(innerPrefix + rootName + "/");
    //tripleGenerator.exprPrefixRef.setValue(rootPrefix + rootName + "/");
  tripleGenerator.exprPrefixRef.setValue(rootId.toString + "/");

    val result: MultiMap[Resource, Model] = tripleGenerator.load(rootId, pageNode);


    // If there are too many triples, just generate an error triple
    val numTriples = countTriples(result);

    // If no triples were generated for a site, do not generate
    // edit links and such
    if(numTriples == 0)
    {
      return// result;
    }

    // If too many triples were generated they will all be discarded
    // and an error message is generated instead
    //Set<Triple> triples = new HashSet<RDFTriple>();
    val model = ModelFactory.createDefaultModel();
    if (maxNumTriples > 0 && numTriples > maxNumTriples) {
      result.clear();

      model.add(rootId, MyVocabulary.DBM_ERROR, ResourceFactory.createPlainLiteral(numTriples
              + " generated triples exceeded the "
              + maxNumTriples + " triple limit."))
    }




    val revisionLink = ResourceFactory.createResource(baseUri + "index.php?title="
        + pageNode.title.encodedWithNamespace + "&oldid="
        + pageNode.revision);

    val editLink = ResourceFactory.createResource(baseUri + "index.php?title="
        + pageNode.title.encodedWithNamespace + "&action=edit");

    model.add(rootId, MyVocabulary.DBM_REVISION, revisionLink);
    model.add(rootId, MyVocabulary.DBM_EDIT_LINK, editLink);
    val now = new GregorianCalendar();
    model.add(rootId, DCTerms.modified, model.createTypedLiteral(now.asInstanceOf[Object]));






    /*
    model.add(rootId, MyVocabulary.DBM_OAIIDENTIFIER, null);
    */

    val oaiId = ResourceFactory.createResource("oai:en:wikipedia:org:")

    result.addBinding(null, model);

    destination.update(rootId, rootId, oaiId, "" + pageNode.id, tripleGenerator.toJava(result))
    //return result;
  }


  def delete(oaiId: Resource): Unit = {
    try {
      destination.delete(oaiId)
    }
    catch {
      case e: Exception => {
        logger.error("Something went wrong", e)
      }
    }
  }
}

