package org.dbpedia.extraction.live.priority;

import org.dbpedia.extraction.live.queue.LiveQueuePriority;

/**
 * Created by IntelliJ IDEA. User: Mohamed Morsey Date: Jul 28, 2010 Time:
 * 5:00:34 PM This class represents the priority of the page, because the page
 * IDs that are extracted through live extraction have higher priority than the
 * page IDs that are extracted through mapping change, and also using Unmodified
 * feeder. Basically we use priority 0 for live, 1 for mapping change, and 2 for
 * unmodified pages
 */
public class PagePriority implements Comparable<PagePriority> {

	public long pageID;
	public LiveQueuePriority pagePriority;
	public String pageTimestamp;

	public PagePriority(long pageid, LiveQueuePriority priority, String timestamp) {
		pageID = pageid;
		pagePriority = priority;
		pageTimestamp = timestamp;
	}

	public PagePriority(long pageid, LiveQueuePriority priority) {
		this(pageid, priority, "");
	}

	// Compare the page priorities and when equal use the timestamps
	// the one with the least timestamp will be processed first.
	public int compareTo(PagePriority page) {
		if (this.pagePriority != page.pagePriority)
			return this.pagePriority.compareTo(page.pagePriority);
		else {
			if (this.pageTimestamp == "" || page.pageTimestamp == "")
				return 0;
			else
				// String compare should do for this (and it's thread safe)
				return this.pageTimestamp.compareTo(page.pageTimestamp);
		}
	}

	public String toString() {
		return "Page ID = " + this.pageID + ", its priority = " + pagePriority
				+ ", and its timestamp = " + pageTimestamp;
	}
}
