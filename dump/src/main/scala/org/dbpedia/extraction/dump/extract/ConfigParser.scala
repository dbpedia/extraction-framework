package org.dbpedia.extraction.dump.extract

import java.util.Properties

abstract class ConfigParser(config : Properties)
{
  protected def splitValue(key: String, sep: Char): List[String] = {
    val values = config.getProperty(key)
    if (values == null) List.empty
    else split(values, sep)
  }

  protected def split(value: String, sep: Char): List[String] = {
    value.split("["+sep+"\\s]+", -1).map(_.trim).filter(_.nonEmpty).toList
  }

  protected def error(message: String, cause: Throwable = null): IllegalArgumentException = {
    new IllegalArgumentException(message, cause)
  }
    
}