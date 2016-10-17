package org.dbpedia.extraction.nif;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

//import com.hp.hpl.jena.rdf.model.*;
//import com.hp.hpl.jena.vocabulary.RDF;
import org.apache.commons.lang3.StringEscapeUtils;

public class NIFCorpusSurfaceFormEnricher {

	/*private File corpusFolder;
	private File[] corpusFiles;
	private Map<String, List<String>> surfaceForms = new HashMap<String, List<String>>();
	//this should probably replaced with proper tokenization
	private Set<String> tokenMarkers = new HashSet<String>();
	
	public NIFCorpusSurfaceFormEnricher(String corpusFolder, String surfaceForms) {
		this.corpusFolder = new File(corpusFolder);
		if(this.corpusFolder.isDirectory()) {
			this.corpusFiles = this.corpusFolder.listFiles();
		} else {
			System.out.println(corpusFolder + " is not a folder");
		}

		System.out.println("loading surface forms");
		this.surfaceForms = loadSurfaceForms(new File(surfaceForms));
		System.out.println("loaded "+this.surfaceForms.size()+" surface forms");
		
		//lol
		tokenMarkers.addAll(Arrays.asList(" s,s.s;s:s[s]s/s-s+s*s\"s's´s`s!s?s%s&s(s)s".split("s")));
	}
	
	public Model loadIntoModel(File corpusFile, Model baseModel) {
		try {
			InputStream in = null;
			if(corpusFile.toString().endsWith("gz")) {
				in = new GZIPInputStream(new FileInputStream(corpusFile));
			} else if(corpusFile.toString().endsWith("ttl")) {
				in = new FileInputStream(corpusFile);
			} else
				return null;
			baseModel.read(in,null, "TURTLE");
			return baseModel;
		} catch(IOException ioe) {
			System.out.println("Can't read "+corpusFile.toString());
		}
		return null;
	}
	
	private boolean isToken(String word, String abstractText, int start) {
		boolean markerInFront = false;
		boolean markerInBack = false;
		if(start>0) {
			//has token marker before start
			if(this.tokenMarkers.contains(abstractText.substring(start-1,start))) {
				markerInFront = true;
			//does not need to be its own token if it's a distinct part of a word
			} else if(word.startsWith("-")) {
				markerInFront = true;
			}
		} else {
			markerInFront = true;
		}
		
		if(abstractText.length()>start+word.length()+1) {		
			if(this.tokenMarkers.contains(abstractText.substring(start+word.length(),start+word.length()+1))) {
				markerInBack = true;
			}
		} else {
			markerInBack = true;
		}
		
		return markerInFront&&markerInBack;
	}

	public boolean overlaps(int firstStart, int firstEnd, int secondStart, int secondEnd) {
		if(firstEnd>secondStart&&firstEnd<=secondEnd)
			return true;
		else if(firstStart>=secondStart&&firstStart<secondEnd)
			return true;
		else
			return false;
	}
	
	// add links to words that represent surface forms of the article topic. 
	// i.e. links to mentions of "Leipzig" in the article on "Leipzig"
	// only add links of no other link (overlapping) is set there
	public List<Link> addArticleTopicLinks(String abstractText, String linkUri, List<Link> originalLinks) {
		List<Link> topicLinks = new ArrayList<Link>();
		if(this.surfaceForms.containsKey(linkUri)) {
			for(String surface : this.surfaceForms.get(linkUri)) {
				int start = abstractText.indexOf(surface);
				while (start >= 0) {
					if(isToken(surface, abstractText, start)) {	
						boolean overlapping = false;
						for(Link oLink : originalLinks) {
							if(overlaps(start,start+surface.length(),oLink.getWordStart(),oLink.getWordEnd())) {
								overlapping = true;
								break;
							}
						}
						if(!overlapping) {
							Link surfaceFormLink = new Link();
							surfaceFormLink.setLinkText(surface);
							surfaceFormLink.setUri(encodeUri(linkUri));
							surfaceFormLink.setWordStart(start);
							surfaceFormLink.setWordEnd(start+surface.length());
							surfaceFormLink.setSurfaceFormLink(true);
							topicLinks.add(surfaceFormLink);
						}
					}
					start = abstractText.indexOf(surface, start + 1);
				}
			}
		} else {
			//maybe fallback to find surface form
		}
		
		return topicLinks;
	}
	
	// add links to words that represent surface forms of concepts linked once in the text
	// i.e. a link to the word "Leipzig" in an arbitrary article if "Leipzig" was linked in the text before
	// again, only add link of there are no overlapping links
	public List<Link> addSurfaceFormLinks(String abstractText, List<Link> originalLinks) {
		List<Link> enrichedLinks = new ArrayList<Link>();
		for(Link originalLink : originalLinks) {
			String linkUri = originalLink.getUri();
			if(this.surfaceForms.containsKey(linkUri)) {
				for(String surface : this.surfaceForms.get(linkUri)) {
					//start searching after originalLink's starting position
					//original link establishes meaning, so don't search before it
					int start = abstractText.indexOf(surface, originalLink.getWordStart());
					while (start >= 0) {
						if(isToken(surface, abstractText, start)) {	
							boolean overlapping = false;
							for(Link oLink : originalLinks) {
								if(overlaps(start,start+surface.length(),oLink.getWordStart(),oLink.getWordEnd())) {
									overlapping = true;
									break;
								}
							}
							if(!overlapping) {
								Link surfaceFormLink = new Link();
								surfaceFormLink.setLinkText(surface);
								surfaceFormLink.setUri(encodeUri(linkUri));
								surfaceFormLink.setWordStart(start);
								surfaceFormLink.setWordEnd(start+surface.length());
								surfaceFormLink.setSurfaceFormLink(true);
								enrichedLinks.add(surfaceFormLink);
							}
						}
						start = abstractText.indexOf(surface, start + 1);
					}
				}
			}
		}
		
		return enrichedLinks;
	}
	
	public Map<String, List<String>> loadSurfaceForms(File surfaceForms) {
		Map<String, List<String>> linkToSurface = new HashMap<String, List<String>>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(surfaceForms));
			String line = "";
			while( (line = reader.readLine()) !=null) {
				String[] split = line.split("\t");
				if(split.length==3) {
					if(linkToSurface.containsKey(split[1])) {
						linkToSurface.get(split[1]).add(split[0]);
					} else {
						List<String> surfaces = new ArrayList<String>();
						surfaces.add(split[0]);
						linkToSurface.put(split[1],surfaces);
					}
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return linkToSurface;
	}
	
	public String getTitle(String uri, boolean hasLangPrefix) {
		try {
			uri = URLDecoder.decode(uri, "UTF-8");
			if(hasLangPrefix)
				uri = uri.substring(31, uri.lastIndexOf("/abstract"));
			else
				uri = uri.substring(29, uri.lastIndexOf("/abstract"));
			uri = uri.replace("_", " ");
			if(uri.contains("(")&&uri.contains(")")) {
				uri = uri.substring(uri.indexOf("(")).trim();
			}
			return uri;
		} catch (UnsupportedEncodingException u) {
			//this does not happen
			System.out.println("System does not support UTF-8");
		}
		return null;
	}
	
	public String getPlainUri(String uri) {
		try {
			uri = URLDecoder.decode(uri, "UTF-8");
			uri = StringEscapeUtils.unescapeJava(uri);
			if(uri.contains("/abstract"))
				uri = uri.substring(0, uri.lastIndexOf("/abstract"));
			return uri;
		} catch (UnsupportedEncodingException u) {
			//this does not happen
			System.out.println("System does not support UTF-8");
		} catch (Exception e) {
			System.out.println(uri);
		}
		return null;
	}
	
	public String encodeUri(String plainUri) {
		try {
			return URLEncoder.encode(plainUri,"UTF-8")
				.replace("%2F", "/").replace("%5F", "_").replace("%3A", ":").replace("%2E",".");
		} catch(UnsupportedEncodingException u) {
			//this doesn't happen
		}
		return plainUri;
	}
	
	public List<Link> enrichLinks(String abstractString, String plainUri, List<Link> originalLinks) {
		
		//add links for the topic first
		List<Link> links = addArticleTopicLinks(abstractString, plainUri, originalLinks); 
		//add surface form links for original links now
		links.addAll(addSurfaceFormLinks(abstractString, originalLinks));
		//now add original links
		//encode the uri to be sure
		for(Link orig : originalLinks) {
			orig.setUri(encodeUri(orig.getUri()));
			links.add(orig);
		}
		//sort by start index
		Collections.sort(links);
		return links;
	}
	
	public void enrichCorpus(WikiCorpusGenerator gen, String outpath, String lang) {
		int totalLinks = 0;
		int total = 0;
		long tempTime = System.currentTimeMillis();
		float totalTime = 0;
		DecimalFormat df = new DecimalFormat("#,###,##0.00");
		String notParsed = "";
		for(File file : corpusFiles) {
			if(file.isFile()) {
				try {
					System.out.println("loading model file: "+file.toString());
					Model baseModel = ModelFactory.createDefaultModel();
					Resource contextClass = baseModel.createResource("http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#Context");
					Property referenceContext = baseModel.createProperty("http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#referenceContext");
					Property isString = baseModel.createProperty("http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#isString");
					Property anchorOf = baseModel.createProperty("http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#anchorOf");
					Property identRef = baseModel.createProperty("http://www.w3.org/2005/11/its/rdf#taIdentRef");
					Property beginIndex = baseModel.createProperty("http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#beginIndex");
					Property endIndex = baseModel.createProperty("http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#endIndex");
					baseModel = loadIntoModel(file, baseModel);
					if(baseModel == null)
						continue;
					String outfileName = file.getName().substring(0, file.getName().length()-3);
					BufferedWriter writer = new BufferedWriter(new FileWriter(outpath+outfileName));
					writer.append(gen.makePrefixString());
					
					StringBuilder fileString = new StringBuilder();
					fileString.append(gen.makePrefixString());
					
					//for all context resources of the file = all abstracts
					ResIterator cit = baseModel.listSubjectsWithProperty(RDF.type, contextClass);
					while(cit.hasNext()) {
						total ++;
						if(total%1000==0) {
							tempTime = System.currentTimeMillis() - tempTime;
							totalTime += tempTime;
							System.out.println(lang+": "+total+" in "+df.format(totalTime/60000)+" (1000 in "+(float)tempTime/1000+" ms, "+ totalTime/total +" avg)");
							tempTime = System.currentTimeMillis();		
						}
						Resource contextRes = cit.next();
						String abstractString = (String) baseModel.listObjectsOfProperty(contextRes, isString).next().asLiteral().getValue();
						String plainUri = getPlainUri(contextRes.getURI());
						if(plainUri==null)
							continue;
						int contextEnd = (int) baseModel.listObjectsOfProperty(contextRes, endIndex).next().asLiteral().getValue();
						List<Link> links = new ArrayList<Link>();
	
	//					System.out.println(abstractString);
						
						ResIterator lit = baseModel.listSubjectsWithProperty(referenceContext, contextRes);
						List<Link> originalLinks = new ArrayList<Link>();
						while(lit.hasNext()) {
							Resource linkRes = lit.next();
							String surface = baseModel.listObjectsOfProperty(linkRes, anchorOf).next().asLiteral().toString();
							String linkUri = getPlainUri(baseModel.listObjectsOfProperty(linkRes, identRef).next().toString());
							if(linkUri==null)
								continue;
							int start = (int)baseModel.listObjectsOfProperty(linkRes, beginIndex).next().asLiteral().getValue();
							int end = (int) baseModel.listObjectsOfProperty(linkRes, endIndex).next().asLiteral().getValue();
							Link oLink = new Link();
							oLink.setLinkText(surface);
							oLink.setUri(linkUri);
							oLink.setWordStart(start);
							oLink.setWordEnd(end);
							originalLinks.add(oLink);
						}
						
						links = enrichLinks(abstractString, plainUri,originalLinks);
						totalLinks += links.size();
						String contextTurtle = gen.makeContext(abstractString, encodeUri(plainUri), contextEnd);
						String linkTurtle = gen.makeWordsFromLinks(links, encodeUri(plainUri), contextEnd);
						writer.append(contextTurtle);
						writer.append(linkTurtle);
						writer.flush();
					}
					writer.close();
					baseModel.close();
				} catch(Exception e) {
					//something went wrong here
					notParsed+=file.getName()+"\n";
					notParsed+=e.getMessage()+"\n";
					e.printStackTrace();
				}
			}
		}
		
		System.out.println("errors in: ");
		System.out.println(notParsed);
		System.out.println("Triples: "+gen.getTriples());
		System.out.println("Number of Links: "+totalLinks);
		System.out.println("Runtime :"+df.format(totalTime/60000)+" min");
	}
	
	public static void main(String[] args) throws Exception {
		String corpusFolder = args[0];
		String surfaceForms = args[1];
		NIFCorpusSurfaceFormEnricher rich = new NIFCorpusSurfaceFormEnricher(corpusFolder,
				surfaceForms);
		//lang as 2 letter string as usual
		String lang = args[2];
		String logfile = "./"+lang+"_enrich.log";
		String outpath = args[3];
		WikiCorpusGenerator gen = new WikiCorpusGenerator(lang, logfile);
		rich.enrichCorpus(gen, outpath, lang);
	}*/
}