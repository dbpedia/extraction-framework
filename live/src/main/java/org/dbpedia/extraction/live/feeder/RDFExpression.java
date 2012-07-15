package org.dbpedia.extraction.live.feeder;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

import java.util.Set;


public class RDFExpression
{
	private Resource rootSubject;
	private Model triples;

	public RDFExpression(Resource rootSubject, Model triples)
	{
		this.rootSubject = rootSubject;
		this.triples = triples;
	}
	
	public Resource getRootSubject()
	{
		return rootSubject;
	}
	
	public Model getTriples()
	{
		return triples;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RDFExpression that = (RDFExpression) o;

        if (rootSubject != null ? !rootSubject.equals(that.rootSubject) : that.rootSubject != null) return false;
        if (triples != null ? !triples.equals(that.triples) : that.triples != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = rootSubject != null ? rootSubject.hashCode() : 0;
        result = 31 * result + (triples != null ? triples.hashCode() : 0);
        return result;
    }

    /*
	@Override
	public int hashCode()
	{
		return
			EqualsUtil.hashCode(rootSubject) +
			31 * EqualsUtil.hashCode(triples);
	}
	
	@Override
	public boolean equals(Object other)
	{
		if(!(other instanceof RDFExpression))
			return false;
		
		RDFExpression o = (RDFExpression)other;
		
		return
			EqualsHelper.equals(this.getRootSubject(), o.getRootSubject()) &&
			EqualsHelper.equals(this.getTriples(), o.getTriples());
	}
	
	@Override 
	public String toString()
	{
		return
			"root = " + rootSubject + " " +
			"rest = " + triples;
	}
	*/
}