package org.dbpedia.util.text.html;

import static org.dbpedia.util.text.html.HtmlReferenceException.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dbpedia.util.text.ParseException;
import org.dbpedia.util.text.ParseExceptionHandler;
import org.dbpedia.util.text.ParseExceptionThrower;


/**
 */
public class HtmlCoder
{
  /** used in {@link #encode(int)} */
  private final XmlCodes _encode;
  
  /** used in {@link #decode(int)} */
  private final XmlCodes _keep;
  
  /** the error handler, used in {@link #error(int, String, String)} */
  private ParseExceptionHandler _handler = ParseExceptionThrower.INSTANCE;
  
  /** {@link String} to transcode */
  private String _in;
  
  /** output {@link StringBuilder}, {@code null} if nothing in {@link #_in input} was transcoded. */
  private StringBuilder _out;

  /** last (exclusive) position up to which {@link #_in input} was copied to {@link #_out output}. */
  private int _last = 0;
  
  /**
   * @param encode defines if ampersand, brackets and quotes should be encoded. Must not be {@code null}
   */
  public HtmlCoder( XmlCodes encode )
  {
    if (encode == null) throw new NullPointerException("characters to encode");
    _encode = _keep = encode;
  }
  
  /**
   * @param encodePlain defines if ampersand, brackets and quotes that are not encoded should be 
   * changed to encoded form. Must not be {@code null}
   * @param keepEncoded defines if ampersand, brackets and quotes that are already encoded should 
   * be kept in encoded form or should be decoded. Must not be {@code null}
   */
  public HtmlCoder( XmlCodes encodePlain, XmlCodes keepEncoded )
  {
    if (encodePlain == null) throw new NullPointerException("characters to change from plain to encoded form");
    if (keepEncoded == null) throw new NullPointerException("characters to keep in encoded form");
    _encode = encodePlain;
    _keep = keepEncoded;
  }
  
  /**
   * Set the {@link ParseExceptionHandler error handler}. Before this method is called, 
   * a {@link ParseExceptionThrower} is used.
   * @param handler the {@link ParseExceptionHandler error handler}, must not be {@code null}
   */
  public void setErrorHandler( ParseExceptionHandler handler )
  {
    if (handler == null) throw new NullPointerException("handler");
    _handler = handler;
  }

  /**
   * Transcode HTML to XML: decode HTML entity references and numeric character references
   * to their Unicode characters, optionally encode brackets and quotes. Note: this mix of
   * decoding and encoding is a bit strange, but necessary for Wikipedia / DBpedia text.
   * 
   * @param string {@link String} to transcode, must not be {@code null}
   * @return result, given {@link String} if nothing is transcoded
   */
  public String code( String string )
  {
    run(string, null);
    return _out != null ? _out.toString() : _in;
  }
  
  /**
   * Transcode HTML to XML: decode HTML entity references and numeric character references
   * to their Unicode characters, optionally encode brackets and quotes. Note: this mix of
   * decoding and encoding is a bit strange, but necessary for Wikipedia / DBpedia text.
   * 
   * @param string {@link String} to transcode, must not be {@code null}
   * @param target target {@link StringBuilder}, must not be {@code null}
   */
  public void code( String string, StringBuilder target )
  {
    run(string, target);
  }
  
  /**
   * Transcode HTML to XML: decode HTML entity references and numeric character references
   * to their Unicode characters, optionally encode brackets and quotes. Note: this mix of
   * decoding and encoding is a bit strange, but necessary for Wikipedia / DBpedia text.
   * @param in {@link String} to transcode, must not be {@code null}
   * @param out target {@link StringBuilder}, may be {@code null}
   */
  private void run( String in, StringBuilder out )
  {
    if (in == null) throw new NullPointerException("string");
    _in = in;
    _out = out;
    _last = 0;
    
    int pos = 0;
    while (pos < _in.length())
    {
      int skip = _in.charAt(pos) == '&' ? decode(pos) : encode(pos);
      pos += skip; 
    }
    
    if (_out != null) _out.append(_in, _last, _in.length());
  }

  /**
   * Encode character at given position. Character at given position must not be {@code &}.
   * @param pos position in {@link #_in input}.
   * @return {@code 1}
   */
  private int encode( int pos )
  {
    String encoded = _encode.encode(_in.charAt(pos));
    if (encoded != null) append(pos, 1, encoded);
    return 1;
  }

  /**
   * Decode reference starting at given position in {@link #_in input}.
   * @param pos position of {@code &} in {@link #_in input}.
   * @return number of skipped characters
   */
  private int decode( int pos )
  {
    int sem = _in.indexOf(';', pos);
    if (sem == -1) return error(pos, NOT_CLOSED, null);
    int len = sem - pos + 1; // includes '&' and ';'
    if (len - 2 == 0) return error(pos, EMPTY, null);
    if (len - 2 < MIN_NAME_LEN) return error(pos, TOO_SHORT, _in.substring(pos + 1, sem));
    if (len - 2 > MAX_NAME_LEN) return error(pos, TOO_LONG, null);
    boolean numeric = _in.charAt(pos + 1) == '#';
    int decoded;
    if (numeric)
    {
      decoded = -1;
      try
      {
        if (_in.charAt(pos + 2) == 'x')
        {
          decoded = Integer.parseInt(_in.substring(pos + 3, sem), 16);
        }
        else
        {
          decoded = Integer.parseInt(_in.substring(pos + 2, sem), 10);
        }
      }
      catch (NumberFormatException e) 
      { 
        /* fall through */ 
      }
      
      if (decoded < 0 || decoded > Character.MAX_CODE_POINT) return error(pos, BAD_NUMBER, _in.substring(pos + 2, sem));
    }
    else
    {
      String name = _in.substring(pos + 1, sem);
      Integer obj = ENTITIES.get(name);
      if (obj == null) return error(pos, BAD_NAME, name);
      decoded = obj.intValue();
    }
    
    String encoded = _keep.encode(decoded);
    if (encoded == null)
    {
      // decode reference. Note: some chars are illegal, but their character references are 
      // illegal as well, so decoding them doesn't make things worse. garbage in, garbage out.
      append(pos, len, decoded);
    }
    else if (numeric)
    {
      // change numeric character reference to nicer entity reference
      append(pos, len, encoded);
    }
    // else do nothing - don't replace entity reference by itself
    
    return len;
  }

  /**
   * @param pos position in {@link #_in input}
   * @param description error message, must not be {@code null}
   * @param reference the content of the invalid reference, may be {@code null}
   * @return {@code 1}
   * @throws ParseException if {@link #_handler error handler} wants to
   */
  private int error( int pos, String description, String reference )
  {
    ParseException error = new HtmlReferenceException(_in, pos, description, reference);
    _handler.error(error);
    // if handler didn't throw exception, escape bad reference and go on
    append(pos, 1, "&amp;");
    return 1;
  }
  
  /**
   * Copy characters in {@link #_in input} from {@link #_last last position} (inclusive) up to 
   * given current position (exclusive) to {@link #_out output}, append given Unicode code point 
   * to {@link #_out output}, and set {@link #_last last position} to given current position plus 
   * given number of characters.
   * @param pos position up to which {@link #_in input} should be copied to {@link #_out output}
   * @param skip number of characters that should be skipped in {@link #_in input}
   * @param code Unicode code point that should be appended to {@link #_out output}
   */
  private void append( int pos, int skip, int code )
  {
    if (_out == null) _out = new StringBuilder();
    _out.append(_in, _last, pos);
    _last = pos + skip;
    _out.appendCodePoint(code);
  }

  /**
   * Copy characters in {@link #_in input} from {@link #_last last position} (inclusive) up to 
   * given current position (exclusive) to {@link #_out output}, append given {@link String} 
   * to {@link #_out output}, and set {@link #_last last position} to given current position plus 
   * given number of characters.
   * @param pos position up to which {@link #_in input} should be copied to {@link #_out output}
   * @param skip number of characters that should be skipped in {@link #_in input}
   * @param str replacement {@link String} that should be appended to {@link #_out output}
   */
  private void append( int pos, int skip, String str )
  {
    if (_out == null) _out = new StringBuilder();
    _out.append(_in, _last, pos);
    _last = pos + skip;
    _out.append(str);
  }

  /** 
   * Minimum length of entity names. 
   * Also works for minimum numeric character references: {@code &#x0;} and {@code &#0;}
   */
  private static final int MIN_NAME_LEN = 2;
  
  /** 
   * Maximum length of entity names. 
   * Also works for maximum numeric character references: {@code &#x10FFFF;} and {@code &#1114111;}
   */
  private static final int MAX_NAME_LEN = 8;
  
  // -----------------------------------------------------------------------------------------------
  // The following code generates the entity map below. 
  // -----------------------------------------------------------------------------------------------

  /** http content type */
  private static final String CONTENT_TYPE = "text/html; charset=";
  
  /** pattern for entity names. some names end with numbers. */
  private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z]{"+MIN_NAME_LEN+","+MAX_NAME_LEN+"}|[A-Za-z]{"+MIN_NAME_LEN+","+(MAX_NAME_LEN - 2)+"}\\d{0,2}");

  /** pattern for entity values in the html-encoded DTD */
  private static final Pattern VALUE_PATTERN = Pattern.compile("\"&amp;#(\\d{2,4});\"");
  
  /** order names case insensitive, except those that only differ in case. */
  private static final Comparator<String> PRETTY_ORDER =
  new Comparator<String>()
  {
    @Override
    public int compare( String s1, String s2 )
    {
      int comp = String.CASE_INSENSITIVE_ORDER.compare(s1, s2);
      return comp == 0 ? s1.compareTo(s2) : comp;
    }
  };
  
  /**
   * Generates Java code for mapping the HTML entities specified in
   * <a href="http://www.w3.org/TR/html4/sgml/entities.html">http://www.w3.org/TR/html4/sgml/entities.html</a>
   * @throws IOException if reading fails 
   */
  public static void generate() 
  throws IOException 
  {
    List<String> strange = new ArrayList<String>();
    Map<String, Integer> entities = new TreeMap<String, Integer>(PRETTY_ORDER);
    
    URL url = new URL("http://www.w3.org/TR/html4/sgml/entities.html");
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    try
    {
      String contentType = conn.getContentType();
      if (! contentType.startsWith(CONTENT_TYPE)) throw new IOException("don't like content-type ["+contentType+"]");
      Charset charset = Charset.forName(contentType.substring(CONTENT_TYPE.length()));
      
      InputStream in = conn.getInputStream();
      try
      {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset));
        String line;
        while ((line = reader.readLine()) != null)
        {
          if (line.startsWith("&lt;!ENTITY"))
          {
            String[] parts = line.split("\\s+", -1);
            
            if (parts.length < 4) throw error("expected at least [4] whitespace-separated parts, found ["+parts.length+"]", line);
            
            if (! parts[0].equals("&lt;!ENTITY")) throw error("first part should be '&lt;!ENTITY', but is '"+parts[0]+"'", line);
            
            Matcher matcher = NAME_PATTERN.matcher(parts[1]);
            if (! matcher.matches()) throw error("second part should match regex '"+NAME_PATTERN+"', but is '"+parts[1]+"'", line);
            String name = parts[1];
            
            if (! parts[2].equals("CDATA")) throw error("third part should be 'CDATA', but is '"+parts[2]+"'", line);
            
            matcher = VALUE_PATTERN.matcher(parts[3]);
            if (! matcher.matches()) throw error("fourth part should match regex '"+VALUE_PATTERN+"', but is '"+parts[3]+"'", line);
            Integer value = Integer.valueOf(matcher.group(1));
            
            entities.put(name, value);
          }
          else if (line.contains("ENTITY"))
          {
            strange.add(line);
          }
        }
      }
      finally
      {
        in.close();
      }
    }
    finally
    {
      conn.disconnect();
    }
    
    // apos is not an entity in HTML, but in XML and XHTML, so to be safe...
    entities.put("apos", new Integer('\''));
    
    System.out.println("/** Map from entity names (without leading {@code &} and trailing {@code ;}) to Unicode code points. */");
    System.out.println("private static final Map<String, Integer> ENTITIES = new HashMap<String, Integer>();");
    System.out.println("static");
    System.out.println("{");
    for (Entry<String, Integer> entry : entities.entrySet())
    {
      System.out.println("ENTITIES.put(\""+entry.getKey()+"\", Integer.valueOf("+entry.getValue()+"));");
    }
    System.out.println("}");
    
    System.out.println();
    System.out.println();
    
    System.err.println("lines in input document that contain 'ENTITY' but do not start with '&lt;!ENTITY'");
    for (String line : strange)
    {
      System.err.println(line);
    }
  }

  /**
   * @param msg error message
   * @param line whole line from input document
   * @return an {@link IllegalArgumentException} whose message includes the given message and line
   */
  private static IllegalArgumentException error( String msg, String line )
  {
    return new IllegalArgumentException(msg+". line: "+line);
  }
  
  // -----------------------------------------------------------------------------------------------
  // The entity map code was generated - do not edit. See above.
  // -----------------------------------------------------------------------------------------------

  /** Map from entity names (without leading {@code &} and trailing {@code ;}) to Unicode code points. */
  private static final Map<String, Integer> ENTITIES = new HashMap<String, Integer>();
  static
  {
    ENTITIES.put("Aacute", Integer.valueOf(193));
    ENTITIES.put("aacute", Integer.valueOf(225));
    ENTITIES.put("Acirc", Integer.valueOf(194));
    ENTITIES.put("acirc", Integer.valueOf(226));
    ENTITIES.put("acute", Integer.valueOf(180));
    ENTITIES.put("AElig", Integer.valueOf(198));
    ENTITIES.put("aelig", Integer.valueOf(230));
    ENTITIES.put("Agrave", Integer.valueOf(192));
    ENTITIES.put("agrave", Integer.valueOf(224));
    ENTITIES.put("alefsym", Integer.valueOf(8501));
    ENTITIES.put("Alpha", Integer.valueOf(913));
    ENTITIES.put("alpha", Integer.valueOf(945));
    ENTITIES.put("amp", Integer.valueOf(38));
    ENTITIES.put("and", Integer.valueOf(8743));
    ENTITIES.put("ang", Integer.valueOf(8736));
    ENTITIES.put("apos", Integer.valueOf(39));
    ENTITIES.put("Aring", Integer.valueOf(197));
    ENTITIES.put("aring", Integer.valueOf(229));
    ENTITIES.put("asymp", Integer.valueOf(8776));
    ENTITIES.put("Atilde", Integer.valueOf(195));
    ENTITIES.put("atilde", Integer.valueOf(227));
    ENTITIES.put("Auml", Integer.valueOf(196));
    ENTITIES.put("auml", Integer.valueOf(228));
    ENTITIES.put("bdquo", Integer.valueOf(8222));
    ENTITIES.put("Beta", Integer.valueOf(914));
    ENTITIES.put("beta", Integer.valueOf(946));
    ENTITIES.put("brvbar", Integer.valueOf(166));
    ENTITIES.put("bull", Integer.valueOf(8226));
    ENTITIES.put("cap", Integer.valueOf(8745));
    ENTITIES.put("Ccedil", Integer.valueOf(199));
    ENTITIES.put("ccedil", Integer.valueOf(231));
    ENTITIES.put("cedil", Integer.valueOf(184));
    ENTITIES.put("cent", Integer.valueOf(162));
    ENTITIES.put("Chi", Integer.valueOf(935));
    ENTITIES.put("chi", Integer.valueOf(967));
    ENTITIES.put("circ", Integer.valueOf(710));
    ENTITIES.put("clubs", Integer.valueOf(9827));
    ENTITIES.put("cong", Integer.valueOf(8773));
    ENTITIES.put("copy", Integer.valueOf(169));
    ENTITIES.put("crarr", Integer.valueOf(8629));
    ENTITIES.put("cup", Integer.valueOf(8746));
    ENTITIES.put("curren", Integer.valueOf(164));
    ENTITIES.put("Dagger", Integer.valueOf(8225));
    ENTITIES.put("dagger", Integer.valueOf(8224));
    ENTITIES.put("dArr", Integer.valueOf(8659));
    ENTITIES.put("darr", Integer.valueOf(8595));
    ENTITIES.put("deg", Integer.valueOf(176));
    ENTITIES.put("Delta", Integer.valueOf(916));
    ENTITIES.put("delta", Integer.valueOf(948));
    ENTITIES.put("diams", Integer.valueOf(9830));
    ENTITIES.put("divide", Integer.valueOf(247));
    ENTITIES.put("Eacute", Integer.valueOf(201));
    ENTITIES.put("eacute", Integer.valueOf(233));
    ENTITIES.put("Ecirc", Integer.valueOf(202));
    ENTITIES.put("ecirc", Integer.valueOf(234));
    ENTITIES.put("Egrave", Integer.valueOf(200));
    ENTITIES.put("egrave", Integer.valueOf(232));
    ENTITIES.put("empty", Integer.valueOf(8709));
    ENTITIES.put("emsp", Integer.valueOf(8195));
    ENTITIES.put("ensp", Integer.valueOf(8194));
    ENTITIES.put("Epsilon", Integer.valueOf(917));
    ENTITIES.put("epsilon", Integer.valueOf(949));
    ENTITIES.put("equiv", Integer.valueOf(8801));
    ENTITIES.put("Eta", Integer.valueOf(919));
    ENTITIES.put("eta", Integer.valueOf(951));
    ENTITIES.put("ETH", Integer.valueOf(208));
    ENTITIES.put("eth", Integer.valueOf(240));
    ENTITIES.put("Euml", Integer.valueOf(203));
    ENTITIES.put("euml", Integer.valueOf(235));
    ENTITIES.put("euro", Integer.valueOf(8364));
    ENTITIES.put("exist", Integer.valueOf(8707));
    ENTITIES.put("fnof", Integer.valueOf(402));
    ENTITIES.put("forall", Integer.valueOf(8704));
    ENTITIES.put("frac12", Integer.valueOf(189));
    ENTITIES.put("frac14", Integer.valueOf(188));
    ENTITIES.put("frac34", Integer.valueOf(190));
    ENTITIES.put("frasl", Integer.valueOf(8260));
    ENTITIES.put("Gamma", Integer.valueOf(915));
    ENTITIES.put("gamma", Integer.valueOf(947));
    ENTITIES.put("ge", Integer.valueOf(8805));
    ENTITIES.put("gt", Integer.valueOf(62));
    ENTITIES.put("hArr", Integer.valueOf(8660));
    ENTITIES.put("harr", Integer.valueOf(8596));
    ENTITIES.put("hearts", Integer.valueOf(9829));
    ENTITIES.put("hellip", Integer.valueOf(8230));
    ENTITIES.put("Iacute", Integer.valueOf(205));
    ENTITIES.put("iacute", Integer.valueOf(237));
    ENTITIES.put("Icirc", Integer.valueOf(206));
    ENTITIES.put("icirc", Integer.valueOf(238));
    ENTITIES.put("iexcl", Integer.valueOf(161));
    ENTITIES.put("Igrave", Integer.valueOf(204));
    ENTITIES.put("igrave", Integer.valueOf(236));
    ENTITIES.put("image", Integer.valueOf(8465));
    ENTITIES.put("infin", Integer.valueOf(8734));
    ENTITIES.put("int", Integer.valueOf(8747));
    ENTITIES.put("Iota", Integer.valueOf(921));
    ENTITIES.put("iota", Integer.valueOf(953));
    ENTITIES.put("iquest", Integer.valueOf(191));
    ENTITIES.put("isin", Integer.valueOf(8712));
    ENTITIES.put("Iuml", Integer.valueOf(207));
    ENTITIES.put("iuml", Integer.valueOf(239));
    ENTITIES.put("Kappa", Integer.valueOf(922));
    ENTITIES.put("kappa", Integer.valueOf(954));
    ENTITIES.put("Lambda", Integer.valueOf(923));
    ENTITIES.put("lambda", Integer.valueOf(955));
    ENTITIES.put("lang", Integer.valueOf(9001));
    ENTITIES.put("laquo", Integer.valueOf(171));
    ENTITIES.put("lArr", Integer.valueOf(8656));
    ENTITIES.put("larr", Integer.valueOf(8592));
    ENTITIES.put("lceil", Integer.valueOf(8968));
    ENTITIES.put("ldquo", Integer.valueOf(8220));
    ENTITIES.put("le", Integer.valueOf(8804));
    ENTITIES.put("lfloor", Integer.valueOf(8970));
    ENTITIES.put("lowast", Integer.valueOf(8727));
    ENTITIES.put("loz", Integer.valueOf(9674));
    ENTITIES.put("lrm", Integer.valueOf(8206));
    ENTITIES.put("lsaquo", Integer.valueOf(8249));
    ENTITIES.put("lsquo", Integer.valueOf(8216));
    ENTITIES.put("lt", Integer.valueOf(60));
    ENTITIES.put("macr", Integer.valueOf(175));
    ENTITIES.put("mdash", Integer.valueOf(8212));
    ENTITIES.put("micro", Integer.valueOf(181));
    ENTITIES.put("middot", Integer.valueOf(183));
    ENTITIES.put("minus", Integer.valueOf(8722));
    ENTITIES.put("Mu", Integer.valueOf(924));
    ENTITIES.put("mu", Integer.valueOf(956));
    ENTITIES.put("nabla", Integer.valueOf(8711));
    ENTITIES.put("nbsp", Integer.valueOf(160));
    ENTITIES.put("ndash", Integer.valueOf(8211));
    ENTITIES.put("ne", Integer.valueOf(8800));
    ENTITIES.put("ni", Integer.valueOf(8715));
    ENTITIES.put("not", Integer.valueOf(172));
    ENTITIES.put("notin", Integer.valueOf(8713));
    ENTITIES.put("nsub", Integer.valueOf(8836));
    ENTITIES.put("Ntilde", Integer.valueOf(209));
    ENTITIES.put("ntilde", Integer.valueOf(241));
    ENTITIES.put("Nu", Integer.valueOf(925));
    ENTITIES.put("nu", Integer.valueOf(957));
    ENTITIES.put("Oacute", Integer.valueOf(211));
    ENTITIES.put("oacute", Integer.valueOf(243));
    ENTITIES.put("Ocirc", Integer.valueOf(212));
    ENTITIES.put("ocirc", Integer.valueOf(244));
    ENTITIES.put("OElig", Integer.valueOf(338));
    ENTITIES.put("oelig", Integer.valueOf(339));
    ENTITIES.put("Ograve", Integer.valueOf(210));
    ENTITIES.put("ograve", Integer.valueOf(242));
    ENTITIES.put("oline", Integer.valueOf(8254));
    ENTITIES.put("Omega", Integer.valueOf(937));
    ENTITIES.put("omega", Integer.valueOf(969));
    ENTITIES.put("Omicron", Integer.valueOf(927));
    ENTITIES.put("omicron", Integer.valueOf(959));
    ENTITIES.put("oplus", Integer.valueOf(8853));
    ENTITIES.put("or", Integer.valueOf(8744));
    ENTITIES.put("ordf", Integer.valueOf(170));
    ENTITIES.put("ordm", Integer.valueOf(186));
    ENTITIES.put("Oslash", Integer.valueOf(216));
    ENTITIES.put("oslash", Integer.valueOf(248));
    ENTITIES.put("Otilde", Integer.valueOf(213));
    ENTITIES.put("otilde", Integer.valueOf(245));
    ENTITIES.put("otimes", Integer.valueOf(8855));
    ENTITIES.put("Ouml", Integer.valueOf(214));
    ENTITIES.put("ouml", Integer.valueOf(246));
    ENTITIES.put("para", Integer.valueOf(182));
    ENTITIES.put("part", Integer.valueOf(8706));
    ENTITIES.put("permil", Integer.valueOf(8240));
    ENTITIES.put("perp", Integer.valueOf(8869));
    ENTITIES.put("Phi", Integer.valueOf(934));
    ENTITIES.put("phi", Integer.valueOf(966));
    ENTITIES.put("Pi", Integer.valueOf(928));
    ENTITIES.put("pi", Integer.valueOf(960));
    ENTITIES.put("piv", Integer.valueOf(982));
    ENTITIES.put("plusmn", Integer.valueOf(177));
    ENTITIES.put("pound", Integer.valueOf(163));
    ENTITIES.put("Prime", Integer.valueOf(8243));
    ENTITIES.put("prime", Integer.valueOf(8242));
    ENTITIES.put("prod", Integer.valueOf(8719));
    ENTITIES.put("prop", Integer.valueOf(8733));
    ENTITIES.put("Psi", Integer.valueOf(936));
    ENTITIES.put("psi", Integer.valueOf(968));
    ENTITIES.put("quot", Integer.valueOf(34));
    ENTITIES.put("radic", Integer.valueOf(8730));
    ENTITIES.put("rang", Integer.valueOf(9002));
    ENTITIES.put("raquo", Integer.valueOf(187));
    ENTITIES.put("rArr", Integer.valueOf(8658));
    ENTITIES.put("rarr", Integer.valueOf(8594));
    ENTITIES.put("rceil", Integer.valueOf(8969));
    ENTITIES.put("rdquo", Integer.valueOf(8221));
    ENTITIES.put("real", Integer.valueOf(8476));
    ENTITIES.put("reg", Integer.valueOf(174));
    ENTITIES.put("rfloor", Integer.valueOf(8971));
    ENTITIES.put("Rho", Integer.valueOf(929));
    ENTITIES.put("rho", Integer.valueOf(961));
    ENTITIES.put("rlm", Integer.valueOf(8207));
    ENTITIES.put("rsaquo", Integer.valueOf(8250));
    ENTITIES.put("rsquo", Integer.valueOf(8217));
    ENTITIES.put("sbquo", Integer.valueOf(8218));
    ENTITIES.put("Scaron", Integer.valueOf(352));
    ENTITIES.put("scaron", Integer.valueOf(353));
    ENTITIES.put("sdot", Integer.valueOf(8901));
    ENTITIES.put("sect", Integer.valueOf(167));
    ENTITIES.put("shy", Integer.valueOf(173));
    ENTITIES.put("Sigma", Integer.valueOf(931));
    ENTITIES.put("sigma", Integer.valueOf(963));
    ENTITIES.put("sigmaf", Integer.valueOf(962));
    ENTITIES.put("sim", Integer.valueOf(8764));
    ENTITIES.put("spades", Integer.valueOf(9824));
    ENTITIES.put("sub", Integer.valueOf(8834));
    ENTITIES.put("sube", Integer.valueOf(8838));
    ENTITIES.put("sum", Integer.valueOf(8721));
    ENTITIES.put("sup", Integer.valueOf(8835));
    ENTITIES.put("sup1", Integer.valueOf(185));
    ENTITIES.put("sup2", Integer.valueOf(178));
    ENTITIES.put("sup3", Integer.valueOf(179));
    ENTITIES.put("supe", Integer.valueOf(8839));
    ENTITIES.put("szlig", Integer.valueOf(223));
    ENTITIES.put("Tau", Integer.valueOf(932));
    ENTITIES.put("tau", Integer.valueOf(964));
    ENTITIES.put("there4", Integer.valueOf(8756));
    ENTITIES.put("Theta", Integer.valueOf(920));
    ENTITIES.put("theta", Integer.valueOf(952));
    ENTITIES.put("thetasym", Integer.valueOf(977));
    ENTITIES.put("thinsp", Integer.valueOf(8201));
    ENTITIES.put("THORN", Integer.valueOf(222));
    ENTITIES.put("thorn", Integer.valueOf(254));
    ENTITIES.put("tilde", Integer.valueOf(732));
    ENTITIES.put("times", Integer.valueOf(215));
    ENTITIES.put("trade", Integer.valueOf(8482));
    ENTITIES.put("Uacute", Integer.valueOf(218));
    ENTITIES.put("uacute", Integer.valueOf(250));
    ENTITIES.put("uArr", Integer.valueOf(8657));
    ENTITIES.put("uarr", Integer.valueOf(8593));
    ENTITIES.put("Ucirc", Integer.valueOf(219));
    ENTITIES.put("ucirc", Integer.valueOf(251));
    ENTITIES.put("Ugrave", Integer.valueOf(217));
    ENTITIES.put("ugrave", Integer.valueOf(249));
    ENTITIES.put("uml", Integer.valueOf(168));
    ENTITIES.put("upsih", Integer.valueOf(978));
    ENTITIES.put("Upsilon", Integer.valueOf(933));
    ENTITIES.put("upsilon", Integer.valueOf(965));
    ENTITIES.put("Uuml", Integer.valueOf(220));
    ENTITIES.put("uuml", Integer.valueOf(252));
    ENTITIES.put("weierp", Integer.valueOf(8472));
    ENTITIES.put("Xi", Integer.valueOf(926));
    ENTITIES.put("xi", Integer.valueOf(958));
    ENTITIES.put("Yacute", Integer.valueOf(221));
    ENTITIES.put("yacute", Integer.valueOf(253));
    ENTITIES.put("yen", Integer.valueOf(165));
    ENTITIES.put("Yuml", Integer.valueOf(376));
    ENTITIES.put("yuml", Integer.valueOf(255));
    ENTITIES.put("Zeta", Integer.valueOf(918));
    ENTITIES.put("zeta", Integer.valueOf(950));
    ENTITIES.put("zwj", Integer.valueOf(8205));
    ENTITIES.put("zwnj", Integer.valueOf(8204));
  }
  
}
