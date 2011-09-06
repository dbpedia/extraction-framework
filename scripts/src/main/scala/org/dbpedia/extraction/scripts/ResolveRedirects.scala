package org.dbpedia.extraction.scripts

import io.Source
import java.lang.IllegalArgumentException
import java.io.{FileOutputStream, File, PrintStream}

/**
 * Script to resolve redirects in object positions.
 * To fit all redirects in memory, use -Xmx4G. (for DBpedia 3.6)
 */
object ResolveRedirects
{
    val REPLACE_ORIGINAL_FILE = true  // if set to true, the original files are renamed to "filename.unresolvedRedirects" and the resolved ones get the original name

    def main(args : Array[String])
    {
        val start = System.currentTimeMillis()

        val redirectsDatasetFileName = args.head
        val datasetsToBeModified = args.tail

        val language = getLanguage(redirectsDatasetFileName)
        val namespace = if (language == "en") "http://dbpedia.org/resource/" else "http://" + language + ".dbpedia.org/resource/"

        val redirects = loadRedirects(redirectsDatasetFileName, namespace)

        for(datasetFileName <- datasetsToBeModified)
        {
            if(getLanguage(datasetFileName) != language)
            {
                println("*** skipping "+datasetFileName+" because language "+getLanguage(datasetFileName)+" did not match redirects language "+language+" ***")
            }
            else
            {
                resolveRedirects(redirects, datasetFileName, namespace)
            }
        }

        println("time: " + (System.currentTimeMillis()-start)/1000 + " s")
    }

    private def loadRedirects(fileName : String, resourceNamespacePrefix : String) : Map[String,String] =
    {
        println("Loading redirects")
        var redirects : Map[String,String] = Map()
        val tripleSource = new LineSubjectObjectSource(fileName)

        for((line, subj, obj) <- tripleSource)
        {
            val uri = stripUri(subj, resourceNamespacePrefix)
            redirects = redirects.updated(uri, stripUri(obj, resourceNamespacePrefix))
        }

        println("  building transitive closure")
        // resolve transitive closure
        for((source,target) <- redirects)
        {
            var cyclePrevention : Set[String] = Set(source)
            var closure = target
            while( redirects.contains(closure) && !cyclePrevention.contains(closure) )
            {
                closure = redirects(closure)
                cyclePrevention += closure
            }
            redirects = redirects.updated(source, closure)
        }

        redirects
    }

    private def resolveRedirects(redirects : Map[String,String], fileName : String, resourceNamespacePrefix : String)
    {
        println("Resolving redirects in "+fileName)
        var resolvedFileName = fileName + ".redirectsResolved"

        val out = new PrintStream(new FileOutputStream(resolvedFileName), true, "UTF-8")
        var resolvedRedirects = 0
        try
        {
            val tripleSource = new LineSubjectObjectSource(fileName)
            tripleSource.foreach{case (line, subj, obj) =>
            {
                val objUri = stripUri(obj, resourceNamespacePrefix)
                redirects.get(objUri) match
                {
                    case Some(resolvedUri) =>
                    {
                        //println("  resolved "+objUri+" -> "+resolvedUri)
                        out.println(line.replace(obj, unstripUri(resolvedUri, resourceNamespacePrefix)))
                        resolvedRedirects += 1
                        resolvedUri
                    }
                    case None => out.println(line)
                }
            }}
        }
        finally
        {
            out.close()
        }

        if(REPLACE_ORIGINAL_FILE)
        {
            new File(fileName).renameTo(new File(fileName + ".unresolvedRedirects"))
            new File(resolvedFileName).renameTo(new File(fileName))
            resolvedFileName = fileName
        }
        println("Resolved "+resolvedRedirects+" redirects in "+resolvedFileName)
    }

    private val ObjectPropertyQuadsRegex = """^(<[^>]+>) (<[^>]+>) (<[^>]+>) (<[^>]+>)? ?\.$""".r
    private val DatatypePropertyQuadsRegex = """^(<[^>]+>) (<[^>]+>) (".+"\S*) (<[^>]+>)? ?\.$""".r
    private class LineSubjectObjectSource(fileName : String) extends Traversable[(String,String,String)]
    {
        override def foreach[U](f : ((String,String,String)) => U)
        {
            for(line <- Source.fromFile(fileName, "UTF-8").getLines())
            {
                line match
                {
                    case ObjectPropertyQuadsRegex(subj, pred, obj, provenance) => f(line, subj, obj)
                    case DatatypePropertyQuadsRegex(subj, pred, obj, provenance) => f(line, subj, obj)
                    case _ if line.nonEmpty => throw new IllegalArgumentException("line did not match n-quads syntax: "+line)
                    case _ =>
                }
            }
        }
    }

    private val FileNameRegex = """.+_(\w\w)\.n[tq]""".r
    private def getLanguage(datasetFileName : String) : String =
    {
        datasetFileName match
        {
            case FileNameRegex(lang) => lang
            case _ => throw new Exception("did not recognize DBpedia file name pattern: "+datasetFileName)
        }
    }

    private def stripUri(fullUri : String, namespace : String) : String =
    {
        fullUri.replace(namespace, "").replace("<", "").replace(">", "")
    }

    private def unstripUri(strippedUri : String, namespace : String) : String =
    {
        "<"+namespace+strippedUri+">"
    }

}