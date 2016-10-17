package org.dbpedia.extraction.nif;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.dbpedia.extraction.util.UriUtils;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

public class LinkExtractor implements NodeVisitor {

	private boolean inLink = false;
	private int skipLevel = -1;
	private String text = "";
	private List<Paragraph> paragraphs = null;
	private Paragraph paragraph = null;
    private Link tempLink;
	private int offset;
	private boolean inSup = false;
	private String language = "";
	
	public LinkExtractor(int startOffset, String language) {
        paragraphs = new ArrayList<Paragraph>();
		offset = startOffset;
		this.language = language;
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

		if(skipLevel>0) {
			return;
		}

		if(node.nodeName().equals("#text")) {
		  String tempText = node.toString(); 
		  //replace no-break spaces because unescape doesn't deal with them
		  tempText = StringEscapeUtils.unescapeHtml4(tempText).replace("\u00A0", " ");
		  
		  //exclude spoken versions, citation numbers etc
		  if(tempText.equals("Listen")) {
		  	return;
		  } else if (tempText.matches("\\[[0-9?]+\\]")) {
		  	return;
		  } else if (tempText.matches("[0-9]+px")) {
			return;
		  }
		  
		  //remove superscripts with additional information. example: de/Leipzig
		  if(inSup) {
			  if(tempText.equals("?")||tempText.equals("i")||tempText.equals("/")||tempText.equals(",")||tempText.equals("|")) {
				  return;
			  }
		  }
		  
		  if(tempText.contains("\\")) {
			  tempText = tempText.replace("\\", "");
		  }
		  if(tempText.contains("\n")) {
			  tempText = tempText.replace("\n", "");
		  }
		  int escapeCount = 0;
		  if(tempText.contains("\"")) {
		  	tempText = tempText.replace("\"", "\\\"");
		  	escapeCount = StringUtils.countMatches(tempText, "\\");
		  }
		  int length = tempText.length();
		  length = length - escapeCount;

		  int beforeOffset = offset;
		  offset += length; 
	
		  //specific fix when there are two paragraphs following each other and the whitespace is missing
		  if(node.parent().nextSibling()!=null) {
			  if(node.parent().nodeName().equals("p")
					  &&node.parent().nextSibling().nodeName().equals("p")
					  &&!tempText.endsWith(" ")) {
				  tempText+=" ";
				  offset++;
			  }
		  }
		 
		  //this text node is the content of an <a> element: make a new nif:Word
		  if(inLink) {
			  if(tempText.endsWith(" ")) {
				  
				  tempText = tempText.substring(0, tempText.length()-1);
				  offset--;
			  }
			  tempLink.setLinkText(tempText);
			  tempLink.setWordStart(beforeOffset);
			  tempLink.setWordEnd(offset);
		  }
          if(paragraph == null)
              paragraph = new Paragraph(offset, "");
          paragraph.addText(tempText);
		  text += tempText;

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
            if(paragraph != null)
                paragraphs.add(paragraph);
            paragraph = new Paragraph(offset, "");
		} else if(node.nodeName().equals("code")||node.nodeName().equals("sub")||node.nodeName().equals("em")
				||node.nodeName().equals("i")||node.nodeName().equals("b")||node.nodeName().equals("dfn")||node.nodeName().equals("kbd")
				||node.nodeName().equals("tt")||node.nodeName().equals("abbr")||node.nodeName().equals("li")) {
			//don't skip the text in code, sup or sub texts
		} else if(node.nodeName().equals("sup")) {
			inSup = true;
		} else {
			//skip all other tags
			if(!node.nodeName().equals("span")) {
					skipLevel = depth;
			} else {
				if(node.attr("id").equals("coordinates")||node.attr("class").equals("audio")||node.attr("class").equals("noprint"))
					skipLevel = depth;
			}
			
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
			if(!this.language.equals("en")) {

				uri="http://"+this.language+".dbpedia.org/resource/"+uri.substring(uri.indexOf("?title=")+7);
				
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

		return UriUtils.uriToIri(uri);
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
		
		if(node.nodeName().equals("sup")&&inSup) {
			inSup = false;
		}
		    	
	}
	
	public String getText() {
		return text;
	}
	
	public List<Paragraph> getParagraphs() {
		if(paragraph != null)
        {
            paragraphs.add(paragraph);
            paragraph = null;
        }
        return paragraphs;
	}
	
	public int getOffset() {
		return offset;
	}

}
