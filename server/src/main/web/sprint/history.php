<?php

$directory = "data";

echo(getDirectoryList($directory));


  function getDirectoryList ($base) 
  {

    // create an array to hold directory list
    $results = array();

    // create a handler for the directory
    $dHandler = opendir($base);

    // open directory and walk through the filenames
    while ($dir = readdir($dHandler)) {      
      if (preg_match("/^201/",$dir)) { // starts with 201? (2011, 2012, ...)
        $fHandler = opendir("$base/$dir");
        
        $langResults = array();
        while( $file = readdir($fHandler)) {
	   if ($file != "." && $file != "..") {		
              $language = substr($file,0,2);
              $stats =  trim(file_get_contents("$base/$dir/$file"));
	      $langResults[] = "\n\t \"$language\":  $stats ";
	   }	   
        }
        $results[] = "\"$dir\": { " . implode(",",$langResults) . "  \n}";
        closedir($fHandler);
      }
    }

    // tidy up: close the handler
    closedir($dHandler);

    $strResults = "{\n" . implode(", ",$results) . "\n}";
    // done!
    return $strResults;

  }

?>
