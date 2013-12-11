package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import java.net.{URLEncoder, URI}
import scala.language.reflectiveCalls

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: 11/29/11
 * Time: 5:49 PM
 * Extracts the information the describes the contributor (editor) of a Wikipedia page, such as his username, and his ID.
 */

class ContributorExtractor( context : {
  def ontology : Ontology
  def language : Language } ) extends PageNodeExtractor
{

  override val datasets = Set(DBpediaDatasets.RevisionMeta)

  override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
  {
    if(node.title.namespace != Namespace.Main) return Seq.empty

    if(node.contributorID <= 0) return Seq.empty

    val pageURL = "http://" + context.language.wikiCode + ".wikipedia.org/wiki/" + node.root.title.encoded;

    //Required predicates
    val contributorPredicate = "http://dbpedia.org/meta/contributor";
    //val contributorNamePredicate = "http://dbpedia.org/property/contributorName";
    val contributorIDPredicate = "http://dbpedia.org/meta/contributorID";

    //Object values
    val contributorName =  node.contributorName;

    //The username may contain spaces, so we should replace the spaces with underscores "_", as they ar not allowed in
    //URLs
    val contributorNameWithoutSpaces = contributorName.replace (" ", "_");
    val contributorURL =  "http://dbpedia.org/contributor/" + URLEncoder.encode (contributorNameWithoutSpaces,"UTF-8");

    val contributorID =  node.contributorID;

    //    val quadPageWithContributor = new Quad(context.language, DBpediaDatasets.Revisions, pageURL, contributorPredicate,
    //      contributorName, node.sourceUri, context.ontology.getDatatype("xsd:string").get );
    //Required Quads
    val quadPageWithContributor = new Quad(context.language, DBpediaDatasets.RevisionMeta, pageURL, contributorPredicate,
      contributorURL, node.sourceUri, null );

    val quadContributorName = new Quad(context.language, DBpediaDatasets.RevisionMeta, contributorURL,
      context.ontology.properties.get("rdfs:label").get,
      contributorName, node.sourceUri, context.ontology.datatypes.get("xsd:string").get );

    val quadContributorID = new Quad(context.language, DBpediaDatasets.RevisionMeta, contributorURL, contributorIDPredicate,
      contributorID.toString, node.sourceUri, context.ontology.datatypes.get("xsd:integer").get );

    Seq(quadPageWithContributor, quadContributorName, quadContributorID);

  }
}