package org.dbpedia.util.text.nt;

import junit.framework.TestCase;

public class NtCharsDecoderTest extends TestCase
{
  public void testGood()
  {
    good("\\\\", "\\\\");
    good("\\\"", "\\\"");
    good("\\t", "\\t");
    good("\\n", "\\n");
    good("\\r", "\\r");

    good("\\\\\\\"\\t\\n\\r", "\\\\\\\"\\t\\n\\r");
    good("foo\\\\bar\\\"baz\\tfob\\nbob\\rboz", "foo\\\\bar\\\"baz\\tfob\\nbob\\rboz");

    good("\\u0000", 0x0000);
    good("\\u1234", 0x1234);
    good("\\u12345", 0x1234, '5');
    good("\\U00000000", 0x00000000);
    good("\\U00012345", 0x00012345);
    good("\\U000123456", 0x00012345, '6');
    good("\\U0010FFFF", 0x0010FFFF);
    good("F\\U0010FFFFF", 'F', 0x0010FFFF, 'F');
    good("\\u0020", " ");
    good("\\U00000020", " ");

    // we also escape decoded chars
    good("\\n\\u000A\\U0000000A", "\\n\\n\\n");
    good("\\n\\u000D\\U00000009", "\\n\\r\\t");
    good("\\r\\u0009\\U0000000A", "\\r\\t\\n");
    good("\\u00E4\\u00F6\\u00FC", "äöü");
    good("\\u00C4\\u00D6\\u00DC\\u00E4\\u00F6\\u00FC", "ÄÖÜäöü");

    // we also escape unenscaped chars
    good("\"\n\r\t", "\\\"\\n\\r\\t");
    good("äöü", "äöü");
    good("ÄÖÜäöü", "ÄÖÜäöü");
    good("ÄÖÜ\\u00E4\\u00F6\\u00FC", "ÄÖÜäöü");
    good("\\u00C4\\u00D6\\u00DCäöü", "ÄÖÜäöü");
  }

  public void testBad()
  {
    bad("\\");
    bad("\\x");
    bad("\\a");
    bad("\\\\\\");

    // missing u after backslash
    bad("\\u00E4\\u00F6\\00FC");

    bad("\\u1"); // too short
    bad("\\u12"); // too short
    bad("\\u123"); // too short

    bad("\\U1"); // too short
    bad("\\U12"); // too short
    bad("\\U1234"); // too short
    bad("\\U123456"); // too short
    bad("\\U0012345"); // too short
    bad("\\U0001234G"); // not hex
    bad("\\U00110000"); // too high
  }

  private void good( String string, int ... codePoints )
  {
    NtCharsDecoder decoder = new NtCharsDecoder(string);
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
    NtCharsDecoder decoder = new NtCharsDecoder(string);
    decoder.decode();
    assertEquals(result, decoder.result());
  }

  private void bad( String string )
  {
    try
    {
      new NtCharsDecoder(string).decode();
      fail("exception expected");
    }
    catch (IllegalArgumentException e)
    {
      // e.printStackTrace();
      assertEquals("invalid N-Triples string ["+string+"]", e.getMessage());
    }
  }

}
