<?php
require_once("config.php");

foreach($languages as $language) {
  $langStats = trim(file_get_contents("data/$language.stats.json"));
  if (strlen($langStats)>0) { 
	$json[] = "\"" . $language . "\":" . $langStats; 
  } else {
	$json[] = "\"" . $language . "\":" . "{\"templates\":0,\"template_occurrences\":0,\"property_occurrences\":0}" ;
  }  
}
echo "{" . implode(",",$json) . "}";

?>
