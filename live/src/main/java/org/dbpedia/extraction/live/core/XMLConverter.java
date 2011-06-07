package org.dbpedia.extraction.live.core;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
//import org.jbind.xml.dom3.core.DomDocument;
//import org.jbind.xml.dom3.core.DomElement;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.bind.attachment.AttachmentMarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Jul 2, 2010
 * Time: 5:05:08 PM
 * This class is originally created by Christian W�rker <christian.wuerker@ceus-media.de> and Norman Heino <norman.heino@gmail.com>
 * Converts Virtuoso-specific SPARQL results XML format into an
 * array representation that can be further processed.
 *
 * The array structure conforms to what you would get by
 * applying json_decode to a JSON-encoded SPARQL result set
 * (@link {http://www.w3.org/TR/rdf-sparql-json-res/}).
 */
public class XMLConverter{

    //Initializing the Logger
    private static Logger logger = null;

    /**
     * @var array
     */
    protected HashMap _namespaces = new HashMap();
    //_namespaces.put("xml", "http://www.w3.org/XML/1998/namespace");

    public static void init()
    {
        try
        {
            logger = Logger.getLogger(Class.forName("XMLConverter").getName());
        }
        catch (Exception exp)
        {

        }
    }

    /**
     * Detects namespaces and prefixes from a DOM document
     *
     * @param document
     */
    public void detectNamespacesFromDocument(Document document)
    {

        NodeList nodes = document.getElementsByTagNameNS("*", "*");
        
        for (int i = 0; i<nodes.getLength(); i++) {
            Node node = nodes.item(i); 
            if (this._namespaces.get(node.getPrefix()) == null) {
                this._namespaces.put(node.getPrefix(),  node.getNamespaceURI());
                //this._namespaces[node.prefix] = node.namespaceURI;
            }
        }
    }

/**
     * Converts an XML result set to an array
     */
    public HashMap toArray(String xmlSparqlResults)
    {
        try{

            String xmlString;
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document xmlDoc = builder.parse(new InputSource(new StringReader(xmlSparqlResults)));

            /*DomDocument document = new DomDocument();
            document.createTextNode()
            //This is used to remove white spaces instead of document->preserveWhiteSpace = false used in php
            document.normalize();*/
            xmlDoc.normalize();

            this.detectNamespacesFromDocument(xmlDoc);

            ArrayList vars = new ArrayList();
            ArrayList bindings = new ArrayList();

            Node root = xmlDoc.getFirstChild();
            Node set = root.getFirstChild();

            NodeList setChildNodes = set.getChildNodes();
            for (int i=0; i< setChildNodes.getLength(); i++) {

                Node node = setChildNodes.item(i);

                if (!node.hasChildNodes()) {
                    continue;
                }

                HashMap row = new HashMap();
                NodeList nodeChildList = node.getChildNodes();

                for (int j = 0; j < nodeChildList.getLength(); j++) {
                    Node binding = nodeChildList.item(j);
                    // exlude text nodes etc.
                    if (!(binding instanceof Element)) {
                        continue;
                    }

                    Attr attrKey    = ((Element) binding).getAttributeNodeNS(this._namespaces.get("rs").toString(), "name");
                    Element nodeValue  = (Element)binding.getFirstChild();
                    String dataKey = attrKey.getValue();

                    if(!vars.contains(dataKey)){
                        vars.add(dataKey);
                    }

                    HashMap attributes = new HashMap();
                    NamedNodeMap nodeAttributeList = nodeValue.getAttributes();
                    for (int k = 0; k < nodeAttributeList.getLength(); k++) {
                        Node attribute = nodeAttributeList.item(i);
                        attributes.put(attribute.getNodeName(), attribute.getNodeValue());
                    }

                    if(attributes.containsKey("resource")){

                        HashMap tempHashmap = new HashMap();
                        tempHashmap.put("value", nodeValue.getAttributeNodeNS(this._namespaces.get("rdf").toString(), "resource").getValue());
                        tempHashmap.put("type", "uri");

                        row.put(dataKey, tempHashmap);

                    }
                    else if(attributes.containsKey("nodeID")){

                        HashMap tempHashmap = new HashMap();
                        tempHashmap.put("value", nodeValue.getAttributeNodeNS(this._namespaces.get("rdf").toString(), "nodeID").getValue());
                        tempHashmap.put("type", "bnode");

                        row.put(dataKey, tempHashmap);

                    }
                    else{

                        // literal
                        Attr lang     = nodeValue.getAttributeNodeNS(this._namespaces.get("xml").toString(), "lang");
                        Attr datatype = nodeValue.getAttributeNodeNS(this._namespaces.get("rdf").toString(), "datatype");

                        HashMap tempHashmap = new HashMap();
                        tempHashmap.put("value", nodeValue.getTextContent());
                        //TODO this statement is not as the original one in php ('type' => $datatype ? 'typed-literal' : 'literal')
                        tempHashmap.put("type", datatype.getValue());

                        row.put(dataKey, tempHashmap);

                        if (datatype != null) {
                            
                            HashMap datatypeHashMap = (HashMap)row.get(dataKey);
                            datatypeHashMap.put("datatype", datatype.getValue().toString());
                        }

                        if (lang != null) {
                            HashMap datatypeHashMap = (HashMap)row.get(dataKey);
                            datatypeHashMap.put("xml:lang", lang.getValue().toString());
                        }
                    }
                }

                // add row
                bindings.add(row);
            }

            HashMap result = new HashMap();

            HashMap varsHashMap = new HashMap();
            varsHashMap.put("vars", vars);

            result.put("head", varsHashMap);
            result.put("bindings", bindings);

            return result;

        }
        catch(Exception exp){
            logger.error(exp.getMessage());
            return null;
        }

    }
}
