package org.dbpedia.extraction.live.feeder;

import com.hp.hpl.jena.rdf.model.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.util.PrefixMapping2;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.util.StringUtil;

/**
 * Interface for objects which generate triples
 * 
 * @author raven
 * 
 */
interface ITripleGenerator
{
	Model generate(Model result, Resource subject, Property property, String value, String lang)
		throws Exception;

	// public void setExprPrefix(String prefix);
}

class TripleGeneratorDecorator
	implements ITripleGenerator
{
	protected ITripleGenerator	tripleGenerator;

	public TripleGeneratorDecorator(ITripleGenerator tripleGenerator)
	{
		this.tripleGenerator = tripleGenerator;
	}

	/*
	 * @Override public void setExprPrefix(String prefix) {
	 * tripleGenerator.setExprPrefix(prefix); }
	 */

	@Override
	public Model generate(Model result, Resource subject, Property property, String value,
			String lang)
		throws Exception
	{
		return tripleGenerator.generate(result, subject, property, value, lang);
	}

}

/**
 * Splits the value by a given separator and passes each fragment to the given
 * generator and collects the partial results
 * 
 * @author raven
 * 
 */
class ListTripleGenerator
	// implements ITripleGenerator
	extends TripleGeneratorDecorator
{
	private String	separator;
	// private ITripleGenerator generator;

	private Logger	logger	= Logger.getLogger(ListTripleGenerator.class);

	public ListTripleGenerator(String separator,
			ITripleGenerator tripleGenerator)
	{
		super(tripleGenerator);
		this.separator = separator;
		// this.tripleGenerator = tripleGenerator;
	}

	@Override
	public Model generate(Model result, Resource subject, Property property, String value,
			String lang)
		throws Exception
	{
		//Model result = new HashModel();

		for (String part : value.split(separator)) {
			try {
				tripleGenerator.generate(result, subject, property, part, lang);
			}
			catch (Exception e) {
				logger.warn(ExceptionUtils.getStackTrace(e));
			}
		}

		return result;
	}
}

class LiteralTripleGenerator
	implements ITripleGenerator
{
	@Override
	public Model generate(Model result, Resource subject, Property property, String value,
			String lang)
	{
		// Ignore empty triples
		value = value.trim();
		if (value.isEmpty())
			return result;

        result.add(subject, property, result.createLiteral(value, lang));
        return result;
	}
}

/**Since the templates are changed and the format of listing the labels are changed as well, a new generator is required
 * They now has the form of
 *   {{label|el|Πληροφορίες προσώπου}}
 {{label|en|person}}
 {{label|de|Person}}
 {{label|sl|Oseba}}
 {{label|pt|pessoa}}
 {{label|fr|personne}}
 {{label|es|persona}}
 {{label|ja|人_(法律)}}
 {{label|nl|persoon}}
 * */

class LabelsTripleGenerator implements ITripleGenerator{
    private static Logger logger = Logger.getLogger(LabelsTripleGenerator.class);
    @Override
    public Model generate(Model result, Resource subject, Property property, String value,
                          String lang)
    {
        // Ignore empty triples
        value = value.trim();
        if (value.isEmpty())
            return result;

        //The labels are separated with a new line
        String []labels = value.split("\n");
        for(String lblForLanguage:labels){

            //The label is like "{{label|en|person}}", so we should remove braces and split with '|'
            String labelParts [] = lblForLanguage.replace("{","").replace("}","").split("\\|");
            String labelLanguage = labelParts[1];
            String labelValue = labelParts[2];
            logger.info("Label = " + labelValue);
            result.add(subject, ResourceFactory.createProperty("http://www.w3.org/2000/01/rdf-schema#label"), result.createLiteral(labelValue, labelLanguage));
        }

//        result.add(subject, property, result.createLiteral(value, lang));
        return result;
    }
}

/**
 * Similar to LabelsTripleGenerator but for comments
 * */

class CommentsTripleGenerator implements ITripleGenerator{
    private static Logger logger = Logger.getLogger(LabelsTripleGenerator.class);
    @Override
    public Model generate(Model result, Resource subject, Property property, String value,
                          String lang)
    {
        // Ignore empty triples
        value = value.trim();
        if (value.isEmpty())
            return result;

        //The labels are separated with a new line
        String []labels = value.split("\n");
        for(String lblForLanguage:labels){

            //The label is like "{{label|en|person}}", so we should remove braces and split with '|'
            String labelParts [] = lblForLanguage.replace("{","").replace("}","").split("\\|");
            String labelLanguage = labelParts[1];
            String labelValue = labelParts[2];
            logger.info("comment = " + labelValue);
            result.add(subject, ResourceFactory.createProperty("http://www.w3.org/2000/01/rdf-schema#comment"), result.createLiteral(labelValue, labelLanguage));
        }

//        result.add(subject, property, result.createLiteral(value, lang));
        return result;
    }
}

class StringReference
{
	private String	value;

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}
}

/*
 * class PrefixedStringTransformer implements Transformer<String, String> {
 * private String prefix;
 * 
 * public String getPrefix() { return prefix; }
 * 
 * public void setPrefix(String prefix) { this.prefix = prefix; }
 * 
 * @Override public String transform(String value) { return prefix + value; } }
 */

class MosTripleGenerator
	implements ITripleGenerator
{
	private static Logger	logger	= Logger
											.getLogger(MosTripleGenerator.class);

	private StringReference	exprPrefixRef;
	private PrefixMapping	prefixResolver;

	public MosTripleGenerator(StringReference exprPrefixRef,
			PrefixMapping prefixResolver)
	{
		this.exprPrefixRef = exprPrefixRef;
		this.prefixResolver = prefixResolver;
	}

	/*
	 * public void setExprPrefix(String exprPrefix) { this.exprPrefix =
	 * exprPrefix; }
	 */

	@Override
	public Model generate(Model result, Resource subject, Property property, String value,
			String lang)
		throws Exception
	{
		value = value.trim();
		if (value.isEmpty()) {
			return result;
        }


		try {
			RDFExpression expr = RDFUtil.interpretMos(value, prefixResolver,
					exprPrefixRef.getValue(), false);

			// if for some reason (is there one?) the rootSuject is null
			// return no triple
			if (expr.getRootSubject() == null) {
				return result;
            }

			Resource o = expr.getRootSubject();

			result.add(expr.getTriples());
			result.add(subject, property, o);
		}
		catch (Exception e) {
			String msg = "Errornous expression for predicate '"
					+ property.toString() + "': "
					+ StringUtil.cropString(value.trim(), 50, 20);

			result.add(subject, MyVocabulary.DBM_ERROR, result.createLiteral(msg));

			logger.warn(ExceptionUtils.getStackTrace(e));
		}

		return result;
	}
}

/**
 * Predicate and object are 'static'. Only the supplied subject is used for
 * triple generation.
 * 
 * @author raven
 * 
 */
class StaticTripleGenerator
	implements ITripleGenerator
{
	private Property predicate;
	private RDFNode object;

	public StaticTripleGenerator(Property predicate, RDFNode object)
	{
		this.predicate = predicate;
		this.object = object;
	}

	@Override
	public Model generate(Model result, Resource subject, Property property, String value,
			String lang)
		throws Exception
	{

        result.add(subject, predicate, object);

        return result;
	}
}

/**
 * A triple generator that first delegates generation request to the
 * mainGenerator. If no triples are generated by it, the request is delegated to
 * the alternativeGenerator.
 * 
 * @author raven
 * 
 */
class AlternativeTripleGenerator
	implements ITripleGenerator
{
	private ITripleGenerator	mainGenerator;
	private ITripleGenerator	alternativeGenerator;

	public AlternativeTripleGenerator(ITripleGenerator firstGenerator,
			ITripleGenerator alternativeGenerator)
	{
		this.mainGenerator = firstGenerator;
		this.alternativeGenerator = alternativeGenerator;
	}

	@Override
	public Model generate(Model result, Resource subject, Property property, String value,
			String lang)
		throws Exception
	{
        Model tmp = ModelFactory.createDefaultModel();

		mainGenerator.generate(tmp, subject, property, value, lang);

		if (tmp.isEmpty()) {
		    alternativeGenerator.generate(tmp, subject, property, value,
					lang);
        }

        result.add(tmp);
        return result;
	}
}

/**
 * This is the actual extractor (PropDefExtractor is just the handler)
 * 
 * @author raven
 * 
 * /
class TBoxTripleGenerator
{
	// TODO Make that configurable
	// private static final String DEFAULT_LANGUAGE = "en";

	/*
	private static final String				OBJECT_PROPERTY		= "DBpedia_ObjectProperty";
	private static final String				DATATYPE_PROPERTY	= "DBpedia_DatatypeProperty";
	private static final String				CLASS				= "DBpedia_Class";
	 *
	private static final String				OBJECT_PROPERTY		= "ObjectProperty";
	private static final String				DATATYPE_PROPERTY	= "DatatypeProperty";
	private static final String				CLASS				= "Class";
	
	
	private Logger							logger				= Logger
																		.getLogger(TBoxTripleGenerator.class);

	private StringReference					exprPrefixRef		= new StringReference();
	private PrefixMapping prefixResolver;

	// NOTE: This extractor uri is NOT THIS CLASS' name!!!
	/*
	 * private static final Resource extractorUri = Resource .create(DBM +
	 * PropertyDefinitionExtractor.class .getSimpleName());
	 * /

	// mapping for class definition attributes to triple generators
	private Map<String, ITripleGenerator>	classToGenerator	= new HashMap<String, ITripleGenerator>();

	// Default values for class (will be used if the parameter is not present)
	private Map<String, ITripleGenerator>	classDefaults		= new HashMap<String, ITripleGenerator>();

	private Map<String, ITripleGenerator>	propertyToGenerator	= new HashMap<String, ITripleGenerator>();

	private Map<String, ITripleGenerator>	objectDefaults		= new HashMap<String, ITripleGenerator>();

	private Map<String, ITripleGenerator>	dataToGenerator		= new HashMap<String, ITripleGenerator>();

	private Map<String, ITripleGenerator>	dataDefaults		= new HashMap<String, ITripleGenerator>();

	public void setExprPrefix(String value)
	{
		exprPrefixRef.setValue(value);
	}

	public TBoxTripleGenerator(
	// StringReference exprPrefixRef,
			PrefixMapping prefixResolver)
	{
		// this.exprPrefix = exprPrefix;
		this.prefixResolver = prefixResolver;

		ITripleGenerator literalTripleGenerator = new LiteralTripleGenerator();

		ITripleGenerator mosListTripleGenerator = new ListTripleGenerator(",",
				new MosTripleGenerator(exprPrefixRef, prefixResolver));

		ITripleGenerator fallbackSubClassGenerator = new StaticTripleGenerator(RDFS.subClassOf, OWL.Thing);

		/*
		 * ITripleGenerator subClassTripleGenerator = new
		 * AlternativeTripleGenerator( mosListTripleGenerator,
		 * fallbackSubClassGenerator);
		 * /

		classToGenerator.put("rdfs:label", literalTripleGenerator);
		classToGenerator.put("rdfs:comment", literalTripleGenerator);
		classToGenerator.put("owl:equivalentClass", mosListTripleGenerator);
		classToGenerator.put("owl:disjointWith", mosListTripleGenerator);
		classToGenerator.put("rdfs:seeAlso", mosListTripleGenerator);
		classToGenerator.put("rdfs:subClassOf", mosListTripleGenerator);// )subClassTripleGenerator);

		classDefaults.put("rdfs:subClassOf", fallbackSubClassGenerator);

		propertyToGenerator.put("rdfs:label", literalTripleGenerator);
		propertyToGenerator.put("rdfs:comment", literalTripleGenerator);
		propertyToGenerator.put("owl:equivalentProperty",
				mosListTripleGenerator);
		propertyToGenerator.put("rdfs:seeAlso", mosListTripleGenerator);
		propertyToGenerator.put("rdfs:subPropertyOf", mosListTripleGenerator);
		propertyToGenerator.put("rdfs:domain", mosListTripleGenerator);
		propertyToGenerator.put("rdfs:range", mosListTripleGenerator);
		propertyToGenerator.put("rdf:type", mosListTripleGenerator);

		dataToGenerator.put("rdfs:label", literalTripleGenerator);
		dataToGenerator.put("rdfs:comment", literalTripleGenerator);
		dataToGenerator.put("owl:equivalentProperty", mosListTripleGenerator);
		dataToGenerator.put("rdfs:seeAlso", mosListTripleGenerator);
		dataToGenerator.put("rdfs:subPropertyOf", mosListTripleGenerator);
		dataToGenerator.put("rdfs:domain", mosListTripleGenerator);
		dataToGenerator.put("rdfs:range", mosListTripleGenerator);
		dataToGenerator.put("rdf:type", mosListTripleGenerator);
	}

}
*/