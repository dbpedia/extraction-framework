package org.dbpedia.util.text;

/**
 * Appends Unicode strings or code points to a {@link StringBuilder}.
 */
public interface Appender {
  
  /**
   * @param builder may or may not be null, depending on use case.
   * @param code code point to append
   */
  public void append(StringBuilder builder, int code);

  /**
   * @param builder may or may not be null, depending on use case.
   * @param str String to append
   */
  public void append(StringBuilder builder, String str);
  
}
