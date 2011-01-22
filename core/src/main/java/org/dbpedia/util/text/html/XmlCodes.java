package org.dbpedia.util.text.html;


/**
 * Most combinations don't make much sense, but it's your choice...
 */
public enum XmlCodes
{
  /** encode neither ampersand, brackets nor quotes. */
  NONE(false, false, false),
  
  /** encode ampersand, but neither brackets nor quotes. */
  AMP(true, false, false),
  
  /** encode brackets, but not ampersand and quotes. */
  BRA(false, true, false),
  
  /** encode quotes, but not ampersand and brackets. */
  QUO(false, false, true),
  
  /** encode brackets and quotes, but not ampersand. */
  BRA_QUO(false, true, true),
  
  /** encode ampersand and quotes, but not brackets. */
  AMP_QUO(true, false, true),
  
  /** encode ampersand and brackets, but not quotes. Good for xml element content. */
  AMP_BRA(true, true, false),
  
  /** encode ampersand, brackets and quotes. Good for xml attribute content. */
  AMP_BRA_QUO(true, true, true);
  
  /** map from char to xml entity reference */
  private final String[] _codes = new String[64];
  
  /**
   * @param amp should ampersand ({@code &}) be encoded?
   * @param bra should opening ({@code <}) and closing ({@code >}) brackets be encoded?
   * @param quo should quotes ({@code "}) and apostrophes ({@code '}) be encoded?
   * @return the {@link XmlCodes} for the given choice, never {@code null}
   */
  public static XmlCodes choose( boolean amp, boolean bra, boolean quo )
  {
    return amp ? bra ? quo ? AMP_BRA_QUO : AMP_BRA : quo ? AMP_QUO : AMP : bra ? quo ? BRA_QUO : BRA : quo ? QUO : NONE;
  }
  
  /**
   * @param amp should ampersand ({@code &}) be encoded?
   * @param brackets should opening ({@code <}) and closing ({@code >}) brackets be encoded?
   * @param quotes should quotes ({@code "}) and apostrophes ({@code '}) be encoded?
   */
  private XmlCodes( boolean amp, boolean brackets, boolean quotes )
  {
    if (amp)
    {
      _codes['&'] = "&amp;";
    }
    
    if (brackets)
    {
      _codes['<'] = "&lt;";
      _codes['>'] = "&gt;";
    }

    if (quotes)
    {
       _codes['\"'] = "&quot;";
       _codes['\''] = "&apos;";
    }
  }
  
  /**
   * @param code Unicode code point
   * @return xml entity reference for the given Unicode code point
   */
  public String encode( int code )
  {
    return code >= 64 ? null : _codes[code];
  }
}
