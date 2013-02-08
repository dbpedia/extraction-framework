package org.dbpedia.util.text.uri;

import java.nio.charset.Charset;

/**
 * Similar to java.net.URLDecoder, but ignores invalid escape sequences and doesn't translate '+' to space.
 */
public class UriDecoder {
  
  /**
   * @param string encoded string
   * @return decoded string
   */
  public static String decode( String string )
  {
    UriDecoder decoder = new UriDecoder(string);
    decoder.decode();
    return decoder.result();
  }
  
  private final static Charset UTF_8 = Charset.forName("UTF-8");
  
  private final String _str;
  
  private StringBuilder _sb = null;
  
  private int _last = 0;
  
  private int _start = -1;
  
  private byte[] _bytes = new byte[8];
  
  private int _count = 0;
  
  public UriDecoder( String string ) 
  {
    if (string == null) throw new NullPointerException("string");
    _str = string;
  }
  
  public void decode()
  {
    for (int pos = 0; pos < _str.length(); pos++)
    {
      char c = _str.charAt(pos);
      
      if (c == '%')
      {
        int b = getByte(pos + 1);
        if (b >= 0)
        {
          addByte(b, pos);
          pos += 2;
          continue;
        }
      }
        
      useBytes(pos);
    }
    
    useBytes(_str.length());
    if (_sb != null) _sb.append(_str, _last, _str.length());
  }

  private void addByte(int b, int pos) 
  {
    if (_count == _bytes.length) System.arraycopy(_bytes, 0, _bytes = new byte[_count << 1], 0, _count);
    _bytes[_count] = (byte)b;
    if (_start == -1) _start = pos;
    _count++;
  }

  public String result()
  {
    return _sb == null ? _str : _sb.toString();
  }
    
  private void useBytes(int pos) 
  {
    if (_count == 0) return;
    if (_sb == null) _sb = new StringBuilder();
    _sb.append(_str, _last, _start);
    _sb.append(new String(_bytes, 0, _count, UTF_8));
    _start = -1;
    _count = 0;
    _last = pos;
  }
  
  private int getByte(int pos) 
  {
    if (pos + 2 > _str.length()) return -1;
    
    try 
    {
      return Integer.parseInt(_str.substring(pos, pos + 2), 16);
    } 
    catch (NumberFormatException e)
    {
      return -1;
    }
  }
  
}
