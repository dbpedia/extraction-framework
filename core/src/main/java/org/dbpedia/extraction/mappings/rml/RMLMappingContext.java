package org.dbpedia.extraction.mappings.rml;

import be.ugent.mmlab.rml.model.RMLMapping;
import org.dbpedia.extraction.mappings.Redirects;
import org.dbpedia.extraction.ontology.Ontology;
import org.dbpedia.extraction.util.Language;

/**
 * Contains the configuration of a mapping
 */
public class RMLMappingContext {

    private Ontology ontology;
    private Language language;
    private Redirects redirects;
    private RMLMapping mappingDocument;

    public RMLMappingContext(Ontology ontology, Language language, Redirects redirects, RMLMapping mappingDocument) {
        this.ontology = ontology;
        this.language = language;
        this.redirects = redirects;
        this.mappingDocument = mappingDocument;
    }

    public Ontology getOntology() {
        return ontology;
    }

    public Language getLanguage() {
        return language;
    }

    public Redirects getRedirects() {
        return redirects;
    }

    public RMLMapping getMappingDocument() {
        return mappingDocument;
    }
}
