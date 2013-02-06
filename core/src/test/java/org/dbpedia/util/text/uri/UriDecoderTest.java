package org.dbpedia.util.text.uri;

import junit.framework.TestCase;

public class UriDecoderTest extends TestCase
{
  public void testGood()
  {
    good("foo", "foo");
    good("%3F", "?");
    good("foo%3Fbar", "foo?bar");
    good("foo%3F%3Fbar", "foo??bar");
    good("foo%3F", "foo?");
    good("foo%C2%A0", "foo\u00A0");
    // test arbitrary 4-byte UTF-8 sequence
    good("foo%F0%A0%A0%A0", 'f', 'o', 'o', 0x20820);
    
    // don't decode invalid sequences
    good("foo%3", "foo%3");
    good("foo%3 ", "foo%3 ");
    good("foo%3G", "foo%3G");
    System.out.println(Integer.toHexString(' '));
    
    // do decode invalid UTF-8 sequences (FFFD is 'unknown')
    good("foo%C0", "foo\uFFFD");
    good("foo%C0%A0", "foo\uFFFD\uFFFD");
    good("foo%C2%A0%C0%A0", "foo\u00A0\uFFFD\uFFFD");
  }
  
  private void good( String string, int ... codePoints )
  {
    UriDecoder decoder = new UriDecoder(string);
    decoder.decode();
    char[] result = new char[codePoints.length * 2];
    int index = 0;
    for (int codePoint : codePoints)
    {
      index += Character.toChars(codePoint, result, index);
    }
    assertEquals(new String(result, 0, index), decoder.result());
  }

  private void good( String string, String result )
  {
    UriDecoder decoder = new UriDecoder(string);
    decoder.decode();
    assertEquals(result, decoder.result());
  }
}
