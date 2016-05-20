package org.dbpedia.extraction.mappings;

import be.ugent.mmlab.rml.mapdochandler.extraction.std.StdRMLMappingFactory;
import be.ugent.mmlab.rml.mapdochandler.retrieval.RMLDocRetrieval;
import be.ugent.mmlab.rml.model.RMLMapping;
import org.openrdf.repository.Repository;
import org.openrdf.rio.RDFFormat;

/**
 * Responsible for parsing RML documents using the MapDocHandler
 */
public class RMLParser {

    /**
     * Parses RML document from a file and returns an RMLMapping object
     */
    public RMLMapping parseFromFile(String pathToRmlDocument) {

        RMLDocRetrieval retriever = new RMLDocRetrieval();
        Repository repo = retriever.getMappingDoc(pathToRmlDocument, RDFFormat.TURTLE);
        StdRMLMappingFactory rmlMappingFactory = new StdRMLMappingFactory();
        Repository preparedRepo = rmlMappingFactory.prepareExtractRMLMapping(repo);
        RMLMapping rmlMapping = rmlMappingFactory.extractRMLMapping(preparedRepo);

        return rmlMapping;
    }



}
