package org.dbpedia.extraction.server

import java.util.logging.{XMLFormatter, LogRecord, Handler}
import xml.{XML, Elem}

class XMLLogHandler extends Handler
{
    var list = List[LogRecord]()

    override def publish(record : LogRecord) : Unit = synchronized
    {
        if(record.getLevel.intValue >= getLevel.intValue) list ::= record
    }

    override def flush() : Unit =
    {
    }

    override def close() : Unit =
    {
    }

    def xml : Elem = synchronized
    {
        val f = new XMLFormatter()
        val logXML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n<log>\n" + list.map(f.format(_)).mkString + "</log>"
        XML.loadString(logXML)
    }
}
