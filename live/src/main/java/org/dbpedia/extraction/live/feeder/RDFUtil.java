package org.dbpedia.extraction.live.feeder;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.rdf.model.impl.StatementImpl;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import org.aksw.commons.jena_owlapi.Conversion;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.coode.owlapi.rdf.model.RDFGraph;
import org.coode.owlapi.rdf.model.RDFTranslator;
import org.dbpedia.extraction.util.StringUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class RDFUtil
{
	private static Logger logger = Logger.getLogger(RDFUtil.class);

	
	private static MessageDigest md5 = null;

	static {
		try{
			md5 = MessageDigest.getInstance("MD5");
		}catch(Exception e) {
			logger.fatal(ExceptionUtils.getStackTrace(e));
			throw new RuntimeException(e);
		}
	}

	public static URI generateMD5HashUri(String prefix, Statement triple)
	{
		return URI.create(prefix + generateMD5(triple));
	}

	public static String generateMD5(Statement triple)
	{
		String str = triple.getSubject().toString() + " "
				+ triple.getPredicate().toString() + " "
				+ triple.getObject().toString();

		return generateMD5(str);
	}

	public static String generateMD5(String str)
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
	
	private static Model stripTypeClass(Model result)
	{
        result.remove(result.listStatements(null, RDF.type, OWL.Class).toList());

		return result;
	}
	
	/**
	 */
	public static RDFExpression interpretMos(String text,
			PrefixMapping prefixResolver, String innerPrefix, boolean generateTypeClass)
		throws Exception
	{
		RDFExpression e = parseManchesterOWLClassExpression(text, prefixResolver, generateTypeClass);
		
		// Replace anonymous nodes with resources generated from their hash
		Map<RDFNode, Resource> map = relabelBlankNodes(e.getTriples(), innerPrefix);

		// Relabel nodes like cyc:...
		// An exception is thrown if relabelling fails
		// e.g. if myCostumPrefix:birthPlace cannot be resolved

		// Resolve the root subject
		resolve(e.getRootSubject(), prefixResolver, map);

		// And all triples
		map.putAll(resolve(e.getTriples(), prefixResolver));

		Model triples = relableStatements(e.getTriples(), map);

		Resource rootNode = map.get(e.getRootSubject());
		if (rootNode == null)
			rootNode = e.getRootSubject();

		

		// Return if there is no root
		RDFExpression result = rootNode == null
			? null
			: new RDFExpression(rootNode, triples);
		

		String msg = "Statements generated for MOS-Expression '" + text + "':\n";
		
		if(result == null)
			logger.trace(msg + "None");
		else {
			msg += "\tRoot subject = " + rootNode + "\n";
			for(Statement item : triples.listStatements().toSet())
				msg += "\t" + item.toString() + "\n";
			
			logger.trace(msg);
		}
		
		return result;
	}

	private static Map<RDFNode, Resource> resolve(
			Model triples, PrefixMapping resolver)
		throws UnsupportedEncodingException
	{
		Map<RDFNode, Resource> result = new HashMap<RDFNode, Resource>();

		for (Statement triple : triples.listStatements().toSet())
			resolve(triple, resolver, result);

		return result;
	}

	private static void resolve(Statement triple, PrefixMapping resolver,
			Map<RDFNode, Resource> map)
		throws UnsupportedEncodingException
	{
		resolve(triple.getSubject(), resolver, map);
		resolve(triple.getPredicate(), resolver, map);
		resolve(triple.getObject(), resolver, map);
	}

	private static void resolve(RDFNode node, PrefixMapping resolver,
			Map<RDFNode, Resource> map)
		throws UnsupportedEncodingException
	{
		if (node.isAnon() || node.isLiteral())
			return;

		String value = node.toString();

		if (!value.startsWith("http://relabel.me/"))
			return;

		value = value.substring("http://relabel.me/".length());
		// value = URLEncoder.encode(value, "UTF-8");

        String expanded = resolver.expandPrefix(value);
		Resource uri = ResourceFactory.createResource(expanded);

		if (uri == null)
			throw new RuntimeException("'" + value
					+ "' didn't resolve to an uri.");

		Resource mapped = map.get(node);
		if (mapped == null) {
            map.put(node, uri);
        }
	}


	private static Model resolve2(Model triples,
			Transformer<String, Resource> resolver)
	{
		Model result = ModelFactory.createDefaultModel();

		for (Statement triple : triples.listStatements().toSet()) {
			result.add(resolve2(triple, resolver));
        }

		return result;
	}

	private static Statement resolve2(Statement triple, Transformer<String, Resource> resolver)
	{
		Resource s = (Resource) resolve2(triple.getSubject(),
				resolver);

		Property p = (Property) resolve2(triple.getPredicate(),
				resolver);

		RDFNode o = resolve2(triple.getObject(), resolver);

		if (triple.getSubject() == s && triple.getPredicate() == p
				&& triple.getObject() == o)
			return triple;

		return new StatementImpl(s, p, o);
	}

	private static RDFNode resolve2(RDFNode node, Transformer<String, Resource> resolver)
	{
		if (node.isAnon() || node.isLiteral())
			return node;

		//Resource n = (Resource) node;

		String value = node.toString();

		if (!value.startsWith("http://relabel.me/"))
			return node;

		value = value.substring("http://relabel.me/".length());
		Resource uri = resolver.transform(value);

		if (uri == null)
			throw new NullPointerException();

		return uri;
	}

    // Returns all subjects that do not appear as objects
    public static Set<Resource> findRoots(Model model) {
        Set<Resource> result = model.listSubjects().toSet();
        result.removeAll(model.listObjects().toSet());

        return result;
    }

	private static RDFExpression parseManchesterOWLClassExpression(String text, PrefixMapping prefixResolver, boolean generateTypeClass)
		throws Exception
	{
		String base = "http://relabel.me/";

		OWLOntologyManager m = OWLManager.createOWLOntologyManager();
		OWLOntology o = m.createOntology(IRI
                //.create("http://relabel.me"));
                .create("http://this_should_not_show_up_anywhere.org"));

		OWLClassExpression classExpression = ManchesterParse.parse(text, base, prefixResolver);
		
		RDFTranslator t = new RDFTranslator(m, o, false);
		classExpression.accept(t);


		RDFGraph rdfGraph = t.getGraph();


        String str = Conversion.toStringNTriples(rdfGraph);
        Model result = ModelFactory.createDefaultModel();

        logger.info("-----");
        logger.info(str);
        logger.info("-----");

        //System.exit(0);
        result.read(new ByteArrayInputStream(str.getBytes()), null, "N-TRIPLE");


		//Set<RDFResourceNode> rootNodes = rdfGraph.getRootAnonymousNodes();
        Set<Resource> rootNodes = findRoots(result);

        // If the graph is empty, our text will become the root resource
		Resource rootSubject = null;
		if (rootNodes.size() == 0) {
			rootSubject = ResourceFactory.createResource(prefixResolver.expandPrefix(text.trim()));
		}
		else if (rootNodes.size() == 1) {
            rootSubject = rootNodes.iterator().next();

            //result.add(rootSubject, RDF.type, rootSubject);
            //result.write(System.out, "N-TRIPLE");

			//rootSubject = ResourceFactory.createResource(base + rootNodes.iterator().next().toString());
        }
		else {
			throw new RuntimeException("Unexpected multiple root nodes");
        }

		if(!generateTypeClass) {
			result = stripTypeClass(result);
        }
		
		return new RDFExpression(rootSubject, result);
	}

	// public void replaceResource(Resource orig, Resource


	public static Model relableStatements(Model model,
			Map<RDFNode, Resource> map)
	{
		Model result = ModelFactory.createDefaultModel();
		for (Statement triple : model.listStatements().toSet())
			result.add(relableStatement(triple, map));

		return result;
	}

	public static Statement relableStatement(Statement triple,
			Map<RDFNode, Resource> map)
	{
		boolean changed = false;

		Resource s = map.get(triple.getSubject());
		if (s == null) {
			s = triple.getSubject();
			changed = true;
		}

        // TODO Hack, but shouldn't fail too often
		Property p = (Property)map.get(triple.getPredicate());
		if (p == null) {
			p = triple.getPredicate();
			changed = true;
		}

		RDFNode o = map.get(triple.getObject());
		if (o == null) {
			o = triple.getObject();
			changed = true;
		}

		if (changed)
			return new StatementImpl(s, p, o);

		return triple;
	}

	/**
	 * relabels all blank nodes with unique ids. Does nothing if prefix is null.
	 * 
	 */
	public static Map<RDFNode, Resource> relabelBlankNodes(
			Model triples, String prefix)
	{
		Map<RDFNode, Resource> result = new HashMap<RDFNode, Resource>();

		if (prefix == null)
			return result;

		for (Statement triple : triples.listStatements().toSet()) {
			relabelBlankNode(triple.getSubject(), prefix, result);
			relabelBlankNode(triple.getPredicate(), prefix, result);
			relabelBlankNode(triple.getObject(), prefix, result);
		}

		return result;
	}

	// Turns blank nodes into resources
	public static Resource relabelBlankNode(RDFNode node, String prefix,
                                            Map<RDFNode, Resource> oldToNew)
	{
		if (!node.isAnon()) {
			return null;
        }

        String id = StringUtils.md5sum(node.toString());
		Resource newId = oldToNew.get(id);

		if (newId == null) {
			newId = ResourceFactory.createResource(prefix + id);
			oldToNew.put(node, newId);
		}

		return newId;
	}

}
