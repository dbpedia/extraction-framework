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
function request($url){
 
   // is curl installed?
   if (!function_exists('curl_init')){ 
      die('CURL is not installed!');
   }
 
   // get curl handle
   $ch= curl_init();
 
   // set request url
   curl_setopt($ch, 
      CURLOPT_URL, 
      $url);
 
   // return response, don't print/echo
   curl_setopt($ch, 
      CURLOPT_RETURNTRANSFER, 
      true);
 
   /*
   Here you find more options for curl:
   http://www.php.net/curl_setopt
   */		
 
   $response = curl_exec($ch);
 
   curl_close($ch);
 
   return $response;
}
function jsonRemoveUnicodeSequences($json) {
   return str_replace("\\U", "\\u", $json);
}
//$conn   = @odbc_connect('VOS', 'dba', 'dba');
if(true){
    $ok = true;
    $stepping = 10000	;
    $i = 0;
    $f = fopen("translations.csv", "w");
    while($ok){
        $t1 = microtime(true);
        $ok = false;
        $url = "http://wiktionary.dbpedia.org/sparql?query=".urlencode($query." LIMIT $stepping OFFSET ".($i))."&format=json";
        $result = request($url);
        $resClean = jsonRemoveUnicodeSequences($result);
        if(!$result){
            echo "error".PHP_EOL;
        } else {
            $rows = json_decode($resClean, true);
            //var_dump($rows);
            if(count($rows['results']['bindings']) == $stepping){
                $ok = true;
            } else {
                echo "last round".PHP_EOL;
            } 
            if($rows == NULL || !is_array($rows['results']['bindings'])){
                echo "invalid json".PHP_EOL;
                var_dump($rows);
                echo "sparql endpoint returned: ".$resClean.PHP_EOL;
            }
            foreach($rows['results']['bindings'] as $row){
                if(!isset($row['sword']) || !isset($row['slang'])  || !isset($row['spos']) ){
                    $swordRes = $row['swordRes']['value'];
                    $tail = array_pop(explode('/', $swordRes)); 
                    $parts = explode('-', $tail);
                    if(count($parts)==2) continue;
                    $sword = array_shift($parts); //first
                    $spos = array_pop($parts); //last
                    if(is_numeric($spos)){
                        $spos = array_pop($parts); //last
                    }
                    $slang = implode('-', $parts); //rest
                } else {
                    $sword = $row['sword']['value'];
                    $spos = stripNS($row['spos']['value']);
                    $slang = stripNS($row['slang']['value']);
                }
                if(!isset($row['tword']) || !isset($row['tlang']) ){
                    $twordRes = $row['twordRes']['value'];
                    $tail = array_pop(explode('/', $twordRes)); 
                    $parts = explode('-', $tail);
                    $numParts = count($parts);
                    if($numParts < 2) continue;
                    $firstUpper = 1;

                    while(!startWithUpper($parts[$firstUpper]) && $firstUpper < ($numParts-1)){
                        $firstUpper++;
                    }
                    
                    if(!isset($row['tword'])){
                        $tword = implode('-', array_slice($parts, 0, $firstUpper));
                    } else {
                        $tword = $row['tword']['value'];
                    }
                    if(!isset($row['tlang'])){
                        $tlang = implode('-', array_slice($parts, $firstUpper, count($parts) - $firstUpper));
                    } else {
                        $tlang = $row['tlang']['value'];
                    }
                } else {
                    $tword = $row['tword']['value'];
                    $tlang = stripNS($row['tlang']['value']);
                }
                $ssense = "";
                if(isset($row['ssense'])){
                    $ssense = $row['ssense']['value'];
                }
                $line = '"'.addslashes(trim($sword)).'","'.addslashes(trim($slang)).'","'.addslashes(trim($spos)).'","'.addslashes(trim($ssense)).'","'.addslashes(trim($tword)).'","'.addslashes(trim($tlang)).'"'.PHP_EOL; 
                fwrite($f, $line);

            }
        }
        $i += $stepping;
        echo "$i done. last $stepping took ".(microtime(true)-$t1)."s".PHP_EOL;
    }
    echo "finished";
    fclose($f);
} else echo "no virtuoso connection".PHP_EOL;
?>
