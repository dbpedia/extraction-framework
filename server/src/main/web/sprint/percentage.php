<?php
$contents = file_get_contents("http://mappings.dbpedia.org/server/p");
$start = stripos($contents,"<body>");
$end = stripos($contents,"</body>");
$p = trim(substr($contents,$start+7,$end-$start-7));

echo '{ "percentage": "'.$p.'" }';

?>

