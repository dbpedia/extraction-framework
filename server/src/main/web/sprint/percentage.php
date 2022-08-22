<?php
$contents = file_get_contents("http://mappings.dbpedia.org/sprint/data/en.stats.json");
$p = json_decode($contents,true);
echo '{ "percentage": "'.$p["template_occurrences"].'" }';
?>

