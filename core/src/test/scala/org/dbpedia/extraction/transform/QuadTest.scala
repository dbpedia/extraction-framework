package org.dbpedia.extraction.transform

import org.junit.Assert._

/**
 * TODO: turn this into an actual JUnit test.
 */
object QuadTest {
  
  def main(args: Array[String]) {
    
    val string = Quad.string
    val langString = Quad.langString
      
    good("""<> <> <> .""", "", "", "", null, null, null)
    good("""<http://www.springernature.com/scigraph/things/articles/00004e48f8491a981bdd8cffc9693228> <http://www.springernature.com/scigraph/ontologies/core/hasContribution> <http://www.springernature.com/scigraph/things/contributions/0589515196e0384623915bc131604822> .""",
      "http://www.springernature.com/scigraph/things/articles/00004e48f8491a981bdd8cffc9693228", "http://www.springernature.com/scigraph/ontologies/core/hasContribution", "http://www.springernature.com/scigraph/things/contributions/0589515196e0384623915bc131604822", null, null, null)
    good(""" <> <> <> . """, "", "", "", null, null, null)
    good("\t<>\t<>\t<>\t.\t", "", "", "", null, null, null)
    good("""<s> <p> <o> . """, "s", "p", "o", null, null, null)
    good("""<> <> "" .""", "", "", "", string, null, null)
    good("""<> <> "\"" . """, "", "", """\"""", string, null, null)
    good("""<> <> "\\\"" . """, "", "", """\\\"""", string, null, null)
    good("""<s> <p> "v" . """, "s", "p", "v", string, null, null)
    good("""<s> <p> "v"^^<t> . """, "s", "p", "v", "t", null, null)
    good("""<s> <p> "v"@l . """, "s", "p", "v", langString, "l", null)
    good("""<s> <p> "v" <c>. """, "s", "p", "v", string, null, "c")
    good("""<s> <p> "v"^^<t> <c>. """, "s", "p", "v", "t", null, "c")
    good("""<s> <p> "v"@l <c>. """, "s", "p", "v", langString, "l", "c")
    good("""<s> <p> "v"@en-us <c>. """, "s", "p", "v", langString, "en-us", "c")
    
    // N-Triples requires space between tokens (not sure about Turtle), but we accept lines without spaces.
    good("""<><><>.""", "", "", "", null, null, null)
    good("""<><>"".""", "", "", "", string, null, null)
    good("""<s><p>"v"@l<c>.""", "s", "p", "v", langString, "l", "c")
    
    // these should all be bad, but some aren't...
    good("""< <> <> <> <>.""", " <", "", "", null, null, "") // unclosed value
    good("""<> < <> <> <>.""", "", " <", "", null, null, "") // unclosed value
    good("""<> <> < <> <>.""", "", "", " <", null, null, "") // unclosed value
    good("""<> <> <> < <>.""", "", "", "", null, null, " <") // unclosed value
    bad ("""<> <> <> <> <.""") // unclosed value
    good("""< <> <> <>.""", " <", "", "", null, null, null) // unclosed value
    good("""<> < <> <>.""", "", " <", "", null, null, null) // unclosed value
    good("""<> <> < <>.""", "", "", " <", null, null, null) // unclosed value
    bad ("""<> <> <> <.""") // unclosed value
    good("""<s> <p> "v"@en--us <c>. """, "s", "p", "v", langString, "en--us", "c") // multiple -- should not be allowed
    good("""<s> <p> "v"@en- <c>. """, "s", "p", "v", langString, "en-", "c") // trailing - should not be allowed
    
    // these should probably be good...
    bad("""<s> <p> "v"@en-US <c>. """) // we don't allow uppercase in language tag
    
    bad("") // empty line
    bad("# <> <> <> .") // comment line
    bad("""<""") // missing value
    bad("""<> <""") // missing value
    bad("""<> <> <""") // missing value
    bad("""<> <> """") // missing value
    bad("""<> <> <> <""") // missing value
    bad("""<> <> "" <""") // missing value
    bad("""<> <> <> <> <> .""") // too many values
    bad("""<> <> "" <> <> .""") // too many values
    bad("""<> <> .""") // missing value
    bad("""<> "" .""") // missing predicate
    bad("""<> <> <>""") // missing dot
    bad("""<> <> <> . <>""") // stuff after dot
    bad("<> <> <> . \r") // CR is not allowed
    bad("<> <> <> . \n") // LF is not allowed
    bad("""<> <> " . """) // unclosed value
    bad("""<> <> < . """) // unclosed value
    bad("""<> <> "\\"" . """) // wrong escape sequence
    bad("""<s> <p> "v"@""") // missing language tag
    bad("""<s> <p> "v"@ . """) // missing language tag
    bad("""<s> <p> "v"^^<""") // missing datatype
    bad("""<s> <p> "v"^^< . """) // missing datatype
    bad("""<s> <p> "v"^<t> . """) // broken datatype syntax
    bad("""<s> <p> "v"@l^^<t> <c>. """) // language and datatype must not both be present
    bad("""<s> <p> "v"^^<t>@l <c>. """) // language and datatype must not both be present
  }
  
  def good(line: String, subject: String, predicate: String, value: String, datatype: String, language: String, context: String): Unit = {
    val option = Quad.unapply(line)
    assertTrue("failed to parse line ["+line+"]", option.isDefined)
    val quad = option.get
    assertEquals("subject", subject, quad.subject)
    assertEquals("predicate", predicate, quad.predicate)
    assertEquals("value", value, quad.value)
    assertEquals("datatype", datatype, quad.datatype)
    assertEquals("language", language, quad.language)
    assertEquals("context", context, quad.context)
  }

  def bad(line: String): Unit = {
    val option = Quad.unapply(line)
    assertFalse("didn't expect result for line ["+line+"], but got "+option, option.isDefined)
  }
}