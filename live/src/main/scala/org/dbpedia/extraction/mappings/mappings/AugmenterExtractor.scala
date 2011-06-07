package org.dbpedia.extraction.mappings

import java.lang.String
import org.dbpedia.extraction.wikiparser.PageNode
import org.dbpedia.extraction.destinations.{Dataset, Quad, Graph}
import collection.mutable.{HashSet, Set, MultiMap, HashMap}
import com.hp.hpl.jena.query.QuerySolution
import io.Source
import com.hp.hpl.jena.rdf.model.{ModelFactory, Model, Literal}
import com.hp.hpl.jena.datatypes.TypeMapper


object AugmentExtractorConstants
{
  val categoryPrefix = "http://dbpedia.org/resource/Category:"

}

class NTripleFileQuadIterator
//  extends Iterator[Quad]
{
  val categoryDump = "/home/raven/Desktop/dbpedia/datasets/3.5.1/article_categories_en.nt"

  def parseNTripleURI(str : String) : Option[String] =
  {
    if(str.startsWith("<") && str.endsWith(">"))
      return Some(str.substring(1, str.length - 2))

    return None
  }

  
  def getCategoriesFromDump() : Iterable[String] =
  {
    val lines = Source.fromFile(categoryDump).getLines

    println("Starting reading model")
    val result = new HashSet[String]
    lines.zipWithIndex.foreach{case(tmp, lineNumber) => {
      val line = tmp.trim

      if(lineNumber % 1000000 == 0)
        println(lineNumber)

      val parts = line.split("\\s+", 3)

      parseNTripleURI(parts(2)) match { case Some(uri) => result.add(uri) case None => }
    }}
    println("Done reading model")

    return result
  }


}


object AugmenterExtractorUtils
{
  def canonicalize(str : String) : String = {
    return str.replace("_", " ").trim.replace("\\s+", " ").toLowerCase
  }
}


/**
 * Created by Claus Stadler
 * Date: Sep 8, 2010
 * Time: 12:58:45 AM
 *
 * A decorator that generates triples based on the output of
 * another extractor (the decoratee)
 *
 * The newly generated triples are part of the specified dataset
 */
class AugmenterExtractor(val decoratee : Extractor, val dataset : Dataset,
        val labelToURIs : MultiMap[String, String], val relationPredicate : String)
  extends Extractor
{
  def extract(page: PageNode, subjectUri: String, context: PageContext) : Graph = {

    val base = decoratee.extract(page, subjectUri, context)

    val newQuads = new HashSet[Quad]

    base.quads.foreach(quad => {
      extractCategoryName(quad) match {
        case None =>
        case Some(categoryName) => {

          val resources = relatedResources(categoryName, labelToURIs)

          resources.foreach(res => {
            val newQuad = new Quad(quad.extractionContext, dataset, quad.subject, relationPredicate, res, quad.context, null)

            newQuads.add(newQuad)
          })

        }
      }
    })

    return new Graph(newQuads.toList)
    //val result = base.merge(new Graph(newQuads.toList))


    //return result
  }


  def extractCategoryName(quad: Quad) : Option[String] = {
    val categoryPrefix = AugmentExtractorConstants.categoryPrefix

    if(!(quad.predicate == "http://www.w3.org/TR/skos-reference/skos.html#subject" && quad.value.startsWith(categoryPrefix)))
      return None
   
    return Some(quad.value.substring(categoryPrefix.length))
  }





  /**
   *
   */
  def relatedResources(rawCategoryName : String, labelToURIs : MultiMap[String, String]) : Set[String] = {

    var matches = new HashSet[String]

    val categoryName = " " + AugmenterExtractorUtils.canonicalize(rawCategoryName) + " "

    labelToURIs.filter(e => categoryName.contains(" " + e._1 + " ")).foreach{case(label, uri) => {
      matches.filter(label.contains(_)).foreach(matches.remove(_))
      matches.add(label)
    }}

    var result = new HashSet[String]

    matches.foreach(labelToURIs.get(_) match { case Some(uris) => {uris.foreach(result.add(_))} case None => })

    return result
  }




  /**
   *  Returns a mapping of categoryNames to related URIs
   *
   */

  def createMapping(categories : Iterable[String], labelToURIs : MultiMap[String, String]) : HashMap[String, String]=
  {
    //val countries = reverse(uriToLabels)

    //var bestMatches = new HashMap[String, Set[String]]() with MultiMap[String, String];
    var bestMatches = new HashMap[String, String]

    categories.zipWithIndex.foreach{case (categoryUri, counter) => {

      if(counter % 1000 == 0) {
        println(counter)
      }

      val prefix = AugmentExtractorConstants.categoryPrefix
      if(!categoryUri.startsWith(prefix)) {
        println("Warning: Non-category: " + categoryUri)

      }
      else {

        var categoryName = categoryUri.substring(prefix.length)

        categoryName = AugmenterExtractorUtils.canonicalize(categoryName)

        var candidate = "";

        var parts = categoryName.split("of ", 2)
        if(parts.length == 2)
          candidate = parts(1)

        parts = categoryName.split("in ", 1)
        if(parts.length == 2)
          candidate = parts(1)

        candidate = AugmenterExtractorUtils.canonicalize(candidate)

        if(!candidate.isEmpty) {
          //println(candidate)

          labelToURIs.foreach(item => {
            val countryName = item._1
            if(candidate.contains(AugmenterExtractorUtils.canonicalize(countryName))) {

              //println("Match: " + candidate + ", " + countryName + "  --- fullcat: " + categoryName);

              bestMatches.get(categoryName) match {
              case None => { bestMatches.put(categoryUri, countryName) }
              case Some(oldCountryName) => {
                if(countryName.length > oldCountryName.length) {
                  bestMatches.put(categoryUri, countryName)
                  //println(countryName + "| replaced | " + oldCountryName);
                }
                }
              }

            }
          })
        }
      }
    }}

    //println("bestMatches: " + bestMatches)

    // Based on the best matches, we can now create the action objects
    // for dealing with resoures that are linked to a category

    // The resulting object should be serialized for reuse

    val result = new HashMap[String, String];
    bestMatches.foreach(entry => {

      labelToURIs.get(entry._2) match {
        case None => {}
        case Some(set) => {
          if(set.size != 1) {
            println("Warning: Non-1:1-Mapping found: " + entry._1 + " -> " + entry);
          }

          if(set.size == 1) {
            val targetURI = set.iterator.next
            //println("Mapped " + entry._1 + " to " + targetURI)
            result.put(entry._1, targetURI)
          }
        }
      }
    })

    return result
  }
}

/*
  def reverse(map : MultiMap[String, String]) : MultiMap[String, String] = {
    val result = new HashMap[String, Set[String]]() with MultiMap[String, String]

    map.foreach(q => q._2.foreach(v => result.add(v, q._1)))

    return result
  }
*/