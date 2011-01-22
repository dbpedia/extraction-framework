package org.dbpedia.util.text;

/**
 */
public interface ParseExceptionHandler
{
  /**
   * @param error never {@code null}
   * @throws ParseException maybe...
   */
  public void error( ParseException error );
}
