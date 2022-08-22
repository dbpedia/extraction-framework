/**
 * Created by Chile on 3/9/2016.
 */

var langs = null;
var tableIds = {};
var nShort = null;
var oShort = null;
var listData = {};
var typeToId = {};
var div = d3.select("body")
    .append("div")                          // declare the tooltip div
    .attr("class", "tooltip")              // apply the 'tooltip' class
    .style("opacity", 0);                  // set the opacity to nil

function init(generalStatsUrl, shortName, selectedLangs, tableids, optionalSecondUrl, optionalShort)
{
    tableIds = tableids;
    nShort = shortName;
    oShort = optionalShort;
    for(var k in tableIds)
    {
        typeToId[tableIds[k]] = k;
        if(tableIds[k] == "compare")
            tableIds[k] = {id : k, name: shortName, table: null, type : tableIds[k], columns : [], rows: null, data: null, optData : null};
        else
            tableIds[k] = {id : k, name: shortName, table: null, type : tableIds[k], columns : [], rows: null, data: null};
    }
    if(optionalSecondUrl)
    {
        sendRequest(optionalSecondUrl, "GET", null, true, optionalStatsLoaded, function () {});
    }
    sendRequest(generalStatsUrl, "GET", null, true, generalStatsLoaded, function () {});
    langs = selectedLangs;
}

function initList(url, tableid, type, selectedLangs, rowselection)
{
    langs = selectedLangs.slice();
    selectedLangs.unshift(type);
    tableIds[tableid] = {id : tableid, name: type, table: null, type : type, columns : selectedLangs, rows: rowselection, data : null, optData : null};
    typeToId[type] = tableid;
    if(type == "Properties")
        sendRequest(url, "GET", null, true, propertiesLoaded, function () {});
    if(type == "Types")
        sendRequest(url, "GET", null, true, typesLoaded, function () {});
}

function propertiesLoaded(e)
{
    if(e.target.status < 205)
    {
        var t = tableIds[typeToId["Properties"]];
        t.data = JSON.parse(e.target.responseText);
        reDrawTable(typeToId["Properties"], t.columns, langs)
    }
}

function typesLoaded(e)
{
    if(e.target.status < 205)
    {
        var t = tableIds[typeToId["Types"]];
        t.data = JSON.parse(e.target.responseText);
        reDrawTable(typeToId["Types"], t.columns, langs)
    }
}

function generalStatsLoaded(e)
{
    if(e.target.status < 205)
    {
        var t = tableIds[typeToId["general"]];
        t.data = JSON.parse(e.target.responseText);
        t.columns = Object.keys(t.data["en"]);
        if(typeToId["compare"] != null && tableIds[typeToId["compare"]] != null)
        {
            tableIds[typeToId["compare"]].data = t.data;
            if(tableIds[typeToId["compare"]].optData != null)
                reDrawTable(typeToId["compare"], t.columns, langs);
        }
        if(t.optData === undefined)
            reDrawTable(typeToId["general"], t.columns, langs);
        else if(t.optData != null)
            reDrawTable(typeToId["general"], t.columns, langs);
    }
}

function optionalStatsLoaded(e)
{
    if(e.target.status < 205)
    {
        var t = tableIds[typeToId["compare"]];
        t.optData = JSON.parse(e.target.responseText);
        t.columns = Object.keys(t.optData["en"]);
        if(t.data != null)
            reDrawTable(typeToId["compare"], t.columns , langs);
    }
}

function tabulateList(tableId, columns, typeOfData) {
    var table = d3.select("#" + tableId + "Div")
        .append("table")
        .attr("border", "0")
        .attr("id", tableId)
        .attr("class", "cell-border")
        .attr("cellspacing", "0"),
        thead = table.append("thead"),
        tbody = table.append("tbody")
            .attr("overflow-y", "auto")
            .attr("overflow-x", "hidden"),
        tfoot = table.append("tfoot");

    thead.append("tr")
        .attr("style", "background: white;")
        .selectAll("th")
        .data(columns)
        .enter()
        .append("th")
        .attr("style", "color: blue !important;")
        .attr("bgcolor","#FFFFFF")
        .html(function(column) {
            if(column != typeOfData)
                return "<strong><a href='#zzz' title='" + getLanguageFromWikicode(column) + "'>" + column + "</a></strong>";
            else
                return "<strong>" + column + "</strong>"
        });

    var rows = tbody.selectAll("tr")
        .data(tableIds[tableId].rows)
        .enter()
        .append("tr");

    var cells = rows.selectAll("td")
        .data(function(prop) {
            return columns.map(function(column) {
                if(column == typeOfData)
                    return { column: column, value: prop };
                else
                    return { column: column, value : numberWithCommas(tableIds[tableId].data[column][prop.replace("http://","")])};
            });
        })
        .enter()
        .append("td")
        .html(function(d) {
            var ret = null;
            if(d.column == typeOfData)
                ret = "<strong><a href='http://" + d.value.replace("http://","") + "'>" + d.value.substr(d.value.lastIndexOf("/")+1) + "</a></strong>";
            else
                ret = d.value
            return ret;
        });
}

function tabulate(tableId, columns) {

    var table = d3.select("#" + tableId + "Div")
        .append("table")
        .attr("border", "0")
        .attr("id", tableId)
        .attr("class", "cell-border")
        .attr("cellspacing", "0"),
        thead = table.append("thead"),
        tbody = table.append("tbody")
            .attr("overflow-y", "auto")
            .attr("overflow-x", "hidden"),
        tfoot = table.append("tfoot");

    //append the header row
    var colSpanCols = [];
    colSpanCols.push("Language");

    var th1 = thead.append("tr");
        th1.attr("style", "background: white;");
        th1.append("th")
            .text("Language")
            .attr("style", "color: blue !important;");

    if(tableIds[tableId].type == "compare") {
        var th2 = thead.append("tr");
        th2.attr("style", "background: white;");
        th2.append("th");

        for (var i = 0; i < columns.length; i = i + 1) {
            colSpanCols.push(columns[i] + "-prevV");
            colSpanCols.push(columns[i]);
            colSpanCols.push(columns[i] + "-%");

            th2.append("th").text(oShort).attr("style", "color: blue !important;");
            th2.append("th").text(nShort).attr("style", "color: blue !important;");
            th2.append("th").text("%").attr("style", "color: blue !important;");
            th1.append("th")
                .attr("colspan", "3")
                .text(columns[i])
                .attr("style", "color: blue !important;");
        }
    }
    else if(tableIds[tableId].type == "general")
    {
        for (var i = 0; i < columns.length; i = i + 1) {
            colSpanCols.push(columns[i]);
            th1.append("th")
                .text(columns[i])
                .attr("style", "color: blue !important;");
        }
    }

    // create a row for each object in the data
    var rows = tbody.selectAll("tr")
        .data(langs)
        .enter()
        .append("tr");

    var cells = rows.selectAll("td")
        .data(function(lang) {
            return colSpanCols.map(function(column) {
                if(column == "Language")
                    return { column: column, value:lang };
                else if(column.endsWith("-prevV"))
                    return { column: column, value: numberWithCommas(noNegative(tableIds[tableId].optData[lang][column.substr(0, column.length-6)]))};
                else if(column.endsWith("-%"))
                    return { column: column, value: numberWithCommas(calcPercDiff(tableIds[tableId].optData[lang][column.substr(0, column.length-2)], tableIds[tableId].data[lang][column.substr(0, column.length-2)], 1))};
                else
                    return { column: column, value: numberWithCommas(noNegative(tableIds[tableId].data[lang][column]))};
            });
        })
        .enter()
        .append("td")
        .html(function(d) {
            if(d.column == "Language")
                return "<strong><a href='#zzz' title='" + getLanguageFromWikicode(d.value) + "'>" + d.value + "</a></strong>";
            else
                return d.value
        });
}

function redrawTables(columns)
{
    for(var i in Object.keys(tableIds))
        reDrawTable(Object.keys(tableIds)[i], columns, langs)
}

function reDrawTable(tableId, columns, s)
{
    var allLangs = Object.keys(tableIds[tableId].data).sort();
    if(columns === undefined || columns == null)
    {
        columns = tableIds[tableId].columns;
        if(tableIds[tableId].type == "Properties" || tableIds[tableId].type == "Types")
        {
            columns = $('#' + tableId + 'langselect').val();
            columns.unshift(tableIds[tableId].columns[0]);
        }
    }
    if(s === undefined || s == null)
        s = $('#' + tableId + 'langselect').val();
    if(s == "all")
        s = allLangs;
    selected = s;
    langs = s;

    if(tableIds[tableId]["table"] != null) {
        tableIds[tableId]["table"].destroy();
        $('#' + tableId).remove();
    }

    if(tableIds[tableId].type == "general" || tableIds[tableId].type == "compare")
        tabulate(tableId, columns);
    else
        tabulateList(tableId,columns,tableIds[tableId].type);  //is a list type

    $('#' + tableId).on('draw.dt', function(){
        var list = document.createElement('select');
        list.setAttribute("id", tableId + "langselect");
        list.setAttribute("multiple", "");
        list.setAttribute("style", "width:400px");
        for (var i = 0; i < allLangs.length; i++) {
            var opt = document.createElement('option');
            opt.innerHTML =  allLangs[i];
            opt.value = allLangs[i];
            opt.title = getLanguageFromWikicode(allLangs[i]) + " (" + allLangs[i] + ")";
            if(langs.indexOf(allLangs[i].trim()) >= 0)
                opt.setAttribute("selected", "");
            list.appendChild(opt);
        }
        var but = document.createElement('input');
        but.setAttribute("id", tableId + "langSelectButton");
        but.setAttribute("type", "button");
        but.setAttribute("value", "show languages");
        but.setAttribute("onclick", "reDrawTable('" + tableId + "')");
        but.setAttribute("style", "margin: 0px 4px; border: none; box-shadow: none; padding: 3px 6px; text-transform: initial;");
        var all = document.createElement('input');
        all.setAttribute("id", tableId + "showAllButton");
        all.setAttribute("type", "button");
        all.setAttribute("value", "show all");
        all.setAttribute("onclick", "reDrawTable('" + tableId + "', null, 'all')");
        all.setAttribute("style", "margin: 0px 4px; border: none; box-shadow: none; padding: 3px 6px; text-transform: initial;");
        var div = document.createElement('div');
        div.setAttribute("style", "float:left");
        div.appendChild(list);
        div.appendChild(but);
        div.appendChild(all);

        $('#' + tableId + 'languagediv').html(div);

        $('#' + tableId + 'langselect').chosen();
    });


    if(tableIds[tableId].type == "compare")
    {
        tableIds[tableId]["table"] = $('#' + tableId).DataTable(getTableConfig(tableId, cellCallback));
        $('#' + tableId + "_wrapper").css("width", "1800px");
    }
    else
        tableIds[tableId]["table"] = $('#' + tableId).DataTable(getTableConfig(tableId));
}

var cellCallback = function (td, cellData, rowData, row, col){
    if ( Number.isInteger(parseInt(cellData)) && parseInt(cellData) > 0 ) {
        $(td).css('color', 'green')
    }
};

function getTableConfig(tbleId, createdCell) {
    var zw = null;
    if(createdCell)
        zw =[3,6,9,12,15,18,21];
    else
        zw = [1];
    return {
        "sDom": 'l<"#' + tbleId + 'languagediv">t',
        "paging": false,
        "bAutoWidth": false,
        "createdCell": createdCell,
        "columnDefs": [
            { className: "dt-left", targets: [0] },
            { createdCell: createdCell, targets: zw },
            { className: "dt-right", targets: '_all' }
        ]
    };
}