/**
 * Created by Chile on 1/12/2016.
 */


function prettify(input, format)
{
    var agentUrl = location.protocol+'//'+location.hostname+(location.port ? ':'+location.port: '')+
        '/dataid/publisher/prettyprintid' + (format != null ? ('?format=' + encodeURI(format)) : '');
    var method = "POST";
    return sendRequest(agentUrl, method, input, false, function () {}, function () {});
}

var delay = (function(){
    var timer = 0;
    return function(callback, ms){
        clearTimeout (timer);
        timer = setTimeout(callback, ms);
    };
})();

function sendRequest(url, method, data, async, onLoad, onError)
{
    var request = new XMLHttpRequest();
    request.onload = onLoad;
    request.onerror = onError;
    request.open(method, url, async);
    request.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
    request.send(data);
    return request.responseText
}

function readableNumber(number, divisor)
{
    if(typeof number == "string")
        number = parseFloat(number);
    var res = number / divisor;
    if(res < divisor)
        return res.toFixed(1).toString() + " k";
    res = res / divisor;
    if(res < divisor)
        return res.toFixed(1).toString() + " M";
    res = res / divisor;
    if(res < divisor)
        return res.toFixed(1).toString() + " G";
    res = res / divisor;
    return res.toFixed(1).toString() + " T";
}

function numberWithCommas(number) {
    if(number === undefined || number == null)
        return "0";
    return number.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

function noNegative(number) {
    if(number > 0)
        return number;
    else
        return 0;
}

function calcPercDiff(older, newer, factional) {
    if(older == 0)
        return 0.0;
    var diff = newer - older;
    diff = diff/(older/100);
    return diff.toFixed(factional)
}

function subArrayTest(sub, array)
{
    if(!Array.isArray(array))
        array = [array];
    if(!Array.isArray(sub))
        sub = [sub];
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

function getIfContains(valueArray, substring, field){
    var arr = null;
    if(Array.isArray(valueArray))
        arr= valueArray;
    else
        arr = [valueArray];
    for(var i in arr){
        if(!field){
            if(typeof arr[i] === 'string' && arr[i].indexOf(substring) >= 0)
                return arr[i]
        }
        else{
            if(typeof arr[i][field] === 'string' && arr[i][field].indexOf(substring) >= 0)
                return arr[i][field]
        }

    }
    return null
}

!function(a){"use strict";
    var b=function(d,e){this.settings=a.extend({},b.DEFAULTS),this.scrollbarWidth=c.getScrollbarWidth(),this.isInput="input"==d[0].tagName.toLowerCase(),this.active=!1,this.$el=d,this.$el.wrap('<div class="highlightTextarea"></div>'),this.$main=this.$el.parent(),this.$main.prepend('<div class="highlightTextarea-container"><div class="highlightTextarea-highlighter"></div></div>'),this.$container=this.$main.children().first(),this.$highlighter=this.$container.children(),this.setOptions(e),this.settings.id&&(this.$main[0].id=this.settings.id),this.settings.resizable&&this.applyResizable(),this.updateCss(),this.bindEvents(),this.highlight()};
    b.DEFAULTS={words:{},ranges:{},color:"#ffff00",caseSensitive:!0,resizable:!1,id:"",debug:!1},b.prototype.highlight=function(){var b=c.htmlEntities(this.$el.val()),d=this;
        a.each(this.settings.words,function(a,c){b=b.replace(new RegExp("("+c.join("|")+")",d.regParam),'<mark style="background-color:'+a+';">$1</mark>')}),a.each(this.settings.ranges,function(a,d){if(d.start<b.length){b=c.strInsert(b,d.end,"</mark>");
            var e='<mark style="background-color:'+d.color+';"';
            null!=d["class"]&&(e+='class="'+d["class"]+'"'),e+=">",b=c.strInsert(b,d.start,e)}}),this.$highlighter.html(b),this.updateSizePosition()},b.prototype.setWords=function(a){this.setOptions({words:a,ranges:{}})},b.prototype.setRanges=function(a){this.setOptions({words:{},ranges:a})},b.prototype.enable=function(){this.bindEvents(),this.highlight()},b.prototype.disable=function(){this.unbindEvents(),this.$highlighter.empty()},b.prototype.destroy=function(){this.disable(),c.cloneCss(this.$container,this.$el,["background-image","background-color","background-position","background-repeat","background-origin","background-clip","background-size","background-attachment"]),this.$main.replaceWith(this.$el),this.$el.removeData("highlighter")},b.prototype.setOptions=function(b){"object"!=typeof b||a.isEmptyObject(b)||(a.extend(this.settings,b),this.regParam=this.settings.caseSensitive?"gm":"gim",a.isEmptyObject(this.settings.words)?a.isEmptyObject(this.settings.ranges)||(this.settings.words={},this.settings.ranges=c.cleanRanges(this.settings.ranges,this.settings.color)):(this.settings.words=c.cleanWords(this.settings.words,this.settings.color),this.settings.ranges={}),this.settings.debug?this.$main.addClass("debug"):this.$main.removeClass("debug"),this.active&&this.highlight())},b.prototype.bindEvents=function(){if(!this.active){this.active=!0;
        var b=this;
        this.$highlighter.on({"this.highlighter":function(){b.$el.focus()}}),this.$el.on({"input.highlightTextarea":c.throttle(function(){this.highlight()},100,this),"resize.highlightTextarea":c.throttle(function(){this.updateSizePosition(!0)},50,this),"scroll.highlightTextarea select.highlightTextarea":c.throttle(function(){this.updateSizePosition()},50,this)}),this.isInput&&this.$el.on({"keydown.highlightTextarea keypress.highlightTextarea keyup.highlightTextarea":function(){setTimeout(a.proxy(b.updateSizePosition,b),1)},"blur.highlightTextarea":function(){this.value=this.value,this.scrollLeft=0,b.updateSizePosition.call(b)}})}},b.prototype.unbindEvents=function(){this.active&&(this.active=!1,this.$highlighter.off(".highlightTextarea"),this.$el.off(".highlightTextarea"))},b.prototype.updateCss=function(){c.cloneCss(this.$el,this.$main,["float","vertical-align"]),this.$main.css({width:this.$el.outerWidth(!0),height:this.$el.outerHeight(!0)}),c.cloneCss(this.$el,this.$container,["background-image","background-color","background-position","background-repeat","background-origin","background-clip","background-size","background-attachment","padding-top","padding-right","padding-bottom","padding-left"]),this.$container.css({top:c.toPx(this.$el.css("margin-top"))+c.toPx(this.$el.css("border-top-width")),left:c.toPx(this.$el.css("margin-left"))+c.toPx(this.$el.css("border-left-width")),width:this.$el.width(),height:this.$el.height()}),c.cloneCss(this.$el,this.$highlighter,["font-size","font-family","font-style","font-weight","font-variant","font-stretch","line-height","vertical-align","word-spacing","text-align","letter-spacing","text-rendering"]),this.$el.css({background:"none"})},b.prototype.applyResizable=function(){jQuery.ui&&this.$el.resizable({handles:"se",resize:c.throttle(function(){this.updateSizePosition(!0)},50,this)})},b.prototype.updateSizePosition=function(a){a&&(this.$main.css({width:this.$el.outerWidth(!0),height:this.$el.outerHeight(!0)}),this.$container.css({width:this.$el.width(),height:this.$el.height()}));
        var b,c=0;
        this.isInput?b=99999:(("scroll"==this.$el.css("overflow")||"scroll"==this.$el.css("overflow-y")||"hidden"!=this.$el.css("overflow")&&"hidden"!=this.$el.css("overflow-y")&&this.$el[0].clientHeight<this.$el[0].scrollHeight)&&(c=this.scrollbarWidth),b=this.$el.width()-c),this.$highlighter.css({width:b,height:this.$el.height()+this.$el.scrollTop(),top:-this.$el.scrollTop(),left:-this.$el.scrollLeft()})};
    var c=function(){};
    c.getScrollbarWidth=function(){var b=a('<div style="width:50px;height:50px;overflow:auto"><div>&nbsp;</div></div>').appendTo("body"),c=b.children(),d=c.innerWidth()-c.height(100).innerWidth();
        return b.remove(),d},c.cloneCss=function(a,b,c){for(var d=0,e=c.length;
                                                            e>d;
                                                            d++)b.css(c[d],a.css(c[d]))},c.toPx=function(b){if(b!=b.replace("em","")){var c=a('<div style="font-size:1em;margin:0;padding:0;height:auto;line-height:1;border:0;">&nbsp;</div>').appendTo("body");
        return b=Math.round(parseFloat(b.replace("em",""))*c.height()),c.remove(),b}return parseInt(b!=b.replace("px","")?b.replace("px",""):b)},c.htmlEntities=function(b){return b?a("<div></div>").text(b).html():""},c.strInsert=function(a,b,c){return a.slice(0,b)+c+a.slice(b)},c.throttle=function(a,b,c){var d={pid:null,last:0};
        return function(){function e(){return d.last=(new Date).getTime(),c?a.apply(c,Array.prototype.slice.call(g)):a.apply(h,Array.prototype.slice.call(g))}var f=(new Date).getTime()-d.last,g=arguments,h=this;
            return f>b?e():(clearTimeout(d.pid),void(d.pid=setTimeout(e,b-f)))}},c.cleanWords=function(b,d){var e={};
        a.isArray(b)||(b=[b]);
        for(var f=0,g=b.length;
            g>f;
            f++){var h=b[f];
            if(a.isPlainObject(h)){e[h.color]||(e[h.color]=[]),a.isArray(h.words)||(h.words=[h.words]);
                for(var i=0,j=h.words.length;
                    j>i;
                    i++)e[h.color].push(c.htmlEntities(h.words[i]))}else e[d]||(e[d]=[]),e[d].push(c.htmlEntities(h))}return e},c.cleanRanges=function(b,c){var d=[];
        (a.isPlainObject(b)||a.isNumeric(b[0]))&&(b=[b]);
        for(var e=0,f=b.length;
            f>e;
            e++){var g=b[e];
            if(a.isArray(g))d.push({color:c,start:g[0],end:g[1]});
            else if(g.ranges){(a.isPlainObject(g.ranges)||a.isNumeric(g.ranges[0]))&&(g.ranges=[g.ranges]);
                for(var h=0,i=g.ranges.length;
                    i>h;
                    h++)a.isArray(g.ranges[h])?d.push({color:g.color,"class":g["class"],start:g.ranges[h][0],end:g.ranges[h][1]}):(g.ranges[h].length&&(g.ranges[h].end=g.ranges[h].start+g.ranges[h].length),d.push(g.ranges[h]))}else g.length&&(g.end=g.start+g.length),d.push(g)}d.sort(function(a,b){return a.start==b.start?a.end-b.end:a.start-b.start});
        var j=-1;
        return a.each(d,function(b,c){c.start>=c.end&&a.error("Invalid range end/start"),c.start<j&&a.error("Ranges overlap"),j=c.end}),d.reverse(),d},a.fn.highlightTextarea=function(c){var d=arguments;
        return this.each(function(){var e=a(this),f=e.data("highlighter"),g="object"==typeof c&&c;
            (f||"destroy"!=c)&&(f||(f=new b(e,g),e.data("highlighter",f)),"string"==typeof c&&f[c].apply(f,Array.prototype.slice.call(d,1)))})}}(window.jQuery||window.Zepto);

