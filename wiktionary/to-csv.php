#!/usr/bin/php
<?php 
$query = "
PREFIX terms:<http://wiktionary.dbpedia.org/terms/>
PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>
PREFIX dc:<http://purl.org/dc/elements/1.1/>
SELECT ?swordRes ?sword ?slang ?spos ?ssense ?twordRes ?tword ?tlang
FROM <http://wiktionary.dbpedia.org/>
WHERE {
    ?swordRes terms:hasTranslation ?twordRes .
    OPTIONAL {
        ?swordRes rdfs:label ?sword .
        ?swordRes dc:language ?slang .
        ?swordRes terms:hasPoS ?spos .
    }
    OPTIONAL { ?swordRes terms:hasMeaning ?ssense . }
    OPTIONAL { 
        ?twordBaseRes terms:hasLangUsage ?twordRes . 
        ?twordBaseRes rdfs:label ?tword .
    }
    OPTIONAL { ?twordRes dc:language ?tlang . }
}
";

function stripNS($s){
  return substr($s, 36);
}
function startWithUpper($str){
  if(strlen($str)==0) return false;
  return $str{0} === strtoupper($str{0});
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
                if(empty($row['sword']) || empty($row['slang'])  || empty($row['spos']) ){
                    $swordRes = $row['swordRes'];
                    $tail = array_pop(explode('/', $twordRes)); 
                    $parts = explode('-', $tail);
                    if(count($parts)==2) continue;
                    $sword = array_shift($parts); //first
                    $spos = array_pop($parts); //last
                    if(is_numeric($spos)){
                        $spos = array_pop($parts); //last
                    }
                    $slang = implode('-', $parts); //rest
                } else {
                    $sword = $row['sword'];
                    $spos = stripNS($row['spos']);
                    $slang = stripNS($row['slang']);
                }
                if(empty($row['tword']) || empty($row['tlang']) ){
                    $twordRes = $row['twordRes'];
                    $tail = array_pop(explode('/', $twordRes)); 
                    $parts = explode('-', $tail);
                    $numParts = count($parts);
                    if($numParts < 2) continue;
                    $firstUpper = 1;

                    while(!startWithUpper($parts[$firstUpper]) && $firstUpper < ($numParts-1)){
                        $firstUpper++;
                    }
                    
                    if(empty($row['tword'])){
                        $tword = implode('-', array_slice($parts, 0, $firstUpper));
                    } else {
                        $tword = $row['tword'];
                    }
                    if(empty($row['tlang'])){
                        $tlang = implode('-', array_slice($parts, $firstUpper, count($parts) - $firstUpper));
                    } else {
                        $tlang = $row['tlang'];
                    }
                } else {
                    $tword = $row['tword'];
                    $tlang = $row['tword'];
                }
                $line = '"'.addslashes(trim($sword)).'","'.addslashes(trim($slang)).'","'.addslashes(trim($spos)).'","'.addslashes(trim($row['ssense'])).'","'.addslashes(trim($tword)).'","'.addslashes(trim($tlang)).'"'.PHP_EOL; 
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
