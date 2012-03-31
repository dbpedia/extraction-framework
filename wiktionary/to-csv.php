#!/usr/bin/php
<?php 
$query = "
PREFIX terms:<http://wiktionary.dbpedia.org/terms/>
PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>
PREFIX dc:<http://purl.org/dc/elements/1.1/>
SELECT ?sword ?slang ?spos ?ssense ?tword ?tlang
FROM <http://wiktionary.dbpedia.org/>
WHERE {
    ?swordRes terms:hasTranslation ?twordRes .
    ?swordRes rdfs:label ?sword .
    ?swordRes dc:language ?slang .
    ?swordRes terms:hasPoS ?spos .
    OPTIONAL { ?swordRes terms:hasMeaning ?ssense . }
    ?twordBaseRes terms:hasLangUsage ?twordRes . 
    ?twordRes dc:language ?tlang .
    ?twordBaseRes rdfs:label ?tword .
}
";

function stripNS($s){
  return substr($s, 36);
}

$conn   = @odbc_connect('VOS', 'dba', 'dba');
if($conn){
    $ok = true;
    $i = 0;
    $f = fopen("translations.csv", "w");
    while($ok){
        $t1 = microtime();
        $ok = false;
        $result = odbc_exec($conn, 'CALL DB.DBA.SPARQL_EVAL(\'' . $query.' LIMIT 100 OFFSET '.$i . '\', NULL, 0)');
        if(!$result){
            echo "error";
        } else {
            while ($row = odbc_fetch_array($result)){
                $line = '"'.addslashes(trim($row['sword'])).'","'.addslashes(stripNS($row['slang'])).'","'.addslashes(stripNS($row['spos'])).'","'.addslashes(trim($row['ssense'])).'","'.addslashes(trim($row['tword'])).'","'.addslashes(stripNS($row['tlang'])).'"'.PHP_EOL; 
                fwrite($f, $line);
                $ok = true;
            }
        }
        //echo "took ".(microtime()-$t1)."ms";
        $i += 100;
    }
    fclose($f);
} else echo "no virtuoso connection".PHP_EOL;
?>
