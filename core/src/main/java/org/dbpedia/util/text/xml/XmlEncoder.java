package org.dbpedia.util.text.xml;

public class XmlEncoder
{
  public static String encode( String string )
  {
    return encode(string, false);
  }
  
  public static void encode( String str, StringBuilder target )
  {
    encode(str, target, false);
  }

  public static String encodeAttr( String string )
  {
    return encode(string, true);
  }
  
  public static void encodeAttr( String string, StringBuilder target )
  {
    encode(string, target, true);
  }
  
  public static String encode( String string, boolean attr )
  {
    XmlEncoder encoder = new XmlEncoder(string);
    encoder.encode(attr);
    return encoder.result();
  }
  
  public static void encode( String string, StringBuilder target, boolean attr )
  {
    new XmlEncoder(string, target).encode(attr);
  }
  
  private final String _str;
  
  private int _last = 0;
  
  private StringBuilder _sb;

  public XmlEncoder( String str )
  {
    if (str == null) throw new NullPointerException("string");
    _str = str;
    _sb = null;
  }

  public XmlEncoder( String string, StringBuilder target )
  {
    if (string == null) throw new NullPointerException("string");
    if (target == null) throw new NullPointerException("target");
    _str = string;
    _sb = target;
  }

  public void encode( boolean attr )
  {
    for (int pos = 0; pos < _str.length(); pos++)
    {
      char ch = _str.charAt(pos);
      if (ch == '<') append(pos, "&lt;");
      else if (ch == '>') append(pos, "&gt;");
      else if (ch == '&') append(pos, "&amp;");
      else if (attr)
      {
        if (ch == '\'') append(pos, "&apos;");
        else if (ch == '\"') append(pos, "&quot;");
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
    _sb.append(_str, _last, pos);
    _sb.append(app);
    _last = pos + 1;
  }

}
