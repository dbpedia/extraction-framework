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
                          <a href="#" class="btn btn-default">Update</a>
                        </div>
                    </div>
                    <div class="col-md-6">
                        Update Interval: <b id="update_interval">0</b> seconds
                        <input id="update_input" class="form-control ng-pristine ng-valid ng-scope" type="range" min="0" max="60" step="1" oninput="update_label()">
                    </div>   
                </div>
            </div>
            <hr/>
            <div style="display: inline-block; width: 1200px; ">
                <div class="row">
                    <div class="col-md-6">
                        <h3 style="text-align: left">Queued Items</h3>
                            <ul class="list-group">
                                <li class="list-group-item">Cras justo odio</li>
                                <li class="list-group-item">Dapibus ac facilisis in </li>
                                <li class="list-group-item"> Morbi leo risus</li>
                                <li class="list-group-item">Cras justo odio</li>
                                <li class="list-group-item">Dapibus ac facilisis in </li>
                                <li class="list-group-item"> Morbi leo risus</li>
                                <li class="list-group-item">Cras justo odio</li>
                                <li class="list-group-item">Dapibus ac facilisis in </li>
                                <li class="list-group-item"> Morbi leo risus</li>
                                <li class="list-group-item">Cras justo odio</li>
                                <li class="list-group-item">Dapibus ac facilisis in </li>
                                <li class="list-group-item"> Morbi leo risus</li>
                                <li class="list-group-item">Cras justo odio</li>
                                <li class="list-group-item">Dapibus ac facilisis in </li>
                                <li class="list-group-item"> Morbi leo risus</li>
                                <li class="list-group-item">Cras justo odio</li>
                                <li class="list-group-item">Dapibus ac facilisis in </li>
                            </ul>
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
                        <table class="table table-striped table-hover ">
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
                                  <td>Entities updated since start</td>
                                  <td id="stat_2"></td>
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
                                  <td>Triples produced since start</td>
                                  <td id="stat_7"></td>
                                </tr>
                                <tr>
                                  <td>Triples produced since start</td>
                                  <td id="stat_8"></td>
                                </tr>
                                <tr>
                                  <td>Triples produced in the last minute</td>
                                  <td id="stat_9"></td>
                                </tr>
                                <tr>
                                  <td>Triples produced in the last 5 minutes</td>
                                  <td id="stat_10"></td>
                                </tr>
                                <tr>
                                  <td>Triples produced in the last hour</td>
                                  <td id="stat_11"></td>
                                </tr>
                                <tr>
                                  <td>Triples produced in the last day</td>
                                  <td id="stat_12"></td>
                                </tr>
                                <tr>
                                  <td>Average triples per extractions</td>
                                  <td id="stat_13"></td>
                                </tr>
                                <tr>
                                  <td>Items in queue</td>
                                  <td id="stat_14"></td>
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
        $(document).ready(function() {
            $('#call').click(function (){
                $.ajax({
                    type: "get",
                    url: "hello", //this is my servlet
                    data: "",
                    success: function(msg){
                        console.log(msg);
                    }
                });
            });
            update_label();
        });

        function update_label(){
            $("#update_interval").html($("#update_input").val());
        }

    </script>
