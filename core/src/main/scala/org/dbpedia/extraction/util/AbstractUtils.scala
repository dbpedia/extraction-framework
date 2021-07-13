package org.dbpedia.extraction.util

object AbstractUtils {

  /**
      this method removes broken information with brackets like (; some info), (, some info) or ()
   */
  def removeBrokenBracketsInAbstracts(text: String): String = {
    var closeBrackets = 0
    val result = new StringBuilder()
    var bracketsWithSemicolon = 0
    var skipBrackets = 0
    for (i <- 0 until text.length) {
      if (text(i) == '(') {
        if ((i < text.length-1) && (text(i+1) == ';' || text(i+1) ==',') && bracketsWithSemicolon == 0) {
          bracketsWithSemicolon = 1
        }
        else if (bracketsWithSemicolon > 0) {
          bracketsWithSemicolon += 1
        }
        else if ((i < text.length-1) && (text(i+1) == ')')) {
          skipBrackets = 2
        }
      }
      else if (text(i) == ')' ) {
        closeBrackets += 1
        if (closeBrackets == bracketsWithSemicolon) {
          bracketsWithSemicolon = 0
          closeBrackets = 0
          skipBrackets += 1
        }
      }
      if (bracketsWithSemicolon == 0 && skipBrackets == 0) {
        // if the previous character was space and the next is also space then we skip it
        if (!(result.nonEmpty && result.last == ' ' && text(i) == ' ' )) {
          result.append(text(i))
        }
      }
      if (skipBrackets > 0) {
        skipBrackets -= 1
      }
    }
    result.toString().trim
  }
}
