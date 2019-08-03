package org.dbpedia.iri;

import org.dbpedia.extraction.util.StringUtils;
import sun.nio.cs.ThreadLocalCoders;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
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
     gen-delims  =  "%3A", "%3F", "%23", "%5B", "%5D", "%2F", "%40",
     gen-delims =  ":", "?", "#", "[", "]", "/", "@",
     sub-delims  =  "%21", "%24", "%26", "%27", "%28", "%29", "%2A", "%2B", "%2C", "%3B", "%3D"
     sub-delims = "!", "$", "&", "'", "(", ")", "*", "+", ",", ";", "="

     Exceptions:

     Systems accepting IRIs MAY also deal with the printable characters in
     US-ASCII that are not allowed in URIs, namely
     "<", ">", '"', space, "{", "}", "|", "\", "^", and "`", in step 2 above.

     The following gen-delims are allowed "/",
     TODO  :@? are left decoded, if found decoded
     TODO  "^` are left decoded, if found decoded
     All sub-delims are used by DBpedia though, but dbpedia encodes the Pipe character (|) with %7D
    */
    //private List<String> reserved = Arrays.asList(StringUtils.replacements('%', "#<>[]{}|", 256));
    private List<String> reserved_gen_delim = new ArrayList<String>( Arrays.asList(
            "%3A", "%3F", "%23", "%5B", "%5D", "%40",
            //":", "?",   "#",   "[",   "]",   "@",
            "%3C","%3E","%22","%20","%7B","%7D","%7C","%5C","%5E","%60"
            // "<", ">", '"', " ", "{", "}", "|", "\", "^", "`"

    ));

    public String decode(String uri) {
        String uriString = uri;
        int length = uriString.length();

        if (uriString == null)
            return uriString;

        //if (n == 0)
        //    return uriString;

        // if nothing encoded, return as is
        if (uriString.indexOf('%') < 0){
            return uri;
        }


        StringBuilder sb = new StringBuilder();
        ByteBuffer bb = ByteBuffer.allocate(length);
        CharBuffer cb = CharBuffer.allocate(length);
        CharsetDecoder dec = ThreadLocalCoders.decoderFor("UTF-8")
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);


        boolean betweenBrackets = false;
        char c = uriString.charAt(0);
        for (int i = 0; i < length; ) {
            assert c == uriString.charAt(i);    // Loop invariant

            // between brackets?
            if (c == '[') {
                betweenBrackets = true;
            } else if (betweenBrackets && c == ']') {
                betweenBrackets = false;
            }

            //
            if (c != '%' || betweenBrackets) {
                sb.append(c);
                if (++i >= length)
                    break;
                c = uriString.charAt(i);
                continue;
            }

            bb.clear();
            for (; ; ) {
                assert (length - i >= 2);
                if (i + 2 < length) {
                    char c1 = uriString.charAt(++i);
                    char c2 = uriString.charAt(++i);
                    // Checks if the encoded symbol is not a reserved character or invalid

                    // what does the line below even do?
                    //int code = IriCharacters.decode("%" + c1 + c2).charAt(0);

                    //System.out.println("char %"+c1+c2);

                    if("%20".equals("%"+c1+c2)){
                        bb.put((byte)'_');
                    }
                    // no decoding
                    else if (!reserved_gen_delim.contains("%"+c1+""+c2))
                        bb.put(IriCharacters.decode(c1, c2));
                    else {
                        bb.put((byte) '%');
                        bb.put((byte) c1);
                        bb.put((byte) c2);
                    }
                } else if (i + 1 < length) {
                    bb.put((byte) '%');
                    bb.put((byte) uriString.charAt(++i));
                } else bb.put((byte) '%');
                if (++i >= length)
                    break;
                c = uriString.charAt(i);
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
