package org.dbpedia.extraction.server.util

import java.net.URLEncoder

object StringUtils {
  
    def urlEncode(name: String) : String = URLEncoder.encode(name, "UTF-8")
      
    /** 
     * Displays minutes, seconds and milliseconds.
     */
    def prettyMillis(millis : Long) = zeros(2, millis / 60000)+':'+zeros(2, millis % 60000 / 1000)+'.'+zeros(3, millis % 1000)+'s'
    
    private val zeros = "0000000000000000000000000"
    
    /**
     * Zeropad givn number.
     * undefined result for more than 20 or so zeros. May even loop forever because of integer overflow.
     */
    private def zeros(z: Int, n: Long) : String = {
        var p = 10
        var e = 1
        while (e < z) {
          if (n < p) return zeros.substring(0, z - e)+n
          p *= 10
          e += 1
        }
        n.toString
    }
}