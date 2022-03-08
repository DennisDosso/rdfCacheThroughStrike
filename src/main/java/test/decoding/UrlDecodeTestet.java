package test.decoding;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class UrlDecodeTestet {

    public static void main(String[] args) {
        String s = "SELECT+%3Fvar1Label+%0AWHERE+%7B%0A++%3Fvar2++%3Chttp%3A%2F%2Fwikiba.se%2Fontology%23timePrecision%3E++%2211%22%5E%5E%3Chttp%3A%2F%2Fwww.w3.org%2F2001%2FXMLSchema%23integer%3E+.%0A++%3Fvar2++%3Chttp%3A%2F%2Fwikiba.se%2Fontology%23timeValue%3E++%3Fvar3+.%0A+FILTER+%28++%28+%28++YEAR+%28++%3Fvar3++%29++%3D++YEAR+%28++NOW+%28++%29++%29++%29+%29+%0A%29+.%0A+FILTER+%28++%28+%28++MONTH+%28++%3Fvar3++%29++%3D++MONTH+%28++NOW+%28++%29++%29++%29+%29+%0A%29+.%0A+FILTER+%28++%28+%28++DAY+%28++%3Fvar3++%29++%3D++DAY+%28++NOW+%28++%29++%29++%29+%29+%0A%29+.%0A++%3Fvar1+%28+%3Chttp%3A%2F%2Fwww.wikidata.org%2Fprop%2FP570%3E+%2F+%3Chttp%3A%2F%2Fwww.wikidata.org%2Fprop%2Fstatement%2Fvalue%2FP570%3E+%29+%3Fvar2+.%0A+SERVICE++%3Chttp%3A%2F%2Fwikiba.se%2Fontology%23label%3E+++%7B%0A++++%3Chttp%3A%2F%2Fwww.bigdata.com%2Frdf%23serviceParam%3E++%3Chttp%3A%2F%2Fwikiba.se%2Fontology%23language%3E++%22en%22.%0A++%7D%0A%7D%0AORDER+BY+ASC%28+%3Fvar3+%29%0A\t2017-07-10 00:00:00\trobotic\tUNKNOWN";


        try {
            String result = java.net.URLDecoder.decode(s, StandardCharsets.UTF_8.name());
            result = result.replaceAll("\n", " ");
            System.out.println(result);
        } catch (UnsupportedEncodingException e) {
            // not going to happen - value came from JDK's own StandardCharsets
        }
    }
}
