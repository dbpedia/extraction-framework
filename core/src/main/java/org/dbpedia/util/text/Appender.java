package org.dbpedia.util.text;

public interface Appender {
  
  public void appendCodePoint(StringBuilder builder, int codePoint);

  public void append(StringBuilder builder, String str);
  
}
