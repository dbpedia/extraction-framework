package org.dbpedia.util.text;

public class DefaultAppender
implements Appender
{
  /** the singleton instance */
  public static final Appender INSTANCE = new DefaultAppender();
  
  public void appendCodePoint(StringBuilder builder, int codePoint)
  {
    builder.appendCodePoint(codePoint);
  }

  public void append(StringBuilder builder, String str)
  {
    builder.append(str);
  }
  
}
