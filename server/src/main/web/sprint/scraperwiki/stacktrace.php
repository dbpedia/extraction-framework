

<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
  <title>
	ScraperWiki / ScraperWiki / source – Bitbucket
</title>
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
  <meta name="description" content="" />
  <meta name="keywords" content="ScraperWiki,Code,repository,for,ScraperWiki.com,source,sourcecode,scraperlibs/scraperwiki/stacktrace.php@c0f684545717" />
  <!--[if lt IE 9]>
  <script src="https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/js/lib/html5.js"></script>
  <![endif]-->

  <script type="text/javascript">
    var MEDIA_URL = "https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/";
    (function (window) {
      // prevent stray occurrences of `console.log` from causing errors in IE
      var console = window.console || (window.console = {});
      console.log || (console.log = function () {});

      window.BB || (window.BB = {});
      window.BB.debug = false;
      window.BB.cname = false;
      window.BB.CANON_URL = 'https://bitbucket.org';
      window.BB.user || (window.BB.user = {});
      window.BB.user.has = (function () {
        var betaFeatures = [];
        
        return function (feature) {
          return _.contains(betaFeatures, feature);
        };
      }());
      window.BB.repo || (window.BB.repo = {});
  
  
    
    
      window.BB.repo.slug = 'scraperwiki';
    
    
      window.BB.repo.owner = {
        username: 'ScraperWiki'
      };
    
  
    }(this));
  </script>


  
  <link rel="stylesheet" href="https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/css/reset.css" />
  <link rel="stylesheet" href="https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/css/defaults.css" />
  <link rel="stylesheet" href="https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/css/layout.css" />
  <link rel="stylesheet" href="https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/css/forms.css" />

  <link rel="stylesheet" href="https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/css/components.css" />
  <link rel="stylesheet" href="https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/css/svn-import.css" />

  <link rel="stylesheet" href="https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/css/admin.css" />
  <link rel="stylesheet" href="https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/css/changesets.css" />
  <link rel="stylesheet" href="https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/css/dashboard.css" />
  <link rel="stylesheet" href="https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/css/descendants.css" />
  <link rel="stylesheet" href="https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/css/explore.css" />
  <link rel="stylesheet" href="https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/css/issues.css" />
  <link rel="stylesheet" href="https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/css/messages.css" />
  <link rel="stylesheet" href="https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/css/profile.css" />
  <link rel="stylesheet" href="https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/css/promo.css" />
  <link rel="stylesheet" href="https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/css/signup.css" />
  <link rel="stylesheet" href="https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/css/wiki.css" />
  <link rel="stylesheet" href="https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/css/zealots.css" />
  <link rel="stylesheet" href="https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/css/jqueryui/jquery-ui-1.8.7.custom.css" />

  <link rel="stylesheet" href="https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/css/jqueryui/jquery.ui.progressbar.css" />

  <link rel="stylesheet" href="https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/css/screen.css" />
  <link rel="stylesheet" href="https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/css/fancybox.css" />
  <link rel="stylesheet" href="https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/css/plans.css" />
  <link rel="stylesheet" href="https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/markitup/skins/simple/style.css" />
  <link rel="stylesheet" href="https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/css/print.css" media="print" />



  <link rel="search" type="application/opensearchdescription+xml" href="/opensearch.xml" title="Bitbucket" />
  <link rel="icon" href="https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/img/logo_new.png" type="image/png" />

  <!--[if IE]>
  <script src="https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/js/lib/excanvas.js"></script>
  <![endif]-->


  <script src="https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/js/lib/bundle.js"></script>



	<link rel="stylesheet" href="https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/css/highlight/trac.css" type="text/css" />


</head>

<body class="">

  <div id="wrapper">



  <div id="header-wrap">
    <div id="header">
    <ul id="global-nav">
      <li><a class="home" href="http://www.atlassian.com">Atlassian Home</a></li>
      <li><a class="docs" href="http://confluence.atlassian.com/display/BITBUCKET">Documentation</a></li>
      <li><a class="support" href="/support">Support</a></li>
      <li><a class="blog" href="http://blog.bitbucket.org">Blog</a></li>
      <li><a class="forums" href="http://groups.google.com/group/bitbucket-users">Forums</a></li>
    </ul>
    <a href="/" id="logo">Bitbucket by Atlassian</a>

    <div id="main-nav" class="clearfix">
    
      <ul class="clearfix">
        <li><a href="/plans">Pricing &amp; Signup</a></li>
        <li><a href="/repo/month">Explore Bitbucket</a></li>
        <li><a href="/account/signin/">Log in</a></li>
        

<li class="search-box">
  <form action="/repo/all">
    <input type="text" name="name" id="search" placeholder="Find a project" />
  </form>
</li>

      </ul>
    
    </div>
    </div>
  </div>

    <div id="header-messages">
  
  
    
    
    
    
  

    
   </div>



    <div id="content">
      <div id="unnamed">
      
	
  





  <script type="text/javascript">
    jQuery(function ($) {
        var cookie = $.cookie,
            cookieOptions, date,
            $content = $('#content'),
            $pane = $('#what-is-bitbucket'),
            $hide = $pane.find('[href="#hide"]').css('display', 'block').hide();

        date = new Date();
        date.setTime(date.getTime() + 365 * 24 * 60 * 60 * 1000);
        cookieOptions = { path: '/', expires: date };

        if (cookie('toggle_status') == 'hide') $content.addClass('repo-desc-hidden');

        $('#toggle-repo-content').click(function (event) {
            event.preventDefault();
            $content.toggleClass('repo-desc-hidden');
            cookie('toggle_status', cookie('toggle_status') == 'show' ? 'hide' : 'show', cookieOptions);
        });

        if (!cookie('hide_intro_message')) $pane.show();

        $hide.click(function (event) {
            event.preventDefault();
            cookie('hide_intro_message', true, cookieOptions);
            $pane.slideUp('slow');
        });

        $pane.hover(
            function () { $hide.fadeIn('fast'); },
            function () { $hide.fadeOut('fast'); });
    });
  </script>



  
  
  
  
  
    <div id="what-is-bitbucket" class="new-to-bitbucket">
      <h2>ScraperWiki <span id="slogan">is sharing code with you</span></h2>
      <img src="https://secure.gravatar.com/avatar/4ba76f697d2fa2d70afa585e6adb6ae9?d=identicon&s=32" alt="" class="avatar" />
      <p>Bitbucket is a code hosting site. Unlimited public and private repositories. Free for small teams.</p>
      <div class="primary-action-link signup"><a href="/account/signup/?utm_source=internal&utm_medium=banner&utm_campaign=what_is_bitbucket">Try Bitbucket free</a></div>
      <a href="#hide" title="Don't show this again">Don't show this again</a>
    </div>
  


<div id="tabs">
  <ul class="tabs">
    <li>
      <a href="/ScraperWiki/scraperwiki/overview">Overview</a>
    </li>

    <li>
      <a href="/ScraperWiki/scraperwiki/downloads">Downloads (0)</a>
    </li>

    

    

    <li class="selected">
      
        <a href="/ScraperWiki/scraperwiki/src/c0f684545717">Source</a>
      
    </li>

    <li>
      <a href="/ScraperWiki/scraperwiki/changesets">Changesets</a>
    </li>

    
      
        <li class="dropdown">
          <a href="/ScraperWiki/scraperwiki/wiki">Wiki</a>
        </li>
      
    

    
      
        <li class="dropdown">
          <a href="/ScraperWiki/scraperwiki/issues?status=new&amp;status=open">Issues (207) &raquo;</a>
          <ul>
            <li><a href="/ScraperWiki/scraperwiki/issues/new">Create new issue</a></li>
            <li><a href="/ScraperWiki/scraperwiki/issues?status=new">New issues</a></li>
            <li><a href="/ScraperWiki/scraperwiki/issues?status=new&amp;status=open">Open issues</a></li>
            <li><a href="/ScraperWiki/scraperwiki/issues?status=resolved&amp;status=invalid&amp;status=duplicate">Closed issues</a></li>
            
            <li><a href="/ScraperWiki/scraperwiki/issues">All issues</a></li>
            <li><a href="/ScraperWiki/scraperwiki/issues/query">Advanced query</a></li>
          </ul>
        </li>
      
    

    

    <li class="secondary">
      <a href="/ScraperWiki/scraperwiki/descendants">Forks/Queues (2)</a>
    </li>

    <li class="secondary">
      <a href="/ScraperWiki/scraperwiki/zealots">Followers (<span id="followers-count">14</span>)</a>
    </li>
  </ul>
</div>


  <div class="repo-menu" id="repo-menu">
    <ul id="repo-menu-links">
     
      <li>
        <a href="/ScraperWiki/scraperwiki/rss" class="rss" title="RSS feed for ScraperWiki">RSS</a>
      </li>
      
        <li>
          <a href="/ScraperWiki/scraperwiki/pull" class="pull-request">
            pull request
          </a>
        </li>
      
      <li><a href="/ScraperWiki/scraperwiki/fork" class="fork">fork</a></li>
      
        <li><a href="/ScraperWiki/scraperwiki/hack" class="patch-queue">patch queue</a></li>
      
      <li>
        <a rel="nofollow" href="/ScraperWiki/scraperwiki/follow" class="follow">follow</a>
      </li>
      
          
      
      
        <li>
          <a class="source">get source</a>
          <ul class="downloads">
            
              <li><a rel="nofollow" href="/ScraperWiki/scraperwiki/get/c0f684545717.zip">zip</a></li>
              <li><a rel="nofollow" href="/ScraperWiki/scraperwiki/get/c0f684545717.tar.gz">gz</a></li>
              <li><a rel="nofollow" href="/ScraperWiki/scraperwiki/get/c0f684545717.tar.bz2">bz2</a></li>
            
          </ul>
        </li>
      
    </ul>

  
    <ul class="metadata">
    
      <li class="branches">branches
        <ul>
          <li>
            <a href="/ScraperWiki/scraperwiki/src/2209f34702a0">stable</a>
          
            <a rel="nofollow" class="menu-compare" href="/ScraperWiki/scraperwiki/compare/default..stable" title="Show changes between stable and default">compare</a>
          
          </li>
          <li>
            <a href="/ScraperWiki/scraperwiki/src/9fc022e96401">scraperwiki</a>
          
            <a rel="nofollow" class="menu-compare" href="/ScraperWiki/scraperwiki/compare/default..scraperwiki" title="Show changes between scraperwiki and default">compare</a>
          
          </li>
          <li>
            <a href="/ScraperWiki/scraperwiki/src/54f97c010c9e">design_revamp</a>
          
            <a rel="nofollow" class="menu-compare" href="/ScraperWiki/scraperwiki/compare/default..design_revamp" title="Show changes between design_revamp and default">compare</a>
          
          </li>
          <li>
            <a href="/ScraperWiki/scraperwiki/src/c0f684545717">default</a>
          
          </li>
        </ul>
      </li>
    
      <li class="tags">tags
        <ul>
          <li><a href="/ScraperWiki/scraperwiki/src/c0f684545717">tip</a>
            </li>
        </ul>
      </li>
    </ul>
  
</div>
<div class="repo-menu" id="repo-desc">
  

    <ul id="repo-menu-links-mini">
      
      <li>
        <a href="/ScraperWiki/scraperwiki/rss" class="rss" title="RSS feed for ScraperWiki"></a>
      </li>
      
        <li>
          <a href="/ScraperWiki/scraperwiki/pull" class="pull-request" title="Pull request"></a>
        </li>
      
      <li><a href="/ScraperWiki/scraperwiki/fork" class="fork" title="Fork"></a></li>
      
        <li><a href="/ScraperWiki/scraperwiki/hack" class="patch-queue" title="Patch queue"></a></li>
      
      <li>
        <a rel="nofollow" href="/ScraperWiki/scraperwiki/follow" class="follow">follow</a>
      </li>
      
          
      
      
        <li>
          <a class="source" title="Get source"></a>
          <ul class="downloads">
            
              <li><a rel="nofollow" href="/ScraperWiki/scraperwiki/get/c0f684545717.zip">zip</a></li>
              <li><a rel="nofollow" href="/ScraperWiki/scraperwiki/get/c0f684545717.tar.gz">gz</a></li>
              <li><a rel="nofollow" href="/ScraperWiki/scraperwiki/get/c0f684545717.tar.bz2">bz2</a></li>
            
          </ul>
        </li>
      
    </ul>

    <h3 id="repo-heading">
      <a href="/ScraperWiki">ScraperWiki</a> /
      <a href="/ScraperWiki/scraperwiki/wiki/Home">ScraperWiki</a>
    
      <span><a href="http://scraperwiki.com/">http://scraperwiki.com/</a></span>
    
    </h3>


  <p class="repo-desc-description">Code repository for <a href="http://ScraperWiki.com" rel="nofollow">ScraperWiki.com</a></p>


  <div id="repo-desc-cloneinfo">Clone this repository (size: 15.8 MB): <a href="https://bitbucket.org/ScraperWiki/scraperwiki" onclick="$('#clone-url-ssh').hide();$('#clone-url-https').toggle();return(false);"><small>HTTPS</small></a> / <a href="ssh://hg@bitbucket.org/ScraperWiki/scraperwiki" onclick="$('#clone-url-https').hide();$('#clone-url-ssh').toggle();return(false);"><small>SSH</small></a><br />
    <pre id="clone-url-https">hg clone <a href="https://bitbucket.org/ScraperWiki/scraperwiki">https://bitbucket.org/ScraperWiki/scraperwiki</a></pre>

    <pre id="clone-url-ssh" style="display:none;">hg clone <a href="ssh://hg@bitbucket.org/ScraperWiki/scraperwiki">ssh://hg@bitbucket.org/ScraperWiki/scraperwiki</a></pre></div>

  <a href="#" id="toggle-repo-content"></a>

  

</div>




      

<div id="source-path" class="layout-box">
	<a href="/ScraperWiki/scraperwiki/src">ScraperWiki</a> /
	
		
			
				<a href="/ScraperWiki/scraperwiki/src/c0f684545717/scraperlibs/">
					scraperlibs
				</a>
			
		
		/
	
		
			
				<a href="/ScraperWiki/scraperwiki/src/c0f684545717/scraperlibs/scraperwiki/">
					scraperwiki
				</a>
			
		
		/
	
		
			
				stacktrace.php
			
		
		
	
</div>


<div id="source-view">
	<table class="info-table">
		<tr>
			<th>r3257:c0f684545717</th>
			<th>74 loc</th>
			<th>2.8 KB</th>
			<th class="source-view-links">
				<a id="embed-link" href="#" onclick="makeEmbed('#embed-link', 'https://bitbucket.org/ScraperWiki/scraperwiki/src/c0f684545717/scraperlibs/scraperwiki/stacktrace.php?embed=t');">embed</a> /
				<a href="/ScraperWiki/scraperwiki/history/scraperlibs/scraperwiki/stacktrace.php">history</a> /
				<a href="/ScraperWiki/scraperwiki/annotate/c0f684545717/scraperlibs/scraperwiki/stacktrace.php">annotate</a> /
				<a href="/ScraperWiki/scraperwiki/raw/c0f684545717/scraperlibs/scraperwiki/stacktrace.php">raw</a> /
				<form action="/ScraperWiki/scraperwiki/diff/scraperlibs/scraperwiki/stacktrace.php" class="source-view-form">
					
					<input type="hidden" name="diff2" value="c0f684545717" />
						<select name="diff1" class="smaller">
							
								
									<option value="a77203078d0e">
										r2902:a77203078d0e
									</option>
								
							
								
									<option value="518dcb948e43">
										r1840:518dcb948e43
									</option>
								
							
								
									<option value="8363c4b7a4ef">
										r1791:8363c4b7a4ef
									</option>
								
							
								
									<option value="9c8bcb949af8">
										r1789:9c8bcb949af8
									</option>
								
							
								
									<option value="d8a53931c920">
										r1788:d8a53931c920
									</option>
								
							
								
									<option value="36826e91ed45">
										r1786:36826e91ed45
									</option>
								
							
								
									<option value="fd3db4bca1a9">
										r1785:fd3db4bca1a9
									</option>
								
							
								
									<option value="c13c4fc21718">
										r1773:c13c4fc21718
									</option>
								
							
								
									<option value="0b6d52a21c88">
										r1772:0b6d52a21c88
									</option>
								
							
								
									<option value="126c6fe0e1d6">
										r1767:126c6fe0e1d6
									</option>
								
							
								
									<option value="45cbb93c27c9">
										r1752:45cbb93c27c9
									</option>
								
							
						</select>
						<input type="submit" value="diff" class="smaller" />
					
				</form>
			</th>
		</tr>
	</table>
	
		<div class="scroll-x">
		
			<table class="highlighttable"><tr><td class="linenos"><div class="linenodiv"><pre><a href="#cl-1"> 1</a>
<a href="#cl-2"> 2</a>
<a href="#cl-3"> 3</a>
<a href="#cl-4"> 4</a>
<a href="#cl-5"> 5</a>
<a href="#cl-6"> 6</a>
<a href="#cl-7"> 7</a>
<a href="#cl-8"> 8</a>
<a href="#cl-9"> 9</a>
<a href="#cl-10">10</a>
<a href="#cl-11">11</a>
<a href="#cl-12">12</a>
<a href="#cl-13">13</a>
<a href="#cl-14">14</a>
<a href="#cl-15">15</a>
<a href="#cl-16">16</a>
<a href="#cl-17">17</a>
<a href="#cl-18">18</a>
<a href="#cl-19">19</a>
<a href="#cl-20">20</a>
<a href="#cl-21">21</a>
<a href="#cl-22">22</a>
<a href="#cl-23">23</a>
<a href="#cl-24">24</a>
<a href="#cl-25">25</a>
<a href="#cl-26">26</a>
<a href="#cl-27">27</a>
<a href="#cl-28">28</a>
<a href="#cl-29">29</a>
<a href="#cl-30">30</a>
<a href="#cl-31">31</a>
<a href="#cl-32">32</a>
<a href="#cl-33">33</a>
<a href="#cl-34">34</a>
<a href="#cl-35">35</a>
<a href="#cl-36">36</a>
<a href="#cl-37">37</a>
<a href="#cl-38">38</a>
<a href="#cl-39">39</a>
<a href="#cl-40">40</a>
<a href="#cl-41">41</a>
<a href="#cl-42">42</a>
<a href="#cl-43">43</a>
<a href="#cl-44">44</a>
<a href="#cl-45">45</a>
<a href="#cl-46">46</a>
<a href="#cl-47">47</a>
<a href="#cl-48">48</a>
<a href="#cl-49">49</a>
<a href="#cl-50">50</a>
<a href="#cl-51">51</a>
<a href="#cl-52">52</a>
<a href="#cl-53">53</a>
<a href="#cl-54">54</a>
<a href="#cl-55">55</a>
<a href="#cl-56">56</a>
<a href="#cl-57">57</a>
<a href="#cl-58">58</a>
<a href="#cl-59">59</a>
<a href="#cl-60">60</a>
<a href="#cl-61">61</a>
<a href="#cl-62">62</a>
<a href="#cl-63">63</a>
<a href="#cl-64">64</a>
<a href="#cl-65">65</a>
<a href="#cl-66">66</a>
<a href="#cl-67">67</a>
<a href="#cl-68">68</a>
<a href="#cl-69">69</a>
<a href="#cl-70">70</a>
<a href="#cl-71">71</a>
<a href="#cl-72">72</a>
<a href="#cl-73">73</a>
<a href="#cl-74">74</a>
</pre></div></td><td class="code"><div class="highlight"><pre><a name="cl-1"></a><span class="cp">&lt;?php</span>
<a name="cl-2"></a>
<a name="cl-3"></a><span class="k">function</span> <span class="nf">exceptionHandler</span><span class="p">(</span><span class="nv">$exception</span><span class="p">,</span> <span class="nv">$script</span><span class="p">)</span> 
<a name="cl-4"></a><span class="p">{</span>
<a name="cl-5"></a>    <span class="nv">$stackdump</span> <span class="o">=</span> <span class="k">array</span><span class="p">();</span> 
<a name="cl-6"></a>    <span class="nv">$scriptlines</span> <span class="o">=</span> <span class="nb">explode</span><span class="p">(</span><span class="s2">&quot;</span><span class="se">\n</span><span class="s2">&quot;</span><span class="p">,</span> <span class="nb">file_get_contents</span><span class="p">(</span><span class="nv">$script</span><span class="p">));</span> 
<a name="cl-7"></a>    <span class="nv">$trace</span> <span class="o">=</span> <span class="nv">$exception</span><span class="o">-&gt;</span><span class="na">getTrace</span><span class="p">();</span> 
<a name="cl-8"></a>    <span class="k">for</span> <span class="p">(</span><span class="nv">$i</span> <span class="o">=</span> <span class="nb">count</span><span class="p">(</span><span class="nv">$trace</span><span class="p">)</span> <span class="o">-</span> <span class="m">2</span><span class="p">;</span> <span class="nv">$i</span> <span class="o">&gt;=</span> <span class="m">0</span><span class="p">;</span> <span class="nv">$i</span><span class="o">--</span><span class="p">)</span>
<a name="cl-9"></a>    <span class="p">{</span>
<a name="cl-10"></a>        <span class="nv">$stackPoint</span> <span class="o">=</span> <span class="nv">$trace</span><span class="p">[</span><span class="nv">$i</span><span class="p">];</span> 
<a name="cl-11"></a>        <span class="nv">$linenumber</span> <span class="o">=</span> <span class="nv">$stackPoint</span><span class="p">[</span><span class="s2">&quot;line&quot;</span><span class="p">];</span> 
<a name="cl-12"></a>        <span class="nv">$stackentry</span> <span class="o">=</span> <span class="k">array</span><span class="p">(</span><span class="s2">&quot;linenumber&quot;</span> <span class="o">=&gt;</span> <span class="nv">$linenumber</span><span class="p">,</span> <span class="s2">&quot;duplicates&quot;</span> <span class="o">=&gt;</span> <span class="m">1</span><span class="p">);</span> 
<a name="cl-13"></a>        <span class="nv">$stackentry</span><span class="p">[</span><span class="s2">&quot;file&quot;</span><span class="p">]</span> <span class="o">=</span> <span class="p">(</span><span class="nv">$stackPoint</span><span class="p">[</span><span class="s2">&quot;file&quot;</span><span class="p">]</span> <span class="o">==</span> <span class="nv">$script</span> <span class="o">?</span> <span class="s2">&quot;&lt;string&gt;&quot;</span> <span class="o">:</span> <span class="nv">$stackPoint</span><span class="p">[</span><span class="s2">&quot;file&quot;</span><span class="p">]);</span> 
<a name="cl-14"></a>
<a name="cl-15"></a>        <span class="k">if</span> <span class="p">((</span><span class="nv">$linenumber</span> <span class="o">&gt;=</span> <span class="m">0</span><span class="p">)</span> <span class="o">&amp;&amp;</span> <span class="p">(</span><span class="nv">$linenumber</span> <span class="o">&lt;</span> <span class="nb">count</span><span class="p">(</span><span class="nv">$scriptlines</span><span class="p">)))</span>
<a name="cl-16"></a>            <span class="nv">$stackentry</span><span class="p">[</span><span class="s2">&quot;linetext&quot;</span><span class="p">]</span> <span class="o">=</span> <span class="nv">$scriptlines</span><span class="p">[</span><span class="nv">$linenumber</span><span class="p">];</span> 
<a name="cl-17"></a>
<a name="cl-18"></a>        <span class="k">if</span> <span class="p">(</span><span class="nb">array_key_exists</span><span class="p">(</span><span class="s2">&quot;args&quot;</span><span class="p">,</span> <span class="nv">$stackPoint</span><span class="p">)</span> <span class="k">and</span> <span class="nb">count</span><span class="p">(</span><span class="nv">$stackPoint</span><span class="p">[</span><span class="s2">&quot;args&quot;</span><span class="p">])</span> <span class="o">!=</span> <span class="m">0</span><span class="p">)</span>
<a name="cl-19"></a>        <span class="p">{</span>
<a name="cl-20"></a>            <span class="nv">$args</span> <span class="o">=</span> <span class="k">array</span><span class="p">();</span> 
<a name="cl-21"></a>            <span class="k">foreach</span> <span class="p">(</span><span class="nv">$stackPoint</span><span class="p">[</span><span class="s2">&quot;args&quot;</span><span class="p">]</span> <span class="k">as</span> <span class="nv">$arg</span> <span class="o">=&gt;</span> <span class="nv">$val</span><span class="p">)</span>
<a name="cl-22"></a>                <span class="nv">$args</span><span class="p">[]</span> <span class="o">=</span> <span class="nv">$arg</span><span class="o">.</span><span class="s2">&quot;=&gt;&quot;</span><span class="o">.</span><span class="nv">$val</span><span class="p">;</span> 
<a name="cl-23"></a>            <span class="nv">$stackentry</span><span class="p">[</span><span class="s2">&quot;furtherlinetext&quot;</span><span class="p">]</span> <span class="o">=</span> <span class="s2">&quot; param values: (&quot;</span><span class="o">.</span><span class="nb">implode</span><span class="p">(</span><span class="s2">&quot;, &quot;</span><span class="p">,</span> <span class="nv">$args</span><span class="p">)</span><span class="o">.</span><span class="s2">&quot;)&quot;</span><span class="p">;</span> 
<a name="cl-24"></a>        <span class="p">}</span>
<a name="cl-25"></a>        <span class="nv">$stackdump</span><span class="p">[]</span> <span class="o">=</span> <span class="nv">$stackentry</span><span class="p">;</span> 
<a name="cl-26"></a>    <span class="p">}</span>
<a name="cl-27"></a>    
<a name="cl-28"></a>    <span class="nv">$linenumber</span> <span class="o">=</span> <span class="nv">$exception</span><span class="o">-&gt;</span><span class="na">getLine</span><span class="p">();</span> 
<a name="cl-29"></a>    <span class="nv">$finalentry</span> <span class="o">=</span> <span class="k">array</span><span class="p">(</span><span class="s2">&quot;linenumber&quot;</span> <span class="o">=&gt;</span> <span class="nv">$linenumber</span><span class="p">,</span> <span class="s2">&quot;duplicates&quot;</span> <span class="o">=&gt;</span> <span class="m">1</span><span class="p">);</span> 
<a name="cl-30"></a>    <span class="nv">$finalentry</span><span class="p">[</span><span class="s2">&quot;file&quot;</span><span class="p">]</span> <span class="o">=</span> <span class="p">(</span><span class="nv">$exception</span><span class="o">-&gt;</span><span class="na">getFile</span><span class="p">()</span> <span class="o">==</span> <span class="nv">$script</span> <span class="o">?</span> <span class="s2">&quot;&lt;string&gt;&quot;</span> <span class="o">:</span> <span class="nv">$exception</span><span class="o">-&gt;</span><span class="na">getFile</span><span class="p">());</span> 
<a name="cl-31"></a>    <span class="k">if</span> <span class="p">((</span><span class="nv">$linenumber</span> <span class="o">&gt;=</span> <span class="m">0</span><span class="p">)</span> <span class="o">&amp;&amp;</span> <span class="p">(</span><span class="nv">$linenumber</span> <span class="o">&lt;</span> <span class="nb">count</span><span class="p">(</span><span class="nv">$scriptlines</span><span class="p">)))</span>
<a name="cl-32"></a>        <span class="nv">$finalentry</span><span class="p">[</span><span class="s2">&quot;linetext&quot;</span><span class="p">]</span> <span class="o">=</span> <span class="nv">$scriptlines</span><span class="p">[</span><span class="nv">$linenumber</span><span class="p">];</span> 
<a name="cl-33"></a>    <span class="nv">$finalentry</span><span class="p">[</span><span class="s2">&quot;furtherlinetext&quot;</span><span class="p">]</span> <span class="o">=</span> <span class="nv">$exception</span><span class="o">-&gt;</span><span class="na">getMessage</span><span class="p">()</span><span class="o">.</span><span class="nb">count</span><span class="p">(</span><span class="nv">$scriptlines</span><span class="p">);</span> 
<a name="cl-34"></a>    <span class="nv">$stackdump</span><span class="p">[]</span> <span class="o">=</span> <span class="nv">$finalentry</span><span class="p">;</span> 
<a name="cl-35"></a>    
<a name="cl-36"></a>    <span class="k">return</span> <span class="k">array</span><span class="p">(</span><span class="s1">&#39;message_type&#39;</span> <span class="o">=&gt;</span> <span class="s1">&#39;exception&#39;</span><span class="p">,</span> <span class="s1">&#39;exceptiondescription&#39;</span> <span class="o">=&gt;</span> <span class="nv">$exception</span><span class="o">-&gt;</span><span class="na">getMessage</span><span class="p">(),</span> <span class="s2">&quot;stackdump&quot;</span> <span class="o">=&gt;</span> <span class="nv">$stackdump</span><span class="p">);</span> 
<a name="cl-37"></a><span class="p">}</span>
<a name="cl-38"></a>
<a name="cl-39"></a>
<a name="cl-40"></a><span class="k">function</span> <span class="nf">errorParser</span><span class="p">(</span><span class="nv">$errno</span><span class="p">,</span> <span class="nv">$errstr</span><span class="p">,</span> <span class="nv">$errfile</span><span class="p">,</span> <span class="nv">$errline</span><span class="p">,</span> <span class="nv">$script</span><span class="p">)</span>
<a name="cl-41"></a><span class="p">{</span>
<a name="cl-42"></a>    <span class="nv">$codes</span> <span class="o">=</span> <span class="k">Array</span><span class="p">(</span>
<a name="cl-43"></a>        <span class="m">1</span> <span class="o">=&gt;</span> <span class="s2">&quot;E_ERROR&quot;</span><span class="p">,</span>
<a name="cl-44"></a>        <span class="m">2</span> <span class="o">=&gt;</span> <span class="s2">&quot;E_WARNING&quot;</span><span class="p">,</span>
<a name="cl-45"></a>        <span class="m">4</span> <span class="o">=&gt;</span> <span class="s2">&quot;E_PARSE&quot;</span><span class="p">,</span>
<a name="cl-46"></a>        <span class="m">8</span> <span class="o">=&gt;</span> <span class="s2">&quot;E_NOTICE&quot;</span><span class="p">,</span>
<a name="cl-47"></a>        <span class="m">16</span> <span class="o">=&gt;</span> <span class="s2">&quot;E_CORE_ERROR&quot;</span><span class="p">,</span>
<a name="cl-48"></a>        <span class="m">32</span> <span class="o">=&gt;</span> <span class="s2">&quot;E_CORE_WARNING&quot;</span><span class="p">,</span>
<a name="cl-49"></a>        <span class="m">64</span> <span class="o">=&gt;</span> <span class="s2">&quot;E_COMPILE_ERROR&quot;</span><span class="p">,</span>
<a name="cl-50"></a>        <span class="m">128</span> <span class="o">=&gt;</span> <span class="s2">&quot;E_COMPILE_WARNING&quot;</span><span class="p">,</span>
<a name="cl-51"></a>        <span class="m">256</span> <span class="o">=&gt;</span> <span class="s2">&quot;E_USER_ERROR&quot;</span><span class="p">,</span>
<a name="cl-52"></a>        <span class="m">512</span> <span class="o">=&gt;</span> <span class="s2">&quot;E_USER_WARNING&quot;</span><span class="p">,</span>
<a name="cl-53"></a>        <span class="m">1024</span> <span class="o">=&gt;</span> <span class="s2">&quot;E_USER_NOTICE&quot;</span><span class="p">,</span>
<a name="cl-54"></a>        <span class="m">2048</span> <span class="o">=&gt;</span> <span class="s2">&quot;E_STRICT&quot;</span><span class="p">,</span>
<a name="cl-55"></a>        <span class="m">4096</span> <span class="o">=&gt;</span> <span class="s2">&quot;E_RECOVERABLE_ERROR&quot;</span><span class="p">,</span>
<a name="cl-56"></a>        <span class="m">8192</span> <span class="o">=&gt;</span> <span class="s2">&quot;E_DEPRECATED&quot;</span><span class="p">,</span>
<a name="cl-57"></a>        <span class="m">16384</span> <span class="o">=&gt;</span> <span class="s2">&quot;E_USER_DEPRECATED&quot;</span><span class="p">,</span>
<a name="cl-58"></a>    <span class="p">);</span>
<a name="cl-59"></a>        <span class="c1">// this function could use debug_backtrace() to obtain the whole stack for this error</span>
<a name="cl-60"></a>    <span class="nv">$stackdump</span> <span class="o">=</span> <span class="k">array</span><span class="p">();</span> 
<a name="cl-61"></a>    <span class="nv">$scriptlines</span> <span class="o">=</span> <span class="nb">explode</span><span class="p">(</span><span class="s2">&quot;</span><span class="se">\n</span><span class="s2">&quot;</span><span class="p">,</span> <span class="nb">file_get_contents</span><span class="p">(</span><span class="nv">$script</span><span class="p">));</span> 
<a name="cl-62"></a>    <span class="nv">$linenumber</span> <span class="o">=</span> <span class="nv">$errline</span><span class="p">;</span> 
<a name="cl-63"></a>    <span class="nv">$errorentry</span> <span class="o">=</span> <span class="k">array</span><span class="p">(</span><span class="s2">&quot;linenumber&quot;</span> <span class="o">=&gt;</span> <span class="nv">$linenumber</span><span class="p">,</span> <span class="s2">&quot;duplicates&quot;</span> <span class="o">=&gt;</span> <span class="m">1</span><span class="p">);</span> 
<a name="cl-64"></a>    <span class="nv">$errorentry</span><span class="p">[</span><span class="s2">&quot;file&quot;</span><span class="p">]</span> <span class="o">=</span> <span class="p">(</span><span class="nv">$errfile</span> <span class="o">==</span> <span class="nv">$script</span> <span class="o">?</span> <span class="s2">&quot;&lt;string&gt;&quot;</span> <span class="o">:</span> <span class="nv">$errfile</span><span class="p">);</span> 
<a name="cl-65"></a>    <span class="k">if</span> <span class="p">((</span><span class="nv">$linenumber</span> <span class="o">&gt;=</span> <span class="m">0</span><span class="p">)</span> <span class="o">&amp;&amp;</span> <span class="p">(</span><span class="nv">$linenumber</span> <span class="o">&lt;</span> <span class="nb">count</span><span class="p">(</span><span class="nv">$scriptlines</span><span class="p">)))</span>
<a name="cl-66"></a>        <span class="nv">$errorentry</span><span class="p">[</span><span class="s2">&quot;linetext&quot;</span><span class="p">]</span> <span class="o">=</span> <span class="nv">$scriptlines</span><span class="p">[</span><span class="nv">$linenumber</span><span class="p">];</span> 
<a name="cl-67"></a>    <span class="nv">$errcode</span> <span class="o">=</span> <span class="nv">$codes</span><span class="p">[</span><span class="nv">$errno</span><span class="p">];</span> 
<a name="cl-68"></a>
<a name="cl-69"></a>    <span class="nv">$stackdump</span><span class="p">[]</span> <span class="o">=</span> <span class="nv">$errorentry</span><span class="p">;</span> 
<a name="cl-70"></a>    <span class="k">return</span> <span class="k">array</span><span class="p">(</span><span class="s1">&#39;message_type&#39;</span> <span class="o">=&gt;</span> <span class="s1">&#39;exception&#39;</span><span class="p">,</span> <span class="s1">&#39;exceptiondescription&#39;</span> <span class="o">=&gt;</span> <span class="nv">$errcode</span><span class="o">.</span><span class="s2">&quot;  &quot;</span><span class="o">.</span><span class="nv">$errstr</span><span class="p">,</span> <span class="s2">&quot;stackdump&quot;</span> <span class="o">=&gt;</span> <span class="nv">$stackdump</span><span class="p">);</span> 
<a name="cl-71"></a><span class="p">}</span>
<a name="cl-72"></a>
<a name="cl-73"></a>
<a name="cl-74"></a><span class="cp">?&gt;</span><span class="x"></span>
</pre></div>
</td></tr></table>
		
		</div>
	
</div>



      </div>
    </div>

  </div>

  <div id="footer">
    <ul id="footer-nav">
      <li>Copyright © 2011 <a href="http://atlassian.com">Atlassian</a></li>
      <li><a href="http://www.atlassian.com/hosted/terms.jsp">Terms of Service</a></li>
      <li><a href="http://www.atlassian.com/about/privacy.jsp">Privacy</a></li>
      <li><a href="//bitbucket.org/site/master/issues/new">Report a Bug</a></li>
      <li><a href="http://confluence.atlassian.com/x/IYBGDQ">API</a></li>
      <li><a href="http://status.bitbucket.org/">Server Status</a></li>
    </ul>
    <ul id="social-nav">
      <li class="blog"><a href="http://blog.bitbucket.org">Bitbucket Blog</a></li>
      <li class="twitter"><a href="http://www.twitter.com/bitbucket">Twitter</a></li>
    </ul>
    <h5>We run</h5>
    <ul id="technologies">
      <li><a href="http://www.djangoproject.com/">Django 1.2.5</a></li>
      <li><a href="//bitbucket.org/jespern/django-piston/">Piston 0.3dev</a></li>
      <li><a href="http://www.selenic.com/mercurial/">Hg 1.8.2</a></li>
      <li><a href="http://www.python.org">Python 2.7.0</a></li>
      <li>r7394:a4196b9dafb6 | bitbucket01</li>
    </ul>
  </div>

  <script src="https://d1ga6s3xdhzo1c.cloudfront.net/6775e5f05d3f/js/lib/global.js"></script>






  <script src="//cdn.optimizely.com/js/4079040.js"></script>
  <script type="text/javascript">
    BB.gaqPush(['_trackPageview']);
  
    /* User specified tracking. */
    BB.gaqPush(
        ['repo._setAccount', 'UA-21451224-2'],
        ['repo._trackPageview']
    );
  
    BB.gaqPush(['atl._trackPageview']);

    

    

    (function () {
        var ga = document.createElement('script');
        ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
        ga.setAttribute('async', 'true');
        document.documentElement.firstChild.appendChild(ga);
    }());
  </script>

</body>
</html>
