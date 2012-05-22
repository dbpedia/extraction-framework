package org.dbpedia.extraction.util

import org.dbpedia.extraction.ontology.datatypes.Datatype
import javax.xml.datatype.DatatypeFactory
import org.dbpedia.extraction.ontology.RdfNamespace

class Date (val year: Option[Int] = None, val month: Option[Int] = None, val day: Option[Int] = None, val datatype: Datatype)
extends Ordered[Date]
{
    private val calendar = Date.datatypeFactory.newXMLGregorianCalendar()

    require(year.isEmpty || year.get != 0, "year must not be 0")
    require(month.isEmpty || month.get != 0, "month must not be 0")
    require(day.isEmpty || day.get != 0, "day must not be 0")

    datatype.name match
    {
         case "xsd:date" =>
         {
             require(!year.isEmpty && !month.isEmpty && !day.isEmpty, "Expected xsd:date")
             calendar.setDay(day.get)
             calendar.setMonth(month.get)
             calendar.setYear(year.get)
         }
         case "xsd:gDay" =>
         {
             require(!day.isEmpty, "Expected xsd:gDay")
             calendar.setDay(day.get)
         }
         case "xsd:gMonth" =>
         {
             require(!month.isEmpty, "Expected xsd:gMonth")
             calendar.setMonth(month.get)
         }
         case "xsd:gYear" =>
         {
             require(!year.isEmpty, "Expected xsd:gYear")
             calendar.setYear(year.get)
         }
         case "xsd:gMonthDay" =>
         {
             require(!month.isEmpty && !day.isEmpty, "Expected xsd:gMonthDay")
             calendar.setDay(day.get)
             calendar.setMonth(month.get)
         }
         case "xsd:gYearMonth" =>
         {
             require(!year.isEmpty && !month.isEmpty, "Expected xsd:gYearMonth")
             calendar.setMonth(month.get)
             calendar.setYear(year.get)
         }
         case _ => throw new IllegalArgumentException("Unsupported datatype: "+datatype)
    }
    
    // calendar.toXMLFormat happily returns strings like 2012-02-31
    require(calendar.isValid, "invalid date "+year.getOrElse("")+"-"+month.getOrElse("")+"-"+day.getOrElse(""))
    // FIXME: this "xsd:" thing is ugly. Datatype should contain its base uri.
    require(RdfNamespace.XSD.prefix+":"+calendar.getXMLSchemaType.getLocalPart == datatype.name, "invalid date "+year.getOrElse("")+"-"+month.getOrElse("")+"-"+day.getOrElse("")+" for "+datatype.name)

    override def compare(that: Date) : Int = this.calendar.compare(that.calendar)
    
    override def equals(other: Any): Boolean = other match {
      case that: Date => this.calendar.equals(that.calendar)
      case _ => false
    }
    
    override def hashCode: Int = this.calendar.hashCode
    
    override def toString = calendar.toXMLFormat
}

object Date
{
    // store the result of this expensive call
    private val datatypeFactory = DatatypeFactory.newInstance()
    
    def merge(dates : List[Date], datatype : Datatype) : Date =
    {
        require(! dates.isEmpty, "dates are required")
        
        val year  = dates.find(_.year.isDefined).map(_.year.get)
        val month = dates.find(_.month.isDefined).map(_.month.get)
        val day   = dates.find(_.day.isDefined).map(_.day.get)

        new Date(year, month, day, datatype)
    }
}