package org.dbpedia.util.text.html;

import static org.dbpedia.util.text.html.HtmlReferenceException.*;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * TODO: test error handling
 */
public class HtmlCoderTest
extends TestCase
{
  public static TestSuite suite()
  {
    TestSuite suite = new TestSuite(HtmlCoderTest.class.getName());
    for (XmlCodes encode : XmlCodes.values())
    {
      for (XmlCodes keep : XmlCodes.values())
      {
        for (int mode = 0; mode < 2; mode++)
        {
          suite.addTest(new HtmlCoderTest(encode, keep, mode == 0));
        }
      }
    }
    return suite;
  }

  private final HtmlCoder _coder;

  private final XmlCodes _encode;

  private final XmlCodes _keep;

  private final boolean _good;

  public HtmlCoderTest( XmlCodes encode, XmlCodes keep, boolean good )
  {
    super("encode "+encode+", keep "+keep+", "+(good ? "good" : "bad")+" test");
    _coder = new HtmlCoder(encode, keep);
    _encode = encode;
    _keep = keep;
    _good = good;
  }

  @Override
  protected void runTest()
  {
    if (_good) good();
    else bad();
  }

  private void good()
  {
    good("", "");
    good(" ", " ");

    // special entities
    good("&amp;", "&amp;");
    good("&gt;", "&gt;");
    good("&lt;", "&lt;");
    good("&apos;", "&apos;");
    good("&quot;", "&quot;");

    // numeric references are converted to names
    good("&#38;", "&amp;");
    good("&#62;", "&gt;");
    good("&#60;", "&lt;");
    good("&#39;", "&apos;");
    good("&#34;", "&quot;");

    good(">", ">");
    good("<", "<");
    good("\'", "\'");
    good("\"", "\"");

    good("&amp;&lt;&gt;&apos;&quot;", "&amp;&lt;&gt;&apos;&quot;");

    // common weirdness...
    good("&amp;amp;", "&amp;amp;");
    good("&amp;lt;br /&amp;gt;", "&amp;lt;br /&amp;gt;");

    // decoded entities
    good("&Auml;", "Ä");
    good("&auml;", "ä");
    good("&Ouml;", "Ö");
    good("&ouml;", "ö");
    good("&Uuml;", "Ü");
    good("&uuml;", "ü");
    good("&szlig;", "ß");
    good("&beta;", "\u03B2");
    good("&Beta;", "\u0392");

    good("&Auml;&auml;&Ouml;&quot;&ouml;&Uuml;&uuml;&apos;&szlig;&beta;&Beta;", "ÄäÖ&quot;öÜü&apos;ß\u03B2\u0392");
    good("<&Auml;x&auml;y&Ouml;z&ouml;1&Uuml;2&uuml;3&szlig; &beta;\n&Beta;\r&gt;", "<ÄxäyÖzö1Ü2ü3ß \u03B2\n\u0392\r&gt;");
    good("<&Auml;&#120;&auml;&#121;&Ouml;&#122;&ouml;&#49;&lt;&Uuml;&#50;&uuml;&#51;&szlig;&#32;&beta;&#10;&Beta;&#13;", "<ÄxäyÖzö1&lt;Ü2ü3ß \u03B2\n\u0392\r");

    good(" &#32; &#34; \" ", "   &quot; \" ");
    good(" &#x20; &#39; \" ", "   &apos; \" ");
    good("&#65;A", "AA");
    good("&#065;B ", "AB ");
    good("&#0000065;BCDEF", "ABCDEF");
    good("&#x41;123", "A123");
    good("&#x041;", "A");
    good("&#x000041;", "A");

    good("A&#xBCDE;F", "A\uBCDEF");
    good(" &#65535;\n", " "+cp(65535)+"\n");
    good("\n&#xFFFF; ", "\n"+cp(65535)+" ");
    good("\n&#65536; ", "\n"+cp(65536)+" ");
    good("\n&#x10000; ", "\n"+cp(65536)+" ");
    good("\r A B\n&#654321; z ", "\r A B\n"+cp(654321)+" z ");
    good(" ! \" \' \u007F&#x7F;&#xFEDCB; 1 2 3 ", " ! \" \' "+cp(0x7F, 0x7F,0xFEDCB)+" 1 2 3 ");
    good("\r < > &#1114111; A >", "\r < > "+cp(1114111)+" A >");
    good("\t&#x10FFFF; 1 2 3 ", "\t"+cp(1114111)+" 1 2 3 ");
  }

  /**
   * @param codes code points
   * @return string containing given code points
   */
  private String cp( int ... codes )
  {
    StringBuilder sb = new StringBuilder();
    for (int code : codes)
    {
      sb.appendCodePoint(code);
    }
    return sb.toString();
  }

  private static char[] CHARS = {'&', '<', '>', '\"', '\''};

  private static String[] REFS = {"@amp;", "@lt;", "@gt;", "@quot;", "@apos;"};

  private void good( String string, String result )
  {
    if (result.contains("@")) throw new IllegalArgumentException("don't use '@' - ["+result+"]");

    // mask '&' entities that are already encoded as '@' entities
    result = result.replace('&', '@');

    // replace characters that should be encoded by their '&' entities
    for (int i = 0; i < CHARS.length; i++)
    {
      char ch = CHARS[i];
      String ref = _encode.encode(ch);
      if (ref != null) result = result.replace(Character.toString(ch), ref);
    }

    // decode the '@' entities that should not be kept encoded
    for (int i = 0; i < CHARS.length; i++)
    {
      char ch = CHARS[i];
      String ref = _keep.encode(ch);
      if (ref == null) result = result.replace(REFS[i], Character.toString(ch));
    }

    // now unmask the '@' entities that should be kept encoded
    result = result.replace('@', '&');

    assertEquals(result, _coder.code(string));
  }

  private void bad()
  {
    bad("&", 0, NOT_CLOSED, null);
    bad(" &", 1, NOT_CLOSED, null);
    bad(" & ", 1, NOT_CLOSED, null);
    bad(" & & ", 1, NOT_CLOSED, null);
    bad("x&x&x", 1, NOT_CLOSED, null);
    bad("  &;  ", 2, EMPTY, null);
    bad(" & ; ", 1, TOO_SHORT, " ");
    bad(" &         ; ", 1, TOO_LONG, null);
    bad(" &&;&gt;", 1, TOO_SHORT, "&");
    bad("&1;", 0, TOO_SHORT, "1");
    bad("&12;", 0, BAD_NAME, "12");
    bad("&xuml;", 0, BAD_NAME, "xuml");
    bad("&12345678;", 0, BAD_NAME, "12345678");
    bad("&lt;&  ;&gt;", 4, BAD_NAME, "  ");
    bad("&lt;&        ;&gt;", 4, BAD_NAME, "        ");
    bad("&123456789;", 0, TOO_LONG, null);

    bad("&lt;&;&gt;", 4, EMPTY, null);
    bad("&lt; &1;&gt;", 5, TOO_SHORT, "1");
    bad(" &lt; &12;&gt;", 6, BAD_NAME, "12");
    bad("< &lt; &12345678;&gt;", 7, BAD_NAME, "12345678");
    bad(" > ; &gt;&123456789;&gt;", 9, TOO_LONG, null);

    bad("foo&#;", 3, TOO_SHORT, "#");
    bad(" &#-1;", 1, BAD_NUMBER, "-1");
    bad("  &#foo;", 2, BAD_NUMBER, "foo");
    bad("  &#0A;", 2, BAD_NUMBER, "0A");
    bad(" ; &#1114112;", 3, BAD_NUMBER, "1114112");
    bad("< > &#1234567;", 4, BAD_NUMBER, "1234567");
    bad(" < > &#12345678;", 5, TOO_LONG, null);

    bad("bar&#x-1;", 3, BAD_NUMBER, "x-1");
    bad("\t&#xfoo;", 1, BAD_NUMBER, "xfoo");
    bad("\n &#xDEFG;", 2, BAD_NUMBER, "xDEFG");
    bad("< \r&#x110000;", 3, BAD_NUMBER, "x110000");
    bad("> < &#x123456;", 4, BAD_NUMBER, "x123456");
    bad(" ;\b; &#x1234567;", 5, TOO_LONG, null);
  }

  private void bad( String string, int pos, String msg, String ref )
  {
    try
    {
      _coder.code(string);
      fail("exception expected");
    }
    catch (HtmlReferenceException e)
    {
      // e.printStackTrace();
      assertEquals(msg+(ref == null ? "" : " ["+ref+"]")+" at position ["+pos+"]: \""+string+"\"", e.getMessage());
    }
  }

}
