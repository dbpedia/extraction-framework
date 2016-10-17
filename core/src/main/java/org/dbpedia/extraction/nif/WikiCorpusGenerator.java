package org.dbpedia.extraction.nif;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
//import org.apache.commons.validator.routines.UrlValidator;
import org.dbpedia.extraction.util.FileLike;
import org.dbpedia.extraction.util.IOUtils;
import org.dbpedia.extraction.util.RichFile;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeTraversor;

/**
 * Extract NIF from Abstract triples including links
 * TODO: links that are plain text are not recognized AND exploded (www.wikipedia.de)
 * TODO: external data like wiki links and spotlight not in there
 * @author Martin Brümmer
 *
 */

public class WikiCorpusGenerator {

/*	private String abstractText = "";
	private String logfile = "";
	private boolean writePrefix = true;
	private int fileNumber = 0;
	private int triples = 0;
	private int abstractLength = 0;
	private int totalResources = 0;
	private int abstractsGenerated = 0;
	private long totalAbstractLength = 0;
	private long totalLinkCount = 0;
	private String language = "";
	boolean failure = false;
	
	public WikiCorpusGenerator(String language, String logfile) {
		this.language = language;
		this.logfile = logfile;
	}
	
	public Set<String> readResources(String fileName) {
		Set<String> resources = new HashSet<String>();
		BufferedReader br = null;
		try {
			InputStream in = null;
			if(fileName.endsWith("gz")) {
				in = new GZIPInputStream(new FileInputStream(fileName));
			} else {
				in = new FileInputStream(fileName);
			}
			br = new BufferedReader(new InputStreamReader(in));
			String line;
		
			while((line = br.readLine()) != null) {  
				resources.add(line.trim());
			}
			br.close();
		} catch(FileNotFoundException fnf) {
			System.out.println("File not found: " + fileName);
		} catch(IOException ioe) {
			System.out.println("Cannot read file: " + fileName);
		} 
		return resources;
	}
	
	public int getTriples() {
		return this.triples;
	}
	
	public void makeCorpusFromFile(String fileName, Set<String> resources, boolean writeData, int resourceCount, String outDir) {
		BufferedReader br;
		try {
			FileLike<File> file = new RichFile(new File(fileName));
			InputStream in = IOUtils.inputStream(file);
			br = new BufferedReader(new InputStreamReader(in));
			String line;
			
			int count = 0;
			int total = 0;
			long tempTime = System.currentTimeMillis();
			float totalTime = 0;
			DecimalFormat df = new DecimalFormat("#,###,##0.00");
			BufferedWriter failwriter = new BufferedWriter(new FileWriter(logfile+".fail",true));
			
			while((line = br.readLine()) != null) {  
				count ++;
				total ++;
				if(total%1000==0) {
					tempTime = System.currentTimeMillis() - tempTime;
					totalTime += tempTime;
					System.out.println(language+": "+total+" in "+df.format(totalTime/60000)+" (1000 in "+(float)tempTime/1000+" ms, "+ totalTime/total +" avg)");
					tempTime = System.currentTimeMillis();		
				}
					
*//*				if(count>40000) {
					count = 0;
					fileNumber++;
					writePrefix=true;
				}*//*
				
				String parse = parseLine(line, resources);
				
				if(failure) {
					failwriter.append(line.substring(0, line.indexOf(" ")));
					failwriter.newLine();
					failwriter.flush();
					failure = false;
				}
				
				if(parse!=null && writeData) {
					writeResources(parse, outDir+language+"/nif_abstracts_"+language);
					if(total>resourceCount)
						break;
				}
			}
			BufferedWriter logwriter = new BufferedWriter(new FileWriter(logfile,true));
			logwriter.append(language+":");
			logwriter.newLine();
			logwriter.append("Created "+fileNumber+" files containing "+triples+" triples in "+df.format(totalTime/60000)+" minutes.\n");
			logwriter.append("Total resources parsed: "+totalResources+" ,total links: "+totalLinkCount+"\n");
			logwriter.append("Abstracts generated: "+abstractsGenerated+"\n");
			logwriter.append("Average abstract length: "+(double)totalAbstractLength/abstractsGenerated+"\n");
			logwriter.append("Average links per abstract: "+(double)totalLinkCount/abstractsGenerated+"\n");
			logwriter.newLine();
			logwriter.flush();
			logwriter.close();
			failwriter.close();
			br.close();
		} catch(FileNotFoundException fnf) {
			System.out.println("File not found: " + fileName);
		} catch(IOException ioe) {
			System.out.println("Cannot read file: " + fileName);
		}
	}
	
	private String parseLine(String line, Set<String> resources) {
		
		int firstBreak = line.indexOf(" ");
		if(firstBreak <=1)
			return null;
		String uri = line.substring(1,firstBreak-1);
		uri = sanitizeUri(uri);
//		System.out.println(uri);
		if(resources!=null) {
			if(resources.contains(uri.trim())) {
				System.out.println("Found: "+uri);
			} else {
				return null;
			}
		}
		line = StringEscapeUtils.unescapeHtml4(line);
		line = removeFormattingMarkup(line);
		line = StringEscapeUtils.unescapeJava(line);	
		
		//firstbreak changed by unescaping unicode characters
		firstBreak = line.indexOf(" ");
		int secondBreak = line.indexOf(" ", firstBreak+1);
		
		String abstractString = line.substring(secondBreak+2).trim();
//		System.out.println(abstractString);
		this.abstractText = "";
		//extracting the paragraphs
		abstractString = getRelevantParagraphs(abstractString, uri);
//		System.out.println("Paragraph: "+abstractString);
		if(abstractString==null)
			return null;
		
		this.abstractLength = 0;
		List<Link> links = getLinkAndText(abstractString, uri);
		
//		System.out.println(abstractText);

		int contextEnd = abstractText.length();
		if(contextEnd==0)
			return null;
		if(links.size()==0)
			return null;
		
		totalLinkCount+=links.size();
		totalAbstractLength += abstractText.length();
		String context = this.makeContext(abstractText, uri, this.abstractLength);
		this.totalResources++;
		if(context==null)
			return null;
		this.abstractsGenerated++;
//		System.out.println(context);
	
		String words = this.makeWordsFromLinks(links, uri, this.abstractLength);
		
		if(!words.isEmpty()) {
			context = context + words;
		}
//			System.out.println(words);
		return context;
	}
	

	
	
	*//**//**
	 * Clean up URIs: Add language subdomain, do URI encoding
	 * @param uri 
	 * @return sanitized URI
	 *//**//*
	private String sanitizeUri(String uri) {
		try {
			uri = uri.replace("index.php?title=","");
			if(this.language!="en") {
				
				uri = uri.substring(("http://"+this.language+".dbpedia.org/resource/").length());
				if(uri.contains("\\u")) {
					uri = StringEscapeUtils.unescapeJava(uri);
					uri = "http://"+this.language+".dbpedia.org/resource/"+URLEncoder.encode(uri
							,"UTF-8")
							.replace("%2F", "/");
				}  else if (uri.contains("%")){
					uri = "http://"+this.language+".dbpedia.org/resource/"+uri;
				} else {
					uri = "http://"+this.language+".dbpedia.org/resource/"+URLEncoder.encode(uri
							,"UTF-8")
							.replace("%2F", "/");
				}
			}
			else {
				uri = uri.substring("http://dbpedia.org/resource/".length());
				uri = StringEscapeUtils.unescapeJava(uri);
				uri = "http://dbpedia.org/resource/"+URLEncoder.encode(uri
						,"UTF-8")
						.replace("%2F", "/");
			}
				
		} catch (Exception e) {
			//this does not happen
			e.printStackTrace();
		}
		return uri;
	}
	
	*//**//**
	 * Find the relevant paragraph of text, that is, the first paragraph after the info table, disambiguation links and quality control labels
	 * @param The full abstract html as extracted by DBpedia extraction framework.
	 * @return The relevant abstract paragraph HTML 
	 *//**//*

	private String getRelevantParagraphs(String abstractString, String resourceUri) {
		
//		System.out.println("Full abstract "+abstractString);
		abstractString = abstractString.replace("<p></p>", "");

		if(abstractString.contains("<ol class=\"references\">")) {
			abstractString = abstractString.substring(0,abstractString.lastIndexOf("<ol class=\"references\">"));
		} else {
			//this seems kinda wrong, because indexof("</p>") could be -1. Doesn't throw errors in 6 languages though, so whatever
			abstractString = abstractString.substring(0,Math.min(abstractString.lastIndexOf("</p>")+4,abstractString.length()));
		}
//		System.out.println(abstractString);
		return abstractString;
	}
	
	
	private String removeFormattingMarkup(String line) {
		line = line.replace("&apos;", "'");
		line = line.replace("<b>", "");
		line = line.replace("</b>", "");
		line = line.replace("<i>", "");
		line = line.replace("</i>", "");
		return line;
	}
	
	public void writeResources(String res, String outFile) {
		try {
			RichFile rf = new RichFile(new File(outFile + ".ttl.bz2"));
			BufferedWriter dumpWriter = new BufferedWriter(IOUtils.writer(rf, !writePrefix, Charset.defaultCharset()));
			if(writePrefix) {
				dumpWriter.append(makePrefixString());
				writePrefix = false;
			}
			dumpWriter.append(res);
			dumpWriter.flush();
			dumpWriter.close();

		} catch (FileNotFoundException fnf) {
			System.out.println("Could not write file "+outFile);
			fnf.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	*//**//**
	 * Extract the abstract text and a number of link objects
	 * @param The HTML string of the relevant abstract paragraphs 
	 * @return a list of Link objects containing the links, anchors and offsets. Abstract text is saved to class variable.
	 *//**//*
	
	private List<Link> getLinkAndText(String line, String resource) {
			
		List<Link> links = new ArrayList<Link>();
		//surrounding it with a span so I can skip top level elements (depth=0 in LinkExtractor)
		line = "<span>"+line+"</span>";
		Document doc = Jsoup.parse(line);
		String abstractText = "";
		List<Node> nodes = doc.select("body").first().childNodes();
		int offset = 0;
		
		for(Node elementNode : nodes) {
			//these are paragraphs
			if(elementNode.nodeName().equals("#text")) {
				String tempText = elementNode.toString().trim();
				if(tempText.isEmpty())
					continue;
				else
					tempText+=" ";
				//replace no break spaces with spaces, to make trim() and replace() work
				tempText = StringEscapeUtils.unescapeHtml4(tempText).replace("\u00A0", " ");
				if(tempText.contains("\\")) {
					tempText = tempText.replace("\\", "");
				}
				
				int escapeCount = 0;
				if(tempText.contains("\"")&&!tempText.trim().equals("\"")) {
					tempText = tempText.replace("\"", "\\\"");
					escapeCount = StringUtils.countMatches(tempText, "\\");
				} else if (tempText.trim().equals("\"")) {				
					tempText="";
				}
				
				tempText = cleanUpWhiteSpace(tempText);
				int textLength = tempText.length();
				textLength = textLength - escapeCount;	
				//special characters like punctuation shouldn't have whitespace before them, so remove it
				if((tempText.startsWith(",")||tempText.startsWith(".")||tempText.startsWith(";")||tempText.startsWith(":"))&&abstractText.endsWith(" ")) {
					abstractText = abstractText.substring(0, abstractText.length()-1);
					abstractText+=tempText;
					offset--;
				} else {
					abstractText+=tempText;
				}
				offset+=textLength;
			} else {		
				LinkExtractor extractor = new LinkExtractor(offset,this.language);
				NodeTraversor traversor = new NodeTraversor(extractor);
				traversor.traverse(elementNode);
				
				String tempText = extractor.getText().replace("\u00A0", " ");
				//german
				if(tempText.contains("Vorlage:")) {
					continue;
				}
				offset = extractor.getOffset();
				  
				abstractText+=tempText;
				if(!abstractText.isEmpty() && !abstractText.endsWith(" ")) {
					abstractText += " ";
					offset ++;
				}
				links.addAll(extractor.getLinks());		
			}
		}
		
		//abstracts may start with spaces if they start with links. 
		//This is a bug of the LinkExtractor I don't want to touch because it may break stuff for some languages.
		//abstracts starting with space need reprocessing of all links and their offsets to remove the space:
		//HACK:
		if(abstractText.startsWith(" ")) {
			
			abstractText = abstractText.substring(1);
			offset--;
			for(int i = 0; i<links.size(); i++) {
				Link link = links.get(i);
				if(link.getWordStart()==0&&link.getWordEnd()==0) {
					continue;
				}
				
				if(link.getWordStart()!=0) {
					link.setWordStart(link.getWordStart()-1);
				} else {
					if(link.getLinkText().startsWith(" ")) {
						link.setLinkText(link.getLinkText().substring(1));
					} else {
						failure = true;
						System.out.println("Error in Link: "+link.getUri());
						System.out.println(resource);
					}
				}
				
				if(link.getWordEnd()!=0) {
					link.setWordEnd(link.getWordEnd()-1);
					if(link.getLinkText().length()>0) {
						links.set(i, link);
					}
				}  else {
					failure = true;
					System.out.println("Error in Link: "+link.getUri());
					System.out.println(resource);
				}	
			}
		}
		
		int beforeTrim = 0;
		int offsetReduce = 0;
		if(!abstractText.startsWith(" ")&&abstractText.endsWith(" ")) {
			beforeTrim = abstractText.length();
			abstractText = abstractText.trim();
			if(beforeTrim>abstractText.length()) {
				offsetReduce = beforeTrim-abstractText.length();
			}
		}
		if(offsetReduce>0) {
			offset -= offsetReduce;
		}

		this.abstractLength = offset;
		this.abstractText = abstractText;
		return links;
	}

	private String cleanUpWhiteSpace(String tempText) {
		tempText = tempText.replace("( ", "(").replace("  ", " ").replace(" ,", ",").replace(" .", ".");
		return tempText;
	}

	public String makePrefixString() {
		String prefix = "";
		prefix += "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n";
		prefix += "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n";
		prefix += "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n";
		prefix += "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n";
		prefix += "@prefix nif: <http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#> .\n";
		prefix += "@prefix prov: <http://www.w3.org/ns/prov#> .\n";
		prefix += "@prefix itsrdf: <http://www.w3.org/2005/11/its/rdf#> .\n\n";
		return prefix;
	}
	
	public String makeContext(String text, String url, int contextEnd) {
		String context = "";
		//check for end, don't make resource if it is 0
		if(contextEnd==0)
			return null;
		context+="<"+url+"/abstract#offset_0_"+contextEnd+">\n";
		context+="\ta nif:String, nif:OffsetBasedString, nif:Context ;\n";
		context+="\tnif:isString \"\"\""+ text +"\"\"\"^^xsd:string;\n";
		context+="\tnif:beginIndex \"0\"^^xsd:int;\n";
		context+="\tnif:endIndex \""+ contextEnd +"\"^^xsd:int;\n";
		String sourceUri = "";
		if(this.language!="en")
			sourceUri= "http://"+this.language+".wikipedia.org/wiki/"+url.substring(("http://"+this.language+".dbpedia.org/resource/").length());
		else
			sourceUri= "http://"+this.language+".wikipedia.org/wiki/"+url.substring(("http://dbpedia.org/resource/").length());
		context+="\tnif:sourceUrl <"+sourceUri+"> .\n\n";
		triples += 7;
		return context;
	}
	
	public String makeWordsFromLinks(List<Link> links, String contextUri, int contextEnd) {
		String words = "";
		
		for(Link link : links) {
			if(link.getWordEnd()-link.getWordStart()==0) 
				continue;
			
			String turtle = "<"+contextUri+"/abstract#offset_"+link.getWordStart()+"_"+link.getWordEnd()+">\n";
			turtle+="\ta nif:String, nif:OffsetBasedString ;\n";
			turtle+="\tnif:referenceContext <"+contextUri+"/abstract#offset_0_"+contextEnd+"> ;\n";
			turtle+="\tnif:anchorOf \"\"\""+link.getLinkText()+"\"\"\"^^xsd:string ;\n";
			turtle+="\tnif:beginIndex \""+link.getWordStart()+"\"^^xsd:int ;\n";
			turtle+="\tnif:endIndex \""+link.getWordEnd()+"\"^^xsd:int ;\n";
			triples += 1;
			if(link.getLinkText().split(" ").length>1) { 
				turtle+="\ta nif:Phrase ;\n";
				triples += 1;
			}
			else {
				turtle+="\ta nif:Word ;\n";
				triples += 1;
			}		
			turtle += "\titsrdf:taIdentRef <"+link.getUri()+"> .\n\n";
			triples += 7;
			words += turtle;
		}
			
		return words;
	}
	
	public String cleanExternalLink(String link) {
		//there are links that contain illegal hostnames
		try {
			if(link.startsWith("//"))
				link = "http:"+link;
			link = URLEncoder.encode(link,"UTF-8");
			link = link.replace("%3A", ":").replace("%2F", "/").replace("%2E", ".");
			String[] schemes = {"http","https","irc","ftp"};
			UrlValidator val = new UrlValidator(schemes);
			if(!val.isValid(link)) {
				return null;
			}	
		} catch(UnsupportedEncodingException e) {
			//this doesn't happen
			e.printStackTrace();
		}
		
		return link;
	}
	
	//this kinda doubles with sanitizeUri but I don't have time to figure this out now
	public String cleanLink(String link) {
		if(!this.language.equals("en")) {
			if(!link.contains(this.language+".dbpedia.org")) {
				link=link.replace("dbpedia.org",this.language+".dbpedia.org");
			}
		}
		return link.replace("index.php?title=", "").replace("&action=edit&redlink=1", "");
	}
	
	//testing method
	public void readFast(String fileName, Set<String> resources, String outfile) {
		BufferedReader br = null;
		
		try {
			InputStream in = null;
			if(fileName.endsWith("gz")) {
				in = new GZIPInputStream(new FileInputStream(fileName));
			} else {
				in = new FileInputStream(fileName);
			}
			br = new BufferedReader(new InputStreamReader(in));
			String line;
			
			int count = 0;
			int total = 0;
			
			while((line = br.readLine()) != null) {  
				count ++;					
				if(count>10000) {
					total+=count;
					System.out.println(language+" "+total);
					count = 0;
				}
				int startLine = 0;
				if(total<startLine)
					continue;
//				line = StringEscapeUtils.unescapeJava(line);	
				
				int firstBreak = line.indexOf(" ");
				
				if(firstBreak <=1)
					continue;
				String uri = line.substring(1,firstBreak-1);
				uri = sanitizeUri(uri);
				
				//only parse relevant resources
				if(resources.contains(uri)){
					System.out.println(uri);
					String parse = parseLine(line, resources);
					System.out.println(parse);

						writeResources(parse, outfile);
						System.out.println("Found in line "+(total+count));
						System.out.println("Total resources parsed: "+totalResources+" ,total links: "+totalLinkCount);
						System.out.println("Abstracts generated: "+abstractsGenerated);
						System.out.println("Average abstract length: "+(double)totalAbstractLength/abstractsGenerated);
						System.out.println("Average links per abstract"+(double)totalLinkCount/abstractsGenerated);
						return;
//					}
				}
				
			}
			System.out.println("Created "+fileNumber+" files containing "+triples+" triples.");
			
			br.close();
		} catch(FileNotFoundException fnf) {
			System.out.println("File not found: " + fileName);
		} catch(IOException ioe) {
			System.out.println("Cannot read file: " + fileName);
		}
	}
	
	public static void main(String[] args) {
		String file = args[0];
		String language = args[1];
		String outfolder = args[2];
		String logfile = outfolder + "abstracts.log";
		WikiCorpusGenerator wiki = new WikiCorpusGenerator(language,logfile);
		wiki.makeCorpusFromFile(file, null, true, 40000, outfolder);

		//alternatively, if single resources are to be converted
//		Set<String> resources = new HashSet<String>();
//		resources.add("http://de.dbpedia.org/resource/Leipzig");
//		wiki.readFast("/media/martin/Elements/abstract_html/dewiki-20150515-long-abstracts.nt.gz", resources, "/media/martin/Elements/corpustest.ttl");

	}*/
	
}
