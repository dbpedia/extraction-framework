package org.dbpedia.extraction.live.publisher;

/**
 * Created by IntelliJ IDEA.
 * Date: Oct 29, 2010
 * Time: 6:26:34 PM
 * This class writes the triples to a file, for live synchronization, it is originally developed by Claus Stadler
 */

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.log4j.Logger;

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

	/*
	public RDFDiffWriter(File basePath, long sequenceId)
	{
		this.basePath = basePath;
		this.sequenceId = sequenceId;
	}
	*/

	public RDFDiffWriter(String baseName)
	{
		this.baseName = baseName;
	}

	// 5 10 100
	public static List<Long> chunkValue(long id, long ...chunkSizes)
	{
		long denominator = 1;
		for(long chunkSize : chunkSizes)
			denominator *= chunkSize;

		List<Long> result = new ArrayList<Long>();

		long remainder = id;
		for(long chunkSize : chunkSizes) {
			long div = remainder / denominator;
			remainder = remainder % denominator;

			result.add(div);

			denominator = denominator / chunkSize;
		}

		result.add(remainder);

		return result;
	}

	/*public static void write(Model model, RDFWriter rdfWriter, String fileName, boolean zip)
		throws IOException
	{
		logger.info("Attempting to write diff-file: " + fileName);

		File file = new File(fileName);

		OutputStream tmp = new FileOutputStream(file);

		OutputStream out;
		if(zip) {
			out = new GzipCompressorOutputStream(tmp);
		}
		else {
			out = tmp;
		}

		rdfWriter.write(model, out, "");

		out.flush();
		out.close();
	}*/

    /**
     * Writes the passed model to file
     * @param model The model containing the triples that should be written
     * @param added Whether the model contains triples that should be added or removed
     * @param baseName  The base file name
     * @param zip   Whether the file should be compressed or not
     * @throws IOException
     */
    public static void write(Model model, boolean added, String baseName, boolean zip)
		throws IOException
	{
        //No data to be written
        if(model.size() <= 0)
            return;
		File file = new File(baseName);
		File parentDir = file.getParentFile();
		if(parentDir != null)
			parentDir.mkdir();

		String fileNameExtension = "nt";
		String jenaFormat = "N-TRIPLE";

		if(zip)
			fileNameExtension += ".gz";


		RDFWriter rdfWriter = ModelFactory.createDefaultModel().getWriter(jenaFormat);


        String fileName = "";
        
        if(added)
            fileName = baseName + ".added." + fileNameExtension;
        else
            fileName = baseName + ".removed." + fileNameExtension;


        logger.info("Attempting to write diff-file: " + fileName);

		File outputFile = new File(fileName);
        System.out.println(fileName);
		OutputStream tmp = new FileOutputStream(outputFile);

		OutputStream out;
		if(zip) {
			out = new GzipCompressorOutputStream(tmp);
		}
		else {
			out = tmp;
		}

		rdfWriter.write(model, out, "");

		out.flush();
		out.close();

//		String addedFileName = baseName + ".added." + fileNameExtension;
//		write(diff.getAdded(), rdfWriter, addedFileName, zip);
//
//		String removedFileName = baseName + ".removed." + fileNameExtension;
//		write(diff.getRemoved(), rdfWriter, removedFileName, zip);
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
		String jenaFormat = "N-TRIPLE";

		if(zip)
			fileNameExtension += ".gz";

        String fileName = "";

        if(added)
            fileName = baseName + ".added." + fileNameExtension;
        else
            fileName = baseName + ".removed." + fileNameExtension;


        logger.info("Attempting to write diff-file: " + fileName);

		File outputFile = new File(fileName);
        System.out.println(fileName);
		tmp = new FileOutputStream(outputFile);

//		OutputStream out;
		if(zip) {
			out = new GzipCompressorOutputStream(tmp);
		}
		else {
			out = tmp;
		}

		out.write(triplesString.getBytes());

		out.flush();
//		out.close();
        }
        finally {
            if(out != null)
                out.close();

            /*if((out != null) && (out != tmp))
                out.close();*/

        }


//		String addedFileName = baseName + ".added." + fileNameExtension;
//		write(diff.getAdded(), rdfWriter, addedFileName, zip);
//
//		String removedFileName = baseName + ".removed." + fileNameExtension;
//		write(diff.getRemoved(), rdfWriter, removedFileName, zip);
	}
}
