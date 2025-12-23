package org.dbpedia.extraction.live.util.iterators;

import org.apache.commons.collections15.Transformer;

import java.util.Iterator;


/**
 * An iterator which allows iterating over items transformed to
 * either collections or iterators.
 * <p/>
 * If the transformer transforms to a collection, the given transformer is
 * automatically wrapped by a transformer to an iterator
 *
 * @param <I>
 * @param <O>
 * @author raven
 */
public class TransformChainIterator<I, O>
        extends PrefetchIterator<O> {
    private Iterator<I> iterator;
    private Transformer<I, Iterator<O>> transformer;


    public TransformChainIterator(Iterator<I> iterator, Transformer<I, Iterator<O>> transformer) {
        this.iterator = iterator;
        this.transformer = transformer;
    }


    @Override
    protected Iterator<O> prefetch()
            throws Exception {
        while (iterator.hasNext()) {
            Iterator<O> result = transformer.transform(iterator.next());

            if (result == null)
                continue;

            return result;
        }

        return null;
    }
}
