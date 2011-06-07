package org.dbpedia.extraction.live.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public class XMLUtil
{
	private static Logger logger = Logger.getLogger(XMLUtil.class);
	
	public static String toString(Node node)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try {
			toText(node, baos);
		}
		catch(Exception e) {
			logger.error(ExceptionUtil.toString(e));
			return null;
		}

		return baos.toString();
	}
	
	public static Document createFromString(String text)
		throws Exception
	{
		return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse
			(new InputSource(new StringReader(text)));
	}

	public static void toText(Node node, OutputStream out)
		throws TransformerFactoryConfigurationError, TransformerException
	{
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		Source source = new DOMSource(node);
		Result output = new StreamResult(out);
		transformer.transform(source, output);
	}

	public static Document openFile(File file)
		throws Exception
	{
		InputStream inputStream = new FileInputStream(file);
		
		Document result = loadFromStream(inputStream);		
		inputStream.close();
		return result;
	}
	
	public static Document openUrl(String location)
		throws Exception
	{
		URL url = new URL(location);
		//System.out.println(url);
	
		URLConnection con = url.openConnection();
		InputStream responseStream = con.getInputStream();
		
		Document result = loadFromStream(responseStream);		
		responseStream.close();
		return result;
	}
	
	public static Document loadFromStream(InputStream inputStream)
		throws Exception
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setNamespaceAware(false);
	    
		DocumentBuilder builder = factory.newDocumentBuilder();
		builder.setEntityResolver(new EntityResolver() {
	
	        @Override
	        public InputSource resolveEntity(String publicId, String systemId)
	                throws SAXException, IOException {
	            //System.out.println("Ignoring " + publicId + ", " + systemId);
	            return new InputSource(new StringReader(""));
	        }
	    });
		
		//System.out.println(StringUtil.toString(inputStream));
		
		Document doc = builder.parse(inputStream);		
		return doc;
	}

    public static String getPageModificationDate(Document node)
	{
		//ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String strModificationDate = null;

		try {
                strModificationDate = node.getElementsByTagName("datestamp").item(0).getTextContent();
//			Transformer transformer = TransformerFactory.newInstance().newTransformer();
//		Source source = new DOMSource(node);
//
//		Result output = new StreamResult(baos);
//		transformer.transform(source, output);
            
            //System.out.println("NODE = " + node.getTextContent());
            //NodeList l = node.getElementsByTagName("metadata");
            //Element author = (Element) authors.item(j);
//            System.out.println("//////////////////////////////////////////////////////////////////");
//            System.out.println("List = " + node.getElementsByTagName("datestamp").item(0).getTextContent());
//            DocumentBuilderFactory factory =
//                        DocumentBuilderFactory.newInstance();
//            DocumentBuilder builder = factory.newDocumentBuilder();
//            Document document = builder.parse(new InputSource(new StringReader(baos.toString())));
//            System.out.println(document.getChildNodes().item(0).getNodeType());
//            System.out.println("//////////////////////////////////////////////////////////////////");

		}
		catch(Exception e)
        {
			logger.error(ExceptionUtil.toString(e));
		}

		return strModificationDate;
	}
	
}
