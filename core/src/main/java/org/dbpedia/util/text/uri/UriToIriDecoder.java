package org.dbpedia.util.text.uri;

import sun.nio.cs.ThreadLocalCoders;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;
import java.util.List;

/**
 * Based on the decode-methods from the URI Class,
 * just that they do not decode the reserved characters
 * listed in the [RFC3987] IRI Specification
 * <p>
 * ignores Invalid Escape Sequences
 */
public class UriToIriDecoder {
    /*
    Reserved by IRI Specification:
     "%3A", "%3F", "%23", "%5B", "%5D", "%2F", "%40", "%21",
     "%24", "%26", "%27", "%28", "%29", "%2A", "%2B", "%2C",
     "%3B", "%3D"
     Most of them are allowed by DBpedia though,
     but dbpedia encodes the Pipe character (|) with %7D
    */
    private List<String> reserved = Arrays.asList("%3F", "%23", "%5B", "%5D", "%7D");

    private int decode(char c) {
        if ((c >= '0') && (c <= '9'))
            return c - '0';
        if ((c >= 'a') && (c <= 'f'))
            return c - 'a' + 10;
        if ((c >= 'A') && (c <= 'F'))
            return c - 'A' + 10;
        assert false;
        return -1;
    }

    private byte decode(char c1, char c2) {
        return (byte) (((decode(c1) & 0xf) << 4)
                | ((decode(c2) & 0xf) << 0));
    }

    public String decode(String s) {
        // + will be interpreted as Whitespace,
        // if the String does not contain %20
        if (s.indexOf('+') > 0){
            if(!s.contains("%20"))
                s = s.replaceAll("\\+", " ");
        }

        if (s == null)
            return s;
        int n = s.length();
        if (n == 0)
            return s;
        if (s.indexOf('%') < 0){
            return s;
        }


        StringBuffer sb = new StringBuffer(n);
        ByteBuffer bb = ByteBuffer.allocate(n);
        CharBuffer cb = CharBuffer.allocate(n);
        CharsetDecoder dec = ThreadLocalCoders.decoderFor("UTF-8")
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);

        char c = s.charAt(0);
        boolean betweenBrackets = false;

        for (int i = 0; i < n; ) {
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
            for (; ; ) {
                assert (n - i >= 2);
                if (i + 2 < n) {
                    char c1 = s.charAt(++i);
                    char c2 = s.charAt(++i);
                    // Checks if the encoded symbol is not a reserved character or invalid

                    if (!reserved.contains("%" + c1 + c2) && c1 <= 'F' && c2 <= 'F')
                        bb.put(decode(c1, c2));
                    else {
                        bb.put((byte) '%');
                        bb.put((byte) c1);
                        bb.put((byte) c2);
                    }
                } else if (i + 1 < n) {
                    bb.put((byte) '%');
                    bb.put((byte) s.charAt(++i));
                } else bb.put((byte) '%');
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
