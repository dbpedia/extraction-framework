<?php 

/**
 * Helper functions to clean up wiki source text for dbpedia.
 */
class DBpediaFunctions {

/* 
To make sure that Apache sends debug output to the browser,
add the following to index.php:

@apache_setenv('no-gzip', 1);
@ini_set('zlib.output_compression', 0);
@ini_set('implicit_flush', 1);
for ($i = 0; $i < ob_get_level(); $i++) { ob_end_flush(); }
ob_implicit_flush(1);

*/

	
	/**
	 * Should Wiki and HTML code be cleaned? If false, generate HTML for the 
	 * introduction as usual. May be useful for debugging.
	 */
	const CLEAN = true;
	
	private static function removeShortIPA( $matches ) {
		return substr_count($matches[1], " ") <= 3 ? "" : $matches[0];
	}
	
	private static function replaceTooManyLinks( $matches ) {
		$match = $matches[0];
		$countLinks = substr_count($match ,"<a ");
		if ($countLinks <= 0) return $match;
		$match_without_links = preg_replace("~<a(\s+[^>]*)?>.*?</a>~", "", $match);
		$matchWords = split(" ", strip_tags($match_without_links));
		return $countLinks / sizeof($matchWords) >= 0.25 ? "" : $match;
	}
	
	/**
	 * Get HTML from $wgOut->getHTML(), call self::cleanHtml(), set HTML
	 * back into $wgOut.
	 */ 
	static function cleanOutput() {
		global $wgOut;
		
		if (! self::CLEAN) return;
		
		// wfProfileIn( __METHOD__ );
		
		$html = $wgOut->getHTML();
		$html = self::cleanHtml($html);
		
		$wgOut->clearHTML();
		$wgOut->addHTML($html);
		
		// wfProfileOut( __METHOD__ );
	}
	
	/**
	 * Use this line for debugging:
	 * echo __LINE__, "<p/>\n", $html, "<hr/>\n";
	 * @param $html the html
	 */ 
	static function cleanHtml( $html ) {
		
		$html = preg_replace("~<br ?/?>~", "", $html);
		
		// remove map references
		// example: http://en.wikipedia.org/w/index.php?title=Attila_the_Hun&oldid=300682051
		$html = preg_replace("~\s?\(see map below\)~", "", $html);

		// <a href="/wikipedia/index.php/Image:Ltspkr.png" title="Image:Ltspkr.png">Image:Ltspkr.png</a>
		$html = preg_replace("~<a\s[^>]*Image:Ltspkr\.png[^>]*>[^<]*</a>~", "", $html);
		$html = preg_replace("~<a\s[^>]*File:Loudspeaker\.svg[^>]*>[^<]*</a>~", "", $html);
		
		// [[]]
		// TODO: Log warning. Links should have been rendered / removed. 
		$html = preg_replace("/\[\[[^\]]*?\]\]/", "", $html);

		// remove nested round brackets: (x(y)z -> (xz
		do {
			$in = $html;
			$html = preg_replace("~(\([^)(]*)\([^)(]*\)~", "$1", $html);
		} while ($html != $in);
		// remove nested brackets: (x[y]z -> (xz#
		do {
			$in = $html;
			// TODO: use "~(\([^)[]*)\[[^)]]*\]~" instead
			$html = preg_replace("~(\([^)]*)\[[^]]*\]~", "$1", $html);
		} while ($html != $in);

		// delete parentheses with too many links
		$html = preg_replace_callback("/\s?\(.*?\)/", 'DBpediaFunctions::replaceTooManyLinks', $html);

		// <a href="http://www.britannica.com/EBchecked/topic/509710/rose" class="external autonumber" title="http://www.britannica.com/EBchecked/topic/509710/rose" rel="nofollow">[1]</a>
		$html = preg_replace("~<a[^>]*class=\"external autonumber[^>]*>.*?</a>~", "", $html);

		$html = self::replaceAudioSpans($html);

		// <sup id="cite_ref-0" class="reference"><a href="#cite_note-0" title="">[1]</a></sup>
		$html = preg_replace("~<sup(\s+[^>]*)?>.*?</sup>~", "", $html);

		$html = preg_replace("~<small(\s+[^>]*)?>.*?</small>~", "", $html);

		// (pronounced <span title="Pronunciation in the International Phonetic Alphabet (IPA)" class="IPA">
		// <span class="IPA
		$html = preg_replace("~\([^)]*<span [^>]*class=\"[^\"]*IPA[^\"]*\"[^>]*>.*?</span>~", "(", $html);

		// <a href="/wikipedia/index.php/Help:Pronunciation" class="mw-redirect" title="Help:Pronunciation">pronounced</a> <span class="IP
		$html = preg_replace("~\(<a href[^>]*>.*?</a> <span [^>]*class=\"[^\"]*IPA[^\"]*\"[^>]*>.*?</span>~", "(", $html);

		// <a href="/wikipedia/index.php?title=Special:Upload&amp;wpDestFile=En-us-Denmark.ogg" class="new" title="En-us-Denmark.ogg">[?d?nm?rk]</a></span>
		$html = preg_replace("~<a .*?>\[(.*?)\]</a>~", "", $html);

		$html = preg_replace("~<a .*?>\[?(.*?)\]?</a>~", "$1", $html);

		//<strong class="error">
		$html = preg_replace("~<strong class=\"error\">.*?</strong>~", "", $html);

		$html = strip_tags($html);

		$html = str_replace("&nbsp;", " ", $html);
		$html = str_replace("&#32;", " ", $html);
		$html = str_replace("&#x20;", " ", $html);
		
		// TODO: why is it necessary to remove '//'? And why escape '/'?
		$html = str_replace("\/\/", "", $html);

		$html = self::removeNonsense($html);

		// remove " '  '" before ")" TODO: why?
		$html = preg_replace("/[\s]*'[\s]*'\)/", ")", $html);
		// remove "'  ' " after "(" TODO: why?
		$html = preg_replace("/\('[\s]*'[\s]*/", "(", $html);

		// remove "xyz:,;..." after "("
		// example: (Sinhalese:, ... )
		$html = preg_replace("/\([^\s\)]*?:[,;]+/", "(", $html);

		// remove "(xyz: ')" and "(xyz: )" 
		// example: (Arabic: ')
		// not necessary - we remove brackets containing colons below
		// $html = preg_replace("/\([^\s\)]*: '?\)/", "", $html);

		$html = self::removeNonsense($html);

		// remove "(... ')"
		$html = preg_replace("/\([^\s\)]* '\)/", "", $html);
		// remove "(' ...)"
		$html = preg_replace("/\(' [^\s\)]*\)/", "", $html);

		$html = self::removeNonsense($html);

		$html = str_replace("\n", " ", $html);

		$html = trim($html);

		//AUDIO stuff:
		// , IPA:?t???si:,
		$html = preg_replace_callback("/,\s*IPA:(.*?),/s", 'DBpediaFunctions::removeShortIPA', $html);

		// remove brackets containing colons.
		$html = preg_replace("/\([^\)]*?:[^\)]*?\)/s", "", $html);

		$html = self::removeNonsense($html);
		
		// if there's a closing bracket before an opening bracket, remove it.
		if (strpos($html, ")") < strpos($html, "(")) {
			$html = preg_replace('/\)/', '', $html, 1);
		}
		
		// Todo: what about "3.4 % of"? 
		$html = preg_replace("/\.([^0-9.][^0-9.])/", ". $1", $html);
		
		$html = preg_replace("/ +/", " ", $html);

		return $html;
	}
	
	private static function removeNonsense($html) {
		
		//Remove spaces before commas and semicolons
		$html = preg_replace("/\s+,/", ",", $html);
		$html = preg_replace("/\s+;/", ";", $html);

		// TODO: when can this happen?
		// $html = preg_replace("/[;|,]..[;|,]/", ",", $html);

		//Remove spaces near brackets
		$html = preg_replace("~\(\s*~", "(", $html);
		$html = preg_replace("~\s*\)~", ")", $html);
		
		//Remove commas etc. near brackets
		$html = preg_replace("~\(([;,:]\s*)*~", "(", $html);
		$html = preg_replace("~(\s*[;,:])*\)~", ")", $html);
		
		//Remove empty brackets
		$html = preg_replace("/\[[\s]*\]/", "", $html);
		$html = preg_replace("/\([\s]*\)/", "", $html);

		return $html;
	}

	/**
	 * TODO: Log if the following matches. It's probably no longer needed.
	 */ 
	private static function replaceAudioSpans( $html ) {
		// <p><span class="unicode audiolink"><a href="http://upload.wikimedia.org/wikipedia/commons/b/b4/Stavanger.ogg" class="internal" title="Stavanger.ogg"><b>Stavanger</b></a></span>
		if ((strpos($html, "<span class=\"unicode audiolink\">") === 0) || (strpos($html, "<b><span class=\"unicode audiolink\">") === 0)) {
			/*
			$html = preg_replace("~<span class=\"unicode audiolink\"><a .*?>(.*?)</a></span>~s", "$1", $html);
			*/
			$html = preg_replace("~<span class=\"unicode audiolink\">(.*?)</span>~s", "$1", $html);
		} else {
			//<span class="unicode audiolink"><a href="/wikipedia/index.php?title=Special:Upload&amp;wpDestFile=Eugen_Berthold_Friedrich_Brecht.ogg" class="new" title="Eugen Berthold Friedrich Brecht.ogg"><b>Eugen Berthold Friedrich Brecht</b></a></span>&nbsp;<span class="metadata audiolinkinfo">>(<a href="/wikipedia/index.php/Wikipedia:Media_help" title="Wikipedia:Media help">help</a>�<a href="/wikipedia/index.php?title=Image:Eugen_Berthold_Friedrich_Brecht.ogg&amp;action=edit&amp;redlink=1" class="new" title="Image:Eugen Berthold Friedrich Brecht.ogg (page does not exist)">info</a>)</small></span>
			$html = preg_replace("~<span class=\"unicode audiolink\"><a [^>]*><b>(.*?)</b></a></span>~s", "$1", $html);
			// <span class="unicode audiolink"><a href="/wikipedia/index.php?title=Special:Upload&amp;wpDestFile=Anime.ogg" class="new" title="Anime.ogg"><i>listen</i></a></span>&nbsp;
			$html = preg_replace("~<span class=\"unicode audiolink\">.*?</span>~s", "", $html);
			
			// replace audio spans in parentheses
			// (<span class="unicode" style="white-space: nowrap;"> <a href="/wikipedia_en/index.php?title=Special:Upload&amp;wpDestFile=It-Leonardo_di_ser_Piero_da_Vinci.ogg" class="new" title="It-Leonardo di ser Piero da Vinci.ogg">pronunciation</a> </span>
			while (preg_match("~\(([^)]*?)<span\sclass=\"unicode\"[^>]*>.*?</span>~s", $html)) {
				$html = preg_replace("~\(([^)]*?)<span\sclass=\"unicode\"[^>]*>.*?</span>~s", "($1", $html);
			}
		}

		// TODO: this shouldn't be replaced when it is at the beginning of the text
		//<span class="metadata audiolinkinfo"><small>(<a href="/wikipedia/index.php/Wikipedia:Media_help" title="Wikipedia:Media help">help</a>�
		$html = preg_replace("~<span class=\"metadata audiolinkinfo\">.*?</span>~s", "", $html);
		$html = preg_replace("~<span class=\"audiolinkinfo\">.*?</span>~s", "", $html);
		
		$html = preg_replace("~<a href=\"\/wikipedia\/index\.php\/Datei:Loudspeaker\.svg\" title=\"Datei:Loudspeaker.svg\">Datei:Loudspeaker\.svg<\/a>~", "", $html);

		// echo $html."<hr>";

		return $html;
	}
		
	/**
	 * Use this line for debugging:
	 * echo __LINE__, "<p/>\n", $wiki, "<hr/>\n";
	 * @param $wiki the wiki source
	 * @return a clean and short version of the given wiki source
	 */ 
	static function getAbstract( $wiki ) {
		
		//wfProfileIn( __METHOD__ );
		
		$section = self::getFirstSection($wiki);
		
		//echo str_replace("\n", "<br>", $section) . "<br>---</br>";
		
		if (self::CLEAN) $section = self::cleanWikiText($section);
		
		// The first occurrence of a bold word probably distinguishes the introductory section.
		$section = self::getBoldSection($section);

		//wfProfileOut( __METHOD__ );
		
		return $section;
	}
	
	// only check the first three sections
	const MAX_SECTIONS = 3;
	
	/**
	 * The first occurrence of a bold word probably distinguishes the introductory section.
	 */
	private static function getFirstSection( $wiki ) {
		// remove comments
		$wiki = preg_replace('~ ?<!--.*?-->~s','', $wiki);
		
		// echo $wiki."<hr>";

		// split text into sections: separated by headings or horizontal lines
		$sections = preg_split("~(^=.*=\s*$|^----.*$)~m", $wiki, self::MAX_SECTIONS + 1, PREG_SPLIT_NO_EMPTY);
		$first_sections = "";
		foreach ($sections as $si => $section) {
			if ($si === self::MAX_SECTIONS) break;
			if (strpos($section, "'''") !== false) {
				// echo "<h1>FOUND ''' IN</H1>" . $section . "<hr>";
				$parts = explode("'''", $section);
				$passed_parts = "";
				foreach ($parts as $part) {
					$passed_parts .= $part;
					$first_section_plus_part = $first_sections . $passed_parts;
					$count_open_brackets = substr_count($first_section_plus_part, "{{");
					$count_closing_brackets =  substr_count($first_section_plus_part, "}}");
					if ($count_open_brackets <= $count_closing_brackets) {
						// echo "<h1>FOUND IN</H1>" . $first_sections . $section . "<hr>";
						return $first_sections . $section;
					}
				}
			}
			$first_sections .= $section;
		}
		
		// No bold word? Just use the first section. TODO: improve the heuristic.
		return strlen($sections[0]) >= 20 ? $sections[0] : "";
	}	

	/**
	 * The first occurrence of a bold word probably distinguishes the introductory section.
	 */
	private static function getBoldSection( $wiki ) {
		// split text into sections: separated by headings or horizontal lines
		$sections = preg_split("~(^=.*=\s*$|^----.*$)~m", $wiki, self::MAX_SECTIONS + 1, PREG_SPLIT_NO_EMPTY);
		foreach ($sections as $si => $section) {
			if ($si === self::MAX_SECTIONS) break;
			// split section into paragraphs: separated by empty lines
			$paras = preg_split("~\n\s*\n~", $section, -1,  PREG_SPLIT_NO_EMPTY);
			foreach ($paras as $pi => $para) {
				// remove all paragraphs before the one containing the bold word
				if (strpos($para, "'''") !== false) {
                    //echo "BOLD SECTION!!! ".$section;
					return $pi === 0 ? $section : implode(' ', array_slice($paras, $pi));
				}
			}
		}
		
		// No bold word? Just use the first section. TODO: improve the heuristic.
		//echo "SECTION: ".$sections[0]."<hr>";

		return strlen($sections[0]) >= 20 ? $sections[0] : "";
	}	
		
	private static function cleanWikiText( $wiki ) {
		
		/*
		$count_open_brackets = substr_count($wiki, "{{");
		$count_closing_brackets =  substr_count($wiki, "}}");

		if ($count_open_brackets > $count_closing_brackets) {
			// If template brackets don't match - which is the case in roughly 
			// 1000 articles of 3 mio - the recursive regex in cleanTemplates()
			// may crash our process with a stack overflow. To avoid this, simply
			// don't extract an abstract for this page.
			// return "";
		}
		*/
		
		$wiki = str_replace("\r\n", "\n", $wiki);
		$wiki = str_replace("\r", "\n", $wiki);
		
		// remove comments
		// moved to getBoldSection $wiki = preg_replace('~ ?<!--.*?-->~s','', $wiki);
		
		// remove tables
		// was: $wiki = preg_replace('~{\|.*?\|}~s','', $wiki);

		
		$wiki = preg_replace('~^[\s|:]*\{\|~m', "12345654321", $wiki);
		$wiki = preg_replace('~^[\s|:]*\|\}(?!\})~m', "98765456789", $wiki);
		
		// echo $wiki . "<hr>";
		$wiki = self::cleanTemplates($wiki);
		// echo $wiki . "<hr>";
		
		$wiki = str_replace("12345654321", "{|", $wiki);
		$wiki = str_replace("98765456789", "|}", $wiki);
		
		// echo $wiki . "<hr>";

		$wiki_array = explode("\n", $wiki);
		//array_walk($wiki_array, create_function('&$temp', $temp = trim($temp)));
		
		$wiki = "";
		
		$c = 0;
		foreach ($wiki_array as $line) {
			$tLine = ltrim($line);
			if ((strlen($tLine) >= 2) && (strncmp($tLine, "{|", 2) == 0)) {
				$c += 1;
			} else if (($c > 0) && (strlen($tLine) >= 2) && (strncmp($tLine, "|}", 2) == 0)) {
				$c -= 1;
				if ($c == 0 && strlen($tLine) >= 3) $wiki .= substr($tline, 2) . "\n";
			} else if ($c == 0) $wiki .= $line . "\n";
		}
		
		/*
		do {
			$in = $wiki;
			if (strpos($wiki, "{|") !== false) {
				$wiki = preg_replace('~\{\|((?!\{\||\|\}).)*\|\}~s', '', $wiki);
            }
		} while ($wiki != $in);
		*/

		// clean <math> </math>
		$wiki = preg_replace("~<math(\s+[^>/]*)?>.*?</math>~s", "", $wiki);

		// clean <imagemap>
		$wiki = preg_replace("~<imagemap(\s+[^>/]*)?>.*?</imagemap>~s", "", $wiki);

		// clean <gallery>
		$wiki = preg_replace("~<gallery(\s+[^>/]*)?>.*?</gallery>~s", "", $wiki);
		
		// first clean references, then clean links (there might be special links with refs as parameters)
		$wiki = self::cleanRefs($wiki);

		$wiki = self::cleanLinks($wiki);
		return $wiki;
	}

	/**
	 * Remove some templates: 'otheruses', 'TOC', 'Unreferenced',
	 * 'Audio', 
	 * and templates that contain line breaks or are in a separate line.
	 * @param $wiki the wiki source
	 * @return the given wiki source minus some templates
	 */ 
	private static function cleanTemplates( $wiki ) {
		
    //Remove single curly braces
    $wiki = preg_replace('/(?<!\{)\{(?!\{)/s', "$1($2", $wiki);
    $wiki = preg_replace('/(?<!\})\}(?!\})/s', "$1)$2", $wiki);

		$wiki = preg_replace('/\{\{otheruses\}\}/xi', "", $wiki);
		$wiki = preg_replace('/\{\{TOC[a-zA-Z]*\}\}/xi', "", $wiki);
		$wiki = preg_replace('/\{\{Unreferenced[^\}]*\}\}/xi', "", $wiki);
		// $wiki = preg_replace('/\{\{Audio\s*\|([^\|\}]*)\|([^\|\}]*)\}\}/i', "$2", $wiki);
		// $wiki = preg_replace('/\(\{\{IPA[^\}]*\}\}\)/', "", $wiki);
		
		//HACK leave Bio Templates in italian Wikipedia
		$wiki = str_replace('{{Bio', 'BIO35363', $wiki);

		// TODO: what does (? > and (?R) mean?
		preg_match_all('/\{\{((?>[^\{\}]+)|(?R))*\}\}/x',$wiki,$templates);
		// preg_match_all('/\{\{(?:(?!\{\{|\}\}).)*\}\}/x',$wiki,$templates);
	
		$wiki_array = split("( |\t)*\n( |\t)*", $wiki);
		array_walk($wiki_array, create_function('&$temp', $temp = trim($temp)));
			
		foreach($templates[0] as $tpl) {
			// echo "TEMPLATE: " . $tpl . "<hr>";

			// echo $tpl . "<hr>";
			// TODO: why do we need this? The regex above 
			// only matches strings that start with '{'.
			if($tpl[0]!='{') {
				continue;
			}
			
			// If the template contains line breaks or is 
			// in a separate line, remove it. 
			if (strpos($tpl, "\n") || in_array($tpl, $wiki_array)) {
				$wiki = str_replace($tpl, "", $wiki);
				// if template would be a seperate line after deleting other templates, we wouldn't find it,
				// so we split the wiki page again
				// example: http://en.wikipedia.org/w/index.php?title=Vladimir_Lenin&oldid=300435749
				// Todo: Time costs, new errors?
				$wiki_array = split("( |\t)*\n( |\t)*", $wiki);
			}
		}
		
		// delete lines with only templates on it, keep lines with at least one word character besides templates
		$wiki_array = split("( |\t)*\n( |\t)*", $wiki);
		array_walk($wiki_array, create_function('&$temp', $temp = trim($temp)));
		foreach ($wiki_array as $id => $line) {
			$line_temp = $line;
			foreach ($templates[0] as $tpl) {
				$line_temp = str_replace($tpl, "", $line_temp);				
			}
			$line_temp = trim(strip_tags($line_temp));
			
			// if we didn't change anything, don't delete the line.
    		// TODO: we want to remove lines that contain stuff like '*' at the start and otherwise only templates,
    		// but we may want to keep lines that contain templates but important wiki markup, like ']]'.
			if (($line_temp !== $line) &&  (! preg_match("~\pL~", $line_temp))) {
				unset($wiki_array[$id]);
			}
		}
		
		$wiki = implode("\n", $wiki_array);
		
		//HACK leave Bio Templates in italian Wikipedia
		$wiki = str_replace('BIO35363', '{{Bio', $wiki);
		
		return $wiki;
	}
	
	/**
	 * Remove special links (ones whose target starts with a namespace). 
	 * @param $wiki the wiki source
	 * @return the given wiki source minus some links
	 */ 
	private static function cleanLinks( $wiki ) {
		return preg_replace("/\[\[[^\|\[\]\{\}\.]+:(?:[^\]]*?\[\[[^\]]*?\]\])*[^\[]*?\]\] */", "", $wiki);
	}
	
	/**
	 * @param $wiki the wiki source
	 * @return the given wiki source minus some links
	 */ 
	private static function cleanRefs( $wiki ) {
		$wiki = preg_replace("~<ref(\s+[^>/]*)?/>~", "", $wiki);
		$wiki = preg_replace("~<ref(\s+[^>/]*)?>.*?</ref>~", "", $wiki);
		return $wiki;
	}
	
	static function print_html_stack() {
		$pathlen = strlen(dirname(__FILE__)) + 1;
		$stack = debug_backtrace(false);
		foreach ($stack as $index => $frame) {
			if ($index > 0) echo $frame['class'], '::', $frame['function'], ' (', $file, ':', $line, ')', "<br/>\n";
			$file = $frame['file'];
			$file = substr($file, $pathlen);
			$line = $frame['line'];
		}
		echo "<hr/>\n";
	}

}
