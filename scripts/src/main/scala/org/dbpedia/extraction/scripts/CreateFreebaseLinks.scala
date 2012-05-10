package org.dbpedia.extraction.scripts

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import io.Source
import java.io.{FileOutputStream, PrintStream, File}
import java.net.URL
import org.dbpedia.extraction.util.{Language,WikiUtil}
import org.dbpedia.extraction.util.RichString.toRichString

/**
 * Created by IntelliJ IDEA.
 * User: Max Jakob
 * Date: 17.11.10
 * Time: 14:52
 * Create a dataset file with owl:sameAs links to Freebase.
 * In order to *not* create sameAs links between DBpedia redirects and "real" Freebase entities
 * that are the target of a redirect, this script needs the DBpedia redirects and disambiguations datasets as input.
 */

object CreateFreebaseLinks
{
    // change this URL to the dump that is closest to the dump date of the DBpedia release
    val freebaseDumpUrl = "http://download.freebase.com/datadumps/latest/freebase-datadump-quadruples.tsv.bz2"

    val freebaseRelationRegex = "^([^\\s]+)\\t[^\\s]+\\t/wikipedia/en\\t([^\\s]+)$".r

    val dbpediaNamespace = "http://dbpedia.org/resource/"
    val sameAsRelation = "http://www.w3.org/2002/07/owl#sameAs"
    val freebaseNamespace = "http://rdf.freebase.com/ns"   //TODO decide if "http://rdf.freebase.com/rdf" is better
    val tripleStr = "<"+dbpediaNamespace+"%s> <"+sameAsRelation+"> <"+freebaseNamespace+"%s> ."


    def main(args : Array[String])
    {
        val outputFileName = args(0)     // file name of dataset file (must end in '.nt')
        val labelsFileName = args(1)     // file name of DBpedia labels dataset
        val redirectsFileName = args(2)  // file name of DBpedia redirects dataset
        val disambigFileName = args(3)   // file name of DBpedia disambiguations dataset

        if(!outputFileName.endsWith(".nt"))
        {
            throw new IllegalArgumentException("file name extension must be '.nt' ("+outputFileName+")")
        }

        val conceptURIs = getConceptURIs(new File(labelsFileName), new File(redirectsFileName), new File(disambigFileName))
        writeLinks(new File(outputFileName), conceptURIs)
    }

    private def writeLinks(outputFile : File, conceptURIs : Set[String])
    {
        val uncompressOutStream = new PrintStream(outputFile, "UTF-8")
        val bz2compressorOutStream = new BZip2CompressorOutputStream(new FileOutputStream(outputFile+".bz2"))
        val compressedOutStream = new PrintStream(bz2compressorOutStream, true, "UTF-8")

        println("Searching for Freebase links in "+freebaseDumpUrl+"...")
        val bz2compressorInStream = new BZip2CompressorInputStream(new URL(freebaseDumpUrl).openStream)
        var count = 0
        for (line <- Source.fromInputStream(bz2compressorInStream, "UTF-8").getLines)
        {
            for (linkString <- getLink(line, conceptURIs))
            {
                uncompressOutStream.println(linkString)
                compressedOutStream.println(linkString)
                count += 1
                if(count%10000 == 0)
                {
                    println("  found "+count+" links to Freebase")
                }
            }

        }
        bz2compressorInStream.close
        compressedOutStream.close
        bz2compressorOutStream.close
        uncompressOutStream.close
        println("Done. Found "+count+" links to Freebase.")
    }

    private def getLink(line : String, conceptURIs : Set[String]) : Option[String] =
    {
        for (relationMatch <- freebaseRelationRegex.findFirstMatchIn(line))
        {
            val dbpediaUri = getDBpediaResourceUri(relationMatch.subgroups(1))
            if(conceptURIs contains dbpediaUri)
            {
                val freebaseUri = relationMatch.subgroups(0)
                return Some(tripleStr.format(dbpediaUri, freebaseUri))
            }
        }
        None
    }

    private def getDBpediaResourceUri(wikipediaLabel : String) : String =
    {
        var uri = wikipediaLabel
        // Freebase encodes Wikipedia pages titles with unicode points that are marked by a dollar sign
        for(codePointMatch <- "\\$(\\w\\w\\w\\w)".r.findAllIn(wikipediaLabel).matchData)
        {
            val codePoint = codePointMatch.subgroups(0)
            uri = uri.replace("$"+codePoint, Integer.parseInt(codePoint, 16).toChar.toString)
        }
        // TODO: which language?
        WikiUtil.wikiEncode(uri).capitalize(Language.English.locale)
    }

    private def loadURIs(dbpediaDataset : File) : Set[String] =
    {
        println("Reading subject URIs from "+dbpediaDataset+"...")
        val lexicon = Source.fromFile(dbpediaDataset).getLines.map(getShortSubjectURI(_)).toSet
        println("Done.")
        lexicon
    }

    private def getShortSubjectURI(ntLine : String) : String =
    {
        ntLine.substring(0, ntLine.indexOf(">")).replace("<http://dbpedia.org/resource/", "").trim
    }

    private def getConceptURIs(labelsFile : File, redirectsFile : File, disambiguationsFile : File) : Set[String] =
    {
        (loadURIs(labelsFile) -- loadURIs(redirectsFile)) -- loadURIs(disambiguationsFile)

//        var conceptCandidates = loadURIs(labelsFile)
//        for(line <- Source.fromFile(redirectsFile, "UTF-8"))
//        {
//            conceptCandidates = conceptCandidates --- getShortSubjectURI(line)
//        }
//        for(line <- Source.fromFile(disambiguationsFile, "UTF-8"))
//        {
//            conceptCandidates = conceptCandidates --- getShortSubjectURI(line)
//        }
//        conceptCandidates
    }

}