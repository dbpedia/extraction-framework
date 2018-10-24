package org.dbpedia.extraction.live.feeder;

import java.util.ArrayList;
import java.util.Collection;
import org.dbpedia.extraction.live.queue.LiveQueueItem;
import org.dbpedia.extraction.live.queue.LiveQueuePriority;


public class EventStreamsFeeder extends Feeder{


  public EventStreamsFeeder(String feederName,
      LiveQueuePriority queuePriority,
      String defaultStartTime, String folderBasePath) {
    super(feederName, queuePriority, defaultStartTime, folderBasePath);
  }

  @Override
  protected void initFeeder() {

  }

  @Override
  protected Collection<LiveQueueItem> getNextItems() {
    ArrayList<LiveQueueItem> queue = new ArrayList<>();
    return queue;
  }
}
