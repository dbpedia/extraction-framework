package org.dbpedia.extraction.live.util.collections;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.Iterator;

/**
 * Currently this is just a list - but it could be extended to a queue
 * 
 * 
 * A persistent ArrayQueue implementation. Each object is preceeded by 4 bytes
 * specifying the length of the object [length][object][length][object]...
 * 
 * 
 * @author raven
 * 
 */
public class PersistentQueue
{
	private int					startIndex	= 0;
	private int					endIndex;

	// private int size;
	private int					objectCount;		// number of objects in the
	// queue

	private RandomAccessFile	file;

	int getStartIndex()
	{
		return startIndex;
	}

	int getEndIndex()
	{
		return endIndex;
	}

	public int getObjectCount()
	{
		return objectCount;
	}

	RandomAccessFile getFile()
	{
		return file;
	}

	public PersistentQueue(String filename)
		throws Exception
	{
		File f = new File(filename);
		if (f.exists()) {
			file = new RandomAccessFile(filename, "rw");
			readHeader();
		}
		else {
			file = new RandomAccessFile(filename, "rw");
			writeHeader();
		}
	}

	private final String	magic	= "http://dbpedia.org/persistent-array-queue/alpha";

	private void readHeader()
		throws IOException
	{
		file.seek(0);
		// byte[] bufMagic = new byte[52];
		// file.read(bufMagic);

		// String m = new String(bufMagic);
		String m = file.readUTF();

		System.out.println(m.length());
		System.out.println(magic.length());

		if (!m.equals(magic)) {
			throw new RuntimeException("Unsupported magic value");
		}

		file.seek(52);

		startIndex = file.readInt();
		endIndex = file.readInt();
		objectCount = file.readInt();
	}

	private void writeHeader()
		throws IOException
	{
		file.seek(0);
		// file.write(magic.getBytes());
		file.writeUTF(magic);
		setStartIndex(64);
		setEndIndex(64);
		setObjectCount(0);
	}

	private void setStartIndex(int value)
		throws IOException
	{
		file.seek(52);
		file.writeInt(value);

		this.endIndex = value;
	}

	private void setEndIndex(int value)
		throws IOException
	{
		file.seek(56);
		file.writeInt(value);

		this.endIndex = value;
	}

	private void setObjectCount(int value)
		throws IOException
	{
		file.seek(60);
		file.writeInt(value);

		this.objectCount = value;
	}

	public void push(Object o)
		throws Exception
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(o);

		byte[] data = baos.toByteArray();

		file.seek(endIndex);
		file.writeInt(data.length);
		file.write(data);

		setEndIndex(endIndex + data.length + 4);
		setObjectCount(objectCount + 1);
	}

	public void clear()
		throws Exception
	{
		file.setLength(64);
		writeHeader();
	}

	public Object pop()
	{
		throw new RuntimeException("Unsupported operation");
	}

	public Iterator<Object> iterator()
	{
		return new PersistentQueueIterator(this);
	}
}
