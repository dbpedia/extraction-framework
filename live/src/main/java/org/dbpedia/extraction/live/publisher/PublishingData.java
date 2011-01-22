package org.dbpedia.extraction.live.publisher;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;

import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.impl.ModelCom;
import org.dbpedia.extraction.live.core.RDFTriple;
import org.openanzo.client.jena.Converter;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Oct 31, 2010
 * Time: 10:59:53 AM
 * This class hold the info required to publish the triples for live synchronization, i.e. the queue of publishing
 * data will contain objects of that type.
 */
public class PublishingData {
    //The caller can either pass the model containing the triples, or pass a string containing the triples as a
    //concatenated string in N-TRIPLES format

    public Model triplesModel;
    public String triplesString="";

    boolean added;

    /**
     * Constructs a PublishingData object
     * @param triplesList   The list of triples to be added or deleted
     * @param isAdded   Boolean flag indicating whether ot add or delete triples
     */
    public PublishingData(ArrayList<RDFTriple> triplesList, boolean isAdded){
        added = isAdded;

        triplesModel = ModelFactory.createDefaultModel();

        for(RDFTriple triple: triplesList){
            //Convert OpenRDF statement to a JENA Triple
            Triple trip = Converter.convert(triple);

            //Convert JENA Triple to JENA Statement
            ModelCom com = new ModelCom(triplesModel.getGraph());

            triplesModel.add(com.asStatement(trip));
        }
    }

    /**
     * Constructs a PublishingData object
     * @param triples   A string containing the triples to be added or deleted in N-TRIPLES format 
     * @param isAdded   Boolean flag indicating whether ot add or delete triples
     */
    public PublishingData(String triples, boolean isAdded){
        added = isAdded;
        triplesString = triples;
    }

    /**
     * Constructs a PublishingData object
     * @param   triplesList The list of triples to be added
     * @param triples   A string containing the triples to be deleted in N-TRIPLES format
     */
    public PublishingData(ArrayList<RDFTriple> triplesList, String triples){
        triplesString = triples;

        triplesModel = ModelFactory.createDefaultModel();

        for(RDFTriple triple: triplesList){
            //Convert OpenRDF statement to a JENA Triple
            Triple trip = Converter.convert(triple);

            //Convert JENA Triple to JENA Statement
            ModelCom com = new ModelCom(triplesModel.getGraph());

            triplesModel.add(com.asStatement(trip));
        }
    }


}
