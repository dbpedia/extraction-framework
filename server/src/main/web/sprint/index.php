
<?php require_once("header.php"); ?>

  <script>
  window.onload = function() {
    var percentages;
    $.get("get_percentage.php", function(result) { 
        percentages = $.parseJSON(result);
        //console.log(percentages);
        
      $(".po").each(function () {
        var id = $(this).attr('id'); 
        var p = Number(percentages[id]["property_occurrences"]);        
	$(this).before("<span class='percent'>"+p+"</span>");       
        $(this).effect("scale", { percent: p*10, direction: 'horizontal' }, 1000);
      });

      $(".to").each(function () {
        var id = $(this).attr('id'); 
        var p = Number(percentages[id]["template_occurrences"]); 
	$(this).before("<span class='percent'>"+p+"</span>");       
        $(this).effect("scale", { percent: p*10, direction: 'horizontal' }, 1000);
      });

      $(".t").each(function () {
        var id = $(this).attr('id'); 
        var p = Number(percentages[id]["templates"]);
	$(this).before("<span class='percent'>"+p+"</span>");       
        $(this).effect("scale", { percent: p*10, direction: 'horizontal' }, 1000);
      });

    }); 

  }
  </script>

<h2>Current Stats</h2>
<?php
require_once("config.php");
echo "<br/><p class='title'>property occurrences</p>";
foreach($languages as $lang) {
  echo "<div id='$lang' class='po'><a href='http://mappings.dbpedia.org/server/statistics/$lang/' class='lang'>$lang</a></div>";
}
echo "<br/><p class='title'>template occurrences</p>";
foreach($languages as $lang) {
  echo "<div id='$lang' class='to'><a href='http://mappings.dbpedia.org/server/statistics/$lang/' class='lang'>$lang</a></div>";
}
echo "<br/><p class='title'>templates</p>";
foreach($languages as $lang) {
  echo "<div id='$lang' class='t'><a href='http://mappings.dbpedia.org/server/statistics/$lang/' class='lang'>$lang</a></div>";
}
?>

<?php require_once("footer.php"); ?>
