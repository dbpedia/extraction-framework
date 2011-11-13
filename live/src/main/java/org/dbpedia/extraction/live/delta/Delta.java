package org.dbpedia.extraction.live.delta;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: 11/12/11
 * Time: 12:33 PM
 * Contains the results of diffing the triples of a specific resource
 */
public class Delta {
    private String _resource;
    private Model _addedTriples;
    private Model _deletedTriples;
    private Model _modifiedTriples;

    /**
     * Constructs the Delta object
     * @param resource  The required resource
     * @param addedTriples  The added triples of the resource
     * @param deletedTriples    The deleted triples of the resource
     * @param modifiedTriples   The modified triples of the resource
     */
    public Delta(String resource, Model addedTriples, Model deletedTriples, Model modifiedTriples){
        _addedTriples = addedTriples;
        _modifiedTriples = modifiedTriples;
        _deletedTriples = deletedTriples;
        _resource = resource;
    }

    /**
     * Returns the resource described with that delta object
     * @return  The resource of that Delta object
     */
    public String getResource(){
        return _resource;
    }

    /**
     * Returns the added triples of the resource described with that delta object
     * @return  Model containing the added triples
     */
    public Model getAddedTriples(){
        return _addedTriples;
    }

    /**
     * Returns the deleted triples of the resource described with that delta object
     * @return  Model containing the deleted triples
     */
    public Model getDeletedTriples(){
        return _deletedTriples;
    }

    /**
     * Returns the modified triples of the resource described with that delta object
     * @return  Model containing the modified triples
     */
    public Model getModifiedTriples(){
        return _modifiedTriples;
    }

}
