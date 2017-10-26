package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.config.ExtractionRecorder
import org.dbpedia.extraction.mappings.Redirects
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.scalatest.Matchers
import org.scalatest.FlatSpec
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.sources.MemorySource
import org.dbpedia.extraction.util.Language
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import scala.reflect.ClassTag

@RunWith(classOf[JUnitRunner])
class DateTimeParserTest extends FlatSpec with Matchers
{
    //gYear positive tests - Input is inside equivalence class

    "DateTimeParser" should "return gYear (2008)" in
    {
        parse("en", "xsd:gYear", "2008") should equal (Some("2008"))
    }

    "DateTimeParser" should "return gYear (20BC)" in
    {
        parse("en", "xsd:gYear", "20 BC") should equal (Some("-0020"))
    }
    // TODO: Add support for century notation
    /*
    "DateTimeParser" should "return gYear (14th century)" in
    {
        parse("en", "xsd:gYear", "14th century") should equal (Some("1300"))
    }
    */

    //gYear negative tests - Input is outside equivalence class

    "DateTimeParser" should "not return gYear (20008)" in
    {
        parse("en", "xsd:gYear", "20008") should equal (None)
    }
    "DateTimeParser" should "not return (20000 BC)" in
    {
        parse("en", "xsd:gYear", "20000 BC") should equal (None)
    }
    "DateTimeParser" should "not return (0000)" in
    {
        parse("en", "xsd:gYear", "0000") should equal (None)        
    }

    //gYear testing different syntax

    "DateTimeParser" should "return gYear (20 AD)" in
    {
            parse("en", "xsd:gYear", "20 AD") should equal (Some ("0020"))
    }
    "DateTimeParser" should "return gYear (AD 20)" in
    {
            parse("en", "xsd:gYear", "AD 20") should equal (Some ("0020"))
    }
    "DateTimeParser" should "return gYear (20 CE)" in
    {
            parse("en", "xsd:gYear", "20 CE") should equal (Some ("0020"))
    }
    "DateTimeParser" should "return gYear (20 BCE)" in
    {
            parse("en", "xsd:gYear", "20 BCE") should equal (Some ("-0020"))
    }
    "DateTimeParser" should "return gYear ([[20 BCE]])" in
    {
                parse("en", "xsd:gYear", "[[20 BCE]]") should equal (Some ("-0020"))
    }
    "DateTimeParser" should "not return gYear ({{Harvnb|Self|p=323.}})" in
    {
                parse("en", "xsd:gYear", "{{Harvnb|Self|p=323.}}") should equal (None)
    }
    // TODO: This is not easy
    /*
    "DateTimeParser" should "not return gYear (File:MunichAgreement .jpg|thumb|300px|)" in
    {
                parse("en", "xsd:gYear", "File:MunichAgreement .jpg|thumb|300px|") should equal (None)
    }
    "DateTimeParser" should "not return gYear (2000 people)" in
    {
                parse("en", "xsd:gYear", "2000 people") should equal (None)
    }
    "DateTimeParser" should "not return gYear (url = http://www.bartleby.com/65/ho/Hoover-J.html)" in
    {
                parse("en", "xsd:gYear", "url = http://www.bartleby.com/65/ho/Hoover-J.html") should equal (None)
    }
    */

    //gMonthDay positive tests - Input is valid

    "DateTimeParser" should "return gMonthDay (4th of July)" in
    {
        parse("en", "xsd:gMonthDay", "4th of July") should equal (Some("--07-04"))
    }

    //gMonthDay negative tests - Input is invalid

    "DateTimeParser" should "not return gMonthDay (32nd of July)" in
    {
           parse("en", "xsd:gMonthDay", "32nd of July") should equal (None)
    }
    "DateTimeParser" should "not return gMonthDay (February)" in
    {
           parse("en", "xsd:gMonthDay", "February ") should equal (None)
    }

    //gMonthDay  testing different syntax

    "Data Parser" should "return gMonthDay (May 1)" in
    {
        parse("en", "xsd:gMonthDay", "May 1") should equal (Some("--05-01"))
    }

    "Data Parser" should "return gMonthDay (December 3rd)" in
    {
        parse("en", "xsd:gMonthDay", "December 3rd") should equal (Some("--12-03"))
    }

    //gYearMonth positive tests - Input is valid

    "DateTimeParser" should "return gYearMonth (June 2007)" in
    {
        parse("en", "xsd:gYearMonth", "June 2007") should equal (Some("2007-06"))
    }

    "DateTimeParser" should "return gYearMonth (June 2007 BC)" in
    {
        parse("en", "xsd:gYearMonth", "June 2007 BC") should equal (Some("-2007-06"))
    }

    //gYearMonth negative tests - Input is invalid

    "DateTimeParser" should "not return gYearMonth (2007)" in
    {
        parse("en", "xsd:YearMonth", "2007") should equal (None)
    }

    //gYearMonth testing different syntax

    "DateTimeParser" should "return gYearMonth (June, 2007)" in
    {
        parse("en", "xsd:gYearMonth", "June, 2007") should equal (Some("2007-06"))
    }

    "DateTimeParser" should "return gYearMonth (1[[429 January]] [[300 AD]])" in
    {
        parse("en", "xsd:gYearMonth", "1[[429 January]] [[300 AD]]") should equal (Some("0300-01"))
    }

    "DateTimeParser" should "return gYearMonth (Bradley's Barn, [[Mt. Juliet]], [[Tennessee]], October 1969)" in
    {
        parse("en", "xsd:gYearMonth", "Bradley's Barn, [[Mt. Juliet]], [[Tennessee]], October 1969") should equal (Some("1969-10"))
    }

    "DateTimeParser" should "return gYearMonth (December 1959, at Dukoff Studios, [[Miami, Florida|Miami, Fla.]])" in
    {
          parse("en", "xsd:gYearMonth", "December 1959, at Dukoff Studios, [[Miami, Florida|Miami, Fla.]]") should equal (Some("1959-12"))
    }

    //date positive tests - Input is valid

    "DateTimeParser" should "return date (June, 21 2007)" in
    {
        parse("en", "xsd:date", "June, 21 2007") should equal (Some("2007-06-21"))
    }

    "DateTimeParser" should "return date (June, 31 2007)" in
    {
      parse("en", "xsd:date", "June, 31 2007") should equal (None)
    }

    "DateTimeParser" should "return date (February, 30 2007)" in
    {
      parse("en", "xsd:date", "February, 30 2007") should equal (None)
    }

    "DateTimeParser" should "return date (June, 21 2007 09:32)" in
    {
        parse("en", "xsd:date", "June, 21 2007 09:32") should equal (Some("2007-06-21"))
    }

    "DateTimeParser" should "return date (June, 21 2007 BC)" in
    {
        parse("en", "xsd:date", "June, 21 2007 BC") should equal (Some("-2007-06-21"))
    }
    "DateTimeParser" should "return date (28 June 548 (aged 48))" in
    {
        parse("en", "xsd:date", "28 June 548 (aged 48)") should equal (Some("0548-06-28"))
    }

    /*"DateTimeParser" should "return date (|1912|10|12|)" in
    {
        parse("en", "xsd:date", "|1912|10|12|") should equal (Some("1912-10-12"))
    }*/

    "DateTimeParser" should "return date (1st May 2006)" in
    {
        parse("en", "xsd:date", "1st May 2006") should equal (Some("2006-05-01"))
    }

    /*"DateTimeParser" should "return date (|1912|1|1)" in
    {
        parse("en", "xsd:date", "|1912|1|1") should equal (Some("1912-01-01"))
    }

    "DateTimeParser" should "return date (|1912|1|1|1934|1|1 /)" in
    {
        parse("en", "xsd:date", "|1912|1|1|1934|1|1") should equal (Some("1912-01-01"))
    }

    "DateTimeParser" should "return date (|1912|1|1|1934|1|1)" in
    {
        parse("en", "xsd:date", "|1912|1|1|1934|1|1") should equal (Some("1934-01-01"))
    }*/

    "DateTimeParser" should "return date (6 June 07)" in
    {
        parse("en", "xsd:date", "6 June 07") should equal (Some("2007-06-06"))
    }

    "DateTimeParser" should "return date (16. March 1969, 08:20 UTC)" in
    {
        parse("en", "xsd:date", "16. March 1969, 08:20 UTC") should equal (Some("1969-03-16"))
    }

    // TODO: Should we account for Locale rules for dates?
    // e.g. DateFormat.SHORT for Locale.GERMAN uses dots to separate parts of the date
    // or DateFormat.SHORT for Locale.ENGLISH expects the month to be before the day,
    // although this is only true for en-US while en-GB is the opposite
    "DateTimeParser" should "return date Jan 10th 2007" in
    {
        parse("en", "xsd:date", "10/01/2007") should equal (Some("2007-01-10"))
    }

    "DateTimeParser" should "return date Jan 10th 2200" in
    {
        parse("en", "xsd:date", "10-01-2200") should equal (Some("2200-01-10"))
    }

    //date negative tests - Input is invalid

    "DateTimeParser" should "not return date ([[13991-10-25]])" in
    {
        parse("en", "xsd:date", "[[13991-10-25]]") should equal (None)
    }

    "DateTimeParser" should "not return date (19999-12-24)" in
    {
        parse("en", "xsd:date", "19999-12-24") should equal (None)
    }

    "DateTimeParser" should "not return date (00-44-00000)" in
    {
        parse("en", "xsd:date", "00-44-00000") should equal (None)
    }

    "DateTimeParser" should "not return date (010/10/20072)" in
    {
        parse("en", "xsd:date", "010/10/20072") should equal (None)
    }

    //date testing different syntax
    // This should fail as 2 digits years are too ambiguous, better not extract anything
    "DateTimeParser" should "return date (07-24-07)" in
    {
        parse("en", "xsd:date", "07-24-07") should equal (None) // (Some("2007-07-24"))
    }
    "DateTimeParser" should "return date ([[September 26]] , 1995)" in
    {
        parse("en", "xsd:date", "[[September 26]] , 1995") should equal (Some("1995-09-26"))
    }

    "DateTimeParser" should "return date ({{birth date |1912|10|12|}})" in
    {
        parse("en", "xsd:date", "{{birth date |1912|10|12|}}") should equal (Some("1912-10-12"))
    }

    "DateTimeParser" should "return date ({{Birth date|1974|8|16|df=no}})" in
    {
        parse("en", "xsd:date", "{{Birth date|1974|8|16|df=no}}") should equal (Some("1974-08-16"))
    }

    "DateTimeParser" should "return date ([[1st May]] [[2006]])" in
    {
        parse("en", "xsd:date", "[[1st May]] [[2006]]") should equal (Some("2006-05-01"))
    }

    "DateTimeParser" should "return date ({{start date|2006|9|16}})" in
    {
        parse("en", "xsd:date", "{{start date|2006|9|16}}") should equal (Some("2006-09-16"))
    }

    "DateTimeParser" should "return date ({{Bda|1981|06|03}})" in
    {
        parse("en", "xsd:date", "{{Bda|1981|06|03}}") should equal (Some("1981-06-03"))
    }

    "DateTimeParser" should "return date ({{Bda|1981|06|03|mf=y}})" in
    {
        parse("en", "xsd:date", "{{Bda|1981|06|03|mf=y}}") should equal (Some("1981-06-03"))
    }

    "DateTimeParser" should "return date ({{Birth date|df=y|1879|3|14}})" in
    {
        parse("en", "xsd:date", "{{Birth date|df=y|1879|3|14}}") should equal (Some("1879-03-14"))
    }

    "DateTimeParser" should "return date ({{Birth date|df=yes|1879|3|14}})" in
    {
        parse("en", "xsd:date", "{{Birth date|df=yes|1879|3|14}}") should equal (Some("1879-03-14"))
    }

    "DateTimeParser" should "return date ({{Birth date|mf=y|1879|3|14}})" in
    {
        parse("en", "xsd:date", "{{Birth date|mf=y|1879|3|14}}") should equal (Some("1879-03-14"))
    }

    "DateTimeParser" should "return date ({{Birth date|mf=yes|1879|3|14}})" in
    {
        parse("en", "xsd:date", "{{Birth date|mf=yes|1879|3|14}}") should equal (Some("1879-03-14"))
    }

    "DateTimeParser" should "return date ({{Birth date| df=y |1879|3|14}})" in
    {
        parse("en", "xsd:date", "{{Birth date| df=y |1879|3|14}}") should equal (Some("1879-03-14"))
    }

    "DateTimeParser" should "return date ({{Birth date| df=yes |1879|3|14}})" in
    {
        parse("en", "xsd:date", "{{Birth date| df=yes |1879|3|14}}") should equal (Some("1879-03-14"))
    }

    "DateTimeParser" should "return date ({{Birth date| mf=y |1879|3|14}})" in
    {
        parse("en", "xsd:date", "{{Birth date| mf=y |1879|3|14}}") should equal (Some("1879-03-14"))
    }

    "DateTimeParser" should "return date ({{Birth date| mf=yes |1879|3|14}})" in
    {
        parse("en", "xsd:date", "{{Birth date| mf=yes |1879|3|14}}") should equal (Some("1879-03-14"))
    }

    "DateTimeParser" should "return date ({{birth date|1849|4|24|mf=y}})" in
    {
        parse("en", "xsd:date", "{{birth date|1849|4|24|mf=y}}") should equal (Some("1849-04-24"))
    }

    "DateTimeParser" should "return date ({{Birth date | 1993 | 2 | 24 | df=yes }})" in
    {
        parse("en", "xsd:date", "{{Birth date | 1993 | 2 | 24 | df=yes }}") should equal (Some("1993-02-24"))
    }

    "DateTimeParser" should "return date ({{birth date and age|1941|2|20}})" in
    {
        parse("en", "xsd:date", "{{birth date and age|1941|2|20}}") should equal (Some("1941-02-20"))
    }

    "DateTimeParser" should "return date ({{Birth date and age | 1993 | 2 | 24 | df=yes }})" in
    {
        parse("en", "xsd:date", "{{Birth date and age | 1993 | 2 | 24 | df=yes }}") should equal (Some("1993-02-24"))
    }

    "DateTimeParser" should "return date ({{birth date and age|1955|10|28}})" in
    {
        parse("en", "xsd:date", "{{birth date and age|1955|10|28}}") should equal (Some("1955-10-28"))
    }

    "DateTimeParser" should "return date ({{birth date and age|1973|2|18}})" in
    {
        parse("en", "xsd:date", "{{birth date and age|1973|2|18}}") should equal (Some("1973-02-18"))
    }

    "DateTimeParser" should "return date ({{birth date and age|1965|2|5|df=y}})" in
    {
        parse("en", "xsd:date", "{{birth date and age|1965|2|5|df=y}}") should equal (Some("1965-02-05"))
    }

    // We do not have redirects in the test context
    /* https://en.wikipedia.org/w/index.php?title=Template:Dda&action=edit => #REDIRECT [[Template:Death date and age]]
    "DateTimeParser" should "return date ({{Dda|1966|7|19|1887|5|21|df=yes}})" in
    {
        parse("en", "xsd:date", "{{Dda|1966|7|19|1887|5|21|df=yes}}") should equal (Some("1966-07-19"))
    }
     */

    "DateTimeParser" should "return date ({{Death date|1993|2|4|df=yes}})" in
    {
        parse("en", "xsd:date", "{{Death date|1993|2|4|df=yes}}") should equal (Some("1993-02-04"))
    }

    "DateTimeParser" should "return date ({{Death date and age|1916|7|3|1849|4|24|mf=y}})" in
    {
        parse("en", "xsd:date", "{{Death date and age|1916|7|3|1849|4|24|mf=y}}") should equal (Some("1916-07-03"))
    }

    "DateTimeParser" should "return date ({{death date and age|1966|7|19|1887|5|21}})" in
    {
        parse("en", "xsd:date", "{{death date and age|1966|7|19|1887|5|21}}") should equal (Some("1966-07-19"))
    }

    "DateTimeParser" should "return date ({{Death date and age|df=yes|1955|4|18|1879|3|14}})" in
    {
        parse("en", "xsd:date", "{{Death date and age|df=yes|1955|4|18|1879|3|14}}") should equal (Some("1955-04-18"))
    }

    "DateTimeParser" should "return date ({{BirthDeathAge|1976|1|1|2007|1|1}})" in
    {
        parse("en", "xsd:date", "{{birthDeathAge|1976|1|1|2007|1|1}}") should equal (Some("2007-01-01"))
    }

    "DateTimeParser" should "return date ({{birthDeathAge|1976|1|1|2007|1|1}})" in
    {
        parse("en", "xsd:date", "{{birthDeathAge|1976|1|1|2007|1|1}}") should equal (Some("2007-01-01"))
    }

    "DateTimeParser" should "return date ({{BirthDeathAge|B|1976|1|1|2007|1|1}})" in
    {
        parse("en", "xsd:date", "{{birthDeathAge|B|1976|1|1|2007|1|1}}") should equal (Some("1976-01-01"))
    }

    "DateTimeParser" should "return date ({{birthDeathAge|B|1976|1|1|2007|1|1}})" in
    {
        parse("en", "xsd:date", "{{birthDeathAge|B|1976|1|1|2007|1|1}}") should equal (Some("1976-01-01"))
    }

    "DateTimeParser" should "return date 16 Jan 1948 from {{death-date|January 16, 1948}}" in
    {
      println(parse("en", "xsd:date", "{{death-date|January 16, 1948 }} (aged {{age|1878|7|13|1948|1|16}})").get)
      parse("en", "xsd:date", "{{death-date|January 16, 1948 }} (aged {{age|1878|7|13|1948|1|16}})") should equal (Some("1948-01-16"))
    }

    "DateTimeParser" should "return date 16 Jan 1948 from {{birth-date|January 16, 1948}}" in
    {
        parse("en", "xsd:date", "{{birth-date|January 16, 1948}}") should equal (Some("1948-01-16"))
    }

    "DateTimeParser" should "return date (02 May 151)" in
    {
        parse("en", "xsd:date", "02 May 151") should equal (Some("0151-05-02"))
    }

    "DateTimeParser" should "return date (09:32, April 6 2000 (UTC))" in
    {
        parse("en", "xsd:date", "09:32, April 6 2000 (UTC)") should equal (Some("2000-04-06"))
    }

    // https://github.com/dbpedia/extraction-framework/issues/163
    "DateTimeParser" should """return date April 5th 2012 from 'April 5th 2012'""" in
    {
        parse("en", "xsd:date", "April 5th 2012") should equal (Some("2012-04-05"))
    }
    "DateTimeParser" should "return year 1470" in
    {
        parse("en", "xsd:gYear", "1470, about") should equal (Some("1470"))
    }
    "DateTimeParser" should """return date March 16, 1520 from '1520, March 16'""" in
    {
        parse("en", "xsd:date", "1520, March 16") should equal (Some("1520-03-16"))
    }
    "DateTimeParser" should "return date October 26, 1945" in
    {
        parse("en", "xsd:date", "October 26, 1945 (possibly)") should equal (Some("1945-10-26"))
    }
    // Should this return both dates?!
    /*"DateTimeParser" should "return date February 20, 1784 from 'February 20 or 29, 1784'" in
    {
        parse("en", "xsd:date", "February 20 or 29, 1784") should equal (Some("1784-02-20"))
    }*/
    // end of https://github.com/dbpedia/extraction-framework/issues/163

    "DateTimeParser" should "return date (April 6. 2000)" in
    {
        parse("en", "xsd:date", "April 6. 2000") should equal (Some("2000-04-06"))
    }
    "DateTimeParser" should "return date (April 6 2007)" in
    {
        parse("en", "xsd:date", "April 6 2007") should equal (Some("2007-04-06"))
    }
    "DateTimeParser" should "return date (6 April 2007)" in
    {
        parse("en", "xsd:date", "6 April 2007") should equal (Some("2007-04-06"))
    }

    "DateTimeParser" should "return date (2 May 207 (UTC))" in
    {
        parse("en", "xsd:date", "2 May 207 (UTC)") should equal (Some("0207-05-02"))
    }
    "DateTimeParser" should "return date (12 June 2008)" in
    {
        parse("en", "xsd:date", "12 June 2008") should equal (Some("2008-06-12"))
    }
    "DateTimeParser" should "return date (grr10/10/2007bla)" in
    {
        parse("en", "xsd:date", "grr10/10/2007bla") should equal (Some("2007-10-10"))
    }

    //greek date tests

    "DateTimeParser" should "return date (02 Μαρτίου 151)" in
    {
        parse("el", "xsd:date", "02 Μαρτίου 151") should equal (Some("0151-03-02"))
    }
    "DateTimeParser" should "return gYear (20 π.Χ.)" in
    {
        parse("el", "xsd:gYear", "20 π.Χ.") should equal (Some("-0020"))
    }
    "DateTimeParser" should "return gYear (20 πΧ)" in
    {
        parse("el", "xsd:gYear", "20 πΧ") should equal (Some("-0020"))
    }
    "DateTimeParser" should "return gYear (20 Π.Χ.)" in
    {
        parse("el", "xsd:gYear", "20 Π.Χ.") should equal (Some("-0020"))
    }
    "DateTimeParser" should "return gYear (20 ΠΧ)" in
    {
        parse("el", "xsd:gYear", "20 ΠΧ") should equal (Some("-0020"))
    }
    "DateTimeParser" should "return gYear (20 μ.Χ.)" in
    {
        parse("el", "xsd:gYear", "20 μ.Χ.") should equal (Some ("0020"))
    }
    "DateTimeParser" should "return gYear (20 μΧ)" in
    {
        parse("el", "xsd:gYear", "20 μΧ") should equal (Some ("0020"))
    }
    /*"DateTimeParser" should "return gYear (14ος αιώνας)" in
    {
        parse("el", "xsd:gYear", "14ος αιώνας") should equal (Some("1300"))
    }*/
    "DateTimeParser" should "return gMonthDay (4η ιουλίου)" in
    {
        parse("el", "xsd:gMonthDay", "4η ιουλίου") should equal (Some("--07-04"))
    }
    "DateTimeParser" should "return gYearMonth (σεπτέμβριος 2007)" in
    {
        parse("el", "xsd:gYearMonth", "σεπτέμβριος 2007") should equal (Some("2007-09"))
    }
    "DateTimeParser" should "return gYearMonth (1[[429 ιανουαρίου]] [[300 μ.Χ.]])" in
    {
        parse("el", "xsd:gYearMonth", "1[[429 ιανουαρίου]] [[300 μ.Χ.]]") should equal (Some("0300-01"))
    }
    "DateTimeParser" should "return date (ιούνιος, 21 2007 π.Χ.)" in
    {
        parse("el", "xsd:date", "ιούνιος, 21 2007 π.Χ.") should equal (Some("-2007-06-21"))
    }
    "DateTimeParser" should "return date (1η δεκεμβρίου 2006)" in
    {
        parse("el", "xsd:date", "1η δεκεμβρίου 2006") should equal (Some("2006-12-01"))
    }
    "DateTimeParser" should "return date ([[1η μαΐου]] [[2006]])" in
    {
        parse("el", "xsd:date", "[[1η μαΐου]] [[2006]]") should equal (Some("2006-05-01"))
    }
    "DateTimeParser" should "return date (12 ιουνίου 2008)" in
    {
        parse("el", "xsd:date", "12 ιουνίου 2008") should equal (Some("2008-06-12"))
    }
    "DateTimeParser" should "return date (12 ιούνιος 2008)" in
    {
        parse("el", "xsd:date", "12 ιούνιος 2008") should equal (Some("2008-06-12"))
    }
    "DateTimeParser" should "return date ([[2 Νοεμβρίου]] [[1911]])" in
    {
        parse("el", "xsd:date", "[[2 Νοεμβρίου]] [[1911]]") should equal (Some("1911-11-02"))
    }
    "DateTimeParser" should "return date ({{ηθηλ|1996|03|18|1911|11|2}})" in
    {
        parse("el", "xsd:date", "{{ηθηλ|1996|03|18|1911|11|2}}") should equal (Some("1996-03-18"))
    }
    "DateTimeParser" should "return date ({{ηγη|1996|03|18}})" in
    {
        parse("el", "xsd:date", "{{ηθηλ|1996|03|18}}") should equal (Some("1996-03-18"))
    }
  
    
    "DateTimeParser" should "return date (1996 March the 18th)" in
    {
        parse("fr", "xsd:date", "{{Date|18|03|1996}}") should equal (Some("1996-03-18"))
    }
    "DateTimeParser" should "return date (1996 September the 19th)" in
    {
        parse("fr", "xsd:date", "{{date|19|septembre|1996}}") should equal (Some("1996-09-19"))
    }
    "DateTimeParser" should "return date (March the 15th of the year 44 before J.-C.)" in
    {
        parse("fr", "xsd:date", "15 mars 44 av. J.-C.") should equal (Some("-0044-03-15"))
    }
    "DateTimeParser" should "return date (March the 15th of the year 44 after J.-C.)" in
    {
        parse("fr", "xsd:date", "15 mars 44 ap. J.-C.") should equal (Some("0044-03-15"))
    }
    "DateTimeParser" should "return date (March the 15th of the year 1944)" in
    {
        parse("fr", "xsd:date", "15 mars 44") should equal (Some("1944-03-15"))
    }
    "DateTimeParser" should "return date (January the 1st of the year -711)" in
    {
        parse("fr", "xsd:date", "{{Date de naissance|1|1|-711}}") should equal (Some("-0711-01-01"))
    }
    /*"DateTimeParser" should "return date (July the 13th of the year 100 before J.-C.)" in
    {
        parse("fr", "xsd:date", "13 juillet -100 av. J.-C.") should equal (Some("-0100-07-13"))
    }*/
    "DateTimeParser" should "not read \"15\" as a date" in
    {
        parse("fr", "xsd:date", "15") should equal (None)
    }
    "DateTimeParser" should "not read \"15.25\" as a date" in
    {
    	parse("en", "xsd:date", "15.25") should equal (None)
    	parse("fr", "xsd:date", "15.25") should equal (None)
    }
    "DateTimeParser" should "not read \"15,25\" as a date" in
    {
    	parse("en", "xsd:date", "15,25") should equal (None)
    	parse("fr", "xsd:date", "15,25") should equal (None)
    }

    "DataParser" should "return date ({{生年月日と年齢|1989|7|23}})" in
    {
        parse("ja", "xsd:date", "{{生年月日と年齢|1989|7|23}}") should equal (Some("1989-07-23"))
    }
    "DataParser" should "return date ({{生年月日と年齢|1989|7|23|死亡}})" in
    {
        parse("ja", "xsd:date", "{{生年月日と年齢|1989|7|23|死亡}}") should equal (Some("1989-07-23"))
    }
    "DataParser" should "return date ({{生年月日と年齢2|1988|6|10|1961|7|4}})" in
    {
        parse("ja", "xsd:date", "{{生年月日と年齢2|1988|6|10|1961|7|4}}") should equal (Some("1961-07-04"))
    }
    "DataParser" should "return date ({{生年月日と馬齢|p=0|2002|1|1}})" in
    {
        parse("ja", "xsd:date", "{{生年月日と馬齢|p=0|2002|1|1}}") should equal (Some("2002-01-01"))
    }
    "DataParser" should "return date ({{死亡年月日と没年齢|1940|10|9|1980|12|8}})" in
    {
        parse("ja", "xsd:date", "{{死亡年月日と没年齢|1940|10|9|1980|12|8}}") should equal (Some("1980-12-08"))
    }
    "DataParser" should "return date ({{死亡年月日と没馬齢|p=1|1973|4|19|1978|3|5}})" in
    {
        parse("ja", "xsd:date", "{{死亡年月日と没馬齢|p=1|1973|4|19|1978|3|5}}") should equal (Some("1978-03-05"))
    }
    "DataParser" should "return date ({{没年齢|1940|10|9|1980|12|8}})" in
    {
        parse("ja", "xsd:date", "{{没年齢|1940|10|9|1980|12|8}}") should equal (Some("1980-12-08"))
    }
    "DataParser" should "return date ({{没年齢2|1940|10|9|1980|12|8}})" in
    {
        parse("ja", "xsd:date", "{{没年齢2|1893|10|9|1947|12|8}}") should equal (Some("1947-12-08"))
    }
    "DataParser" should "return date ({{年数|1940|10|9|1980|12|8}})" in
    {
        parse("ja", "xsd:date", "{{年数|1989|7|23}}") should equal (Some("1989-07-23"))
    }
    "DataParser" should "return gYear ({{年|1980}})" in
    {
        parse("ja", "xsd:gYear", "{{年|1980}}") should equal (Some("1980"))
    }
    "DataParser" should "return gYear (1990年)" in
    {
        parse("ja", "xsd:gYear", "1990年") should equal (Some("1990"))
    }
    "DataParser" should "return date (紀元前20年)" in
    {
        parse("ja", "xsd:gYear", "紀元前20年") should equal (Some("-0020"))
    }
    "DataParser" should "return date (前259年)" in
    {
        parse("ja", "xsd:gYear", "前259年") should equal (Some("-0259"))
    }
    "DataParser" should "return date (西暦2012年)" in
    {
        parse("ja", "xsd:gYear", "西暦2012年") should equal (Some("2012"))
    }


    private val wikiParser = WikiParser.getInstance()

    private def parse(language : String, datatype : String, input : String) : Option[String] =
    {
        val lang = Language(language)
        val context = new
        {
            def language : Language = lang
            def ontology: Ontology = ontology
            def redirects : Redirects = new Redirects(Map())
            def recorder[T: ClassTag] : ExtractionRecorder[T] = new ExtractionRecorder[T]()
        }
        val dateParser = new DateTimeParser(context, new Datatype(datatype), false)
        val page = new WikiPage(WikiTitle.parse("TestPage", lang), input)

      wikiParser(page) match {
        case Some(n) => dateParser.parse(n).map(_.toString)
        case None => None
      }


    }
}
