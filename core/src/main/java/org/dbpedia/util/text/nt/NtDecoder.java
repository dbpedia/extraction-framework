package org.dbpedia.util.text.nt;

/**
 * see <a href="http://www.w3.org/TR/rdf-testcases/#ntrip_strings">N-Triples Strings</a>
 */
public class NtDecoder
{
  /**
   * @param string encoded string
   * @return decoded string
   */
  public static String decodeNt( String string )
  {
    NtDecoder decoder = new NtDecoder(string);
    decoder.decode();
    return decoder.result();
  }
  
  /**
   * @param string encoded string
   * @param target target {@link StringBuilder}, must not be {@code null}
   */
  public static void decodeNt( String string, StringBuilder target )
  {
    new NtDecoder(string, target).decode();
  }
  
  private final String _str;
  
  private StringBuilder _sb = null;
  
  private int _last = 0;
  
  public NtDecoder( String string )
  {
    if (string == null) throw new NullPointerException("string");
    _str = string;
  }

  public NtDecoder( String string, StringBuilder target )
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
        if (pos + 1 == _str.length()) throw error();
        ch  = _str.charAt(pos + 1);
        
        if (ch == 't') append(pos, '\t');
        else if (ch == 'n') append(pos, '\n');
        else if (ch == 'r') append(pos, '\r');
        else if (ch == '"') append(pos, '\"');
        else if (ch == '\\') append(pos, '\\');
        else if (ch == 'u') appendCode(pos, 6);
        else if (ch == 'U') appendCode(pos, 10);
        else throw error();
        
        pos = _last - 1; // loop will increment pos by one
      }
    }
    
    if (_sb != null) _sb.append(_str, _last, _str.length());
  }
  
  public String result()
  {
    return _sb == null ? _str : _sb.toString();
  }

  private void append( int pos, char app )
  {
    if (_sb == null) _sb = new StringBuilder();
    _sb.append(_str, _last, pos);
    _sb.append(app);
    _last = pos + 2;
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
    _sb.append(_str, _last, pos);
    
    try
    {
      _sb.appendCodePoint(code);
    }
    catch (IllegalArgumentException e)
    {
      throw error(e);
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

