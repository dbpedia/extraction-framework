package org.dbpedia.extraction.live.util.collections;


import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.Iterator;


import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.util.ExceptionUtil;
import org.dbpedia.extraction.live.util.iterators.PrefetchIterator;



/**
 * Small issue on the iterators: It somehow happend that there was no resumption
 * token and 0 nodes were matched - despite that being harmless it shouldn't
 * happen
 * 
 * 
 * @author raven
 * 
 */

public class PersistentQueueIterator
	extends PrefetchIterator<Object>
{
	private static Logger	logger	= Logger
											.getLogger(PersistentQueueIterator.class);

	private PersistentQueue	queue;
	private int				startIndex;

	PersistentQueueIterator(PersistentQueue queue)
	{
		this.queue = queue;
		this.startIndex = queue.getStartIndex();
	}

	@Override
	protected Iterator<Object> prefetch()
	{
		try {
			if (startIndex == queue.getEndIndex())
				return null;

			RandomAccessFile file = queue.getFile();
			file.seek(startIndex);
			int length = file.readInt();

			byte[] buffer = new byte[length];
			file.read(buffer, 0, length);

			ObjectInputStream ois = new ObjectInputStream(
					new ByteArrayInputStream(buffer));
			Object result = ois.readObject();

			startIndex += length + 4;

			return Collections.singleton(result).iterator();
		}
		catch (Exception e) {
			logger.warn(ExceptionUtil.toString(e));
		}
		return null;
	}

}
