package org.dbpedia.extraction.util

import java.io.{File, FileNotFoundException}
import scala.io.{Source, Codec}
import org.dbpedia.extraction.util.RichFile._

/**
 * Recursively iterates through a directory and calls a user-defined function on each file. 
 *
 * @param baseDir Absolute path to base dir, using forward slashes.  Never null.
 * @param skipNames Names (not paths) of files and directories to skip, e.g. '.svn'.
 * If empty, all files and directories will be included.
 * @param paths array of strings, paths of files to use, relative to base dir, using forward slashes.
 * If empty, all files and directories will be included.
 * @throws FileNotFoundException if the given base could not be found
 */
class FileProcessor(baseDir : File, filter : (String => Boolean))
{
    /**
     * TODO: use proper JavaDoc for this comment.
     * processes a single file. params: file path (relative to base dir) and file content.
     */
    type Processor = (String, String) => Unit
    
    if(!baseDir.exists) throw new FileNotFoundException("'" + baseDir + "' not found")
  
    /**
     * Processes files.
     *
     * @param processor callback function to process a single file
     */
    def processFiles(processor : Processor) : Unit = 
    {
        processRecursive(baseDir, processor)
    }

    /**
     * @param file must be base dir or a child of it.
     * @param processor callback function to process a single file. Must have two string 
     * parameters: file path (relative to base dir) and file content.
     */
    private def processRecursive( file : File, processor : Processor ) : Unit =
    {
        if(file.isFile)
        {
          val path = baseDir.relativize(file)
          if (!filter(path)) return

          val source = readFileContents(file)
          processor(path, source)
        }
        else
        {
            for(childFile <- file.listFiles())
            {
                processRecursive(childFile, processor)
            }
        }
    }
    
    private def readFileContents(file : File)(implicit codec : scala.io.Codec = Codec.UTF8) : String =
    {
      // TODO: this decodes the file one character at a time, which may be inefficient 
      Source.fromFile(file, 65536)(codec).mkString
    }
}
