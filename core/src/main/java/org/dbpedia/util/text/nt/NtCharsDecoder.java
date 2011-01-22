package org.dbpedia.util.text.nt;

/**
 * see <a href="http://www.w3.org/TR/rdf-testcases/#ntrip_strings">N-Triples Strings</a>
 * Decodes only Unicode encoded chars. Escapes special chars like lines breaks,
 * whether they were Unicode encoded or backslash escaped or plain. 
 */
public class NtCharsDecoder
{
  /**
   * @param string encoded string
   * @return decoded string
   */
  public static String decodeNtChars( String string )
  {
    NtCharsDecoder decoder = new NtCharsDecoder(string);
    decoder.decode();
    return decoder.result();
  }
  
  /**
   * @param string encoded string
   * @param target target {@link StringBuilder}, must not be {@code null}
   */
  public static void decodeNtChars( String string, StringBuilder target )
  {
    new NtCharsDecoder(string, target).decode();
  }
  
  /** chars that must be escaped, whether they were encoded or escaped or plain. */
  private static final String[] SPECIAL = new String[128];
  static
  {
    SPECIAL['\t'] = "\\t";
    SPECIAL['\n'] = "\\n";
    SPECIAL['\r'] = "\\r";
    SPECIAL['\"'] = "\\\"";
    SPECIAL['\\'] = "\\\\";
  }
  
  private final String _str;
  
  private StringBuilder _sb = null;
  
  private int _last = 0;
  
  public NtCharsDecoder( String string )
  {
    if (string == null) throw new NullPointerException("string");
    _str = string;
  }

  public NtCharsDecoder( String string, StringBuilder target )
  {
    if (string == null) throw new NullPointerException("string");
    if (target == null) throw new NullPointerException("target");
    _str = string;
    _sb = target;
  }

  public void decode()
  {
    for (int pos = 0; pos < _str.length(); pos++)
    {
      char ch = _str.charAt(pos);
      
      if (ch == '\\')
      {
        pos += 1;
        
        if (pos == _str.length()) throw error();
        ch  = _str.charAt(pos);
        
        // escaped special char? copy it unchanged
        if (ch == 't' || ch == 'n' || ch == 'r' || ch == '"' || ch == '\\') continue;
        else if (ch == 'u') appendCode(pos - 1, 6);
        else if (ch == 'U') appendCode(pos - 1, 10);
        else throw error();
        
        pos = _last - 1; // loop will increment pos by one
      }
      else if (ch < 128)
      {
        // escape special chars if necessary
        String rep = SPECIAL[ch];
        if (rep != null) append(pos, rep);
      }
    }
    
    if (_sb != null) _sb.append(_str, _last, _str.length());
  }
  
  public String result()
  {
    return _sb == null ? _str : _sb.toString();
  }

  private void append( int pos, String app )
  {
    if (_sb == null) _sb = new StringBuilder();
    _sb.append(_str.substring(_last, pos));
    _sb.append(app);
    _last = pos + 1;
  }

  private void appendCode( int pos, int length )
  {
    if (pos + length > _str.length()) throw error();
    
    int code;
    try
    {
      String hex = _str.substring(pos + 2, pos + length);
      code = Integer.parseInt(hex, 16);
    }
    catch (NumberFormatException e)
    {
      throw error(e);
    }
    
    if (_sb == null) _sb = new StringBuilder();
    _sb.append(_str.substring(_last, pos));
    
    String rep = code < 128 ? SPECIAL[code] : null;
    if (rep != null)
    {
      _sb.append(rep);
    }
    else
    {
      try
      {
        _sb.appendCodePoint(code);
      }
      catch (IllegalArgumentException e)
      {
        throw error(e);
      }
    }
    
    _last = pos + length;
  }

  private IllegalArgumentException error()
  {
    return error(null);
  }

  private IllegalArgumentException error( Throwable cause )
  {
    // TODO: use ParseException
    return new IllegalArgumentException("invalid N-Triples string ["+_str+"]", cause);
  }

}

