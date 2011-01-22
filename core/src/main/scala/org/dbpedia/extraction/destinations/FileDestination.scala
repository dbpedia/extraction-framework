package org.dbpedia.extraction.destinations

import formatters.NTriplesFormatter
import java.net.URLEncoder
import collection.mutable.HashMap
import java.io._

/**
 * A destination which writes all statements to files.
 * This class is thread-safe.
 *
 * @param formatter The formatter used to serialize the statements. If no formatter is provided, the statements are written using the N-Triples format.
 * @param baseDir The base directory of the output files. If no base directory is provided, the output is written to current directory.
 * @param filePattern The pattern according to which the statements are split into different files by their dataset.
 * If no pattern is provided, one file for each dataset is generated. In this case the file names are generated from the name of the corresponding dataset.
 */
class FileDestination(formatter : Formatter = new NTriplesFormatter(),
                      baseDir : File = new File("."),
                      filePattern : (Dataset => String) = FileDestination.defaultFilePattern, append : Boolean = false) extends Destination
{
    baseDir.mkdirs()

	private val writers = HashMap[String, Writer]()

    private var closed = false

	override def write(graph : Graph) : Unit = synchronized
	{
        if(closed) throw new IllegalStateException("Trying to write to a closed destination")

        for((dataset, quads) <- graph.quadsByDataset)
        {
            val writer = getWriter(dataset)

            for(quad <- quads)
            {
                formatter.write(quad, writer)
            }
        }
    }

    override def close() = synchronized
    {
        if(!closed)
        {
            for(writer <- writers.values)
            {
                formatter.writeFooter(writer)
                writer.close()
            }
            closed = true
        }
    }

    private def getWriter(dataset : Dataset) : Writer =
    {
        val fileName = filePattern(dataset)

        writers.get(fileName) match
        {
            case Some(writer) => writer
            case None =>
            {
                val file = new File(baseDir + "/" + fileName)
                file.getParentFile.mkdirs

                val stream = new FileOutputStream(file, append)
                val writer = new OutputStreamWriter(stream, "UTF-8")
                formatter.writeHeader(writer)
                writers(fileName) = writer

                writer
            }
        }

    }
}

private object FileDestination
{
    private def defaultFilePattern(dataset : Dataset) : String =
    {
         URLEncoder.encode(dataset.name, "UTF-8") + ".nq"
    }
}