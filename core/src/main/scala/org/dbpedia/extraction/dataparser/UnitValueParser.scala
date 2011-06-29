package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.ontology.datatypes.{Datatype, DimensionDatatype, UnitDatatype}

import org.dbpedia.extraction.wikiparser._
import java.text.{ParseException, NumberFormat}
import java.util.logging.{Level, Logger}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.mappings.Redirects
import java.lang.Double

class UnitValueParser( extractionContext : {
                           def ontology : Ontology
                           def language : Language
                           def redirects : Redirects },
                        inputDatatype : Datatype,
                        strict : Boolean = false,
                        multiplicationFactor : Double = 1.0) extends DataParser
{
    private val logger = Logger.getLogger(classOf[UnitValueParser].getName)

    private val parserUtils = new ParserUtils(extractionContext)

    private val durationParser = new DurationParser(extractionContext)

    private val language = extractionContext.language.wikiCode

    override val splitPropertyNodeRegex = """<br\s*\/?>|\n| and | or """  //TODO this split regex might not be complete
    
    private val prefix = if(strict) """\s*""" else """[\D]*?"""

    private val postfix = if(strict) """\s*""" else ".*"
    
    private val unitRegexLabels = UnitValueParser.cleanRegex(inputDatatype match
    {
        case dt : DimensionDatatype => dt.unitLabels.mkString("|")
        case dt : UnitDatatype => dt.dimension.unitLabels.mkString("|")
        case dt => throw new IllegalArgumentException("Invalid datatype: " + dt)
    })

    private val ValueRegex1 = ("""(?iu)""" + prefix + """(-?[0-9]+(?:\,[0-9]{3})*(?:\.[0-9]+)?)""" + postfix).r

    private val ValueRegex2 = ("""(?iu)""" + prefix + """(-?[0-9]+(?:\.[0-9]{3})*(?:\,[0-9]+)?)""" + postfix).r

    private val UnitRegex = ("""(?iu)""" + """(?<!\w)(""" + unitRegexLabels + """)(?!/)(?!\\)(?!\w)(?!\d)""").r
    
    /** Merging strings with feet and inches: 'x ft y in' and convert them into centimetres */
    private val UnitValueRegex1a = ("""(?iu)""" + prefix + """(-?[0-9]+)\040*ft\040*([0-9]+)\040*in\b""" + postfix).r
    
    /** Catches number and unit: e.q. 1.120.500,55 km */
    private val UnitValueRegex1b = ("""(?iu)""" + prefix + """(?<!-)(-?[0-9]+(?:\,[0-9]{3})*(?:\.[0-9]+)?)(?:&nbsp;)*\040*\(?\[?\[?(""" + unitRegexLabels + """)(?!/)(?!\\)(?!\w)""" + postfix).r
    
    /** If different units are present, e.g.: 10 mi. (16.0934 km); the first will be returned */
    //TODO remove?
    private val UnitValueRegex1c = ("""(?iu)""" + prefix + """(?<!-)(-?[0-9]+(?:\,[0-9]{3})*(?:\.[0-9]+)?)(?:&nbsp;)*\040*\(?\[?\[?(""" + unitRegexLabels +
                                    """)[\s]*\([\s]*(?:[0-9]+(?:\.[0-9]+)?)[\s]*(?:""" + unitRegexLabels + """)[\s]*\)[\s]*""" + postfix).r
                                   
    /** Catches number and unit: e.q. 1.120.500,55 km */
    private val UnitValueRegex2a = ("""(?iu)""" + prefix + """(?<!-)(-?[\-0-9]+(?:\.[0-9]{3})*(?:\,[0-9]+)?)(?:&nbsp;)*\040*\(?\[?\[?(""" + unitRegexLabels + """)(?!/)(?!\\)(?!\w)""" + postfix).r
    
    /** If different units are present, e.g.: 10 mi. (16.0934 km); the first will be returned */
    //TODO remove?
    private val UnitValueRegex2b = ("""(?iu)""" + prefix + """(?<!-)(-?[\-0-9]+(?:\.[0-9]{3})*(?:\,[0-9]+)?)(?:&nbsp;)*\040*\(?\[?\[?(""" + unitRegexLabels +
                                    """)[\s]*\([\s]*(?:[0-9]+(?:\,[0-9]+)?)[\s]*(?:""" + unitRegexLabels + """)[\s]*\)[\s]*""" + postfix).r


    private val PrefixUnitValueRegex1 = ("""(?iu)""" + prefix + """(""" + unitRegexLabels + """)\]?\]?\040*(?<!-)([\-0-9]+(?:\,[0-9]{3})*(?:\.[0-9]+)?)""" + postfix).r

    private val PrefixUnitValueRegex2 = ("""(?iu)""" + prefix + """(""" + unitRegexLabels + """)\]?\]?\040*(?<!-)([\-0-9]+(?:\.[0-9]{3})*(?:\,[0-9]+)?)""" + postfix).r

    override def parse(node : Node) : Option[(Double, UnitDatatype)] =
    {
        val errors = if(logger.isLoggable(Level.FINE)) Some(new ParsingErrors()) else None

        for(result <- catchTemplates(node, errors))
        {
            return Some(result)
        }

        for(parseResult <- StringParser.parse(node))
        {
            val text = parserUtils.convertLargeNumbers(parseResult)

            inputDatatype match
            {
                case dt : DimensionDatatype if (dt.name == "Time") => for(duration <- catchDuration(text)) return Some(duration)
                case dt : UnitDatatype if (dt.dimension.name == "Time") => for(duration <- catchDuration(text)) return Some(duration)
                case _ =>
            }

            catchUnitValue(text, errors) match
            {
                case Some(result) => return Some(result)
                case None =>
                {
                    //No unit value found
                    if(inputDatatype.isInstanceOf[UnitDatatype])
                    {
                        for( value <- catchValue(text);
                             result <- generateOutput(value, None, errors) )
                        {
                            return Some(result)
                        }
                    }
                    else
                    {
                        errors.foreach(_.add("Could not find any unit value in '" + text + "'"))
                    }
                }
            }
        }

        for(e <- errors)
        {
            logger.fine("Could not extract " + inputDatatype.name + " value from " + node + " on page " + node.root.title + " line " + node.line + ".\n" + e)
        }

        None
    }

    /**
     * This Method parse property templates like {{convert|...}
     */
    private def catchTemplates(node : Node, errors : Option[ParsingErrors]) : Option[(Double, UnitDatatype)] =
    {
        // If the node is not a TemplateNode run catchTemplates() for all childs
        if(!node.isInstanceOf[TemplateNode])
        {
            if(!strict)
            {
                for (child <- node.children;
                     result <- catchTemplates(child, errors) )
                {
                    return Some(result)
                }

                return None;
            }
            else
            {
                node.children match
                {
                    case (child : TemplateNode) :: Nil => return catchTemplates(child, errors)
                    case _ => return None
                }
            }
        }

        val templateNode = node.asInstanceOf[TemplateNode]
        val templateName = extractionContext.redirects.resolve(templateNode.title).decoded


        val childrenChilds = for(child <- node.children) yield
            { for(childrenChild @ TextNode(_, _)<- child.children) yield childrenChild }
        
        ///////////////////////////////////////////////////////////////////////////////////////
        // Start of template parsing
        ///////////////////////////////////////////////////////////////////////////////////////
        // How to:
        // There are two cases how templates are build
        //  - only values
        //    {{convert|original_value|original_unit|conversion_unit|round_to|...}}
        //  - key and value as a pair connected by "="
        //    {{height|first_unit=first_value|second_unit=second_value|...}}
        // The first value after "{{" is the templateName and every "|" will result in a new
        // PropertyNode of the TemplateNode. The $childrenChilds[][] array contains the
        // TextNodes of these children.
        // With $childrenChilds[0][0]->getText() you get the text from the first TextNode of
        // the first PropertyNode. For example:
        // {{convert|ORIGINAL_VALUE|original_unit|conversion_unit|round_to|...}} or
        // {{height|first_unit=FIRST_VALUE|second_unit=second_value|...}}
        // With $childrenChilds[1][0]->getText() you get the text from the first TextNode
        // of the second PropertyNode.
        // With $childrenChilds[0][0]->getParent()->getKey() you get the key of the first
        // PropertyNode. For example:
        // {{height|FIRST_UNIT=first_value|second_unit=second_value|...}}
        // The first case (convert template example) has no key.
        ///////////////////////////////////////////////////////////////////////////////////////
 
        var value : Option[String] = None
        var unit : Option[String] = None
        
        // http://en.wikipedia.org/wiki/Template:Convert
        // http://it.wikipedia.org/wiki/Template:Converti
        // {{convert|original_value|original_unit|conversion_unit|round_to|...}}
        // TODO {{convert|3.21|m|cm}} and other occurences of two units help finding the dimension
        // TODO resolve template redirects 
        if (templateName == "Convert" || templateName == "Converti")
        {
            for (valueProperty <- templateNode.property("1"); unitProperty <- templateNode.property("2"))
            {
                value = valueProperty.children.collect{case TextNode(text, _) => text}.headOption
                unit = unitProperty.children.collect{case TextNode(text, _) => text}.headOption
            }
        }
        // http://en.wikipedia.org/wiki/Template:Height
        // {{height|first_unit=first_value|second_unit=second_value|...}}
        else if (templateName == "Height")
        { 
            for (property <- templateNode.property("1"))
            {
                value = property.children.collect{case TextNode(text, _) => text}.headOption
                unit = Some(property.key)
            }
            // If the TemplateNode has a second PropertyNode ...
            for (secondProperty <- templateNode.property("2"))
            {
                val secondUnit = secondProperty.key
                // If the height template contains foot and inch they will converted into centimetres.
                if (unit == "ft" && secondUnit == "in")
                {
                    val secondValue = secondProperty.children.collect{case TextNode(text, _) => text}.headOption
                    try
                    {
                        val ftToCm = value.get.toDouble * 30.48
                        val inToCm = secondValue.get.toDouble * 2.54

                        value = Some((ftToCm + inToCm).toString)
                        unit = Some("centimetre")
                    }
                    catch { case _ => }
                }
            }
        }
        // http://en.wikipedia.org/wiki/Template:Auto_in
        // {{Auto in|value|round_to}}
        else if (templateName == "Auto in")
        {
            for (valueProperty <- templateNode.property("1"))
            {
                value = valueProperty.children.collect{case TextNode(text, _) => text}.headOption
            }
            unit = Some("inch")
        }
        // http://en.wikipedia.org/wiki/Template:Km_to_mi
        // {{km to mi|value|...}}
        else if (templateName == "Km to mi")
        {
            for (valueProperty <- templateNode.property("1"))
            {
                value = valueProperty.children.collect{case TextNode(text, _) => text}.headOption
            }
            unit = Some("kilometre")
        }
        // http://en.wikipedia.org/wiki/Template:Km2_to_mi2
        // {{km2 to mi2|value|...}}
        else if (templateName == "Km2 to mi2")
        {
            for (valueProperty <- templateNode.property("1"))
            {
                value = valueProperty.children.collect{case TextNode(text, _) => text}.headOption
            }
            unit = Some("square kilometre")
        }
        // http://en.wikipedia.org/wiki/Template:Pop_density_km2_to_mi2
        // {{Pop density km2 to mi2|value|...}}
        // {{PD km2 to mi2|value|...}}
        else if (templateName == "Pop density km2 to mi2" || templateName == "Pd km2 to mi2")
        {
            for (valueProperty <- templateNode.property("1"))
            {
                value = valueProperty.children.collect{case TextNode(text, _) => text}.headOption
            }
            unit = Some("inhabitants per square kilometre")
        }
        // http://en.wikipedia.org/wiki/Template:Ft_to_m
        // {{ft to m|value|...}}
        else if (templateName == "Ft to m")
        {
            for (valueProperty <- templateNode.property("1"))
            {
                value = valueProperty.children.collect{case TextNode(text, _) => text}.headOption
            }
            unit = Some("foot")
        }
        // http://en.wikipedia.org/wiki/Template:Ftom
        // {{ftom|value|...}}
        else if (templateName == "Ftom")
        {
            for (valueProperty <- templateNode.property("1"))
            {
                value = valueProperty.children.collect{case TextNode(text, _) => text}.headOption
            }
            unit = Some("metre")
        }
        // If there is no mapping defined for the template -> return null and log it
        else
        {
            errors.foreach(_.add("Unknown template: \"" + templateName + "\""))
            return None
        }
        ///////////////////////////////////////////////////////////////////////////////////////
        // End of template parsing
        ///////////////////////////////////////////////////////////////////////////////////////
        
        // If there is a mapping but the parsing falied -> return None
        if(value.isEmpty)
        {
            return None
        }
        
        generateOutput(value.get, unit, errors)
    }

    private def catchValue(input : String) : Option[String] =
    {
        if (language == "en" || language == "ja" || language == "zh")
        {
            input match
            {
                case ValueRegex1(value) => Some(value)
                case _ => None
            }
        }
        else
        {
            input match
            {
                case ValueRegex2(value) => Some(value)
                case _ => None
            }
        }
    }

    private def catchDuration(input : String) : Option[(Double, UnitDatatype)] =
    {
        durationParser.parseToSeconds(input, inputDatatype) match
        {
            case Some(result) => Some((result, extractionContext.ontology.getDatatype("second").get.asInstanceOf[UnitDatatype]))
            case None => None
        }
    }
    
    private def catchUnit(input : String) : Option[String] =
    {
        UnitRegex.findFirstIn(input)
    }
    
    /**
     * Returns unit and value for an Object
     * string with feet and inches will be converted in centimetre
     * 1 in = 2.54 cm
     * 1 ft = 30.48 cm
     *
     * The value and Unit of the passed value will be returned in an Array
     *
     * @param   string  $input  text
     * @return  array   the value at offset[0] and a UnitDataType object at offset[1].
     */
    private def catchUnitValue(input : String, errors : Option[ParsingErrors]) : Option[(Double, UnitDatatype)] =
    {
        val inputDimension = inputDatatype match
        {
            case dt : UnitDatatype => dt.dimension
            case dt : DimensionDatatype => dt
        }

        val catchPrefixedUnit = inputDimension.name == "Currency"

        // english, japanese and chinese Wikipedia articles
        // numbers with a . as decimal separator and a , as thousand separator
        if (language == "en" || language == "ja" || language == "zh")
        {
            input match
            {
                case UnitValueRegex1a(feet, inch) =>
                {
                    try
                    {
                        val ftToCm = feet.toDouble * 30.48
                        val inToCm = inch.toDouble * 2.54
            
                        generateOutput((ftToCm + inToCm).toString, Some("centimetre"), errors)
                    }
                    catch
                    {
                        case _ : NumberFormatException => None
                    }
                }
                case UnitValueRegex1b(value, unit) => generateOutput(value, Some(unit), errors)
                case UnitValueRegex1c(value, unit) => generateOutput(value, Some(unit), errors)
                case PrefixUnitValueRegex1(unit, value) if catchPrefixedUnit => generateOutput(value, Some(unit), errors)
                case _ => None
            }
        }
        // for wikipedia artikels in german, french, italian, spanish ...
        // numbers with a , as decimal separator and a . as thousand separator
        else
        {
            input match
            {
                case UnitValueRegex2a(value, unit) => generateOutput(value, Some(unit), errors)
                case UnitValueRegex2b(value, unit) => generateOutput(value, Some(unit), errors)
                case PrefixUnitValueRegex2(unit, value) if catchPrefixedUnit => generateOutput(value, Some(unit), errors)
                case _ => None
            }
        }
    }
    
    /**
     * Creates the output tuple, the number at 0, the unit at 1.
     */
    private def generateOutput(valueString : String, unitString : Option[String] = None, errors : Option[ParsingErrors]) : Option[(Double, UnitDatatype)] =
    {
        val numberFormat = NumberFormat.getNumberInstance(extractionContext.language.locale)
        
        val value = 
        {
            try
            {
                numberFormat.parse(valueString).doubleValue * multiplicationFactor
            }
            catch
            {
                case ex : ParseException =>
                {
                    errors.foreach(_.add("Could not find number in '" + valueString + "'"))
                    return None
                }
            }
        }

        //Determine the datatype of the value
        val valueUnit = unitString match
        {
            // The unit is explicitly provided
            case Some(unitName) => inputDatatype match
            {
                case inputUnit : UnitDatatype => inputUnit.dimension.unit(unitName) match
                {
                    case Some(unit) => unit
                    case None =>
                    {
                        errors.foreach(_.add("Given unit '" + unitName + "' not found"))
                        return None
                    }
                }
                case inputDimension : DimensionDatatype => inputDimension.unit(unitName) match
                {
                    case Some(unit) => unit
                    case None =>
                    {
                        errors.foreach(_.add("Given unit '" + unitName + "' not found in dimension " + inputDimension))
                        return None
                    }
                }
                case _ => throw new Exception("Unexpected input datatype")
            }
            //No unit is explicitly provided
            case None => inputDatatype match
            {
                case inputUnit : UnitDatatype => inputUnit
                case _ =>
                {
                    errors.foreach(_.add("Value '" + valueString + "' found without any unit"))
                    return None
                }
            }
        }

        Some(value, valueUnit)
    }
}

private object UnitValueParser
{
    /**
     * Escapes characters that would otherwise be interpreted as a meta-character in the regex
     */
    private def cleanRegex(regex : String) : String =
    {
        regex.replace("""\\""", """\\\\""")
          .replace("""^""","""\^""")
          .replace("""$""","""\$""")
    }
}