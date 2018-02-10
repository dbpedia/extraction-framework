package org.dbpedia.extraction.nif;

import org.apache.commons.lang3.StringEscapeUtils;
import org.dbpedia.extraction.util.Language;
import org.dbpedia.iri.UriUtils;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class LinkExtractor implements NodeVisitor {

	private boolean inLink = false;
	private int skipLevel = -1;
	private List<Paragraph> paragraphs = null;
	private Paragraph paragraph = null;
    private Link tempLink;
	private boolean inSup = false;
	private boolean invisible = false;
    private NifExtractorContext context;
	private ArrayList<String> errors = new ArrayList<>();
	
	public LinkExtractor(NifExtractorContext context) {
        paragraphs = new ArrayList<Paragraph>();
		this.context = context;
	}
	
	/**
	 * Gets called when entering an element
	 * -handle text cleanup and remove Wikipedia specific stuff like reference numbers
	 * -if we encounter a link, we make a new nif:Word
	 * -we get the text out of a whitelist of elements. 
	 *   If we encounter a non-whitelisted element, we set this.skipLevel to the current depth 
	 *   of the dom tree and skip everything until we are back to that depth
	 * -this thing badly needs refactoring
	 */
	
	public void head(Node node, int depth) {

		if(skipLevel>=0)
			return;

        if(paragraph == null)
            paragraph = new Paragraph(0, "", "p");
		//ignore all content inside invisible tags
		if(invisible || node.attr("style").matches(".*display\\s*:\\s*none.*")) {
			invisible = true;
			return;
		}

		if(node.nodeName().equals("#text")) {
		  String tempText = node.toString();
		  //replace no-break spaces because unescape doesn't deal with them
		  tempText = StringEscapeUtils.unescapeHtml4(tempText);
          tempText = org.dbpedia.extraction.util.StringUtils.escape(tempText, replaceChars());
		  tempText = tempText.replace("\\n", "\n").replace("\\t", "\t").replace("\\r", "");

		  //this text node is the content of an <a> element: make a new nif:Word
		  if(inLink) {
              if(!tempText.trim().startsWith(this.context.wikipediaTemplateString + ":"))  //not!
              {
                  tempLink.setLinkText(tempText);
                  tempLink.setWordStart(paragraph.getLength() + (Paragraph.FollowedByWhiteSpace(paragraph.getText()) ? 1 : 0));
                  paragraph.addText(tempText);
                  tempLink.setWordEnd(paragraph.getLength());
              }
              else{                                            // -> filter out hidden links to the underlying template
				  errors.add("found Template in resource: " + this.context.resource + ": " + tempText);
				  return;
			  }
		  }
		  else
		    paragraph.addText(tempText);

		} else if(node.nodeName().equals("a")) {
            String link = node.attr("href");
            //remove internal links linking to mediawiki meta pages. Also removes links that contain ":".
            if (link.contains("mediawiki") && !link.contains(":")) {
                tempLink = new Link();
                String uri = cleanLink(node.attr("href"), false);
                setUri(uri);
            } else if (link.contains("mediawiki") && link.contains(":")) {

                if (!node.childNodes().isEmpty()) {
                    if (node.childNode(0).nodeName().equals("#text") &&
                            node.childNode(0).toString().contains(":") &&
                            !node.childNode(0).toString().contains("http")) {
                        tempLink = new Link();
                        String uri = cleanLink(node.attr("href"), false);
                        setUri(uri);
                    }
                } else {
                    skipLevel = depth;
                }

            } else if (node.attr("class").equals("external text")) {
                //don't skip external links
                tempLink = new Link();
                String uri = cleanLink(node.attr("href"), true);
                setUri(uri);

            } else {
                skipLevel = depth;
            }
        } else if(node.nodeName().equals("p")) {
            if(paragraph != null) {
                addParagraph("p");
            }
            else
                paragraph = new Paragraph(0, "", "p");
		} else if(node.nodeName().equals("sup")) {
			inSup = true;
        } else if(node.nodeName().matches("h\\d")) {
            addParagraph(node.nodeName());
        } else if(node.nodeName().equals("table")) {
            addParagraph("table");
			paragraph.addStructure(paragraph.getLength(), node.outerHtml(), "table", node.attr("class"), node.attr("id"));
            addParagraph("p");
            skipLevel = depth;
        } else if(node.nodeName().equals("span")) {
			//denote notes
		    if(node.attr("class").contains("notebegin"))
                addParagraph("note");

        } else if(node.nodeName().equals("math"))  {
            addParagraph("math");
            paragraph.addStructure(paragraph.getLength(), node.outerHtml(), "math", "tex", null);
            addParagraph("p");
            skipLevel = depth;
        }
	}

	private void setUri(String uri) {
		if(uri!=null) {
            tempLink.setUri(uri);
            tempLink.setExternal(true);
            inLink = true;
        } else {
            tempLink = new Link();
        }
	}
	
	private String cleanLink(String uri, boolean external) {
		if(!external) {
			if(!this.context.language.equals("en")) {

				uri="http://"+this.context.language+".dbpedia.org/resource/"+uri.substring(uri.indexOf("?title=")+7);
				
			} else {
				uri="http://dbpedia.org/resource/"+uri.substring(uri.indexOf("?title=")+7);
			}
			uri = uri.replace("&action=edit&redlink=1", "");
			
		} else {
			//there are links that contain illegal hostnames
			try {

				if(uri.startsWith("//"))
					uri = "http:"+uri;
				uri = URLEncoder.encode(uri,"UTF-8");
				uri = uri.replace("%3A", ":").replace("%2F", "/").replace("%2E", ".");
					
			} catch(UnsupportedEncodingException e) {
				//this doesn't happen
				e.printStackTrace();
			}
		}

		return UriUtils.uriToDbpediaIri(uri).toString();
	}
	
	public void tail(Node node, int depth) {
		
		if(skipLevel>0) {
			if(skipLevel==depth) {
				skipLevel = -1;
				return;
			} else {
				return;
			}
		}

		if(node.nodeName().equals("a")&&inLink) {
			inLink = false;
			paragraph.addLink(tempLink);
			tempLink = new Link();
		}
		else if(invisible && node.attr("style").matches(".*display\\s*:\\s*none.*")) {
            invisible = false;
        }
        else if(node.nodeName().equals("p") && paragraph != null) {
            addParagraph("p");
        }
        else if(node.nodeName().equals("sup") && inSup) {
			inSup = false;
		}
        else if(node.nodeName().matches("h\\d")) {
            addParagraph("p");
        }
        else if(node.nodeName().equals("span")) {
            if(node.attr("class").contains("noteend"))
                addParagraph("p");
        }
	}
	
	public List<Paragraph> getParagraphs() {
		if(paragraph != null && paragraph.getLength() > 0)
        {
            paragraphs.add(paragraph);
            paragraph = null;
        }
        return paragraphs;
	}

	private void addParagraph(String newTag){
	    if(paragraph.getLength() != 0 || paragraph.getHtmlStrings().size() > 0)
            paragraphs.add(paragraph);

	    paragraph = new Paragraph(0, "", (newTag == null ? "p" : newTag));
    }

	public int getTableCount(){
		int count =0;
		for(Paragraph p : this.getParagraphs()){
			count += paragraph.getHtmlStrings().size();
		}
		return count;
	}

	public ArrayList<String> getErrors(){
		return errors;
	}

    private String[] replaceChars() {
        String[] rep = new String[256];
        rep['\n'] = "";
        rep['\u00A0'] = " ";
        return rep;
    }

    public static class NifExtractorContext {
        private String language;
        private String resource;
        private String wikipediaTemplateString;

        public NifExtractorContext(Language language, String resource, String templateString){
            this.language = language.wikiCode();
            this.resource = resource;
            this.wikipediaTemplateString = templateString;
        }
    }
}
