/**
 * 
 */
package org.dbpedia.util.text;

/**
 */
@SuppressWarnings("serial")
public class ParseException 
extends RuntimeException
{
  /** input {@link String} in which the error was found, may be {@code null} */
  private final String _input;
  
  /** position in {@link #_input input string} where the error was found */
  private final int _position;

  /** short description of the error, may be {@code null} */
  private final String _description;
  
  /**
   * @param input input {@link String} in which the error was found, may be {@code null}
   * @param position position in input {@link String} where the error was found.
   * If an input {@link String} is given, position must be greater than or equal to zero and less 
   * than the length of the input {@link String}, or {@code -1} to indicate that position is unknown.
   * If no input {@link String} is given position is set to {@code -1}.
   * @param description short description of the error, must not be {@code null}
   * @throws IllegalArgumentException if an input {@link String} is given and position is invalid
   */
  public ParseException( String input, int position, String description )
  {
    this(buildMessage(input, position, description), input, position, description);
  }
  
  /**
   * @param message exception message
   * @param input input {@link String} in which the error was found, may be {@code null}
   * @param position position in input {@link String} where the error was found.
   * If an input {@link String} is given, position must be greater than or equal to zero and less 
   * than the length of the input {@link String}, or {@code -1} to indicate that position is unknown.
   * If no input {@link String} is given position is set to {@code -1}.
   * @param description short description of the error, may be {@code null}
   * @throws IllegalArgumentException if an input {@link String} is given and position is invalid
   */
  public ParseException( String message, String input, int position, String description )
  {
    super(message);
    _input = input;
    _position = checkPosition(input, position);
    _description = description;
  }
  
  /**
   * @param input input {@link String} in which the error was found, may be {@code null}
   * @param position position in input {@link String} where the error was found.
   * If an input {@link String} is given, position must be greater than or equal to zero and less 
   * than the length of the input {@link String}, or {@code -1} to indicate that position is unknown.
   * If no input {@link String} is given, position is ignored.
   * @param description short description of the error, must not be {@code null}
   * @return exception message
   * @throws IllegalArgumentException if an input {@link String} is given and position is invalid
   */
  private static String buildMessage( String input, int position, String description )
  {
    StringBuilder sb = new StringBuilder();
    addDescription(description, sb);
    addDetail(input, position, sb);
    return sb.toString();
  }

  /**
   * @param description short description of the error, must not be {@code null}
   * @param target must not be {@code null}
   */
  protected static void addDescription( String description, StringBuilder target )
  {
    if (target == null) throw new NullPointerException("target");
    if (description == null) throw new NullPointerException("description");
    
    target.append(description);
  }

  /**
   * Add 80 characters from given input {@link String} around the given position to given
   * {@link StringBuilder}. Do nothing if input {@link String} is {@code null} or position 
   * is {@code -1}.
   * @param input input {@link String} in which the error was found, may be {@code null}
   * @param position position in input {@link String} where the error was found.
   * If an input {@link String} is given, position must be greater than or equal to zero and less 
   * than the length of the input {@link String}, or {@code -1} to indicate that position is unknown.
   * If no input {@link String} is given, position is ignored.
   * @param target must not be {@code null}
   * @throws IllegalArgumentException if an input {@link String} is given and position is invalid
   */
  protected static void addDetail( String input, int position, StringBuilder target )
  {
    if (target == null) throw new NullPointerException("target");
    if (checkPosition(input, position) == -1) return;  
        
    int length = input.length();
    target.append(" at position [").append(position).append("]: \"");
    if (position - 30 > 0) target.append("...");
    target.append(input.substring(Math.max(position - 30, 0), Math.min(position + 50, length)));
    if (position + 50 < length) target.append("...");
    target.append("\"");
  }

  /**
   * @param input input {@link String} in which the error was found, may be {@code null}
   * @param position position in input {@link String} where the error was found.
   * If an input {@link String} is given, position must be greater than or equal to zero and less 
   * than the length of the input {@link String}, or {@code -1} to indicate that position is unknown.
   * If no input {@link String} is given, position is ignored.
   * @return given position if it is valid and an input {@link String} is given, {@code -1} if
   * no input {@link String} is given
   * @throws IllegalArgumentException if an input {@link String} is given and position is invalid
   */
  protected static int checkPosition( String input, int position )
  {
    if (input == null || position == -1) return -1; 
    int length = input.length();
    if (position < 0 || position >= length) throw new IllegalArgumentException("invalid position ["+position+"] for string of length ["+length+"]");
    return position;
  }

  /**
   * @return input {@link String} in which the error was found, may be {@code null}
   */
  public String getInput()
  {
    return _input;
  }

  /**
   * @return position in {@link #getInput() input string} where the error was found.
   * If an input string is given, position is greater than or equal to zero and less 
   * than the length of the input string, or {@code -1} to indicate that position is unknown.
   * If no input string is given, position is {@code -1}.
   */
  public int getPosition()
  {
    return _position;
  }

  /**
   * @return short description of the error, may be {@code null}
   */
  public String getDescription()
  {
    return _description;
  }

}
