/**
 * 
 */
package org.dbpedia.util.text.html;

import org.dbpedia.util.text.ParseException;

/**
 */
@SuppressWarnings("serial")
public class HtmlReferenceException 
extends ParseException
{
  /** error description */
  public static final String NOT_CLOSED = "unclosed reference";
  
  /** error description */
  public static final String EMPTY = "empty reference";
  
  /** error description */
  public static final String TOO_SHORT = "invalid reference (too short)";
  
  /** error description */
  public static final String TOO_LONG = "invalid reference (too long)";
  
  /** error description */
  public static final String BAD_NAME = "reference to unknown entity";
  
  /** error description */
  public static final String BAD_NUMBER = "invalid numeric reference";
  
  /** the content of the invalid reference, may be {@code null} */
  private final String _reference;
  
  /**
   * @param input input {@link String} in which the error was found, may be {@code null}
   * @param position position in input {@link String} where the error was found.
   * If an input {@link String} is given, position must be greater than or equal to zero and less 
   * than the length of the input {@link String}, or {@code -1} to indicate that position is unknown.
   * If no input {@link String} is given position is set to {@code -1}.
   * @param description a short description why the reference is invalid, must not be {@code null}
   * @param reference the content of the invalid reference, may be {@code null}
   */
  public HtmlReferenceException( String input, int position, String description, String reference )
  {
    super(buildMessage(input, position, description, reference), input, position, description);
    _reference = reference;
  }
  
  /**
   * @param input input {@link String} in which the invalid reference was found, must not be {@code null}
   * @param position position in input {@link String} where the invalid reference was found, 
   * must be greater than or equal to zero and less than the length of the input {@link String}
   * @param description a short description why the reference is invalid, must not be {@code null}
   * @param reference the content of the invalid reference, may be {@code null}
   * @return exception message
   */
  private static String buildMessage( String input, int position, String description, String reference )
  {
    StringBuilder sb = new StringBuilder();
    addDescription(description, sb);
    if (reference != null) sb.append(" [").append(reference).append("]");
    addDetail(input, position, sb);
    return sb.toString();
  }

  /**
   * @return the invalid reference, may be {@code null}
   */
  public String getReference()
  {
    return _reference;
  }
  
}
