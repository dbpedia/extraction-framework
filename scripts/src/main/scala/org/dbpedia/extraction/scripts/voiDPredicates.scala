package org.dbpedia.extraction.scripts

import org.openrdf.model.impl._;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: May 28, 2010
 * Time: 11:40:32 AM
 * This object contains all predicates used within the voiD file
 */


object voiDPredicates
{
      //Important predicates
      val PredType = new URIImpl("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
      val PredSubdataset = new URIImpl("http://rdfs.org/ns/void#subset");
      val PredDataDump = new URIImpl("http://rdfs.org/ns/void#dataDump");
      val PredLanguage = new URIImpl("http://purl.org/dc/terms/language");
      val PredSparqlEndPoint = new URIImpl("http://rdfs.org/ns/void#sparqlEndpoint");
      val PredNumberOftriples = new URIImpl("http://rdfs.org/ns/void#triples");
      //val PredCompressedFileSize = new URIImpl("http://open.vocab.org/terms/compressedFileSize");
      //val PredFileSize = new URIImpl("http://open.vocab.org/terms/fileSize");
      val PredFileSize = new URIImpl("http://www.semanticdesktop.org/ontologies/nfo/#fileSize");
      val PredUncompressedFileSize = new URIImpl("http://www.semanticdesktop.org/ontologies/nfo/#uncompressedSize");
      val PredSubjectTarget = new URIImpl("http://rdfs.org/ns/void#subjectsTarget");
      val PredObjectTarget = new URIImpl("http://rdfs.org/ns/void#objectsTarget");
      val PredTitle = new URIImpl("http://purl.org/dc/terms/title");
      val PredDescription = new URIImpl("http://purl.org/dc/terms/description");
      val PredTestPredicate = new URIImpl("http://purl.org/dc/terms/test");
      val PredFormat = new URIImpl("http://purl.org/dc/terms/format");
      val PredHomepage = new URIImpl("http://xmlns.com/foaf/0.1/homepage");
      val PredLinkPredicate = new URIImpl("http://rdfs.org/ns/void#linkPredicate");
      val PredURIRegexPattern = new URIImpl("http://rdfs.org/ns/void#uriRegexPattern");
      val PredModified = new URIImpl("http://purl.org/dc/terms/modified");
      val PredSource = new URIImpl("http://purl.org/dc/terms/source");
      val PredContributor  = new URIImpl("http://purl.org/dc/terms/contributor");
      val PredExampleResource = new URIImpl("http://rdfs.org/ns/void#exampleResource");
}