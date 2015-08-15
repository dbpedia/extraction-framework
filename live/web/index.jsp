<%@ page import="java.io.*,java.util.*,org.dbpedia.extraction.live.core.LiveOptions,org.dbpedia.extraction.live.statistics.*;" %>
<%! String passw, path, req; %> 
<%! boolean admin = false; %> 
<%! String wikiAPI = LiveOptions.options.get("localApiURL"); %> 
<%! int counter, numItems = Statistics.numItems; %> 
<%
	response.setHeader("Cache-Control","no-cache"); 
	response.setHeader("Pragma","no-cache"); 
	response.setDateHeader ("Expires", -1); 

    path = getServletContext().getRealPath("/") + "/../adminPassword.txt";
    passw = new Scanner(new File(path)).nextLine();
    req = request.getParameter("password");
    if(req != null && req.equals(passw))
        admin = true;
%>
<html>
    <head>
        <meta charset="UTF-8">
        <script src="http://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
        <!-- Latest compiled and minified CSS -->
        <link rel="stylesheet" href="https://bootswatch.com/superhero/bootstrap.min.css">
        <link rel="stylesheet" href="custom.css">
        <!-- Latest compiled and minified JavaScript -->
        <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/js/bootstrap.min.js"></script>
        
        <title>DBpedia Live Administrative Interface</title>
    </head>
    <body>
        <div id="wrapper" style="text-align: center">
            <div style="display: inline-block; width: 1200px; ">
                <h1>DBpedia Live Administrative Interface</h1>
                <h3 style="text-align: left">Live Control</h3>
                <div class="row">
                	<div class="col-md-6">
	                    <div style="padding-top: 8px; padding-bottom: 8px; text-align: left">
	                    	<b>Processors State: </b> <span class="text-success" id="processorState">running</span>
	                    </div>
                    	<div class="btn-group btn-group-justified">
                            <% if (admin) { %>
                                <a id="bt_start" href="#" class="btn btn-success">Start</a>
                                <a id="bt_stop" href="#" class="btn btn-danger">Stop</a>
                            <% } %>
                          <a id="bt_update" href="#" class="btn btn-default">Update</a>
                        </div>
                    </div>
                    <div class="col-md-6">
                        Update Interval: <b id="update_interval">0</b> seconds
                        <input id="update_input" class="form-control ng-pristine ng-valid ng-scope" type="range" value="10" min="1" max="60" step="1" oninput="update_label()">
                    </div>   
                </div>

                <div id="dangerAlert" class="alert alert-dismissible alert-danger" style="display: none;">
				  <button type="button" class="close" onclick="hideElem('dangerAlert')">x</button>
				  <div id = "dangerAlertText"></div>
				</div>

				<div id="successAlert" class="alert alert-dismissible alert-success" style="display: none;">
				  <button type="button" class="close" onclick="hideElem('successAlert')">x</button>
				  <div id = "successAlertText"></div>
				</div>

				<div id="warningAlert" class="alert alert-dismissible alert-warning" style="display: none;">
				  <button type="button" class="close" onclick="hideElem('warningAlert')">x</button>
				  <div id = "warningAlertText"></div>
				</div>

            </div>
            
            <hr/>
            <div style="display: inline-block; width: 1200px; ">
                <div class="row">
                    <div class="col-md-6">
                        <div class="tabbable">
                          	<ul class="nav nav-tabs">
                            	<li class="active"><a href="#tab1" data-toggle="tab">Queued Items</a></li>
                            	<li><a href="#tab2" data-toggle="tab">Processed Items</a></li>
                          	</ul>
                          	<div class="tab-content">
                            	<div class="tab-pane active" id="tab1">
                              		<ul class="list-group">
                              		<%for ( counter = 0; counter < numItems; counter++){
									   out.println("<li class=\"list-group-item\" id=\"q_" + counter + "\"></li>");
									}%>
                              		</ul>
                            	</div>
                            	<div class="tab-pane" id="tab2">
                                  	<ul class="list-group">
                                  	<%for ( counter = 0; counter < numItems; counter++){
									   out.println("<li class=\"list-group-item\" id=\"extr_" + counter + "\"></li>");
									}%>
                                  	</ul>
                            	</div>
                          	</div>
                        </div>
                        <% if (admin) { %>
                            <h4 style="text-align: left">Add Item to Queue</h4>
                            <div class="form-group">
                                <div class="input-group">
                                    <input id="txt_addItem" type="text" class="form-control">
                                    <span class="input-group-btn">
                                        <button id="bt_addItem" class="btn btn-default" type="button">Add</button>
                                    </span>
                                </div>
                            </div>
                        <% } %>
                    </div>
                    <div class="col-md-6">
                        <h3 style="text-align: left">Statistics</h3>
                        <table class="table table-striped table-hover">
                            <thead>
                                <tr>
	                                <th>Title</th>
	                                <th>Value</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                  <td>Time passed since start </td>
                                  <td id="stat_1"></td>
                                </tr>
                                <tr>
                                  <td>Pages processed since last update</td>
                                  <td id="stat_14"></td>
                                </tr>
                                <tr>
                                  <td>Pages processed in the last minute</td>
                                  <td id="stat_3"></td>
                                </tr>
                                <tr>
                                  <td>Pages processed in the last 5 minutes</td>
                                  <td id="stat_4"></td>
                                </tr>
                                <tr>
                                  <td>Pages processed in the last hour</td>
                                  <td id="stat_5"></td>
                                </tr>
                                <tr>
                                  <td>Pages processed in the last day</td>
                                  <td id="stat_6"></td>
                                </tr>
                                <tr>
                                  <td>Pages processed since start</td>
                                  <td id="stat_2"></td>
                                </tr>
                                <tr>
                                  <td>Triples produced since last update</td>
                                  <td id="stat_15"></td>
                                </tr>
                                <tr>
                                  <td>Triples produced in the last minute</td>
                                  <td id="stat_8"></td>
                                </tr>
                                <tr>
                                  <td>Triples produced in the last 5 minutes</td>
                                  <td id="stat_9"></td>
                                </tr>
                                <tr>
                                  <td>Triples produced in the last hour</td>
                                  <td id="stat_10"></td>
                                </tr>
                                <tr>
                                  <td>Triples produced in the last day</td>
                                  <td id="stat_11"></td>
                                </tr>
                                <tr>
                                  <td>Triples produced since start</td>
                                  <td id="stat_7"></td>
                                </tr>
                                <tr>
                                  <td>Average triples per extraction</td>
                                  <td id="stat_12"></td>
                                </tr>
                                <tr>
                                  <td>Items in queue</td>
                                  <td id="stat_13"></td>
                                </tr>
                            </tbody>
                        </table>
                    </div>   
                </div>
            </div>
        </div>
    </body>
    <footer>

        <hr/>
        <p style="text-align: center">&copy Copyright 2015 by DBpedia. All Rights Reserved.</p>
    </footer>
    <script type="text/javascript">
        var timer;
        $(document).ready(function() {
            update_label();
            update();
            $('#bt_update').click(function (){
                update();
            });
            $('#bt_start').click(function (){
                control("start");
            });
            $('#bt_stop').click(function (){
                control("stop");
            });
            $('#bt_addItem').click(addItem);
            
        });

        var ent_all = 0, trp_all = 0;
        function update(){
            $.ajax({
                type: "get",
                url: "stats",
                data: "",
                success: function(msg){
                	console.log(msg);
                    stats = JSON.parse(msg);
                    if(stats != null){
                        $( "#stat_1" ).html(stats.timePassed);
                        $( "#stat_2" ).html(stats.entityAll);
                        $( "#stat_3" ).html(stats.entity1m);
                        $( "#stat_4" ).html(stats.entity5m);
                        $( "#stat_5" ).html(stats.entity1h);
                        $( "#stat_6" ).html(stats.entity1d);
                        $( "#stat_7" ).html(stats.triplesAll);
                        $( "#stat_8" ).html(stats.triples1m);
                        $( "#stat_9" ).html(stats.triples5m);
                        $( "#stat_10" ).html(stats.triples1h);
                        $( "#stat_11" ).html(stats.triples1d);
                        $( "#stat_12" ).html(stats.avrgTriples);
                        $( "#stat_13" ).html(stats.itemsQueued);
                        $( "#stat_14" ).html(stats.entityAll - ent_all);
                        $( "#stat_15" ).html(stats.triplesAll - trp_all);

                        ent_all = stats.entityAll;
                        trp_all = stats.triplesAll;

                        $( "#processorState" ).html(stats.state);
                        switch (stats.state) {
                        	case "stopped":
	                        	$( "#processorState" ).attr("class", "text-danger");
	                        	break;
                        	case "starting":
	                        	$( "#processorState" ).attr("class", "text-warning");
                        		break;
                        	case "running":
	                        	$( "#processorState" ).attr("class", "text-success");
	                        	break;
                        }                        

                        var c = <%= numItems - 1 %>;
                        for (var i in stats.extractedTitles) {
                        	if(c < 0) return; 
                        	var id = "#extr_" + c; 
                        	var elem = stats.extractedTitles[i];
                        	var wiki = "<a target=\"_blank\" href=\"" + elem.wikiURI + "\">Wikipedia</a>";
                        	var dbpedia = "<a target=\"_blank\" href=\"" + elem.dbpediaURI + "\">DBpedia</a>";
						    $( id ).html("" + elem.title + "   (" + wiki + " / " + dbpedia + ")");
							c--;
						}
						if($("#dangerAlert").html().indexOf("Connection Error") != -1)
							hideElem("dangerAlert");
						
						//create array with ids and get the wiki page url
						var arr_pages = [];
						for (var i in stats.queued) {
							arr_pages.push(stats.queued[i].id);
						}
						var pages = arr_pages.join("|");
						var json = getURLfromID(pages);
						c = 0;
                        for (var i in stats.queued) {
                        	if(c > <%= numItems - 1 %>) return; 
                        	var id = "#q_" + c; 
                        	var key = "" + stats.queued[i].id;
                        	var elem = json.query.pages[key];
                        	var wiki = elem.title;
                        	var priorityRaw = stats.queued[i].priority;
                        	var priority = priorityRaw.split(" ")[0];
						    $( id ).html(wiki + createPriorityLabel(priority));
							c++;
						}
                    }
                }, 
                error: function (xhr, ajaxOptions, thrownError) {
                	$("#dangerAlertText").html("<strong>Connection Error:</strong> There was a problem connecting to the server!");
                	$("#dangerAlert").show();
                }
            });
        }

        function createPriorityLabel(priority){
        	var result = "<span style=\"float: right;\" class=\"label ";
        	switch(priority) {
			    case "Live":
			        result += "label-primary\">Live</span>"; break;
			    case "Manual":
			        result += "label-danger\">Manual</span>"; break;
			    case "Mapping":
			        result += "label-success\">Mapping</span>"; break;
			    case "Ontology":
			        result += "label-warning\">Ontology</span>"; break;
			    case "Unmodified":
			        result += "label-info\">Unmodified</span>"; break;
			    default:
			        result += "label-default\">" + priority + "</span>"; break;
			}
			return result;
        }

        function control(ty){
        	if(ty == "start"){
        		$("#warningAlertText").html("<strong>Please Wait!</strong> DBpedia Live is starting.");
                $("#warningAlert").show();
        	}
        	var formData = {type:ty, password:"<%= req %>"}
        	$.ajax({
                type: "get",
                url: "control",
                data: formData,
                success: function(msg){
                	json = JSON.parse(msg);
                	if(json.result == true){
                		$("#successAlertText").html("<strong>Success!</strong> " + json.message);
                		$("#successAlert").show();
                	}else{
						$("#dangerAlertText").html("<strong>Error!</strong> " + json.message);
                		$("#dangerAlert").show();
                	}
                	$("#warningAlert").hide();
                }, 
                error: function (xhr, ajaxOptions, thrownError) {
                	$("#dangerAlertText").html("<strong>Connection Error:</strong> There was a problem connecting to the server!");
                	$("#dangerAlert").show();
                }
            });
        }

        function addItem(){
        	var title = $("#txt_addItem").val();
        	var itemID = getWikiItemID(title);
        	console.log(itemID);
        	if(itemID == -1){
	        	$("#dangerAlertText").html("<strong>Error: </strong> There is no page with the title \"" + title + "\"");
	            $("#dangerAlert").show();
            }else{
            	// request the server to add a new item to the queue
            	var formData = {item: itemID, password:"<%= req %>"}
	        	$.ajax({
			        type: "GET",
			        url: "additem",
			        data: formData,
			        success: function (msg) {
			            json = JSON.parse(msg);
	                	if(json.result == true){
	                		$("#successAlertText").html("<strong>Success!</strong> " + json.message);
	                		$("#successAlert").show();
	                	}else{
							$("#dangerAlertText").html("<strong>Error!</strong> " + json.message);
	                		$("#dangerAlert").show();
	                	}
			        },
			        error: function (errorMessage) {}
		    	});
            }
        }

        function getURLfromID(pages){
        	var data = { 'pageids': pages};
			var res;
        	$.ajax({
		        type: "GET",
		        url: "<%= wikiAPI %>?action=query&format=json&prop=info&inprop=url&" + EncodeQueryData(data),
		        async: false,
		        dataType: "json",
		        success: function (data) {
		            res = JSON.parse(JSON.stringify(data));
		        },
		        error: function (errorMessage) {}
	    	});
	    	return res;
        }

        function getWikiItemID(item){
			var data = { 'titles': item};
			var res = "-1";
        	$.ajax({
		        type: "GET",
		        url: "<%= wikiAPI %>?action=query&format=json&" + EncodeQueryData(data),
		        async: false,
		        dataType: "json",
		        success: function (data) {
		            var json = JSON.parse(JSON.stringify(data));
		            res = Object.keys(json.query.pages)[0];
		        },
		        error: function (errorMessage) {}
	    	});
	    	return res;
        }

        function update_label(){
            $("#update_interval").html($("#update_input").val());
            clearInterval(timer);
            timer = setInterval(update, $("#update_input").val() * 1000);
        }

        function EncodeQueryData(data){
		   var ret = [];
		   for (var d in data)
			  ret.push(encodeURIComponent(d) + "=" + encodeURIComponent(data[d]));
		   return ret.join("&");
		}
        function hideElem(elem){
        	$("#" + elem).hide();
        }
    </script>
