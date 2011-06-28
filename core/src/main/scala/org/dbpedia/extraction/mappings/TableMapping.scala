package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.{OntologyClass, OntologyProperty}
import org.dbpedia.extraction.destinations.{Graph, DBpediaDatasets, Quad}

class TableMapping( mapToClass : OntologyClass,
                    correspondingClass : OntologyClass,
                    correspondingProperty : OntologyProperty,
                    keywords : String,
                    header : String,
                    mappings : List[PropertyMapping],
                    extractionContext : ExtractionContext ) extends ClassMapping
{
    val keywordDef = keywords.split(';').map { _.split(',').map(_.trim.toLowerCase) }

    val headerDef = header.split(';').map { _.split(',').map { _.split('&').map(_.trim) } }

    override def extract(node : Node, subjectUri : String, pageContext : PageContext) : Graph = node match
    {
        case tableNode : TableNode => extractTable(tableNode, subjectUri, pageContext)
        case _ => new Graph()
    }

    def extractTable(tableNode : TableNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        def writeType(rowNode : Node, instanceUri :String, clazz : OntologyClass, graph : Graph) : Graph =
        {
            var thisGraph = graph

            val quad = new Quad(extractionContext.language, DBpediaDatasets.OntologyTypes, instanceUri, extractionContext.ontology.getProperty("rdf:type").get, clazz.uri, rowNode.sourceUri)
            thisGraph = graph.merge(new Graph(quad))

            for(baseClass <- clazz.subClassOf)
            {
                thisGraph = graph.merge(writeType(rowNode, instanceUri, baseClass, thisGraph))
            }

            thisGraph
        }

        val tableHeader = extractTableHeader(tableNode)

        //TODO ignore tables with less than 2 rows

        if(!containsKeywords(tableHeader))
        {
            return new Graph()
        }

        val processedTableNode = preprocessTable(tableNode)

        var graph = new Graph()

        for( rowNode <- processedTableNode.children.tail;
             templateNode <- createTemplateNode(rowNode, tableHeader) )
        {
            //Create a new ontology instance
            val correspondingInstance = findCorrespondingInstance(tableNode)

            //Generate instance URI
            val instanceUri = pageContext.generateUri(correspondingInstance.getOrElse(subjectUri), rowNode.children.head);

            //Add new ontology instance
            graph = writeType(rowNode, instanceUri, mapToClass, graph)

            //Link new instance to the corresponding Instance
            for(corUri <- correspondingInstance)
            {
                //TODO write generic and specific properties
                val quad = new Quad(extractionContext.language, DBpediaDatasets.OntologyProperties, corUri, correspondingProperty, instanceUri, rowNode.sourceUri)
                graph = graph.merge(new Graph(quad))
            }

            //Extract properties
            graph = mappings.map(mapping => mapping.extract(templateNode, instanceUri, pageContext))
                .foldLeft(graph)(_ merge _)
        }

        graph
    }

    /**
     * Extracts the table header.
     */
    private def extractTableHeader(node : TableNode) : List[String] =
    {
        //TODO consider table whose header is in the second row
        //TODO use StringParser instead of calling retrieveText (Sometimes links are used aswell)
        for( headerRow <- node.children.headOption.toList;
             headerCell <- headerRow.children;
             text <- headerCell.retrieveText )
        yield text.toLowerCase
    }

    /**
     * Checks if a table header contains the keywords of this mapping.
     */
    private def containsKeywords(tableHeader : List[String]) : Boolean =
    {
        keywordDef.forall(_.exists(keyword =>
            tableHeader.exists(columnHeader =>
                columnHeader.contains(keyword))))
    }

    private def preprocessTable(tableNode : TableNode) : TableNode =
    {
        var newRows = tableNode.children.head :: Nil

        var previousRow = newRows.head.children
        for(rowNode <- tableNode.children.tail)
        {
            var newRow = List[TableCellNode]()

            val previousRowIter = previousRow.iterator
            val currentRowIter = rowNode.children.iterator
            var previousCell = if(previousRowIter.hasNext) previousRowIter.next() else null
            var currentCell = if(currentRowIter.hasNext) currentRowIter.next() else null

            var done = false
            while(!done)
            {
                if(previousCell != null && previousCell.annotation("rowspan").get.asInstanceOf[Int] > 1)
                {
                    previousCell.setAnnotation("rowspan", previousCell.annotation("rowspan").get.asInstanceOf[Int] - 1)
                    newRow ::= previousCell

                    previousCell = if(previousRowIter.hasNext) previousRowIter.next() else null
                }
                else if(currentCell != null)
                {
                    newRow ::= currentCell

                    previousCell = if(previousRowIter.hasNext) previousRowIter.next() else null
                    currentCell = if(currentRowIter.hasNext) currentRowIter.next() else null
                }
                else
                {
                    done = true
                }
            }

            newRow = newRow.reverse
            previousRow = newRow
            newRows ::= new TableRowNode(newRow, rowNode.line)
        }

        //Create table node
        val newTableNode = TableNode(tableNode.caption, newRows.reverse, tableNode.line)

        //Link node to the original AST
        newTableNode.parent = tableNode.parent

        newTableNode
    }

    private def createTemplateNode(rowNode : TableRowNode, tableHeader : List[String]) : Option[TemplateNode] =
    {
        //Only accept rows which have the same number of cells than the header)
        if(rowNode.children.size != tableHeader.size)
        {
            return None
        }

        var propertyNodes = List[PropertyNode]()

        //Iterate throw all column definitions of the header definition
        for(columnDefinition <- headerDef)
        {
            var columnMatchings = List[ColumnMatching]()

            //Iterate throw all columns in the header and collect matchings
            for((column, columnIndex) <- tableHeader.zipWithIndex)
            {
                //Iterate through all alternatives of this columnDefinition
                for(columnAlternative <- columnDefinition)
                {
                    //Match this alternative column definition with the column header
                    var startIndex = -1
                    var endIndex = -1
                    var i = 0
                    var done = false
                    for(keyword <- columnAlternative; if !done)
                    {
                        i = column.indexOf(keyword, i)

                        if(i == -1)
                        {
                            done = true
                        }

                        if(startIndex == -1)
                        {
                            startIndex = i
                        }

                        endIndex = i + keyword.size
                    }

                    if(i != -1)
                    {
                        //Found new column matching
                        val propertyName = columnAlternative.mkString("&")
                        columnMatchings ::= new ColumnMatching(propertyName, columnIndex, startIndex, endIndex)
                    }
                }
            }

            if(!columnMatchings.isEmpty)
            {
                //Sort all column matchings and select first one
                val bestMatching = columnMatchings.min

                //Create new property node from the best matching and the current row
                val children  = rowNode.children(bestMatching.columnIndex).children
                propertyNodes ::= PropertyNode(bestMatching.propertyName, children, rowNode.line)
            }
        }

        //Create template node
        val templateNode = TemplateNode(rowNode.root.title, propertyNodes.reverse, rowNode.line)

        //Link node to the original AST
        templateNode.parent = rowNode.parent.parent

        Some(templateNode)
    }

    private def findCorrespondingInstance(tableNode : TableNode) : Option[String] =
    {
        if(correspondingProperty == null)
        {
            return None
        }

        //Find template node which comes just above this table
        var lastPageTemplate : Option[Node] = None
        for(pageTemplate <- tableNode.root.children; if pageTemplate.isInstanceOf[TemplateNode] )
        {
            if(pageTemplate.line < tableNode.line && !pageTemplate.annotation(TemplateMapping.CLASS_ANNOTATION).isEmpty)
            {
                lastPageTemplate = Some(pageTemplate)
            }
        }

        //Check if found template has been mapped to corresponding Class
        var correspondingInstance : Option[String] = None
        for( correspondingTemplate <- lastPageTemplate;
             templateClasses <- correspondingTemplate.annotation(TemplateMapping.CLASS_ANNOTATION);
             templateClass <- templateClasses.asInstanceOf[List[OntologyClass]];
             if correspondingClass == null || templateClass.name == correspondingClass.name )
        {
            //TODO if correspondingClass == null check if templateClass subClassOf correspondingProperty.range

            return Some(correspondingTemplate.annotation(TemplateMapping.INSTANCE_URI_ANNOTATION).get.asInstanceOf[String])
        }

        None
    }

    private class ColumnMatching( val propertyName : String,
                                  val columnIndex : Int,
                                  private val startIndex : Int,
                                  private val endIndex : Int ) extends Ordered[ColumnMatching]
    {
        def compare(that : ColumnMatching) : Int =
        {
            //Prefer matchings with an low start index
            if (startIndex != that.startIndex)
            {
                return if(startIndex < that.startIndex) -1 else 1
            }

            //Prefer matchings with property names with many conjuctive parts (e.g. 'power&kW' over 'power')
            val countA = propertyName.count(_ == '&')
            val countB = that.propertyName.count(_ == '&')
            if(countA != countB)
            {
                return if(countA > countB) -1 else 1
            }

            //Prefer short matchings (e.g. 'power kw' (from def: 'power&kW') over  'power kW (PS)' (from def: 'power&PS'))
            if (endIndex != that.endIndex)
            {
                return if(endIndex < that.endIndex) -1 else 1
            }

            //Give up and consider this two matchings as equal
            0;
        }
    }
}
