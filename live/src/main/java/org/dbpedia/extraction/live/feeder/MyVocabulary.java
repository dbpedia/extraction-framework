package org.dbpedia.extraction.live.feeder;


import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.OWL;


public class MyVocabulary
{
    public static final String NS = "http://dbpedia.org/meta/";

	public static final Property DBM_ORIGIN = createProperty("origin");

    //public DBM_EXTRACTED_BY(DBM + "origin"),

    public static final Property DBM_PAGEID = createProperty("pageid");
	public static final Property DBM_MEMBER_OF = createProperty("memberof");
	public static final Property DBM_GROUP = createProperty("group");
	
	public static final Property DBM_ERROR = createProperty("error");
	
	public static final Property DBM_SOURCE_PAGE = createProperty("sourcepage");

	public static final Property DBM_ASPECT = createProperty("aspect");
	
	//OWL("http://www.w3.org/2002/07/owl#"),

	//OWL_AXIOM(OWL. + "Axiom"),
	public static final Property OWL_ANNOTATED_SOURCE  = createOwlProperty("annotatedSource");
	public static final Property OWL_ANNOTATED_PROPERTY = createOwlProperty("annotatedProperty");
	public static final Property OWL_ANNOTATED_TARGET = createOwlProperty("annotatedTarget");

        /*
	DC("http://purl.org/dc/terms/"),
	DC_MODIFIED(DBM + "modified"),
	DC_CREATED(DC + "created"),
*/
	// following predicates are used in the default graph
	// and are attached directly to a subject
	public static final Property DBM_REVISION = createProperty("revisionlink");
	
	// Note: using the oiaidentifier instead of the page id makes wiki pages
	// unique acroos multiple wikis
	public static final Property DBM_OAIIDENTIFIER = createProperty("oaiidentifier");
	public static final Property DBM_PAGE_ID = createProperty("pageid");
	public static final Property DBM_EDIT_LINK = createProperty("editlink");

	
	public static final String DBM_TEMP_ID = NS + "temp_id";
	public static final String DBM_TEMP_ID_NS = NS + "id/";
	//OWL_AXIOM(OWL2 + "Axiom"),
	//RDF("http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
	//RDF_TYPE(RDF + "type"),

    public static Property createProperty(String suffix)
    {
        return ResourceFactory.createProperty(NS + suffix);
    }

    public static Property createOwlProperty(String suffix)
    {
        return ResourceFactory.createProperty(OWL.NS + suffix);
    }
}
