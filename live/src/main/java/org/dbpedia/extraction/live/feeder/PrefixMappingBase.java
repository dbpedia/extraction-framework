package org.dbpedia.extraction.live.feeder;

import com.hp.hpl.jena.shared.PrefixMapping;

/**
 * @author Claus Stadler
 *         <p/>
 *         Date: 9/15/11
 *         Time: 12:40 PM
 */
public class PrefixMappingBase
    extends PrefixMappingDecorator
{
    private String base;

    protected PrefixMappingBase(PrefixMapping decoratee, String base) {
        super(decoratee);
        this.base = base;
    }

    @Override
    public String expandPrefix(String prefixed) {
        return !prefixed.contains(":")
            ? base + prefixed
            : super.expandPrefix(prefixed);
    }
}
