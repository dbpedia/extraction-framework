<%@ page import="java.io.*,java.util.*" %>
<%! String passw, path, req; %> 
<%! boolean admin = false; %> 
<%
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
                    <div class="col-md-6" style="padding-top: 37px">
                        <div class="btn-group btn-group-justified">
                            <% if (admin) { %>
                                <a href="#" class="btn btn-success">Start</a>
                                <a href="#" class="btn btn-danger">Stop</a>
                            <% } %>
                          <a id="bt_update" href="#" class="btn btn-default">Update</a>
                        </div>
                    </div>
                    <div class="col-md-6">
                        Update Interval: <b id="update_interval">0</b> seconds
                        <input id="update_input" class="form-control ng-pristine ng-valid ng-scope" type="range" value="10" min="1" max="60" step="1" oninput="update_label()">
                    </div>   
                </div>

                <div id="connErrorAlert" class="alert alert-dismissible alert-danger" style="display: none;">
				  <button type="button" class="close" onclick="hideElem('connErrorAlert')">x</button>
				  <strong>Connection Error:</strong> There was a problem connecting to the server!
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
	                              		<li class="list-group-item" id="q_0"></li>
										<li class="list-group-item" id="q_1"></li>
										<li class="list-group-item" id="q_2"></li>
										<li class="list-group-item" id="q_3"></li>
										<li class="list-group-item" id="q_4"></li>
										<li class="list-group-item" id="q_5"></li>
										<li class="list-group-item" id="q_6"></li>
										<li class="list-group-item" id="q_7"></li>
										<li class="list-group-item" id="q_8"></li>
										<li class="list-group-item" id="q_9"></li>
										<li class="list-group-item" id="q_10"></li>
										<li class="list-group-item" id="q_11"></li>
										<li class="list-group-item" id="q_12"></li>
										<li class="list-group-item" id="q_13"></li>
										<li class="list-group-item" id="q_14"></li>
										<li class="list-group-item" id="q_15"></li>
										<li class="list-group-item" id="q_16"></li>
                              		</ul>
                            	</div>
                            	<div class="tab-pane" id="tab2">
                                  	<ul class="list-group">
                                  		<li class="list-group-item" id="extr_0"></li>
										<li class="list-group-item" id="extr_1"></li>
										<li class="list-group-item" id="extr_2"></li>
										<li class="list-group-item" id="extr_3"></li>
										<li class="list-group-item" id="extr_4"></li>
										<li class="list-group-item" id="extr_5"></li>
										<li class="list-group-item" id="extr_6"></li>
										<li class="list-group-item" id="extr_7"></li>
										<li class="list-group-item" id="extr_8"></li>
										<li class="list-group-item" id="extr_9"></li>
										<li class="list-group-item" id="extr_10"></li>
										<li class="list-group-item" id="extr_11"></li>
										<li class="list-group-item" id="extr_12"></li>
										<li class="list-group-item" id="extr_13"></li>
										<li class="list-group-item" id="extr_14"></li>
										<li class="list-group-item" id="extr_15"></li>
										<li class="list-group-item" id="extr_16"></li>
                                  	</ul>
                            	</div>
                          	</div>
                        </div>
                        <% if (admin) { %>
                            <h4 style="text-align: left">Add Item to Queue</h4>
                            <div class="form-group">
                                <div class="input-group">
                                    <input type="text" class="form-control">
                                    <span class="input-group-btn">
                                        <button class="btn btn-default" type="button">Add</button>
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
	                                  <td>Entities updated in the last minute</td>
	                                  <td id="stat_3"></td>
	                                </tr>
	                                <tr>
	                                  <td>Entities updated in the last 5 minutes</td>
	                                  <td id="stat_4"></td>
	                                </tr>
	                                <tr>
	                                  <td>Entities updated in the last hour</td>
	                                  <td id="stat_5"></td>
	                                </tr>
	                                <tr>
	                                  <td>Entities updated in the last day</td>
	                                  <td id="stat_6"></td>
	                                </tr>
	                                <tr>
	                                  <td>Entities updated since start</td>
	                                  <td id="stat_2"></td>
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
        });

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

                            var c = 16;
                            for (var i in stats.extractedTitles) {
                            	if(c < 0) return; 
                            	var id = "#extr_" + c; 
                            	var elem = stats.extractedTitles[i];
                            	var wiki = "<a target=\"_blank\" href=\"" + elem.wikiURI + "\">Wikipedia</a>";
							    $( id ).html("" + elem.title + "   (" + wiki + ")");
								c--;
							}
							hideElem("connErrorAlert");
							console.log(stats.queued);
							c = 0;
                            for (var i in stats.queued) {
                            	if(c > 16) return; 
                            	var id = "#q_" + c; 
                            	var elem = stats.queued[i];
							    $( id ).html(elem);
								c++;
							}
                        }
                    }, 
                    error: function (xhr, ajaxOptions, thrownError) {
                    	$("#connErrorAlert").show();
                    }
                });
        }

        function update_label(){
            $("#update_interval").html($("#update_input").val());
            clearInterval(timer);
            timer = setInterval(update, $("#update_input").val() * 1000);
        }

        function hideElem(elem){
        	$("#" + elem).hide();
        }

    </script>
