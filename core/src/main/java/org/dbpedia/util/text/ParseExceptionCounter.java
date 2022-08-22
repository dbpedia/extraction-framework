package org.dbpedia.util.text;


/**
 * A {@link ParseExceptionHandler} that counts errors.
 */
public class ParseExceptionCounter
implements ParseExceptionHandler
{
  /** number of errors since last {@link #reset()}. */
  private int _errors;
  
  /**
   * Increments number of errors by one.
   * @param error must not be {@code null}
   */
  @Override
  public void error( ParseException error )
  {
    if (error == null) throw new NullPointerException("error");
    _errors++;
  }
  
  /**
   * @return number of errors since last {@link #reset()}
   */
  public int errors()
  {
    return _errors;
  }
  
  /**
   * Set number of errors back to zero.
   */
  public void reset()
  {
    _errors = 0;
  }
}
