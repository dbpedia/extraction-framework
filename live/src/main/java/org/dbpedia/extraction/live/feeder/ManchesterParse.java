package org.dbpedia.extraction.live.feeder;

import java.net.URI;

import com.hp.hpl.jena.shared.PrefixMapping;
import org.apache.commons.collections15.Transformer;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxEditorParser;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.model.*;

import javax.xml.datatype.DatatypeFactory;

public class ManchesterParse {
    //public static final String NS = "http://smi-protege.stanford.edu/ontologies/Parse.owl";

    /*
    public static Transformer<String, IRI> parse(String text, String base, PrefixMapping prefixResolver)
    {
        return new Transformer<String, IRI>() {
            @Override
            public IRI transform(String s) {
                return prefixResolver.expandPrefix(s);
            }
        };
    }*/

    public static OWLClassExpression parse(String text, String base, PrefixMapping prefixResolver)
    {
        try {
/*
        	String text = null;
        	
        	text =
"Pizza that not (hasTopping some (MeatTopping or FishTopping))";
        	//text = "Class1 SubClassOf: not (has_part some Class2)";
*/
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLDataFactory dataFactory = manager.getOWLDataFactory();
            OWLEntityChecker checker = new StupidEntityChecker(manager.getOWLDataFactory(), base, prefixResolver);
            
            ManchesterOWLSyntaxEditorParser parser = new ManchesterOWLSyntaxEditorParser(dataFactory, text);
            parser.setBase(base);
            parser.setOWLEntityChecker(checker);

            return parser.parseClassExpression();
            //OWLAxiom axiom =  parser.parseAxiom();
            //System.out.println("Axiom = " + axiom);
        }
        catch (Throwable t) {
            t.printStackTrace();
        }    	
        
        return null;
    }
    

    private static class StupidEntityChecker implements OWLEntityChecker {
        private OWLDataFactory factory;
        private PrefixMapping resolver;
        
        public StupidEntityChecker(OWLDataFactory factory, String base, PrefixMapping resolver) {
            this.factory = factory;
            this.resolver = resolver;
        }
        


        public OWLClass getOWLClass(String name) {
        	String prefix = "";
        	String className = name;
        	String parts[] = name.split(":", 2);
        	if(parts.length == 2) {
        		prefix = parts[0].trim();
        		className = parts[1].trim();
        	}
        	
            if (prefix.equals("xsd") || Character.isUpperCase(className.toCharArray()[0])) {
            	String expanded = resolver.expandPrefix(name);
                IRI iri = IRI.create(expanded);
            	if(iri != null) {
                	return factory.getOWLClass(iri);
            	}
            }
            
            return null;
        }
        
        public OWLObjectProperty getOWLObjectProperty(String name) {
        	String prefix = "";
        	String className = name;
        	String parts[] = name.split(":", 2);
        	if(parts.length == 2) {
        		prefix = parts[0].trim();
        		className = parts[1].trim();
        	}
        	
            if (!prefix.equals("xsd") && !Character.isUpperCase(className.toCharArray()[0])) {
                String expanded = resolver.expandPrefix(name);
                IRI iri = IRI.create(expanded);
            	if(iri != null) {
                	return factory.getOWLObjectProperty(iri);
            	}
            }
            
            return null;
        }
        
        public OWLAnnotationProperty getOWLAnnotationProperty(String name) {
            return null;
        }

        public OWLDataProperty getOWLDataProperty(String name) {
            return null;
        }

        public OWLDatatype getOWLDatatype(String name) {
            return null;
        }

        public OWLNamedIndividual getOWLIndividual(String name) {
            return null;
        }
        
    }
}