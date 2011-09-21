package org.dbpedia.extraction.live.feeder;


import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.shared.PrefixMapping;

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;


import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.DCTerms;
import org.apache.commons.collections15.MultiMap;
import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.util.sparql.ISparulExecutor;




/**
 * A class which stores the template-parameter and the language tag e.g.
 * {{DBpedia Class | label@de = ... }}
 * 
 * 
 * @author raven
 * 
 */
/*
class KeyInfo
{
	private String	name;
	private String	languageTag;

	public KeyInfo(String name, String languageTag)
	{
		this.name = name;
		this.languageTag = languageTag;
	}

	public String getName()
	{
		return name;
	}

	public String getLanguageTag()
	{
		return languageTag;
	}

	@Override
	public String toString()
	{
		return name + "@" + languageTag;
	}

	// @Override
	// public String
}* /

public class TBoxExtractor
	//implements IHandler<IRecord>
{
	private static final Logger					logger			= Logger
															.getLogger(TBoxExtractor.class);

	private static final String ONTOLOGY_PROPERTY = "OntologyProperty";
	private static final String ONTOLOGY_CLASS = "OntologyClass";
	
	
	// private IGroupTripleManager sys;

	private TBoxTripleGenerator		tripleGenerator;
	// private ISparulExecutor executor;
	private String					rootPrefix;

	private String					innerPrefix;

	// maxNumTriples does not include editLink, pageId and revisionLink triples
	private int						maxNumTriples	= 500;

	private TBoxTripleDestination	destination;

	// private String innerPrefix;
	// private IPrefixResolver prefixResolver;

	// !!! This base uri must be the one to the mediawiki directory !!!
	// e.g. http://en.wikipedia.org/w/
	// NOT: http://en.wikipedia.org/wiki/
	// This is needed because of the edit links which are also extracted
	private String					baseUri;

	public static final URI		extractorUri	= URI
															.create(MyVocabulary.NS
																	+ TBoxExtractor.class
																			.getSimpleName());

	public TBoxExtractor(String indexBaseUri, ISparulExecutor executor,
			String dataGraphName, String metaGraphName, String reifierPrefix,
			String targetPrefix, String innerPrefix,
			PrefixMapping prefixResolver)
		throws NoSuchAlgorithmException
	{
		tripleGenerator = new TBoxTripleGenerator(
		// innerPrefix,
				prefixResolver);

		this.rootPrefix = targetPrefix;
		// this.sys = sys;
		this.baseUri = indexBaseUri;

		this.innerPrefix = innerPrefix;

		// this.executor = executor;

		this.destination = new TBoxTripleDestination(executor, dataGraphName,
				metaGraphName, reifierPrefix);
	}

    /*
	@Override
	public void handle(IRecord item)
	{
		item.accept(this);
	}* /




	private int countTriples(MultiMap<Resource, Model> data)
	{
		int result = 0;
		for(Model item : data.values()) {
			result += item.size();
        }
		
		return result;
	}
	

    /*
	public static String getRootName(MediawikiTitle title)
	{
		String rootName = "error";
		if(title.getNamespaceName().equalsIgnoreCase(ONTOLOGY_PROPERTY)) {
			rootName = StringUtil.lcFirst(title.getShortTitle());
		}
		else if(title.getNamespaceName().equalsIgnoreCase(ONTOLOGY_CLASS)) {
			rootName = StringUtil.ucFirst(title.getShortTitle());

			/*
			if(title.getShortTitle().equalsIgnoreCase("City")) {
				System.out.println("City");
			}
			* /
		}
		else {
			logger.error("Unexpected title: " + title.getFullTitle());
		}
		
		return rootName;
	} */
	

    /*
	public MultiMap<Resource, Model> handle(
			IWikiNode root)
	{

		// find the target name which is the subpage name -- outdated
		// String targetName = WikiParserHelper.extractSubPageName(name);

		//String name = metadata.getTitle().getFullTitle();

		// the new target name is the oai identifier
		// targetPrefix +
		// String targetName = metadata.getOaiId();
		// IRI targetUri = IRI.create(targetName);

		//String rootName = WikiParserHelper.extractSubPageName(name);
		//http://mappings.dbpedia.org/index.php/OntologyProperty:ASide
		//
		
		
		// TODO Deal with subpages
		String rootName = getRootName(metadata.getTitle());
		
		IRI rootId = IRI.create(rootPrefix + rootName);

		//tripleGenerator.setExprPrefix(innerPrefix + rootName + "/");
		tripleGenerator.setExprPrefix(rootPrefix + rootName + "/");

		MultiMap<Resource, Model> result = tripleGenerator.extract(rootId,
				root);


		// If there are too many triples, just generate an error triple
		int numTriples = countTriples(result);

		// If no triples were generated for a site, do not generate
		// edit links and such
		if(numTriples == 0)
		{			
			return result;
		}
		
		// If too many triples were generated they will all be discarded
		// and an error message is generated instead
		Set<RDFTriple> triples = new HashSet<RDFTriple>();
		if (maxNumTriples > 0 && numTriples > maxNumTriples) {
			result.clear();

			triples.add(new RDFTriple(new RDFResourceNode(rootId),
					new RDFResourceNode(MyVocabulary.DBM_ERROR.getIRI()),
					new RDFLiteralNode(numTriples
							+ " generated triples exceeded the "
							+ maxNumTriples + " triple limit.")));
		}


		
		
		String revisionLink = baseUri + "index.php?title="
				+ metadata.getTitle().getFullTitle() + "&oldid="
				+ metadata.getRevision();

		String editLink = baseUri + "index.php?title="
				+ metadata.getTitle().getFullTitle() + "&action=edit";

		triples.add(new RDFTriple(new RDFResourceNode(rootId),
				new RDFResourceNode(MyVocabulary.DBM_REVISION.getIRI()),
				new RDFResourceNode(IRI.create(revisionLink))));

		triples.add(new RDFTriple(new RDFResourceNode(rootId),
				new RDFResourceNode(MyVocabulary.DBM_EDIT_LINK.getIRI()),
				new RDFResourceNode(IRI.create(editLink))));

		triples.add(new RDFTriple(new RDFResourceNode(rootId),
				new RDFResourceNode(MyVocabulary.DBM_OAIIDENTIFIER.getIRI()),
				new RDFResourceNode(IRI.create(metadata.getOaiId()))));

		result.put(null, triples);

		return result;
		/*
		 * group.getGroup().putProperty( new
		 * RDFResourceNode(MyVocabulary.DBM_SOURCE_PAGE.getUri()), new
		 * RDFResourceNode(item.getMetadata().getWikipediaURI())); // remove
		 * group if there are no triples if(group.getTriples() != null &&
		 * group.getTriples().size() == 0) group.setTriples(null);
		 * /
	}

	public void handle(Record item)
	{
		IWikiNode root = item.getContent().getRepresentations().getSingle(
				IWikiNode.class);

		if (root == null) {
			logger.warn("No wiki-node representation for item: "
					+ item.getMetadata().getTitle());
			// return null;
			return;
		}



		//String name = item.getMetadata().getTitle().getFullTitle();
		String rootName = getRootName(item.getMetadata().getTitle());
		//String rootName = WikiParserHelper.extractSubPageName(name);
		
		MultiMap<IRI, Set<RDFTriple>> triples = handle(item.getMetadata(), root);

		logger.info("Updating triples for revision "
				+ item.getMetadata().getRevision() + " of subject "
				+ item.getMetadata().getTitle().getFullTitle());


		IRI rootUri = IRI.create(rootPrefix + rootName);
		
		
		IRI sourcePageIRI = item.getMetadata().getWikipediaURI();

		String oaiId = item.getMetadata().getOaiId();
		IRI oaiIdIRI = IRI.create(oaiId);

		int lastColonIndex = item.getMetadata().getOaiId().lastIndexOf(':');
		String pageId = oaiId.substring(lastColonIndex + 1);

		try {
			destination.update(rootUri, sourcePageIRI, oaiIdIRI, pageId, triples);
		}
		catch (Exception e) {
			logger.error(ExceptionUtil.toString(e));
		}
		/*
		 * try { sys.update(group); } catch(Exception e) {
		 * logger.warn(ExceptionUtil.toString(e)); }
		 * 
		 * return group;
		 * /
	}
	*/


	/**
	 * This function can remove a whole group based on the oai identifier TODO
	 * Currently this method is public in order to allow manual deletions when
	 * something goes wrong, but it will be moved into some specialized version
	 * of a triple manager.
	 * 
	 * @param oaiId
	 * @throws Exception
	 */
	/*
	 * public void deleteGroupByOaiId(String oaiId) throws Exception { URI
	 * targetUri = IRI.create(oaiId);
	 * 
	 * logger.info("Deleting triples for oai-identifier " + oaiId);
	 * 
	 * TripleSetGroup group = new TripleSetGroup(new DBpediaGroupDef(
	 * extractorUri, targetUri)); sys.update(group); }
	 * /


    public void delete(Resource oaiId) {
        try {
            destination.delete(oaiId);
        }
        catch(Exception e)
        {
            logger.error("Something went wrong", e);
        }
    }
}
*/

class TBoxTripleDestination
{
	private static Logger logger = Logger.getLogger(TBoxTripleDestination.class);
	
	private String					reifierPrefix;
	private String					dataGraphName;
	private String					metaGraphName;

	private ISparulExecutor executor;
	// private ISparulExecutor dataExecutor;
	// private ISparulExecutor metaExecutor;
	private MessageDigest			md5;

	private static Resource	origin =
            //ResourceFactory.createResource(MyVocabulary.NS + TBoxExtractor.class.getSimpleName());
            ResourceFactory.createResource(MyVocabulary.NS + "TBoxExtractor");


	public TBoxTripleDestination(ISparulExecutor executor,
			String dataGraphName, String metaGraphName, String refifierPrefix)
		throws NoSuchAlgorithmException
	{
		this.executor = executor;
		this.dataGraphName = dataGraphName;
		this.metaGraphName = metaGraphName;

		this.reifierPrefix = refifierPrefix;

		md5 = MessageDigest.getInstance("MD5");
	}

	
	
	private void deleteFromMetaGraphBySourcePage(Resource sourcePage)
		throws Exception
	{
		String query = DBpediaQLUtil.deleteMetaBySourcePage(
				sourcePage.toString(), metaGraphName);
			/*
			"Delete From <" + metaGraphName + ">\n" +
			"{\n" +
				"?b ?x ?y\n" +
			"}\n" +
			"From <" + metaGraphName + "> \n" +
			"{\n" +					
				"?b <" + MyVocabulary.DBM_SOURCE_PAGE + "> <" + sourcePage + ">  .\n" +
				"?b ?x ?y .\n" +
			"}\n";
			*/
		logger.debug("Running query: 'deleteFromMetaGraphBySourcePage'");
		executor.executeUpdate(query);
	}
	
	private void deleteFromDataGraphBySourcePage(Resource sourcePage)
		throws Exception
	{
		String query = DBpediaQLUtil.deleteDataBySourcePage(
				sourcePage.toString(), dataGraphName, metaGraphName);
			/*
			"Delete From <" + dataGraphName + ">\n" +
			"{\n" +
				"?s ?p ?o\n" +
			"}\n" +
			"From <" + metaGraphName + "> {\n" +
				"?b <" + MyVocabulary.DBM_SOURCE_PAGE + "> <" + sourcePage + "> .\n" +
				"?b <" + MyVocabulary.OWL_ANNOTATED_SOURCE + "> ?s .\n" +
				"?b <" + MyVocabulary.OWL_ANNOTATED_PROPERTY + "> ?p .\n" +
				"?b <" + MyVocabulary.OWL_ANNOTATED_TARGET + "> ?o .\n" +
			"}\n";
*/
		logger.debug("Running query: 'deleteFromDataGraphBySourcePage'");
		executor.executeUpdate(query);
	}
	
	/**
	 * 
	 * @param oaiId
	 * @throws Exception
	 */
	private void deleteFromDataGraph(Resource oaiId)
		throws Exception
	{
		String query =
			"Delete From <" + dataGraphName + ">\n" +
			"{\n" +
				"?s ?p ?o\n" +
			"}\n" +
			"{\n" + 
				"Graph <" + dataGraphName + "> {\n" +
					"?a <" + MyVocabulary.DBM_OAIIDENTIFIER + "> <" + oaiId + ">\n" +
				"}\n" +
				"Graph <" + metaGraphName + "> {\n" +
					"?b <" + MyVocabulary.DBM_SOURCE_PAGE + "> ?a .\n" +
					"?b <" + MyVocabulary.OWL_ANNOTATED_SOURCE + "> ?s .\n" +
					"?b <" + MyVocabulary.OWL_ANNOTATED_PROPERTY + "> ?p .\n" +
					"?b <" + MyVocabulary.OWL_ANNOTATED_TARGET + "> ?o .\n" +
				"}\n" +
			"}\n";


		logger.debug("Running query: 'deleteFromDataGraph'");
		executor.executeUpdate(query);
	}

	private void deleteFromMetaGraph(Resource oaiId)
		throws Exception
	{
		String query =
			"Delete From <" + metaGraphName + ">\n" +
				"{\n" +
					"?b ?x ?y\n" +
				"}\n" +
				"From <" + metaGraphName + "> \n" +
				"{\n" +
					"?t <" + MyVocabulary.OWL_ANNOTATED_PROPERTY + "> <" + MyVocabulary.DBM_OAIIDENTIFIER + "> .\n" +
					"?t <" + MyVocabulary.OWL_ANNOTATED_TARGET + "> <" + oaiId + "> .\n" +
					
					"?t <" + MyVocabulary.DBM_SOURCE_PAGE + "> ?a .\n" +
					"?b <" + MyVocabulary.DBM_SOURCE_PAGE + "> ?a .\n" +
					"?b ?x ?y .\n" +
				"}\n";


		logger.debug("Running query: 'deleteFromMetaGraph'");
		executor.executeUpdate(query);
	}

	private Model reify(Model result, Statement triple, Resource reifier)
	{
		result.add(reifier, MyVocabulary.OWL_ANNOTATED_SOURCE, triple.getSubject());
		result.add(reifier, MyVocabulary.OWL_ANNOTATED_PROPERTY, triple.getPredicate());
		result.add(reifier, MyVocabulary.OWL_ANNOTATED_TARGET, triple.getObject());

		return result;
	}

	private void insertIntoDataGraph(Resource rootId, Resource sourcePage,
			MultiMap<Resource, Model> triples)
		throws Exception
	{
        /*
		Date date = new Date();
		Format formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		String dateString = formatter.format(date);
		int split = dateString.length() - 2;
		dateString = dateString.substring(0, split) + ":" + dateString.substring(split);
		)*/

		
		
		//RDFResourceNode dcModified = new RDFResourceNode(
		//		MyVocabulary.DC_MODIFIED.getIRI());

		//IRI xsdDateTime = IRI.create(XSD.dateTime.getURI());

		Model inserts = ModelFactory.createDefaultModel();

		
		for (Map.Entry<Resource, Collection<Model>> item : triples
				.entrySet()) {

			for (Model tmp : item.getValue()) {
                inserts.add(tmp);
			}
		}

		// RDFResourceNode oaiIdPredicate = new
		// RDFResourceNode(MyVocabulary.DBM_OAIIDENTIFIER.getUri());

		// IRI sourcePage, IRI oaiId,
		/*
		 * inserts.add( new RDFTriple(new RDFResourceNode(sourcePage),
		 * oaiIdPredicate, new RDFResourceNode(oaiId)));
		 */
		logger.debug("Running query: 'insertIntoDataGraph'");
		executor.insert(inserts, dataGraphName);
	}

	private Resource generateMD5HashUri(String pageId, Statement triple)
	{
		return ResourceFactory.createResource(reifierPrefix + pageId + "_" + generateMD5(triple));
	}

	private String generateMD5(Statement triple)
	{
		String str = triple.getSubject().toString() + " "
				+ triple.getPredicate().toString() + " "
				+ triple.getObject().toString();

		return generateMD5(str);
	}

	private String generateMD5(String str)
	{
		md5.reset();
		md5.update(str.getBytes());
		byte[] result = md5.digest();

		StringBuffer hexString = new StringBuffer();
		for (int i = 0; i < result.length; i++) {
			hexString.append(Integer.toHexString(0xFF & result[i]));
		}
		return hexString.toString();
	}

	private void insertIntoMetaGraph(Resource rootIRI, Resource sourcePage, String pageId,
			MultiMap<Resource, Model> triples)
		throws Exception
	{
		/*
		Date date = new Date();
		Format formatter = new SimpleDateFormat("yyyy.MM.dd'T'HH:mm:ss");
		String dateString = formatter.format(date);

		RDFResourceNode dcModified = new RDFResourceNode(
				MyVocabulary.DC_MODIFIED.getIRI());
		IRI xsdDateTime = IRI.create(XSD.dateTime.getURI());*/
        /*
		RDFResourceNode predicateSourcePage = new RDFResourceNode(
				MyVocabulary.DBM_SOURCE_PAGE.getIRI());
		RDFResourceNode objectSourcePage = new RDFResourceNode(rootIRI);
		RDFResourceNode aspectPredicate = new RDFResourceNode(
				MyVocabulary.DBM_ASPECT.getIRI());

		RDFResourceNode originPredicate = new RDFResourceNode(
				MyVocabulary.DBM_EXTRACTED_BY.getIRI());
		*/

		//Set<RDFTriple> inserts = new HashSet<RDFTriple>();
        Model inserts = ModelFactory.createDefaultModel();
		for (Map.Entry<Resource, Collection<Model>> item : triples
				.entrySet()) {
			Resource aspect = item.getKey();

			for (Model tmp : item.getValue()) {
				for (Statement triple : tmp.listStatements().toSet()) {
					Resource reifier = generateMD5HashUri(pageId, triple);

					reify(inserts, triple, reifier);

					/*
					inserts.add(new RDFTriple(reifier, dcModified,
							new RDFLiteralNode(dateString, xsdDateTime)));
					*/
					inserts.add(reifier, MyVocabulary.DBM_SOURCE_PAGE, rootIRI);
					inserts.add(reifier, MyVocabulary.DBM_ORIGIN, origin);

					if (aspect != null) {
						inserts.add(reifier, MyVocabulary.DBM_ASPECT, aspect);
                    }
				}
			}
		}

		logger.debug("Running query: 'insertIntoMetaGraph'");
		executor.insert(inserts, metaGraphName);
	}

	// ttlp insert
	/*
	private void insert(String graphName, Set<RDFTriple> triples)
		throws Exception
	{
		executor.insert(triples, graphName);
	}
	 */
	/*
	private void insert(String graphName, Set<RDFTriple> triples)
		throws Exception
	{
		String query = "Insert into <" + graphName + "> {\n";

		for (RDFTriple triple : triples)
			query += SparqlHelper.toSparqlString(triple) + " .\n";

		query += "}\n";

		executor.executeUpdate(query);
		//System.out.println(graphName);
		//System.out.println(query);
	}
	 */
	public void delete(Resource oaiId)
		throws Exception
	{
		deleteFromDataGraph(oaiId);
		deleteFromMetaGraph(oaiId);		
	}
	
	public void update(Resource rootIRI, Resource sourcePage, Resource oaiId, String pageId,
			MultiMap<Resource, Model> triples)
		throws Exception
	{
		deleteFromDataGraphBySourcePage(rootIRI);
		deleteFromMetaGraphBySourcePage(rootIRI);

		if(triples != null) {
			insertIntoMetaGraph(rootIRI, sourcePage, pageId, triples);
			insertIntoDataGraph(rootIRI, sourcePage, triples);
		}
	}

}
