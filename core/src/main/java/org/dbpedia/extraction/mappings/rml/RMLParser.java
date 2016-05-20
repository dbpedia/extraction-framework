package org.dbpedia.extraction.mappings.rml;

import be.ugent.mmlab.rml.mapdochandler.extraction.std.StdRMLMappingFactory;
import be.ugent.mmlab.rml.mapdochandler.retrieval.RMLDocRetrieval;
import be.ugent.mmlab.rml.model.RMLMapping;
import org.openrdf.repository.Repository;
import org.openrdf.rio.RDFFormat;

/**
 * Responsible for parsing RML documents using the MapDocHandler
 */
public class RMLParser {

    private RMLDocRetrieval retriever;
    private StdRMLMappingFactory rmlMappingFactory;

    public RMLParser() {
        retriever = new RMLDocRetrieval();
        rmlMappingFactory = new StdRMLMappingFactory();
    }

    /**
     * Parses RML document from a file and returns an RMLMapping object
     */
    public RMLMapping parseFromFile(String pathToRmlDocument) {

        Repository repo = retriever.getMappingDoc(pathToRmlDocument, RDFFormat.TURTLE);
        Repository preparedRepo = rmlMappingFactory.prepareExtractRMLMapping(repo);
        RMLMapping rmlMapping = rmlMappingFactory.extractRMLMapping(preparedRepo);
        return rmlMapping;

    }



}
