package org.dbpedia.iri;

import sun.nio.cs.ThreadLocalCoders;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by chile on 30.09.17.
 *
 * Copied part from java.net.URI
 */
public class IriCharacters {


    // -- Character classes for parsing --

    // RFC2396 precisely specifies which characters in the US-ASCII charset are
    // permissible in the various components of a URI reference.  We here
    // define a set of mask pairs to aid in enforcing these restrictions.  Each
    // mask pair consists of two longs, a low mask and a high mask.  Taken
    // together they represent a 128-bit mask, where bit i is set iff the
    // character with value i is permitted.
    //
    // This approach is more efficient than sequentially searching arrays of
    // permitted characters.  It could be made still more efficient by
    // precompiling the mask information so that a character's presence in a
    // given mask could be determined by a single table lookup.

    // Compute the low-order mask for the characters in the given string
    static long lowMask(String chars) {
        int n = chars.length();
        long m = 0;
        for (int i = 0; i < n; i++) {
            char c = chars.charAt(i);
            if (c < 64)
                m |= (1L << c);
        }
        return m;
    }

    // Compute the high-order mask for the characters in the given string
    static long highMask(String chars) {
        int n = chars.length();
        long m = 0;
        for (int i = 0; i < n; i++) {
            char c = chars.charAt(i);
            if ((c >= 64) && (c < 128))
                m |= (1L << (c - 64));
        }
        return m;
    }

    // Compute a low-order mask for the characters
    // between first and last, inclusive
    static long lowMask(int first, int last) {
        long m = 0;
        int f = Math.max(Math.min(first, 63), 0);
        int l = Math.max(Math.min(last, 63), 0);
        for (int i = f; i <= l; i++)
            m |= 1L << i;
        return m;
    }

    // Compute a high-order mask for the characters
    // between first and last, inclusive
    static long highMask(int first, int last) {
        long m = 0;
        int f = Math.max(Math.min(first, 127), 64) - 64;
        int l = Math.max(Math.min(last, 127), 64) - 64;
        for (int i = f; i <= l; i++)
            m |= 1L << i;
        return m;
    }

    // Tell whether the given character is permitted by the given mask pair
    static boolean match(char c, long lowMask, long highMask) {
        if (c < 64)
            return ((1L << c) & lowMask) != 0;
        if (c < 128)
            return ((1L << (c - 64)) & highMask) != 0;
        return false;
    }

    // Character-class masks, in reverse order from RFC2396 because
    // initializers for static fields cannot make forward references.

    static final List<Long> hexAllowed = new ArrayList<Long>();


    // digit    = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" |
    //            "8" | "9"
    static final long L_DIGIT = lowMask('0', '9');
    static final long H_DIGIT = 0L;

    // upalpha  = "A" | "B" | "C" | "D" | "E" | "F" | "G" | "H" | "I" |
    //            "J" | "K" | "L" | "M" | "N" | "O" | "P" | "Q" | "R" |
    //            "S" | "T" | "U" | "V" | "W" | "X" | "Y" | "Z"
    static final long L_UPALPHA = 0L;
    static final long H_UPALPHA = highMask('A', 'Z');

    // lowalpha = "a" | "b" | "c" | "d" | "e" | "f" | "g" | "h" | "i" |
    //            "j" | "k" | "l" | "m" | "n" | "o" | "p" | "q" | "r" |
    //            "s" | "t" | "u" | "v" | "w" | "x" | "y" | "z"
    static final long L_LOWALPHA = 0L;
    static final long H_LOWALPHA = highMask('a', 'z');

    // alpha         = lowalpha | upalpha
    static final long L_ALPHA = L_LOWALPHA | L_UPALPHA;
    static final long H_ALPHA = H_LOWALPHA | H_UPALPHA;

    // alphanum      = alpha | digit
    static final long L_ALPHANUM = L_DIGIT | L_ALPHA;
    static final long H_ALPHANUM = H_DIGIT | H_ALPHA;

    // hex           = digit | "A" | "B" | "C" | "D" | "E" | "F" |
    //                         "a" | "b" | "c" | "d" | "e" | "f"
    static final long L_HEX = L_DIGIT;
    static final long H_HEX = highMask('A', 'F') | highMask('a', 'f');

    static final long L_UCSCHAR = lowMask(0xA0, 0xD7FF) | lowMask(0xF900, 0xFDCF)
            | lowMask(0xFDF0, 0xFFEF) | lowMask(0x10000, 0x1FFFD) | lowMask(0x20000, 0x2FFFD) |
            lowMask(0x30000, 0x3FFFD) | lowMask(0x40000, 0x4FFFD) | lowMask(0x40000, 0x4FFFD) |
            lowMask(0x40000, 0x4FFFD) | lowMask(0x40000, 0x4FFFD) | lowMask(0x40000, 0x4FFFD) |
            lowMask(0x40000, 0x4FFFD) | lowMask(0x40000, 0x4FFFD) | lowMask(0x40000, 0x4FFFD) |
            lowMask(0x40000, 0x4FFFD) | lowMask(0x40000, 0x4FFFD) | lowMask(0x40000, 0x4FFFD);

    static final long H_UCSCHAR = highMask(0xA0, 0xD7FF) | highMask(0xF900, 0xFDCF)
            | highMask(0xFDF0, 0xFFEF) | highMask(0x10000, 0x1FFFD) | highMask(0x20000, 0x2FFFD) |
            highMask(0x30000, 0x3FFFD) | highMask(0x40000, 0x4FFFD) | highMask(0x40000, 0x4FFFD) |
            highMask(0x40000, 0x4FFFD) | highMask(0x40000, 0x4FFFD) | highMask(0x40000, 0x4FFFD) |
            highMask(0x40000, 0x4FFFD) | highMask(0x40000, 0x4FFFD) | highMask(0x40000, 0x4FFFD) |
            highMask(0x40000, 0x4FFFD) | highMask(0x40000, 0x4FFFD) | highMask(0x40000, 0x4FFFD);

    static final long L_IPPRIVATE = lowMask(0xE000, 0xF8FF) | lowMask(0xF0000, 0xFFFFD) | lowMask(0x100000, 0x10FFFD);

    static final long H_IPPRIVATE = highMask(0xE000, 0xF8FF) | highMask(0xF0000, 0xFFFFD) | highMask(0x100000, 0x10FFFD);

    // mark          = "-" | "_" | "." | "!" | "~" | "*" | "'" |
    //                 "(" | ")"
    //static final long L_MARK = lowMask("-_.!~*'()");
    //static final long H_MARK = highMask("-_.!~*'()");

    // unreserved    = alphanum | mark
    static final long L_UNRESERVED = L_ALPHANUM | L_UCSCHAR | lowMask("-._~");
    static final long H_UNRESERVED = H_ALPHANUM | H_UCSCHAR | highMask("-._~");


    static final long L_SUBDELIMS = lowMask("!$&'()*+,;=");
    static final long H_SUBDELIMS = highMask("!$&'()*+,;=");

    static final long L_GENDELIMS = lowMask(":/?#[]@");
    static final long H_GENDELIMS = highMask(":/?#[]@");

    static final long L_IREGNAME = L_UNRESERVED | L_SUBDELIMS;
    static final long H_IREGNAME = H_UNRESERVED | H_SUBDELIMS;

    static final long L_ISEGNC = L_UNRESERVED | L_SUBDELIMS | lowMask("@");
    static final long H_ISEGNC = H_UNRESERVED | H_SUBDELIMS | highMask("@");

    static final long L_ISEG = L_ISEGNC | lowMask(":");
    static final long H_ISEG = H_ISEGNC | highMask(":");

    static final long L_IFRAGMENT = L_ISEGNC | L_IPPRIVATE | lowMask("/?");
    static final long H_IFRAGMENT = H_ISEGNC | H_IPPRIVATE | highMask("/?");

    static final long L_IQUERY = L_ISEGNC | lowMask("/?");
    static final long H_IQUERY = H_ISEGNC | highMask("/?");

    static final long L_IUSER = L_UNRESERVED | L_SUBDELIMS | lowMask(":");
    static final long H_IUSER = H_UNRESERVED | H_SUBDELIMS | highMask(":");





    // reserved      = ";" | "/" | "?" | ":" | "@" | "&" | "=" | "+" |
    //                 "$" | "," | "[" | "]"
    // Added per RFC2732: "[", "]"
    static final long L_RESERVED = lowMask(";/?:@&=+$,[]");
    static final long H_RESERVED = highMask(";/?:@&=+$,[]");

    // The zero'th bit is used to indicate that escape pairs and non-US-ASCII
    // characters are allowed; this is handled by the scanEscape method below.
    static final long L_ESCAPED = 1L;
    static final long H_ESCAPED = 0L;

    // uric          = reserved | unreserved | escaped
    static final long L_URIC = L_RESERVED | L_UNRESERVED | L_ESCAPED;
    static final long H_URIC = H_RESERVED | H_UNRESERVED | H_ESCAPED;

    // pchar         = unreserved | escaped |
    //                 ":" | "@" | "&" | "=" | "+" | "$" | ","
    static final long L_PCHAR = L_UNRESERVED | L_ESCAPED | lowMask(":@&=+$,");
    static final long H_PCHAR = H_UNRESERVED | H_ESCAPED | highMask(":@&=+$,");

    // All valid path characters
    static final long L_PATH = L_PCHAR | lowMask(";/");
    static final long H_PATH = H_PCHAR | highMask(";/");

    // Dash, for use in domainlabel and toplabel
    static final long L_DASH = lowMask("-");
    static final long H_DASH = highMask("-");

    // Dot, for use in hostnames
    static final long L_DOT = lowMask(".");
    static final long H_DOT = highMask(".");

    // userinfo      = *( unreserved | escaped |
    //                    ";" | ":" | "&" | "=" | "+" | "$" | "," )
    //static final long L_USERINFO = L_UNRESERVED | L_ESCAPED | lowMask(";:&=+$,");
    //tatic final long H_USERINFO = H_UNRESERVED | H_ESCAPED | highMask(";:&=+$,");

    // reg_name      = 1*( unreserved | escaped | "$" | "," |
    //                     ";" | ":" | "@" | "&" | "=" | "+" )
    static final long L_REG_NAME = L_UNRESERVED | L_ESCAPED | lowMask("$,;:@&=+");
    static final long H_REG_NAME = H_UNRESERVED | H_ESCAPED | highMask("$,;:@&=+");

    // All valid characters for server-based authorities
    //static final long L_SERVER = L_USERINFO | L_ALPHANUM | L_DASH | lowMask(".:@[]");
    //static final long H_SERVER = H_USERINFO | H_ALPHANUM | H_DASH | highMask(".:@[]");

    // Special case of server authority that represents an IPv6 address
    // In this case, a % does not signify an escape sequence
    //static final long L_SERVER_PERCENT = L_SERVER | lowMask("%");
    //static final long H_SERVER_PERCENT = H_SERVER | highMask("%");
    static final long L_LEFT_BRACKET = lowMask("[");
    static final long H_LEFT_BRACKET = highMask("[");

    // scheme        = alpha *( alpha | digit | "+" | "-" | "." )
    static final long L_SCHEME = L_ALPHA | L_DIGIT | lowMask("+-.");
    static final long H_SCHEME = H_ALPHA | H_DIGIT | highMask("+-.");

    // uric_no_slash = unreserved | escaped | ";" | "?" | ":" | "@" |
    //                 "&" | "=" | "+" | "$" | ","
    static final long L_URIC_NO_SLASH = L_UNRESERVED | L_ESCAPED | lowMask(";?:@&=+$,");
    static final long H_URIC_NO_SLASH = H_UNRESERVED | H_ESCAPED | highMask(";?:@&=+$,");

    // -- Escaping and encoding --

    final static char[] hexDigits = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    static void appendEscape(StringBuffer sb, byte b) {
        sb.append('%');
        sb.append(hexDigits[(b >> 4) & 0x0f]);
        sb.append(hexDigits[(b >> 0) & 0x0f]);
    }

    static void appendEncoded(StringBuffer sb, char c) {
        ByteBuffer bb = null;
        try {
            bb = ThreadLocalCoders.encoderFor("UTF-8")
                    .encode(CharBuffer.wrap("" + c));
        } catch (CharacterCodingException x) {
            assert false;
        }
        while (bb.hasRemaining()) {
            int b = bb.get() & 0xff;
            if (b >= 0x80)
                appendEscape(sb, (byte)b);
            else
                sb.append((char)b);
        }
    }

    // Quote any characters in s that are not permitted
    // by the given mask pair
    //
    static String quote(String s, long lowMask, long highMask) {
        int n = s.length();
        StringBuffer sb = null;
        boolean allowNonASCII = ((lowMask & L_ESCAPED) != 0);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '\u0080') {
                if (!match(c, lowMask, highMask)) {
                    if (sb == null) {
                        sb = new StringBuffer();
                        sb.append(s.substring(0, i));
                    }
                    appendEscape(sb, (byte)c);
                } else {
                    if (sb != null)
                        sb.append(c);
                }
            } else if (allowNonASCII
                    && (Character.isSpaceChar(c)
                    || Character.isISOControl(c))) {
                if (sb == null) {
                    sb = new StringBuffer();
                    sb.append(s.substring(0, i));
                }
                appendEncoded(sb, c);
            } else {
                if (sb != null)
                    sb.append(c);
            }
        }
        return (sb == null) ? s : sb.toString();
    }

    // Encodes all characters >= \u0080 into escaped, normalized UTF-8 octets,
    // assuming that s is otherwise legal
    //
    static String encode(String s) {
        int n = s.length();
        if (n == 0)
            return s;

        // First check whether we actually need to encode
        for (int i = 0;;) {
            if (s.charAt(i) >= '\u0080')
                break;
            if (++i >= n)
                return s;
        }

        String ns = Normalizer.normalize(s, Normalizer.Form.NFC);
        ByteBuffer bb = null;
        try {
            bb = ThreadLocalCoders.encoderFor("UTF-8")
                    .encode(CharBuffer.wrap(ns));
        } catch (CharacterCodingException x) {
            assert false;
        }

        StringBuffer sb = new StringBuffer();
        while (bb.hasRemaining()) {
            int b = bb.get() & 0xff;
            if (b >= 0x80)
                appendEscape(sb, (byte)b);
            else
                sb.append((char)b);
        }
        return sb.toString();
    }

    static int decode(char c) {
        if ((c >= '0') && (c <= '9'))
            return c - '0';
        if ((c >= 'a') && (c <= 'f'))
            return c - 'a' + 10;
        if ((c >= 'A') && (c <= 'F'))
            return c - 'A' + 10;
        assert false;
        return -1;
    }

    static byte decode(char c1, char c2) {
        return (byte)(  ((decode(c1) & 0xf) << 4)
                | ((decode(c2) & 0xf) << 0));
    }

    // Evaluates all escapes in s, applying UTF-8 decoding if needed.  Assumes
    // that escapes are well-formed syntactically, i.e., of the form %XX.  If a
    // sequence of escaped octets is not valid UTF-8 then the erroneous octets
    // are replaced with '\uFFFD'.
    // Exception: any "%" found between "[]" is left alone. It is an IPv6 literal
    //            with a scope_id
    //
    static String decode(String s) {
        if (s == null)
            return s;
        int n = s.length();
        if (n == 0)
            return s;
        if (s.indexOf('%') < 0)
            return s;

        StringBuffer sb = new StringBuffer(n);
        ByteBuffer bb = ByteBuffer.allocate(n);
        CharBuffer cb = CharBuffer.allocate(n);
        CharsetDecoder dec = ThreadLocalCoders.decoderFor("UTF-8")
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);

        // This is not horribly efficient, but it will do for now
        char c = s.charAt(0);
        boolean betweenBrackets = false;

        for (int i = 0; i < n;) {
            assert c == s.charAt(i);    // Loop invariant
            if (c == '[') {
                betweenBrackets = true;
            } else if (betweenBrackets && c == ']') {
                betweenBrackets = false;
            }
            if (c != '%' || betweenBrackets) {
                sb.append(c);
                if (++i >= n)
                    break;
                c = s.charAt(i);
                continue;
            }
            bb.clear();
            int ui = i;
            for (;;) {
                assert (n - i >= 2);
                bb.put(decode(s.charAt(++i), s.charAt(++i)));
                if (++i >= n)
                    break;
                c = s.charAt(i);
                if (c != '%')
                    break;
            }
            bb.flip();
            cb.clear();
            dec.reset();
            CoderResult cr = dec.decode(bb, cb, true);
            assert cr.isUnderflow();
            cr = dec.flush(cb);
            assert cr.isUnderflow();
            sb.append(cb.flip().toString());
        }

        return sb.toString();
    }
}
