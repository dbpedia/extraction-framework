/**
 * Created by Chile on 1/12/2016.
 */
var ids = null;
var datasets = {};
var datasetList = null;
var initCallback = null;
var callbackParam = null;

var tableConfig = {
    "sDom": 'l<"#languagediv">ftip',
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
    getCatalog(catalogUrl);
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
            .html("<a href=\"" + langLink + "\" title='" + mapLanguage(columns[i]) + "'>" + columns[i] + "</a>");
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
                    return "<strong><a href='\"#blank\"'>" + d.row + "</a></strong>";
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
        if(isCononicalized)

            return "<small><a href=\"" + dll + "\" title=\"Canonicalized&nbsp;version&nbsp;of&nbsp;" + row.replace(" en uris", "") + ";&nbsp;Triples:&nbsp;"
                + readableNumber(datasets[column][row]["void:triples"]["@value"], 1000) + "; File&nbsp;size:&nbsp;" + readableNumber(obj["dcat:byteSize"]["@value"], 1024)
                + "; File&nbsp;size&nbsp;(unpacked):&nbsp;" + readableNumber(obj["dataid:uncompressed"]["@value"], 1024)
                + "\">" + getSerializationExtension(dll).substr(1) + "*</a>&nbsp;<a href=\"http://downloads.dbpedia.org/preview.php?file="
                + dll.replace("http://downloads.dbpedia.org/", "").replace(/\//g,'_sl_') + "\" title=\"preview&nbsp;file\">?</a></small><br/>";
        else

            return "<small><a href=\"" + dll + "\" title=\"Localized&nbsp;version&nbsp;of&nbsp;" + row.replace(" en uris", "") + ";&nbsp;Triples:&nbsp;"
                + readableNumber(datasets[column][row]["void:triples"]["@value"], 1000) + "; File&nbsp;size:&nbsp;" + readableNumber(obj["dcat:byteSize"]["@value"], 1024)
                + "; File&nbsp;size&nbsp;(unpacked):&nbsp;" + readableNumber(obj["dataid:uncompressed"]["@value"], 1024)
                + "\">" + getSerializationExtension(dll).substr(1) + "</a>&nbsp;<a href=\"http://downloads.dbpedia.org/preview.php?file="
                + dll.replace("http://downloads.dbpedia.org/", "").replace(/\//g,'_sl_') + "\" title=\"preview&nbsp;file\">?</a></small><br/>";
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
            var zw = null;
            if(datasets["en"][d.row] !== undefined)
                zw = datasets["en"][d.row];
            else
                zw = datasets["de"][d.row];
            div.transition()
                .duration(500)
                .style("opacity", 0);
            div.transition()
                .duration(200)
                .style("opacity", .9);
            div.html("<p>" + zw["dc:description"]["@value"] + "</p><br/>")
                .style("left", (d3.event.pageX) + "px")
                .style("top", (d3.event.pageY) + "px");
        }
    }

    function mapMouseOut(d, eventCol){
        if(eventCol ==0) {
            div.transition()
                .duration(500)
                .style("opacity", 0);
        }
    }

    return table;
}

function getLanguageJson(langs, onload)
{
    for(var i in langs)
    {
        if(Object.keys(datasets).indexOf(langs[i]) == -1)
            sendRequest(ids[langs[i]], "GET", null, true, onload, function () {});
    }
}

function getCatalog(catalogUrl, callback)
{
    sendRequest(catalogUrl, "GET", null, true, catalogLoaded, function () {});
}

function langLoaded(e)
{
    if(e.target.status < 205)
    {
        var json = JSON.parse(e.target.responseText)
        datasets[getLangFromUri(e.target["responseURL"])] = getDatasetsAndDistributionsById(json);
        var zw = Object.keys(datasets);
        if(zw.length == 2)
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
        getLanguageJson(["en","de"], langLoaded);
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
    for(var key in datasets["en"]) {
        if (!key.startsWith("http://") && !key.startsWith("dataid:")) {
            if(key.trim().length > 0 && key.toLowerCase().indexOf("root dataset") == -1)
                ret.push(key);
        }
    }
    for(var key in datasets["de"]) {
        if (!key.startsWith("http://") && !key.startsWith("dataid:")) {
            if(key.trim().length > 0 && key.toLowerCase().indexOf("root dataset") == -1)
                ret.push(key);
        }
    }
    return ret.filter(function(item, pos, self) {
        return self.indexOf(item) == pos;
    });
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

function subArrayTest(sub, array)
{
    if(array.length < sub.length)
        return false;
    var zw = false;
    for(var j in sub){
        if(array.indexOf(sub[j]) == -1)
            zw = true;
    }
    if(zw)
        return false;
    return true;
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
            opt.label = langs[i] + " - " + mapLanguage(langs[i]);
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
    table = $('#table').DataTable(tableConfig);
}

function mapLanguage(iso639_3)
{
    var langmap = {
        "aa": "Afar",
        "ab": "Abkhazian",
        "ace": "Acehnese",
        "af": "Afrikaans",
        "ak": "Akan",
        "als": "Alemannic",
        "am": "Amharic",
        "an": "Aragonese",
        "ang": "Anglo-Saxon",
        "ar": "Arabic",
        "arc": "Assyrian Neo-Aramaic",
        "arz": "Egyptian Arabic",
        "as": "Assamese",
        "ast": "Asturian",
        "av": "Avar",
        "ay": "Aymara",
        "az": "Azerbaijani",
        "ba": "Bashkir",
        "bar": "Bavarian",
        "bat-smg": "Samogitian",
        "bcl": "Central_Bicolano",
        "be": "Belarusian",
        "be-x-old": "Belarusian",
        "bg": "Bulgarian",
        "bh": "Bihari",
        "bi": "Bislama",
        "bjn": "Banjar",
        "bm": "Bambara",
        "bn": "Bengali",
        "bo": "Tibetan",
        "bpy": "Bishnupriya Manipuri",
        "br": "Breton",
        "bs": "Bosnian",
        "bug": "Buginese",
        "bxr": "Buryat ",
        "ca": "Catalan",
        "cbk-zam": "Zamboanga Chavacano",
        "cdo": "Min Dong",
        "ce": "Chechen",
        "ceb": "Cebuano",
        "ch": "Chamorro",
        "cho": "Choctaw",
        "chr": "Cherokee",
        "chy": "Cheyenne",
        "ckb": "Sorani",
        "co": "Corsican",
        "commons": "commons",
        "cr": "Cree",
        "crh": "Crimean Tatar",
        "cs": "Czech",
        "csb": "Kashubian",
        "cu": "Old Church Slavonic",
        "cv": "Chuvash",
        "cy": "Welsh",
        "da": "Danish",
        "de": "German",
        "diq": "Zazaki",
        "dsb": "Lower Sorbian",
        "dv": "Divehi",
        "dz": "Dzongkha",
        "ee": "Ewe",
        "el": "Greek",
        "eml": "Emilian-Romagnol",
        "en": "English",
        "eo": "Esperanto",
        "es": "Spanish",
        "et": "Estonian",
        "eu": "Basque",
        "ext": "Extremaduran",
        "fa": "Persian",
        "ff": "Fula",
        "fi": "Finnish",
        "fiu-vro": "V",
        "fj": "Fijian",
        "fo": "Faroese",
        "fr": "French",
        "frp": "Franco-Proven",
        "frr": "North Frisian",
        "fur": "Friulian",
        "fy": "West Frisian",
        "ga": "Irish",
        "gag": "Gagauz",
        "gan": "Gan",
        "gd": "Scottish Gaelic",
        "gl": "Galician",
        "glk": "Gilaki",
        "gn": "Guarani",
        "got": "Gothic",
        "gu": "Gujarati",
        "gv": "Manx",
        "ha": "Hausa",
        "hak": "Hakka",
        "haw": "Hawaiian",
        "he": "Hebrew",
        "hi": "Hindi",
        "hif": "Fiji Hindi",
        "ho": "Hiri Motu",
        "hr": "Croatian",
        "hsb": "Upper Sorbian",
        "ht": "Haitian",
        "hu": "Hungarian",
        "hy": "Armenian",
        "hz": "Herero",
        "ia": "Interlingua",
        "id": "Indonesian",
        "ie": "Interlingue",
        "ig": "Igbo",
        "ii": "Sichuan Yi",
        "ik": "Inupiak",
        "ilo": "Ilokano",
        "io": "Ido",
        "is": "Icelandic",
        "it": "Italian",
        "iu": "Inuktitut",
        "ja": "Japanese",
        "jbo": "Lojban",
        "jv": "Javanese",
        "ka": "Georgian",
        "kaa": "Karakalpak",
        "kab": "Kabyle",
        "kbd": "Kabardian Circassian",
        "kg": "Kongo",
        "ki": "Kikuyu",
        "kj": "Kuanyama",
        "kk": "Kazakh",
        "kl": "Greenlandic",
        "km": "Khmer",
        "kn": "Kannada",
        "ko": "Korean",
        "koi": "Komi-Permyak",
        "kr": "Kanuri",
        "krc": "Karachay-Balkar",
        "ks": "Kashmiri",
        "ksh": "Ripuarian",
        "ku": "Kurdish",
        "kv": "Komi",
        "kw": "Cornish",
        "ky": "Kirghiz",
        "la": "Latin",
        "lad": "Ladino",
        "lb": "Luxembourgish",
        "lbe": "Lak",
        "lez": "Lezgian",
        "lg": "Luganda",
        "li": "Limburgian",
        "lij": "Ligurian",
        "lmo": "Lombard",
        "ln": "Lingala",
        "lo": "Lao",
        "lt": "Lithuanian",
        "ltg": "Latgalian",
        "lv": "Latvian",
        "map-bms": "Banyumasan",
        "mdf": "Moksha",
        "mg": "Malagasy",
        "mh": "Marshallese",
        "mhr": "Meadow Mari",
        "mi": "Maori",
        "mk": "Macedonian",
        "ml": "Malayalam",
        "mn": "Mongolian",
        "mo": "Moldovan",
        "mr": "Marathi",
        "mrj": "Hill Mari",
        "ms": "Malay",
        "mt": "Maltese",
        "mus": "Muscogee",
        "mwl": "Mirandese",
        "my": "Burmese",
        "myv": "Erzya",
        "mzn": "Mazandarani",
        "na": "Nauruan",
        "nah": "Nahuatl",
        "nap": "Neapolitan",
        "nds": "Low Saxon",
        "nds-nl": "Dutch Low Saxon",
        "ne": "Nepali",
        "new": "Newar",
        "ng": "Ndonga",
        "nl": "Dutch",
        "nn": "Norwegian ",
        "no": "Norwegian ",
        "nov": "Novial",
        "nrm": "Norman",
        "nso": "Northern Sotho",
        "nv": "Navajo",
        "ny": "Chichewa",
        "oc": "Occitan",
        "om": "Oromo",
        "or": "Oriya",
        "os": "Ossetian",
        "pa": "Punjabi",
        "pag": "Pangasinan",
        "pam": "Kapampangan",
        "pap": "Papiamentu",
        "pcd": "Picard",
        "pdc": "Pennsylvania German",
        "pfl": "Palatinate German",
        "pi": "Pali",
        "pih": "Norfolk",
        "pl": "Polish",
        "pms": "Piedmontese",
        "pnb": "Western Panjabi",
        "pnt": "Pontic",
        "ps": "Pashto",
        "pt": "Portuguese",
        "qu": "Quechua",
        "rm": "Romansh",
        "rmy": "Romani",
        "rn": "Kirundi",
        "ro": "Romanian",
        "roa-rup": "Aromanian",
        "roa-tara": "Tarantino",
        "ru": "Russian",
        "rue": "Rusyn",
        "rw": "Kinyarwanda",
        "sa": "Sanskrit",
        "sah": "Sakha",
        "sc": "Sardinian",
        "scn": "Sicilian",
        "sco": "Scots",
        "sd": "Sindhi",
        "se": "Northern Sami",
        "sg": "Sango",
        "sh": "Serbo-Croatian",
        "si": "Sinhalese",
        "simple": "Simple English",
        "sk": "Slovak",
        "sl": "Slovenian",
        "sm": "Samoan",
        "sn": "Shona",
        "so": "Somali",
        "sq": "Albanian",
        "sr": "Serbian",
        "srn": "Sranan",
        "ss": "Swati",
        "st": "Sesotho",
        "stq": "Saterland Frisian",
        "su": "Sundanese",
        "sv": "Swedish",
        "sw": "Swahili",
        "szl": "Silesian",
        "ta": "Tamil",
        "te": "Telugu",
        "tet": "Tetum",
        "tg": "Tajik",
        "th": "Thai",
        "ti": "Tigrinya",
        "tk": "Turkmen",
        "tl": "Tagalog",
        "tn": "Tswana",
        "to": "Tongan",
        "tpi": "Tok Pisin",
        "tr": "Turkish",
        "ts": "Tsonga",
        "tt": "Tatar",
        "tum": "Tumbuka",
        "tw": "Twi",
        "ty": "Tahitian",
        "udm": "Udmurt",
        "ug": "Uyghur",
        "uk": "Ukrainian",
        "ur": "Urdu",
        "uz": "Uzbek",
        "ve": "Venda",
        "vec": "Venetian",
        "vep": "Vepsian",
        "vi": "Vietnamese",
        "vls": "West Flemish",
        "vo": "Volap",
        "wa": "Walloon",
        "war": "Waray-Waray",
        "wo": "Wolof",
        "wuu": "Wu",
        "xal": "Kalmyk",
        "xh": "Xhosa",
        "xmf": "Mingrelian",
        "yi": "Yiddish",
        "yo": "Yoruba",
        "za": "Zhuang",
        "zea": "Zeelandic",
        "zh": "Chinese",
        "zh-classical": "Classical Chinese",
        "zh-min-nan": "Min Nan",
        "zh-yue": "Cantonese",
        "zu": "Zulu"
    };
    return langmap[iso639_3.replace("_", "-")];
}