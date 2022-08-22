<?php
require 'scraperwiki/scraperwiki.php';
require 'scraperwiki/simple_html_dom.php';

$language = $argv[1];
$stats = getStats($language);
echo json_encode($stats)."\n";

function getStats($language) {
  $url = "http://mappings.dbpedia.org/server/statistics/$language/";

  $html = scraperwiki::scrape($url);  

  $dom = new simple_html_dom();
  $dom->load($html);

  $i = 0;
  $fieldNames = array("templates", "template_occurrences", "property_occurrences");
  $stats = array();
  foreach($dom->find('/html/body/p') as $result) {
	$paragraph = $result->plaintext;
	$parsed = explode("\n",$paragraph);
        $percentage = (double) trim($parsed[1]);
        $f = $fieldNames[$i++]; // one of $fieldNames
        $stats[$f] = $percentage;
  }
  return $stats;
}
?>
