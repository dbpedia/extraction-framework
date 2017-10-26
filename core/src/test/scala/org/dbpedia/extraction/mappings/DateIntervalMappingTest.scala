package org.dbpedia.extraction.mappings

import java.io.File

import org.dbpedia.extraction.config.ExtractionRecorder
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.sources.XMLSource
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser._
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers

import scala.reflect.ClassTag

@RunWith(classOf[JUnitRunner])
class DateIntervalMappingTest extends FlatSpec with ShouldMatchers
{
    // gYear - Positive Tests - Input is valid
    "DateIntervalMapping" should "return Seq 1995 2002 @en" in
    {
        parse("en", "xsd:gYear", "1995-2002") should be (Seq("1995", "2002"))
        parse("en", "xsd:gYear", "1995—2002") should be (Seq("1995", "2002"))
        parse("en", "xsd:gYear", "1995{{dash}}2002") should be (Seq("1995", "2002"))
    }
    "DateIntervalMapping" should "return Seq 1995 2002 @fr" in
    {
        parse("fr", "xsd:gYear", "de 1995 à 2002") should be (Seq("1995", "2002"))
    }
    "DateIntervalMapping" should "return Seq 1967 1977 @pt" in
    {
        parse("pt", "xsd:gYear", "[[1967 na música|1967]] - [[1977 na música|1977]]") should be (Seq("1967", "1977"))
    }
    "DateIntervalMapping" should "return Seq 1995 @en" in
    {
        parse("en", "xsd:gYear", "1995-present") should be (Seq("1995"))
        /**
        // FIXME: Not yet supported
        parse("en", "xsd:gYear", "1995-") should be (Seq("1995"))
        */
    }
    "DateIntervalMapping" should "return Seq 1995 @fr" in
    {
        parse("fr", "xsd:gYear", "depuis 1995") should be (Seq("1995"))
        parse("fr", "xsd:gYear", "de 1995 à aujourd'hui") should be (Seq("1995"))
    }
    "DateIntervalMapping" should "return Seq 1995 @es" in
    {
        parse("es", "xsd:gYear", "1995 al presente") should be (Seq("1995"))
        parse("es", "xsd:gYear", "1995 hasta la actualidad") should be (Seq("1995"))
        parse("es", "xsd:gYear", "1995 a la fecha") should be (Seq("1995"))
        parse("es", "xsd:gYear", "1995 en adelante") should be (Seq("1995"))
    }
    "DateIntervalMapping" should "return Seq 1995 1995" in
    {
        parse("en", "xsd:gYear", "1995") should be (Seq("1995", "1995"))
    }
    "DateIntervalMapping" should "return Seq 1995 1997" in
    {
        parse("en", "xsd:gYear", "Interval period:<br>1995-1997") should be (Seq("1995", "1997"))
    }
    "DateIntervalMapping" should "return Seq 1981 2001 2002 2007 2009 @en" in
    {
        //parse("en", "xsd:gYear", "1981–2001, 2002–2007, 2009–present") should be (Seq("1981", "2001", "2002", "2007", "2009"))
        //temporary limited to first interval
        parse("en", "xsd:gYear", "1981–2001, 2002–2007, 2009–present") should be (Seq("1981", "2001"))
    }
    "DateIntervalMapping" should "return Seq 1984 1987 1987 2003 @en" in
    {
        //parse("en", "xsd:gYear", "1984-1987 (Cinema)<br>1987-2003 (solo)") should be (Seq("1984", "1987", "1987", "2003"))
        //temporary limited to first interval
        parse("en", "xsd:gYear", "1984-1987 (Cinema)<br>1987-2003 (solo)") should be (Seq("1984", "1987"))
    }

    // Date - Positive Tests - Input is valid
    "DateIntervalMapping" should "return Seq 2014-07-01 2014-07-01 @en" in
    {
        parse("en", "xsd:date", "2014-07-01") should be (Seq("2014-07-01", "2014-07-01"))
    }
    "DateIntervalMapping" should "return Seq 2014-07-01 2014-07-05 @en" in
    {
        parse("en", "xsd:date", "2014-07-01 - 2014-07-05") should be (Seq("2014-07-01", "2014-07-05"))
    }
    "DateIntervalMapping" should "return Seq 1996-06-03 2008-05-31 @fr" in
    {
        parse("fr", "xsd:date", "[[3 juin]] [[1996]] au [[31 mai]] [[2008]]") should be (Seq("1996-06-03", "2008-05-31"))
    }

    //Date negative tests - Input is not a genuine interval 
    /**
     // FIXME: this would parse for now
    "DateTimeParser" should "not return Seq 2008-02-05 2008-02-05" in
    {
        parse("en", "xsd:date", "2008-02-05-06") should be (Seq())
    }
    */

    //Negative tests - Input is not a date
    "DateTimeParser" should "return Seq.empty" in
    {
        parse("en", "xsd:date", "foo") should be (Seq())
    }
  
  
    private val wikiParser = WikiParser.getInstance()
    private val ontology = {
            val ontoFile = new File("../ontology.xml")
            val ontologySource = XMLSource.fromFile(ontoFile, Language.Mappings)
            new OntologyReader().read(ontologySource)
    }
    private val startYear = ontology.properties.get("activeYearsStartYear")
    private val endYear = ontology.properties.get("activeYearsEndYear")
    private val startDate = ontology.properties.get("activeYearsStartDate")
    private val endDate = ontology.properties.get("activeYearsEndDate")

    private def parse(language : String, datatypeName : String, input : String) : Seq[String] =
    {
        val lang = Language(language)
        val red = new Redirects(Map())

        val context = new
        {
            def language : Language = lang
            def ontology: Ontology = ontology
            def redirects : Redirects = red
            def recorder[T: ClassTag] : ExtractionRecorder[T] = new ExtractionRecorder[T]()
        }
        
        val start = datatypeName match
        {
          case "xsd:gYear" => startYear
          case "xsd:date" => startDate
          case _ => throw new IllegalArgumentException("Not implemented: " + datatypeName)
        }
        val end = datatypeName match
        {
          case "xsd:gYear" => endYear
          case "xsd:date" => endDate
          case _ => throw new IllegalArgumentException("Not implemented: " + datatypeName)
        }

        val dateIntervalMapping = new DateIntervalMapping("fooPeriod", start.get, end.get, context)
        val page = new WikiPage(WikiTitle.parse("TestPage", lang), "{{foo|fooPeriod=" + input + "}}")

        wikiParser(page) match {
          case Some(n) => dateIntervalMapping.extract(n.children(0).asInstanceOf[TemplateNode], "").map{case (quad) => quad.value}
          case None => Seq()
        }
    }
}