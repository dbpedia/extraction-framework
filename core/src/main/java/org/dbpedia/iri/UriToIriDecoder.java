package org.dbpedia.iri;

import sun.nio.cs.ThreadLocalCoders;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

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
    //private List<String> reserved = Arrays.asList(StringUtils.replacements('%', "#<>[]{}|", 256));


    public String decode(String uri) {
        String s = uri;
        if (s == null)
            return s;
        int length = s.length();
        //if (n == 0)
        //    return s;
        if (s.indexOf('%') < 0){
            return uri;
        }


        StringBuilder sb = new StringBuilder();
        ByteBuffer bb = ByteBuffer.allocate(length);
        CharBuffer cb = CharBuffer.allocate(length);
        CharsetDecoder dec = ThreadLocalCoders.decoderFor("UTF-8")
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);

        char c = s.charAt(0);
        boolean betweenBrackets = false;

        for (int i = 0; i < length; ) {
            assert c == s.charAt(i);    // Loop invariant
            if (c == '[') {
                betweenBrackets = true;
            } else if (betweenBrackets && c == ']') {
                betweenBrackets = false;
            }
            if (c != '%' || betweenBrackets) {
                sb.append(c);
                if (++i >= length)
                    break;
                c = s.charAt(i);
                continue;
            }

            bb.clear();
            for (; ; ) {
                assert (length - i >= 2);
                if (i + 2 < length) {
                    char c1 = s.charAt(++i);
                    char c2 = s.charAt(++i);
                    // Checks if the encoded symbol is not a reserved character or invalid
                    int code = IriCharacters.decode("%" + c1 + c2).charAt(0);

                    //if (reserved.get(code) == null)
                        bb.put(IriCharacters.decode(c1, c2));
                    //else {
                     //   bb.put((byte) '%');
                     //   bb.put((byte) c1);
                     //   bb.put((byte) c2);
                    //}
                } else if (i + 1 < length) {
                    bb.put((byte) '%');
                    bb.put((byte) s.charAt(++i));
                } else bb.put((byte) '%');
                if (++i >= length)
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
