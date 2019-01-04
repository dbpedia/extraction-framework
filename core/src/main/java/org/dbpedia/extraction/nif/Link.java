package org.dbpedia.extraction.nif;

public class Link implements Comparable<Link> {

	private String uri = "";
	private String linkText = "";
	private int wordStart = 0;
	private int wordEnd = 0;
	private boolean doesExist = false;
	private boolean external = false;
	private boolean topicLink = false;
	private boolean topicPartLink = false;
	private boolean surfaceFormLink = false;
	
	public Link() {
		
	}
	
	public boolean isSurfaceFormLink() {
		return surfaceFormLink;
	}

	public void setSurfaceFormLink(boolean surfaceFormLink) {
		this.surfaceFormLink = surfaceFormLink;
	}

	public boolean isTopicLink() {
		return topicLink;
	}

	public void setTopicLink(boolean topicLink) {
		this.topicLink = topicLink;
	}

	public boolean isTopicPartLink() {
		return topicPartLink;
	}

	public void setTopicPartLink(boolean topicPartLink) {
		this.topicPartLink = topicPartLink;
	}

	public boolean isDoesExist() {
		return doesExist;
	}

	public void setDoesExist(boolean doesExist) {
		this.doesExist = doesExist;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getLinkText() {
		return linkText;
	}

	public void setLinkText(String linkText) {
		this.linkText = linkText;
	}

	public int getWordStart() {
		return wordStart;
	}

	public void setWordStart(int wordStart) {
		this.wordStart = wordStart;
	}

	public int getWordEnd() {
		return wordEnd;
	}

	public void setWordEnd(int wordEnd) {
		this.wordEnd = wordEnd;
	}

	public boolean isExternal() {
		return external;
	}

	public void setExternal(boolean external) {
		this.external = external;
	}

	@Override
	public int compareTo(Link link) {
		// TODO Auto-generated method stub
		if(this.wordStart==link.getWordStart()) 
			return 0;
		else if(this.wordStart<link.getWordStart())
			return -1;
		else
			return 1;
	}
	
}
