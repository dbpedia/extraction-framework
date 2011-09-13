/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and Cambridge Semantics Incorporated.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * File:        $Source: /cvsroot/slrp/boca/com.ibm.adtech.boca.client.jena/src/com/ibm/adtech/boca/client/jena/Converter.java,v $
 * Created by:  Matthew Roy ( <a href="mailto:mroy@us.ibm.com">mroy@us.ibm.com </a>)
 * Created on:  Jan 15, 2007
 * Revision:	$Id: Converter.java 177 2007-07-31 14:22:31Z mroy $
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cambridge Semantics Incorporated - Fork to Anzo\n
 *******************************************************************************/

package org.dbpedia.extraction.live.helper;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.StatementImpl;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.impl.LiteralLabel;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;




/**
 * @author tgauss
 * @author oliver-maresch@gmx.de
 * @version $Id: SesameJenaUtilities.java,v 1.5 2006/09/02 20:59:00 cyganiak Exp $
 */
public class SesameJenaUtilities {

	final static public String s_bnodePrefix = "urn:bnode:";


    /**
     * Builds a Sesame Statement out of an Jena Triple.
     * @param t the jena triple
     * @param vf the ValueFactory of the Sesame RdfSource, where the Sesame Statement should be stored.
     * @return the Sesame Statement
     */
    public static Statement makeSesameStatement(Triple t, ValueFactory vf){
        Resource subj = makeSesameSubject(t.getSubject(), vf);
        URI pred = makeSesamePredicate(t.getPredicate(), vf);
        Value obj = makeSesameObject(t.getObject(), vf);
        return new StatementImpl(subj, pred, obj);
    }

    /**
     * Builds a Jena subject node from a Sesame Resource.
     */
    public static Node makeJenaSubject(Resource sub){
        if(sub == null){
            return Node.ANY;
        } else if(sub instanceof URI){
            URI uri = (URI) sub;
            if(uri.toString().startsWith("urn:bnode:")){
                return Node.createAnon(new AnonId(uri.toString().substring(10)));
            }else{
                return Node.createURI(uri.toString());
            }
        } else if(sub instanceof BNode){
            return Node.ANY;
        }
        return null;
    }

    /**
     * Builds a Jena predicate Node form a Sesame predicate URI.
     */
    public static Node makeJenaPredicate(URI pred){
        if(pred == null){
            return Node.ANY;
        } else {
            return Node.createURI(pred.toString());
        }
    }

    /**
     * Builds a Jena object Node from a Sesame object Value.
     */
    public static Node makeJenaObject(Value obj){
        Node objnode = null;
        if(obj == null){
            return Node.ANY;
        } else if (obj instanceof Literal) {
            Literal objlit = (Literal) obj;

            String objlabel = objlit.getLabel();
            String objlang = objlit.getLanguage();
            URI objDatatype = objlit.getDatatype();

            if(objDatatype != null  ){
                Model m = ModelFactory.createDefaultModel();
                RDFDatatype dt = null;
                dt = getTypedLiteral(objDatatype.toString());
                RDFNode o = m.createTypedLiteral(objlabel,dt);
                objnode = o.asNode();
            }
	     else if(objlang != null){
		objnode = Node.createLiteral(objlabel, objlang, false);
		}
		else{
                objnode = Node.createLiteral(objlabel, null, false);
            }
        }else{
            URI objuri = (URI)obj;
            if(objuri.toString().startsWith("urn:bnode:")){
                objnode = Node.createAnon(new AnonId(objuri.toString().substring(10)));
            }else{
                objnode = Node.createURI(objuri.toString());
            }
        }
        return objnode;
    }


    /**
     * Builds a Sesame subject Resource from a Jena subject Node.
     */
    public static Resource makeSesameSubject(Node subnode,ValueFactory myFactory){
        Resource mySubject = null;
        if(subnode.isBlank()){
            AnonId id = subnode.getBlankNodeId();
            String subid = id.toString();
            if(subid.startsWith(s_bnodePrefix)){
                mySubject = (URI) myFactory.createURI(subid);
            }else{
                mySubject = (URI) myFactory.createURI(s_bnodePrefix+subid);
            }
        }else{
            mySubject = (URI) myFactory.createURI(subnode.getURI());
        }

        return mySubject;
    }

    /**
     * Builds a Sesame predicate URI from a Jena predicate Node.
     */
    public static URI makeSesamePredicate(Node prednode,ValueFactory myFactory){
		return myFactory.createURI(prednode.getURI());
    }

    /**
     * Builds a Sesame object Value from a Jena object Node.
     */
    public static Value makeSesameObject(Node objnode,ValueFactory myFactory){
		Value myobj = null;
		if(objnode.isBlank()){
				AnonId id = objnode.getBlankNodeId();
				String objid = id.toString();
				if(objid.startsWith(s_bnodePrefix)){
					myobj = (URI) myFactory.createURI(objid);
				}else{
					myobj = (URI) myFactory.createURI(s_bnodePrefix+objid);
				}
		}else{
			if(objnode.isLiteral()){
				LiteralLabel liter = objnode.getLiteral();
				String label = liter.getLexicalForm();
				RDFDatatype dtype = liter.getDatatype();
				String dtypeURI = null;
				if(dtype != null){
					dtypeURI = dtype.getURI();
				}
				if(dtypeURI != null){
					myobj= myFactory.createLiteral(label,myFactory.createURI(dtypeURI));
				}else{
					if(liter.language()!= null){
						myobj = myFactory.createLiteral(label,liter.language());
					}else{
						myobj= myFactory.createLiteral(label);
					}
                }
			}else{
				myobj = (URI) myFactory.createURI(objnode.getURI());
			}
        }

        return myobj;
    }

    /**
	 * Returns the corresponding RDFDatatype to a given URI.
	 *
	 * @param dtype         the datatypes uri
	 * @return corresponding RDFDatatype
	 */
	private static RDFDatatype getTypedLiteral(String dtype){
		try{
		System.out.println("DATATYPE = " + dtype);
		RDFDatatype dt = TypeMapper.getInstance().getTypeByName(dtype);
		return dt;
		}
		catch(Exception exp){
		return null;
		}
	}

}