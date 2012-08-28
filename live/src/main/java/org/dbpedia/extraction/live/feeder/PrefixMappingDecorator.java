package org.dbpedia.extraction.live.feeder;

import com.hp.hpl.jena.shared.PrefixMapping;

import java.util.Map;

/**
 * @author Claus Stadler
 *         <p/>
 *         Date: 9/15/11
 *         Time: 12:39 PM
 */
public class PrefixMappingDecorator
    implements PrefixMapping
{
    private PrefixMapping decoratee;

    protected PrefixMappingDecorator(PrefixMapping decoratee) {
        this.decoratee = decoratee;
    }

    @Override
    public PrefixMapping setNsPrefix(String prefix, String uri) {
        return decoratee.setNsPrefix(prefix, uri);
    }

    @Override
    public PrefixMapping removeNsPrefix(String prefix) {
        return decoratee.removeNsPrefix(prefix);
    }

    @Override
    public PrefixMapping setNsPrefixes(PrefixMapping other) {
        return decoratee.setNsPrefixes(other);
    }

    @Override
    public PrefixMapping setNsPrefixes(Map<String, String> map) {
        return decoratee.setNsPrefixes(map);
    }

    @Override
    public PrefixMapping withDefaultMappings(PrefixMapping map) {
        return decoratee.withDefaultMappings(map);
    }

    @Override
    public String getNsPrefixURI(String prefix) {
        return decoratee.getNsPrefixURI(prefix);
    }

    @Override
    public String getNsURIPrefix(String uri) {
        return decoratee.getNsURIPrefix(uri);
    }

    @Override
    public Map<String, String> getNsPrefixMap() {
        return decoratee.getNsPrefixMap();
    }

    @Override
    public String expandPrefix(String prefixed) {
        return decoratee.expandPrefix(prefixed);
    }

    @Override
    public String shortForm(String uri) {
        return decoratee.shortForm(uri);
    }

    @Override
    public String qnameFor(String uri) {
        return decoratee.qnameFor(uri);
    }

    @Override
    public PrefixMapping lock() {
        return decoratee.lock();
    }

    @Override
    public boolean samePrefixMappingAs(PrefixMapping other) {
        return decoratee.samePrefixMappingAs(other);
    }
}
