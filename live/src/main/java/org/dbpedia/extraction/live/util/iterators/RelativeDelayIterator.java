package org.dbpedia.extraction.live.util.iterators;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.dbpedia.extraction.live.util.ExceptionUtil;
import org.slf4j.LoggerFactory;

import java.util.Iterator;


public class RelativeDelayIterator<T>
        implements Iterator<T> {
    private static Logger logger = LoggerFactory.getLogger(RelativeDelayIterator.class);
    private Iterator<T> iterator;

    private StopWatch stopWatch;
    private int delay;

    public RelativeDelayIterator(Iterator<T> iterator, int delay) {
        this.iterator = iterator;
        this.delay = delay;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public T next() {
        if (stopWatch == null) {
            stopWatch = new StopWatch();
        } else {
            stopWatch.stop();

            long delta = delay - stopWatch.getTime();
            if (delta > 0) {
                try {
                    logger.info("Waiting " + delta + " ms");
                    Thread.sleep(delta, 0);
                } catch (Exception e) {
                    logger.warn(ExceptionUtil.toString(e));
                }
            }

            stopWatch.reset();
        }

        stopWatch.start();
        T result = iterator.next();

        return result;
    }

    @Override
    public void remove() {
        iterator.remove();
    }
}
