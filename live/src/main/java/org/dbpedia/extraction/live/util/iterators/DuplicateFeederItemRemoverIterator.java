package org.dbpedia.extraction.live.util.iterators;

import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.feeder.FeederItem;
import org.dbpedia.extraction.live.util.OAIUtil;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Dimitris Kontokostas
 * Date: 11/20/12
 * Time: 11:58 AM
 * <p/>
 * Skips successive (concerning timestamp) duplicate identifiers. In this
 * example the second '123' item will be skipped. Fri Jan 02 14:50:11 CET 2009:
 * oai:en.wikipedia.org:enwiki:123 Fri Jan 02 14:50:11 CET 2009:
 * oai:en.wikipedia.org:enwiki:456 Fri Jan 02 14:50:11 CET 2009:
 * oai:en.wikipedia.org:enwiki:123
 */
public class DuplicateFeederItemRemoverIterator
        extends PrefetchIterator<FeederItem> {
    private Logger logger = Logger
            .getLogger(DuplicateOAIRecordRemoverIterator.class);

    private Iterator<FeederItem> iterator;
    private Date currentTimestamp = new Date(0L);
    private Set<Long> currentIdentifiers = new HashSet<Long>();
    private DateFormat dateFormat = new SimpleDateFormat(
            OAIUtil.getOAIDateFormatString());

    public DuplicateFeederItemRemoverIterator(Iterator<FeederItem> iterator) {
        this.iterator = iterator;
    }

    @Override
    protected Iterator<FeederItem> prefetch()
            throws Exception {
        while (iterator.hasNext()) {
            FeederItem item = iterator.next();

            Date timestamp = dateFormat.parse(item.getModificationDate());

            if (timestamp.after(currentTimestamp)) {
                currentTimestamp = timestamp;
                currentIdentifiers.clear();
            }

            if (currentIdentifiers.contains(item.getItemID())) {
                logger.debug("Skipping duplicate: " + timestamp + ": "
                        + item.getItemID());
                continue;
            }

            currentIdentifiers.add(item.getItemID());

            return Collections.singleton(item).iterator();
        }

        return null;
    }

}
