package org.dbpedia.extraction.live.feeder;



public class DBpediaQLUtil
{
	/*
	public static String deleteSubResourcesByPageId(int pageId, String dataGraphName)
	{
		return
			"Delete From <" + dataGraphName + ">\n" +
			"{\n" +
				"?sub ?x ?y \n" +
			"}\n" +
			"From <" + dataGraphName + "> {\n" +
				"?sourcePage <" + MyVocabulary.DBM_PAGE_ID + "> " + pageId + "^^xsd:integer .\n" +
				"?sourcePage ?p ?sub . \n" +
				"?sub ?x ?y . \n" +
				"Filter(?sub Like <?sourcePage%>) . " +
			"}\n";		
	}
	*/
	
	public static String deleteSubResourcesByPageId(String sourcePage, String dataGraphName)
	{
		return
			"Delete From <" + dataGraphName + ">\n" +
			"{\n" +
				"?sub ?x ?y \n" +
			"}\n" +
			"From <" + dataGraphName + "> {\n" +
				"<" + sourcePage + "> ?p ?sub . \n" +
				"?sub ?x ?y . \n" +
				"Filter(?sub Like <" + sourcePage + "/%>) . " +
			"}\n";		
	}

	public static String deleteDataByPageId(String sourcePage, String dataGraphName)
	{
		return
			"Delete From <" + dataGraphName + ">\n" +
			"{\n" +
				"<" + sourcePage + "> ?p ?o \n" +
			"}\n" +
			"{\n" +
				"<" + sourcePage + "> ?p ?o \n" +
			"}\n";
	}
	
	public static String getResourceByPageId(int pageId, String dataGraphName)
	{
		return
			"Select ?s \n" +
			"	From <" + dataGraphName + "> \n" +
			"{\n" +
			"	?s <" + MyVocabulary.DBM_PAGE_ID + "> \"" + pageId + "\"^^xsd:integer .\n" +
			"}";
	}

	
	
	
	/*
	public static String deleteDataByPageId(int pageId, String dataGraphName)
	{
		return
			"Delete From <" + dataGraphName + ">\n" +
			"{\n" +
				"?s ?p ?o \n" +
			"}\n" +
			"From <" + dataGraphName + "> {\n" +
				"?s <" + MyVocabulary.DBM_PAGE_ID + "> " + pageId + "^^xsd:integer\n" +
			"}\n";
	}
	*/

	
	
	public static String deleteMetaBySourcePage(String sourcePage, String metaGraphName)
	{
		return
			"Delete From <" + metaGraphName + ">\n" +
			"{\n" +
				"?b ?x ?y\n" +
			"}\n" +
			"From <" + metaGraphName + "> \n" +
			"{\n" +					
				"?b <" + MyVocabulary.DBM_SOURCE_PAGE + "> <" + sourcePage + ">  .\n" +
				"?b ?x ?y .\n" +
			"}\n";
	}
	
	public static String deleteDataBySourcePage(String sourcePage, String dataGraphName, String metaGraphName)
	{
		return
			"Delete From <" + dataGraphName + ">\n" +
			"{\n" +
				"?s ?p ?o\n" +
			"}\n" +
			"From <" + metaGraphName + "> {\n" +
				"?b <" + MyVocabulary.DBM_SOURCE_PAGE + "> <" + sourcePage + "> .\n" +
				"?b <" + MyVocabulary.OWL_ANNOTATED_SOURCE + "> ?s .\n" +
				"?b <" + MyVocabulary.OWL_ANNOTATED_PROPERTY + "> ?p .\n" +
				"?b <" + MyVocabulary.OWL_ANNOTATED_TARGET + "> ?o .\n" +
			"}\n";
	}
}
