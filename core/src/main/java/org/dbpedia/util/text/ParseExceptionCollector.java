package org.dbpedia.util.text;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link ParseExceptionHandler} that collects given errors in a {@link List}.
 */
public class ParseExceptionCollector
implements ParseExceptionHandler
{
  /** collected errors, must not be {@code null}. TODO: might use {@code ? super ParseException} */
  private List<ParseException> _errors;
  
  /**
   * A new {@link List} to collect errors will be created when necessary.
   */
  public ParseExceptionCollector()
  {
    // create list when necessary
  }

  /**
   * @param errors {@link List} in which errors are collected. If {@code null}, a new {@link List}
   * will be created when necessary.
   */
  public ParseExceptionCollector( List<ParseException> errors )
  {
    _errors = errors;
  }

  /**
   * Adds given error to the {@link List}.
   * @param error must not be {@code null}
   */
  @Override
  public void error( ParseException error )
  {
    if (error == null) throw new NullPointerException("error");
    if (_errors == null) _errors = new ArrayList<ParseException>();
    _errors.add(error);
  }
  
  /**
   * @return current {@link List} of errors, may be {@code null} or empty
   */
  public List<ParseException> errors()
  {
    return _errors;
  }
  
  /**
   * Clears the list of errors.
   */
  public void reset()
  {
    _errors.clear();
  }
}
