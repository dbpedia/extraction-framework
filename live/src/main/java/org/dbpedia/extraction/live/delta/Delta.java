package org.dbpedia.extraction.live.delta;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import org.dbpedia.helper.CoreUtil;

import java.io.StringWriter;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: 11/12/11
 * Time: 12:33 PM
 * Contains the results of diffing the triples of a specific resource
 */
public class Delta {

    public enum DiffType{
        ADDED(1), DELETED(2), MODIFIED(3);
        private int code;

        private DiffType(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

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

    /**
     * Create a long string that contains the N-Triples represntation of the added triples.
     * @param bListTriplesForResourceOnly   Determines whether to formulate the string with the triples for which the resource is the subject.
     * @return  The N-Triples representation of that added triples.
     */
    public String formulateAddedTriplesAsNTriples(boolean bListTriplesForResourceOnly){
        if(_addedTriples == null)
            return "";

        if(!bListTriplesForResourceOnly){
            RDFWriter rdfWriter = _addedTriples.getWriter("N-TRIPLE");
            StringWriter writer = new StringWriter();
            rdfWriter.write(_addedTriples, writer,"");
            return writer.getBuffer().toString();
        }
        else {
            SimpleSelector resourceSelector = new SimpleSelector(ResourceFactory.createResource(_resource), null, null, null);
            Model filteredModel = _addedTriples.query(resourceSelector);

            RDFWriter rdfWriter = filteredModel.getWriter("N-TRIPLE");
            StringWriter writer = new StringWriter();
            rdfWriter.write(filteredModel, writer,"");
            return writer.getBuffer().toString();
        }
    }

    /**
     * Create a long string that contains the N-Triples represntation of the deleted triples.
     * @param bListTriplesForResourceOnly   Determines whether to formulate the string with the triples for which the resource is the subject.
     * @return  The N-Triples representation of that deleted triples.
     */
    public String formulateDeletedTriplesAsNTriples(boolean bListTriplesForResourceOnly){
        if(_deletedTriples == null)
            return "";

        /*RDFWriter rdfWriter = _deletedTriples.getWriter("N-TRIPLE");
        StringWriter writer = new StringWriter();
        rdfWriter.write(_deletedTriples, writer,"");
        return writer.getBuffer().toString();*/

        if(!bListTriplesForResourceOnly){
            RDFWriter rdfWriter = _deletedTriples.getWriter("N-TRIPLE");
            StringWriter writer = new StringWriter();
            rdfWriter.write(_deletedTriples, writer,"");
            return writer.getBuffer().toString();
        }
        else {
            SimpleSelector resourceSelector = new SimpleSelector(ResourceFactory.createResource(CoreUtil.encodeURI(_resource)), null, null, null);
            Model filteredModel = _deletedTriples.query(resourceSelector);

            RDFWriter rdfWriter = filteredModel.getWriter("N-TRIPLE");
            StringWriter writer = new StringWriter();
            rdfWriter.write(filteredModel, writer,"");
            return writer.getBuffer().toString();
        }
    }

    /**
     * Create a long string that contains the N-Triples represntation of the modified triples.
     * @param bListTriplesForResourceOnly   Determines whether to formulate the string with the triples for which the resource is the subject.
     * @return  The N-Triples representation of that modified triples.
     */
    public String formulateModifiedTriplesAsNTriples(boolean bListTriplesForResourceOnly){
        if(_modifiedTriples == null)
            return "";
        /*RDFWriter rdfWriter = _modifiedTriples.getWriter("N-TRIPLE");
        StringWriter writer = new StringWriter();
        rdfWriter.write(_modifiedTriples, writer,"");
        return writer.getBuffer().toString();*/

        if(!bListTriplesForResourceOnly){
            RDFWriter rdfWriter = _modifiedTriples.getWriter("N-TRIPLE");
            StringWriter writer = new StringWriter();
            rdfWriter.write(_modifiedTriples, writer,"");
            return writer.getBuffer().toString();
        }
        else {
            SimpleSelector resourceSelector = new SimpleSelector(ResourceFactory.createResource(CoreUtil.encodeURI(_resource)), null, null, null);
            Model filteredModel = _modifiedTriples.query(resourceSelector);

            RDFWriter rdfWriter = filteredModel.getWriter("N-TRIPLE");
            StringWriter writer = new StringWriter();
            rdfWriter.write(filteredModel, writer,"");
            return writer.getBuffer().toString();
        }
    }

}
