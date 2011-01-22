package org.dbpedia.util.text;

/**
 * A {@link ParseExceptionHandler} that never throws the given error.
 */
public class ParseExceptionIgnorer
implements ParseExceptionHandler
{
  /** the singleton instance */
  public static final ParseExceptionHandler INSTANCE = new ParseExceptionIgnorer();
  
  /**
   * No other instances, please.
   */
  private ParseExceptionIgnorer() { /**/ }
  
  /**
   * @param error ignored
   * @throws ParseException never
   */
  @Override
  public void error( ParseException error )
  {
    /* relax, take it easy... */
  }
}
