package org.dbpedia.extraction.server.util

import java.net.URLEncoder

object StringUtils {
  
    def urlEncode(name: String) : String = URLEncoder.encode(name, "UTF-8")
      
}