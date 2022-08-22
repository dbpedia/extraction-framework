package org.dbpedia.util.text;

public class DefaultAppender
implements Appender
{
  /** the singleton instance */
  public static final Appender INSTANCE = new DefaultAppender();
  
  @Override
  public void append(StringBuilder builder, int code)
  {
    builder.appendCodePoint(code);
  }

  @Override
  public void append(StringBuilder builder, String str)
  {
    builder.append(str);
  }
  
}
