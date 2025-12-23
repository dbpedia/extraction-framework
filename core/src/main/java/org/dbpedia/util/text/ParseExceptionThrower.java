package org.dbpedia.util.text;

/**
 * A {@link ParseExceptionHandler} that simply throws the given error.
 */
public class ParseExceptionThrower
implements ParseExceptionHandler
{
  /** the singleton instance */
  public static final ParseExceptionHandler INSTANCE = new ParseExceptionThrower();
  
  /**
   * No other instances, please.
   */
  private ParseExceptionThrower() { /**/ }
  
  /**
   * @param error must not be {@code null}
   * @throws ParseException given error
   */
  @Override
  public void error( ParseException error )
  {
    throw error;
  }
}
