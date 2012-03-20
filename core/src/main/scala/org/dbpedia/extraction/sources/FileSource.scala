package org.dbpedia.extraction.sources

import org.dbpedia.extraction.wikiparser.WikiTitle

import java.util.logging.{Logger, Level}
import java.io.File
import util.control.ControlThrowable
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.util.{WikiUtil, FileProcessor}

/**
 * Reads wiki pages from text files in the file system.
 *
 * @param baseDir Absolute path to base dir, using forward slashes.  Never null.
 * @param filter Function to filter pages by their relative path in respect of the base dir.
 * Pages for which this function returns false, won't be yielded by the source. Forward slashes are used to separate directorys.
 * By default, all files and directories starting with an dot are ignored.
 * @param language The Language of the sources.
 * @throws FileNotFoundException if the given base could not be found
 */
class FileSource(baseDir : File, filter : (String => Boolean) = (path => !path.startsWith(".") && !path.contains("/.")), language : Language = Language.Default) extends Source
{
    private val logger = Logger.getLogger(classOf[FileSource].getName)
    private val fileProcessor = new FileProcessor(baseDir, filter)

    override def foreach[U](f : WikiPage => U) : Unit =
    {
        fileProcessor.processFiles((path : String, source: String) =>
        {
            // cut off '#1.txt' or '.txt' if necessary
            var sep = path.lastIndexOf('#')
            if (sep == -1)
            { 
                sep = path.lastIndexOf('.')
            }
            
            val slash = path.indexOf('/');
            
            var pageName = if(sep > slash) path.substring(0, sep) else path
            pageName = WikiUtil.wikiDecode(pageName, language)
                
            try
            {
            	val title = WikiTitle.parse(pageName, language)

            	f(new WikiPage(title, 0, 0, source))
            }
            catch
            {
                case ex : ControlThrowable => throw ex
            	case ex : Exception => logger.log(Level.WARNING, "Error processing page  " + pageName, ex)
            }
        })
	}

    override def hasDefiniteSize = true
}
