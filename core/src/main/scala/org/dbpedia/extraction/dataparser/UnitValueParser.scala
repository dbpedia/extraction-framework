package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.ontology.datatypes.{Datatype, DimensionDatatype, UnitDatatype}
import org.dbpedia.extraction.wikiparser._
import java.text.ParseException
import java.util.logging.{Level, Logger}

import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.mappings.Redirects
import java.lang.Double

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.dataparser.DataParserConfig

import scala.language.reflectiveCalls

@SoftwareAgentAnnotation(classOf[UnitValueParser], AnnotationType.Parser)
class UnitValueParser( extractionContext : {
                           def ontology : Ontology
                           def language : Language
                           def redirects : Redirects },
                        inputDatatype : Datatype,
                        strict : Boolean = false,
                        multiplicationFactor : Double = 1.0) extends DataParser[Double]
{
    private val logger = Logger.getLogger(getClass.getName)

    private val parserUtils = new ParserUtils(extractionContext)

    private val durationParser = new DurationParser(extractionContext)

    private val language = extractionContext.language.wikiCode

    override val splitPropertyNodeRegex: String = if (DataParserConfig.splitPropertyNodeRegexUnitValue.contains(language))
                                            DataParserConfig.splitPropertyNodeRegexUnitValue(language)
                                          else DataParserConfig.splitPropertyNodeRegexUnitValue("en")

    private val prefix = if(strict) """\s*""" else """[\D]*?"""

    private val postfix = if(strict) """\s*""" else ".*"
    
    private val unitRegexLabels = UnitValueParser.cleanRegex(inputDatatype match
    {
        case dt : DimensionDatatype => dt.unitLabels.mkString("|")
        case dt : UnitDatatype => dt.dimension.unitLabels.mkString("|")
        case dt => throw new IllegalArgumentException("Invalid datatype: " + dt)
    })

    // Allow leading decimal separator, e.g. .0254 = 0.0254
    // See https://github.com/dbpedia/extraction-framework/issues/71
    private val ValueRegex1 = ("""(?iu)""" + prefix + """(-?\.?[0-9]+(?:[\, ][0-9]{3})*(?:\.[0-9]+)?)""" + postfix).r

    // Allow leading decimal separator, e.g. ,0254 = 0,0254
    // See https://github.com/dbpedia/extraction-framework/issues/71
    private val ValueRegex2 = ("""(?iu)""" + prefix + """(-?\,?[0-9]+(?:[\. ][0-9]{3})*(?:\,[0-9]+)?)""" + postfix).r

    private val UnitRegex = ("""(?iu)""" + """(?<!\w)(""" + unitRegexLabels + """)(?!/)(?!\\)(?!\w)(?!\d)""").r
    
    /** Merging strings with feet and inches: 'x ft y in' and convert them into centimetres */
    private val UnitValueRegex1a = ("""(?iu)""" + prefix + """(-?[0-9]+)\040*(?:ft|feet|foot|\047|\054|\140)\040*([0-9]+)\040*(?:in\b|inch\b|inches\b|\047\047|\054\054|\140\140\042)""" + postfix).r
    
    /** Catches number and unit: e.q. 1,120,500.55 km */
    // Allow leading decimal separator, e.g. .0254 = 0.0254
    // See https://github.com/dbpedia/extraction-framework/issues/71
    private val UnitValueRegex1b = ("""(?iu)""" + prefix + """(?<!-)(-?\.?[0-9]+(?:[\, ][0-9]{3})*(?:\.[0-9]+)?)(?:&nbsp;)*\040*\(?\[?\[?(""" + unitRegexLabels +
                                    """)(?!/)(?!\\)(?!\w)""" + postfix).r
    
    /** If different units are present, e.g.: 10 mi. (16.0934 km); the first will be returned */
    //TODO remove?
    // Allow leading decimal separator, e.g. .0254 = 0.0254
    // See https://github.com/dbpedia/extraction-framework/issues/71
    private val UnitValueRegex1c = ("""(?iu)""" + prefix + """(?<!-)(-?\.?[0-9]+(?:[\, ][0-9]{3})*(?:\.[0-9]+)?)(?:&nbsp;)*\040*\(?\[?\[?(""" + unitRegexLabels +
                                    """)[\s]*\([\s]*(?:[0-9]+(?:\.[0-9]+)?)[\s]*(?:""" + unitRegexLabels + """)[\s]*\)[\s]*""" + postfix).r
                                   
    /** Catches number and unit: e.q. 1.120.500,55 km */
    // Allow leading decimal separator, e.g. .0254 = 0.0254
    // See https://github.com/dbpedia/extraction-framework/issues/71
    private val UnitValueRegex2b = ("""(?iu)""" + prefix + """(?<!-)(-?\,?[0-9]+(?:[\. ][0-9]{3})*(?:\,[0-9]+)?)(?:&nbsp;)*\040*\(?\[?\[?(""" + unitRegexLabels + """)(?!/)(?!\\)(?!\w)""" + postfix).r
    
    /** If different units are present, e.g.: 10 mi. (16.0934 km); the first will be returned */
    //TODO remove?
    // Allow leading decimal separator, e.g. .0254 = 0.0254
    // See https://github.com/dbpedia/extraction-framework/issues/71
    private val UnitValueRegex2c = ("""(?iu)""" + prefix + """(?<!-)(-?\,?[0-9]+(?:[\. ][0-9]{3})*(?:\,[0-9]+)?)(?:&nbsp;)*\040*\(?\[?\[?(""" + unitRegexLabels +
                                    """)[\s]*\([\s]*(?:[0-9]+(?:\,[0-9]+)?)[\s]*(?:""" + unitRegexLabels + """)[\s]*\)[\s]*""" + postfix).r


    private val PrefixUnitValueRegex1 = ("""(?iu)""" + prefix + """(""" + unitRegexLabels + """)\]?\]?\040*(?<!-)([\-0-9]+(?:\,[0-9]{3})*(?:\.[0-9]+)?)""" + postfix).r

    private val PrefixUnitValueRegex2 = ("""(?iu)""" + prefix + """(""" + unitRegexLabels + """)\]?\]?\040*(?<!-)([\-0-9]+(?:\.[0-9]{3})*(?:\,[0-9]+)?)""" + postfix).r

    private lazy val MeterUnitDataType = extractionContext.ontology.datatypes("metre").asInstanceOf[UnitDatatype]
    private lazy val FeetUnitDataType = extractionContext.ontology.datatypes("foot").asInstanceOf[UnitDatatype]
    private lazy val InchUnitDataType = extractionContext.ontology.datatypes("inch").asInstanceOf[UnitDatatype]

    private[dataparser] override def parse(node : Node) : Option[ParseResult[Double]] =
    {
        val errors = if(logger.isLoggable(Level.FINE)) Some(new ParsingErrors()) else None

        for(result <- catchTemplates(node, errors))
            return Some(ParseResult(result._1, None, Some(result._2)))

        for(parseResult <- StringParser.parse(node))
        {
            val correctDashes = parseResult.value.replaceAll("["+DataParserConfig.dashVariationsRegex+"]", "-" )
            val text = parserUtils.convertLargeNumbers(correctDashes)

            inputDatatype match
            {
                case dt : DimensionDatatype if dt.name == "Time" => for(duration <- catchDuration(text)) return Some(ParseResult(duration._1, None, Some(duration._2)))
                case dt : UnitDatatype if dt.dimension.name == "Time" => for(duration <- catchDuration(text)) return Some(ParseResult(duration._1, None, Some(duration._2)))
                case _ =>
            }

            catchUnitValue(text, errors) match
            {
                case Some(result) => return Some(ParseResult(result._1, None, Some(result._2)))
                case None =>
                {
                    //No unit value found
                    if(inputDatatype.isInstanceOf[UnitDatatype])
                    {
                        for( value <- catchValue(text);
                             result <- generateOutput(value, None, errors) )
                        {
                            return Some(ParseResult(result._1, None, Some(result._2)))
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
                for (child <- node.children){
                  val zw = catchTemplates(child, errors)
                  if(zw.nonEmpty)
                    return zw
                }
                return None
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
            { for(childrenChild @ TextNode(_, _, _)<- child.children) yield childrenChild }
        
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
                value = valueProperty.children.collect{case TextNode(text, _, _) => text}.headOption
                unit = unitProperty.children.collect{case TextNode(text, _, _) => text}.headOption
            }
        }
        // http://en.wikipedia.org/wiki/Template:Height
        // {{height|m=1.77|precision=0}}
        // {{height|ft=6|in=1}}
        // {{height|ft=6}}
        // TODO: {{height|ft=5|in=7+1/2}}
        else if (templateName == "Height")
        {
            // TODO: Should not be needed anymore, remove?
            for (property <- templateNode.property("1"))
            {
                value = property.children.collect { case TextNode(text, _, _) => text }.headOption
                unit = Some(property.key)
            }
            // If the TemplateNode has a second PropertyNode ...
            /*
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
            } */

            val defaultValue: PropertyNode = PropertyNode("", List(TextNode("0", 0)), 0)

            // Metre and foot/inch parameters cannot co-exist
            findUnitValueProperty(MeterUnitDataType, templateNode) match
            {
                case Some(metres) =>
                    try
                    {
                        val mVal = metres.children.collect { case TextNode(text, _, _) => text }.headOption
                        val mToCm = mVal.get.toDouble * 100.0
                        unit = Some("centimetre")
                        value = Some(mToCm.toString)
                    }
                    catch
                    {
                        case _: Throwable =>
                    }
                case None =>
                    val feet = findUnitValueProperty(FeetUnitDataType, templateNode).getOrElse(defaultValue)
                    val inch = findUnitValueProperty(InchUnitDataType, templateNode).getOrElse(defaultValue)
                    try
                    {
                        val ftVal = feet.children.collect { case TextNode(text, _, _) => text }.headOption
                        val ftToCm = ftVal.get.toDouble * 30.48
                        val inVal = inch.children.collect { case TextNode(text, _, _) => text }.headOption
                        val inToCm = inVal.get.toDouble * 2.54
                        unit = Some("centimetre")
                        value = Some((ftToCm + inToCm).toString)
                    }
                    catch
                    {
                        case _: Throwable =>
                    }
            }
        }
        // http://en.wikipedia.org/wiki/Template:Auto_in
        // {{Auto in|value|round_to}}
        else if (templateName == "Auto in")
        {
            for (valueProperty <- templateNode.property("1"))
            {
                value = valueProperty.children.collect{case TextNode(text, _, _) => text}.headOption
            }
            unit = Some("inch")
        }
        // http://en.wikipedia.org/wiki/Template:Km_to_mi
        // {{km to mi|value|...}}
        else if (templateName == "Km to mi")
        {
            for (valueProperty <- templateNode.property("1"))
            {
                value = valueProperty.children.collect{case TextNode(text, _, _) => text}.headOption
            }
            unit = Some("kilometre")
        }
        // http://en.wikipedia.org/wiki/Template:Km2_to_mi2
        // {{km2 to mi2|value|...}}
        else if (templateName == "Km2 to mi2")
        {
            for (valueProperty <- templateNode.property("1"))
            {
                value = valueProperty.children.collect{case TextNode(text, _, _) => text}.headOption
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
                value = valueProperty.children.collect{case TextNode(text, _, _) => text}.headOption
            }
            unit = Some("inhabitants per square kilometre")
        }
        // http://en.wikipedia.org/wiki/Template:Ft_to_m
        // {{ft to m|value|...}}
        else if (templateName == "Ft to m")
        {
            for (valueProperty <- templateNode.property("1"))
            {
                value = valueProperty.children.collect{case TextNode(text, _, _) => text}.headOption
            }
            unit = Some("foot")
        }
        // http://en.wikipedia.org/wiki/Template:Ftom
        // {{ftom|value|...}}
        else if (templateName == "Ftom")
        {
            for (valueProperty <- templateNode.property("1"))
            {
                value = valueProperty.children.collect{case TextNode(text, _, _) => text}.headOption
            }
            unit = Some("metre")
        }
        // https://en.wikipedia.org/wiki/Template:Duration
        // {{duration|h=1|m=20|s=32}}
        // {{duration|m=20|s=32}}
        // {{duration|1|20|32}}
        // {{duration||20|32}}
        //
        // Parameters are optional and their default value is 0
        else if (templateName == "Duration")
        {
            val defaultValue = PropertyNode("", List(TextNode("0", 0)), 0)

            val hours = templateNode.property("h").getOrElse(templateNode.property("1").getOrElse(defaultValue))
            val minutes = templateNode.property("m").getOrElse(templateNode.property("2").getOrElse(defaultValue))
            val seconds = templateNode.property("s").getOrElse(templateNode.property("3").getOrElse(defaultValue))

            val h = hours.children.collect { case TextNode(t, _, _) => t }.headOption.getOrElse("0").toDouble
            val m = minutes.children.collect { case TextNode(t, _ , _) => t }.headOption.getOrElse("0").toDouble
            val s = seconds.children.collect { case TextNode(t, _, _) => t}.headOption.getOrElse("0").toDouble

            value = Some((h * 3600.0 + m * 60.0 + s).toString)
            unit = Some("second")
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
                case ValueRegex1(value) => //TODO check regular expressions to capture '-'
                {
                    val prefix = if (input.startsWith("-") && !value.startsWith("-")) "-" else ""
                    Some(prefix + value)
                }
                case _ => None
            }
        }
        else
        {
            input match
            {
                case ValueRegex2(value)=>  //TODO check regular expressions to capture '-'
                    {
                        val prefix = if (input.startsWith("-") && !value.startsWith("-")) "-" else ""
                        Some(prefix + value)
                    }
                case _ => None
            }
        }
    }

    private def catchDuration(input : String) : Option[(Double, UnitDatatype)] =
    {
        durationParser.parseToSeconds(input, inputDatatype) match
        {
            case Some(result) => Some((result, extractionContext.ontology.datatypes("second").asInstanceOf[UnitDatatype]))
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
        // FIXME: this must not be hard-coded. Use NumberFormat for language locale.
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
        // for wikipedia articles in german, french, italian, spanish ...
        // numbers with a , as decimal separator and a . as thousand separator
        else
        {
            input match
            {
                case UnitValueRegex2b(value, unit) => generateOutput(value, Some(unit), errors)
                case UnitValueRegex2c(value, unit) => generateOutput(value, Some(unit), errors)
                case PrefixUnitValueRegex2(unit, value) if catchPrefixedUnit => generateOutput(value, Some(unit), errors)
                case _ => None
            }
        }
    }

    private def findUnitValueProperty(dataType: UnitDatatype, templateNode: TemplateNode): Option[PropertyNode] =
    {
        templateNode.keySet.find(dataType.unitLabels.contains).flatMap(templateNode.property)
    }
    
    /**
     * Creates the output tuple, the number at 0, the unit at 1.
     */
    private def generateOutput(valueString : String, unitString : Option[String] = None, errors : Option[ParsingErrors]) : Option[(Double, UnitDatatype)] =
    {
        val value = 
        {
            try
            {
                parserUtils.parse(valueString).doubleValue * multiplicationFactor
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
                //first match case-sensitive so that MW matches and is not equivalent to mW
                case inputUnit : UnitDatatype => inputUnit.dimension.unit(unitName) match
                {
                    case Some(unit) => unit
                    //only then case-insensitive to catch possible case mismatches such as Km i/o km
                    case None => inputUnit.dimension.unit(unitName.toLowerCase) match
                    {
                        case Some(unit) => unit
                        case None =>
                        {
                            errors.foreach(_.add("Given unit '" + unitName + "' not found"))
                            return None
                        }
                    }
                }
                //first match case-sensitive so that MW matches and is not equivalent to mW
                case inputDimension : DimensionDatatype => inputDimension.unit(unitName) match
                {
                    case Some(unit) => unit
                    //only then case-insensitive to catch possible case mismatches such as Km i/o km
                    case None => inputDimension.unit(unitName.toLowerCase) match
                    {
                        case Some(unit) => unit
                        case None =>
                        {
                            errors.foreach(_.add("Given unit '" + unitName + "' not found in dimension " + inputDimension))
                            return None
                        }
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