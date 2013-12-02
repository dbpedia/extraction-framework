package org.dbpedia.extraction.live.publisher;

/**
 * Created by IntelliJ IDEA.
 * Date: Oct 29, 2010
 * Time: 6:26:34 PM
 * This class writes the triples to a file, for live synchronization, it is originally developed by Claus Stadler
 */

// TODO: use java.util.zip.GZIPOutputStream instead, get rid of this dependency
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;


public class RDFDiffWriter
{
	private static final Logger logger = Logger.getLogger(RDFDiffWriter.class);

	//private long sequenceId;
	//private File basePath;

	private String baseName;


	private boolean zip = false;


	public RDFDiffWriter(String baseName)
	{
		this.baseName = baseName;
	}

    /**
     * Writes the passed model to file
     * @param triplesString The triples to be written as a string, in N-TRIPLES format
     * @param added Whether the model contains triples that should be added or removed
     * @param baseName  The base file name
     * @param zip   Whether the file should be compressed or not
     * @throws IOException
     */
    public static void write(String triplesString, boolean added, String baseName, boolean zip)
		throws IOException
	{
        //No data to be written
        OutputStream tmp = null;
		OutputStream out = null;
        try{
        if((triplesString == null) || (triplesString.compareTo("") == 0))
            return;
		File file = new File(baseName);
		File parentDir = file.getParentFile();
		if(parentDir != null)
			parentDir.mkdir();

		String fileNameExtension = "nt";

		if(zip)
			fileNameExtension += ".gz";

        String fileName = "";

        if(added)
            fileName = baseName + ".added." + fileNameExtension;
        else
            fileName = baseName + ".removed." + fileNameExtension;


        logger.info("Attempting to write diff-file: " + fileName);

		File outputFile = new File(fileName);
        logger.info(fileName);
		tmp = new FileOutputStream(outputFile);

		if(zip) {
			out = new GzipCompressorOutputStream(tmp);
		}
		else {
			out = tmp;
		}

		out.write(triplesString.getBytes());

		out.flush();

        } catch (IOException e) {
            throw e;
        }
        finally {
            try {
                if(out != null)
                    out.close();
            } catch (IOException e) {
                throw e;
            }
            try {
                if(tmp != null)
                    tmp.close();
            } catch (IOException e) {
                throw e;
            }
        }

	}
}
