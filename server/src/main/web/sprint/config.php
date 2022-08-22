<?php
//$languages = array("ar","bg","bn","ca","cs","de","el","en","es","eu","fr","ga","hi","hr","hu","it","ja","ko","nl","pl","pt","ru","sl","tr");
require 'scraperwiki/scraperwiki.php';
require 'scraperwiki/simple_html_dom.php';

$languages = getLangs();

function getLangs() {
  $url = "http://mappings.dbpedia.org/server/statistics/";

  $html = scraperwiki::scrape($url);

  $dom = new simple_html_dom();
  $dom->load($html);

  $i = 0;
  $langs = array();
  foreach($dom->find('/html/body/p/a') as $result) {
        $lang = str_replace("/","",trim($result->href));
        $langs[] = $lang;
  }
  return $langs;
}

?>
