/**
 * Created by Chile on 1/12/2016.
 */
var ids = null;
var datasets = {};
var datasetList = null;
var initCallback = null;
var callbackParam = null;
var initLangs = ["en", "fr", "wikidata", "commons"];

var tableConfig = {
    "sDom": 'l<"#languagediv">ft',
    paging: false,
    "columnDefs": [
        { "orderable": false, "targets": '_all' },
        { "orderable": true, "targets": [0] }
    ]
};
var selected = null;
var table = null;

function init(catalogUrl, callback, params)
{
    sendRequest(catalogUrl, "GET", null, true, catalogLoaded, function () {});
    initCallback = callback;
    callbackParam = params;
}

function tabulate(columns) {

// Define 'div' for tooltips
    var div = d3.select("body")
        .append("div")  // declare the tooltip div
        .attr("class", "tooltip")              // apply the 'tooltip' class
        .style("opacity", 0);                  // set the opacity to nil

    var table = d3.select("#canvas")
            .append("table")
            .attr("border","0")
            .attr("id","table")
            .attr("class","display dataTable")
            .attr("width","100%")
            .attr("cellspacing","0"),
        thead = table.append("thead"),
        tbody = table.append("tbody")
            .attr("overflow-y", "auto")
            .attr("overflow-x", "hidden"),
        tfoot = table.append("tfoot");

    var colSpanCols = [];
    colSpanCols.push(columns[0]);

    //append the header row
    var th2 = thead.append("tr")
        .attr("style", "background: white;");
    th2.append("th");
    var th1 = thead.append("tr")
        .attr("style", "background: white;");
    th1.append("th").attr("width", 250).attr("style", "color: #004164;").text("Dataset");

    for(var i = 1; i < columns.length; i= i+1)
    {
        colSpanCols.push(columns[i]);
        colSpanCols.push("-");

        var langLink = getRootDataset(columns[i])["dc:language"]["@id"]

        th2.append("th");
        th2.append("th");
        th1.append("th")
            .attr("colspan","2")
            .html("<a href=\"" + langLink + "\" title='" + getLanguageFromWikicode(columns[i]) + "'>" + columns[i] + "</a>");
    }

    // create a row for each object in the data
    var rows = tbody.selectAll("tr")
        .data(datasetList.filter(function(item){
            if(item.indexOf("en uris") >= 0)
                return false;
            else
                return true;}))
        .enter()
        .append("tr");

    // create a cell in each row for each columnstatObj[lang]
    var canCell = null;
    var cells = rows.selectAll("td")
        .data(function(row) {
            return colSpanCols.map(function(column) {
                return {column: column, row: row};
            });
        })
        .enter()
        .append("td")
        .attr("align", function(d, e){
            if(e % 2 == 0)
                return "left";
            else
                return "right";
        })
        .html(function(d) {
            if(d.row.indexOf("en uris") == -1) {
                if (d.column == "dataset")
                {
                    canCell = null;
                    var url = "http://wiki.dbpedia.org/services-resources/documentation/datasets#" + d.row.replace(/\s/g,'');
                    return "<strong><a href=\"" + url + "\">" + d.row + "</a></strong>";
                }
                if (d.column == "-") {
                    if(canCell != null)
                        return fillTd(canCell.lang, canCell.set, true);
                    else
                        return "<p> </p>";
                }
                canCell = {lang: d.column, set: d.row + " en uris"};
                return fillTd(d.column, d.row, false);
            }
        })
        .on("mouseout", mapMouseOut)
        .on("mouseover", mapMouseOver);

    function fillTd(column, row, isCononicalized)
    {
        if(datasets[column][row] !== undefined) {
            var html = "";
            for (var i in datasets[column][row]["dcat:distribution"]) {
                var id = datasets[column][row]["dcat:distribution"][i];
                if(i != "@id")
                    id = datasets[column][row]["dcat:distribution"][i]["@id"];
                if(datasets[column][id] !== undefined) {
                    html += addDlLinks(column, row, id, isCononicalized);
                }
            }
            return html;
        }
    }

    function addDlLinks(column, row, id, isCononicalized)
    {
        var obj = datasets[column][id];
        var dll = obj["dcat:downloadURL"]["@id"];
        var ret = "<small><a href=\"" + dll + "\" ";
        if(isCononicalized)
            ret += "title=\"Canonicalized&nbsp;version&nbsp;of&nbsp;" + row.replace(" en uris", "");
        else
            ret += "title=\"Localized&nbsp;version&nbsp;of&nbsp;" + row.replace(" en uris", "");

        if(datasets[column][row]["void:triples"])
            ret += ";&nbsp;Triples:&nbsp;" + readableNumber(datasets[column][row]["void:triples"]["@value"], 1000);
        if(obj["dcat:byteSize"])
            ret += "; File&nbsp;size:&nbsp;" + readableNumber(obj["dcat:byteSize"]["@value"], 1024);
        if(obj["dataid:uncompressed"])
            ret += "; File&nbsp;size&nbsp;(unpacked):&nbsp;" + readableNumber(obj["dataid:uncompressed"]["@value"], 1024);

        ret += "\">" + getSerializationExtension(dll).substr(1);
        if(isCononicalized)
            ret += "*";
        ret += "</a>&nbsp;";

        if(obj["dataid:preview"])
            ret += "<a href=\"" + obj["dataid:preview"]["@id"] + "\" title=\"preview&nbsp;file\">?</a>";

        ret += "</small><br/>";
        return ret;
    }

    function getEnUrisId(id, column, row)
    {
        if(datasets[column][row + " en uris"] === undefined)
            return null;
        var extension = getSerializationExtension(id);
        for(var i in datasets[column][row + " en uris"]["dcat:distribution"]) {
            var ret = datasets[column][row + " en uris"]["dcat:distribution"][i]["@id"];
            if (ret.indexOf(extension + ".bz2") >= 0 || ret.indexOf(extension + ".gz") >= 0)
                return ret;
        }
    }

    function mapMouseOver(d, eventCol){
        if(eventCol ==0) {
            div.transition()
                .duration(500)
                .style("opacity", 0);
            div.transition()
                .duration(200)
                .style("opacity", .9);
            div.html("<p>" + getDatasetRow(d)["dc:description"]["@value"] + "</p><br/>")
                .style("left", (d3.event.pageX) + "px")
                .style("top", (d3.event.pageY) + "px")
                .style("display", "initial");
        }
    }

    function mapMouseOut(d, eventCol){
        if(eventCol ==0) {
            div.transition()
                .duration(500)
                .style("opacity", 0);
            div.html("<p>" + getDatasetRow(d)["dc:description"]["@value"] + "</p><br/>")
                .style("display", "none");
        }
    }

    return table;
}

function getDatasetRow(data)
{
    for(var i in initLangs)
        if(datasets[initLangs[i]][data.row] !== undefined)
        {
            return datasets[initLangs[i]][data.row];
        }
}

function getLanguageJson(langs, onload)
{
    for(var i in langs)
    {
        if(Object.keys(datasets).indexOf(langs[i]) == -1)
            sendRequest(ids[langs[i]], "GET", null, true, onload, function () {});
    }
}

function langLoaded(e)
{
    if(e.target.status < 205)
    {
        var json = JSON.parse(e.target.responseText)
        datasets[getLangFromUri(e.target["responseURL"])] = getDatasetsAndDistributionsById(json);
        var zw = Object.keys(datasets);
        if(zw.length == initLangs.length)
        {
            //init done
            datasetList = getDatasetList();
            reDrawTable(callbackParam);
            initCallback();
        }
    }
}

function catalogLoaded(e)
{
    if(e.target.status < 205)
    {
        var json = JSON.parse(e.target.responseText)
        ids = getDataIDsFromCatalog(json);
        getLanguageJson(initLangs, langLoaded);
    }
}

function getLanguages(){
    if(ids == null)
        return;
    return Object.keys(ids).sort();
}

function getSerializationExtension(file)
{
    if(file.endsWith(".gz"))
        file = file.replace(".gz", "");
    if(file.endsWith(".bz2"))
        file = file.replace(".bz2", "");
    return file.substring(file.lastIndexOf("."));
}

function getDataIDsFromCatalog(catalog)
{
    var ret = {};
    for(var i = 0; i < catalog["@graph"].length; i++)
    {
        if(catalog["@graph"][i]["dcat:record"] != null)
        {
            for(var j = 0; j < catalog["@graph"][i]["dcat:record"].length; j++)
            {
                var id = catalog["@graph"][i]["dcat:record"][j]["@id"];

                ret[getLangFromUri(id)] = catalog["@graph"][i]["dcat:record"][j]["@id"];
            }
        }
    }
    return ret;
}

function getLangFromUri(id)
{
    return id.substring(id.lastIndexOf("dataid_")+7, id.indexOf(".", id.lastIndexOf("dataid_")+7));
}

function getDatasetList(){
    var ret = [];
    for(var i in initLangs)
        ret = ret.concat(loadDatasetListOfLang(initLangs[i]));
    return ret.filter(function(item, pos, self) {
        return self.indexOf(item) == pos;
    });
}

function loadDatasetListOfLang(lang)
{
    var ret = [];
    for(var key in datasets[lang]) {
        if (!key.startsWith("http://") && !key.startsWith("dataid:")) {
            if(key.trim().length > 0 && key.toLowerCase().indexOf("root dataset") == -1)
                ret.push(key);
        }
    }
    return ret;
}

function getDatasetsAndDistributionsById(id){
    var ret = {};
    for(var i = 0; i < id["@graph"].length; i++) {
        if (id["@graph"][i]["@type"] == "dataid:Dataset") {
            var title = id["@graph"][i]["dc:title"]["@value"];
            if(title !== undefined && title.trim().length > 0)
                ret[title] = id["@graph"][i];
        }
        if (id["@graph"][i]["@type"] == "dataid:SingleFile") {
            ret[id["@graph"][i]["@id"]] = id["@graph"][i];
        }
        if (id["@graph"][i]["@type"] == "dataid:MediaType") {
            ret[id["@graph"][i]["@id"]] = id["@graph"][i];
        }
    }
    return ret;
}

function getLanguageId(id) {
    if(inp == null)
        inp = JSON.parse(document.getElementById( 'data' ).textContent);
    var ret = [];
    for(var i = 0; i < inp["@graph"].length; i++)
    {
        if(inp["@graph"][i]["level"] != null)
        {
            ret.push(inp["@graph"][i]);
        }
    }
    return ret;
}

function langsAllLoaded(e)
{
    langLoaded(e);
    if(subArrayTest(selected, Object.keys(datasets)))
        reDrawTable(selected);
}

function getRootDataset(lang)
{
    for(var name in datasets[lang])
        if(name.indexOf("root dataset") >= 0)
            return datasets[lang][name]
}

function insertOntologyTable()
{
    var ontoTable = $('#ontologytable');
    var owl = getRootDataset("en")["void:vocabulary"];
    var nt = null;

    if(owl.constructor === Array)
        owl = owl[0]["@id"];
    else
        owl = owl["@id"];
    if(owl.endsWith(".owl"))
        nt = owl.replace(".owl", ".nt")
    if(owl.endsWith(".nt"))
    {
        nt = owl
        owl = owl.replace(".nt", ".owl")
    }
    ontoTable.html("<table><tbody><tr><td><strong>Dataset</strong></td><td><strong>owl</strong></td></tr>" +
        "<tr><td><a href=\"" + document.URL + "#dbpedia-ontology\" name=\"odbpedia-ontology\">DBpedia Ontology</a></td>" +
        "<td><a href=\"" + owl + "\">owl</a><br><a href=\"" + nt + "\">nt</a></td></tr></tbody></table>");
}

function reDrawTable(s)
{
    if(s === undefined || s == null)
        s = $('#langselect').val();
    selected = s;

    if(!subArrayTest(s, Object.keys(datasets))) {
        getLanguageJson(s, langsAllLoaded);
        return;
    }

    s.unshift("dataset");

    if(table != null) {
        table.destroy();
        $('#table').remove();
    }

    tabulate(s);

    $('#table').on('draw.dt', function(){
        var langs = getLanguages();
        var list = document.createElement('select');
        list.setAttribute("id", "langselect");
        list.setAttribute("multiple", "");
        list.setAttribute("style", "width:400px");
        for (var i = 0; i < langs.length; i++) {
            var opt = document.createElement('option');
            opt.innerHTML =  langs[i];
            opt.value = langs[i];
            opt.title = getLanguageFromWikicode(langs[i]) + " (" + langs[i] + ")";
            if(s.indexOf(langs[i].trim()) >= 0)
                opt.setAttribute("selected", "");
            list.appendChild(opt);
        }
        var but = document.createElement('input');
        but.setAttribute("id", "langSelectButton");
        but.setAttribute("type", "button");
        but.setAttribute("value", "show languages");
        but.setAttribute("onclick", "reDrawTable()");
        but.setAttribute("style", "margin: 0px 0px; border: none; box-shadow: none; padding: 3px 6px; text-transform: initial;");
        var div = document.createElement('div');
        div.setAttribute("style", "float:left");
        div.appendChild(list);
        div.appendChild(but);

        $('#languagediv').html(div);

        $('#langselect').chosen();
    });
    $.fn.dataTableExt.afnFiltering.push(
        function( oSettings, aData, iDataIndex ) {
            return aData.slice(1).join('').trim().length > 0;
        }
    );
    table = $('#table').DataTable(tableConfig);
}