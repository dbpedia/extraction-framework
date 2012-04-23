<?php require_once("header.php"); ?>

    <script type="text/javascript" src="https://www.google.com/jsapi"></script>
 
   <script type="text/javascript">
  
Object.size = function(obj) {
    var size = 0, key;
    for (key in obj) {
        if (obj.hasOwnProperty(key)) size++;
    }
    return size;
};

      google.load("visualization", "1", {packages:["corechart"]});
      google.setOnLoadCallback(getData);

      function getData() {
	$.get("history.php", drawCharts);
      }

      function drawCharts(json) {
         //console.log(json);
         var stats = $.parseJSON(json);
  	 var languages = getLanguages(stats).sort();
         drawChart(stats,"property_occurrences","Property Occurrences",languages)
         drawChart(stats,"template_occurrences","Template Occurrences",languages)
         drawChart(stats,"templates","Templates", languages)
      }

      function getLanguages(stats) {
        var first = "";
        for (e in stats) {
	   first = e; 
	   break;
        }
        var languages = new Array();	
        $.each(stats[first], function(l, numbers) {                        
             languages.push(l);
        })
        console.log(languages);
        return languages;
      }  

      function drawChart(stats,field,title,languages) {

        var data = new google.visualization.DataTable();
	// add the date in the x axis
        data.addColumn('string', 'Date');
	
        // now one series per language
        $.each(languages, function(l) { data.addColumn('number', languages[l]); });

        // set number of rows (one per date)
        data.addRows(Object.size(stats)); 
        
	var dIndex = 0;
        $.each(stats, function(d,langStats) {
          var lIndex = 1;
          var formatted = d.substring(0,4) + "-" + d.substring(4,6) + "-" + d.substring(6);
          data.setValue(dIndex, 0, formatted);          
	  $.each(languages, function(l) {
            var numbers = langStats[languages[l]];                        
            var value = parseFloat(numbers[field]);
            //console.log(dIndex,lIndex,value);
	    data.setValue(dIndex,lIndex,value);
            lIndex++; 
          })
          dIndex++;
	});

        var chart = new google.visualization.LineChart(document.getElementById('chart_div_'+field));
        chart.draw(data, {width: 1024, height: 768, title: 'Language Race Progression ('+title+')'});
      }
    </script>
  </head>

  <body>
    <div id="chart_div_property_occurrences"></div>
    <div id="chart_div_template_occurrences"></div>
    <div id="chart_div_templates"></div>
  </body>
</html>

<?php require_once("footer.php"); ?>
