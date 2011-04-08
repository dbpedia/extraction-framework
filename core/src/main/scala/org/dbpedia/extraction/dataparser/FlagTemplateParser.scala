package org.dbpedia.extraction.wikiparser.impl.wikipedia

import java.util.Locale
import org.dbpedia.extraction.wikiparser.{TemplateNode, TextNode, WikiTitle}
import org.dbpedia.extraction.mappings.ExtractionContext

/**
 * Handling of flag templates.
 */

object FlagTemplateParser
{
    //private val codeMap = getCodeMap()

    def getDestination(extractionContext : ExtractionContext, templateNode : TemplateNode) : Option[WikiTitle] =
    {
        val templateName = templateNode.title.decoded
        //getCodeMap return en if language code is not configured
        val language = extractionContext.language.wikiCode
        val langCodeMap = getCodeMap(language)

        if((templateName equalsIgnoreCase "flagicon")              //{{flagicon|countryname|variant=|size=}}
                || (templateName equalsIgnoreCase "flag")          //{{flag|countryname|variant=|size=}}
                || (templateName equalsIgnoreCase "flagcountry"))  //{{flagcountry|countryname|variant=|size=|name=}}  last parameter is alternative name
        {
            for (countryNameNode <- templateNode.property("1"))
            {
                countryNameNode.children.collect{case TextNode(text, _) => text}.headOption match
                {
                    case Some(countryCode : String) if(templateName.length == 3)&&(templateName == templateName.toUpperCase) =>
                    {
                        langCodeMap.get(countryCode).foreach(countryName => return Some(new WikiTitle(countryName)))
                    }
                    case Some(countryName : String) => return Some(new WikiTitle(countryName))
                    case _ =>
                }
            }
        }

        //template name is actually country code for flagicon template
        else if((templateName.length == 3) && (templateName == templateName.toUpperCase))
        {
            langCodeMap.get(templateName).foreach(countryName => return Some(new WikiTitle(countryName)))
        }

        None
    }
    
    //for major languages (e.g fr, de, ...) maybe similar to "en", see 
    //http://download.oracle.com/javase/1.4.2/docs/api/java/util/Locale.html
    private def getCodeMap(language : String) : Map[String, String] = 
    {
        //english (en) as _
        language match
        {
            case "el" =>
            {
                Map(
                    "BLM"->"Άγιος Βαρθολομαίος",
                    "MAF"->"Άγιος Μαρτίνος",
                    "ALA"->"Άαλαντ",
                    "VCT"->"Άγιος Βικέντιος",
                    "SMR"->"Άγιος Μαρίνος",
                    "SPM"->"Άγιος Πέτρος και Μικελόν",
                    "KNA"->"Άγιος Χριστόφορος και Νέβις",
                    "AZE"->"Αζερμπαϊτζάν",
                    "EGY"->"Αίγυπτος",
                    "ETH"->"Αιθιοπία",
                    "HTI"->"Αϊτή",
                    "CIV"->"Ακτή Ελεφαντοστού",
                    "ALB"->"Αλβανία",
                    "DZA"->"Αλγερία",
                    "ASM"->"Αμερικανική Σαμόα",
                    "TLS"->"Ανατολικό Τιμόρ",
                    "AGO"->"Ανγκόλα",
                    "AIA"->"Ανγκουίλα",
                    "AND"->"Ανδόρρα",
                    "ATA"->"Ανταρκτική",
                    "ATG"->"Αντίγκουα και Μπαρμπούντα",
                    "ARG"->"Αργεντινή",
                    "ARM"->"Αρμενία",
                    "ABW"->"Αρούμπα",
                    "AUS"->"Αυστραλία",
                    "AUT"->"Αυστρία",
                    "AFG"->"Αφγανιστάν",
                    "VUT"->"Βανουάτου",
                    "VAT"->"Βατικανό",
                    "BEL"->"Βέλγιο",
                    "VEN"->"Βενεζουέλα",
                    "BMU"->"Βερμούδες",
                    "VNM"->"Βιετνάμ",
                    "MMR"->"Βιρμανία",
                    "BOL"->"Βολιβία",
                    "PRK"->"Βόρεια Κορέα",
                    "BIH"->"Βοσνία-Ερζεγοβίνη",
                    "BGR"->"Βουλγαρία",
                    "BRA"->"Βραζιλία",
                    "IOT"->"Βρετανικό Έδαφος Ινδικού Ωκεανού",
                    "VGB"->"Βρετανικές Παρθένοι Νήσοι",
                    "FRA"->"Γαλλία",
                    "ATF"->"Γαλλικά νότια και ανταρκτικά εδάφη",
                    "GUF"->"Γαλλική Γουιάνα",
                    "PYF"->"Γαλλική Πολυνησία",
                    "DEU"->"Γερμανία",
                    "GEO"->"Γεωργία",
                    "GIB"->"Γιβραλτάρ",
                    "GMB"->"Γκάμπια",
                    "GAB"->"Γκαμπόν",
                    "GHA"->"Γκάνα",
                    "GGY"->"Γκέρνσεϋ",
                    "GUM"->"Γκουάμ",
                    "GLP"->"Γουαδελούπη",
                    "GTM"->"Γουατεμάλα",
                    "GUY"->"Γουιάνα",
                    "GIN"->"Γουινέα",
                    "GNB"->"Γουινέα-Μπισσάου",
                    "GRD"->"Γρενάδα",
                    "GRL"->"Γροιλανδία",
                    "DNK"->"Δανία",
                    "CAF"->"Κεντροαφρικανική Δημοκρατία",
                    "DOM"->"Δομινικανή Δημοκρατία",
                    "PSE"->"Δυτική Όχθη",
                    "ESH"->"Δυτική Σαχάρα",
                    "SLV"->"Ελ Σαλβαδόρ",
                    "CHE"->"Ελβετία",
                    "GRC"->"Ελλάδα",
                    "GRE"->"Ελλάδα",
                    "ERI"->"Ερυθραία",
                    "EST"->"Εσθονία",
                    "ZMB"->"Ζάμπια",
                    "ZWE"->"Ζιμπάμπουε",
                    "ARE"->"Ηνωμένα Αραβικά Εμιράτα",
                    "USA"->"Ηνωμένες Πολιτείες",
                    "UMI"->"Ηνωμένες Πολιτείες - Μικρά απομακρυσμένα νησιά",
                    "GBR"->"Ηνωμένο Βασίλειο",
                    "JPN"->"Ιαπωνία",
                    "IND"->"Ινδία",
                    "IDN"->"Ινδονησία",
                    "JOR"->"Ιορδανία",
                    "IRQ"->"Ιράκ",
                    "IRN"->"Ιράν",
                    "IRL"->"Ιρλανδία",
                    "GNQ"->"Ισημερινή Γουινέα",
                    "ECU"->"Ισημερινός",
                    "ISL"->"Ισλανδία",
                    "ESP"->"Ισπανία",
                    "ISR"->"Ισραήλ",
                    "ITA"->"Ιταλία",
                    "KAZ"->"Καζακστάν",
                    "CMR"->"Καμερούν",
                    "KHM"->"Καμπότζη",
                    "CAN"->"Καναδάς",
                    "QAT"->"Κατάρ",
                    "NLD"->"Κάτω Χώρες",
                    "KEN"->"Κένυα",
                    "CHN"->"Κίνα",
                    "KGZ"->"Κιργιζιστάν",
                    "KIR"->"Κιριμπάτι",
                    "COG"->"Κογκό",
                    "COL"->"Κολομβία",
                    "COM"->"Κομόρες",
                    "CRI"->"Κόστα Ρίκα",
                    "CUB"->"Κούβα",
                    "KWT"->"Κουβέιτ",
                    "HRV"->"Κροατία",
                    "CYP"->"Κύπρος",
                    "COD"->"Λαϊκή Δημοκρατία του Κογκό",
                    "LAO"->"Λάος",
                    "LSO"->"Λεσότο",
                    "LVA"->"Λετονία",
                    "BLR"->"Λευκορωσία",
                    "LBN"->"Λίβανος",
                    "LBR"->"Λιβερία",
                    "LBY"->"Λιβύη",
                    "LTU"->"Λιθουανία",
                    "LIE"->"Λιχτενστάιν",
                    "LUX"->"Λουξεμβούργο",
                    "MYT"->"Μαγιότ",
                    "MDG"->"Μαδαγασκάρη",
                    "MAC"->"Μακάο",
                    "MYS"->"Μαλαισία",
                    "MWI"->"Μαλάουι",
                    "MDV"->"Μαλδίβες",
                    "MLI"->"Μάλι",
                    "MLT"->"Μάλτα",
                    "MAR"->"Μαρόκο",
                    "MTQ"->"Μαρτινίκα",
                    "MUS"->"Μαυρίκιος",
                    "MRT"->"Μαυριτανία",
                    "MNE"->"Μαυροβούνιο",
                    "MEX"->"Μεξικό",
                    "MNG"->"Μογγολία",
                    "MOZ"->"Μοζαμβίκη",
                    "MDA"->"Μολδαβία",
                    "MCO"->"Μονακό",
                    "MSR"->"Μοντσερράτ",
                    "BGD"->"Μπαγκλαντές",
                    "BRB"->"Μπαρμπάντος",
                    "BHS"->"Μπαχάμες",
                    "BHR"->"Μπαχρέιν",
                    "BLZ"->"Μπελίζε",
                    "BEN"->"Μπενίν",
                    "BWA"->"Μποτσουάνα",
                    "BFA"->"Μπουρκίνα Φάσο",
                    "BDI"->"Μπουρούντι",
                    "BTN"->"Μπουτάν",
                    "BRN"->"Μπρουνέι",
                    "NAM"->"Ναμίμπια",
                    "NRU"->"Ναουρού",
                    "NZL"->"Νέα Ζηλανδία",
                    "NCL"->"Νέα Καληδονία",
                    "NPL"->"Νεπάλ",
                    "BVT"->"Μπουβέ",
                    "NFK"->"Νησί Νόρφολκ",
                    "CXR"->"Νησί των Χριστουγέννων",
                    "CCK"->"Νησιά Κόκος (Keeling)",
                    "ALA"->"Ώλαντ",
                    "MNP"->"Βόρειες Μαριάνες Νήσοι",
                    "MHL"->"Νήσοι Μάρσαλ",
                    "PCN"->"Νησιά Πίτκερν",
                    "CYM"->"Νήσοι Καίυμαν",
                    "COK"->"Νήσοι Κουκ",
                    "SLB"->"Νήσοι του Σολομώντος",
                    "FRO"->"Νήσοι Φερόες",
                    "FLK"->"Νήσοι Φώκλαντ",
                    "IMN"->"Νήσος Μαν",
                    "HMD"->"Νήσοι Χερντ και Μακντόναλντ",
                    "NER"->"Νίγηρας",
                    "NGA"->"Νιγηρία",
                    "NIC"->"Νικαράγουα",
                    "NIU"->"Νιούε",
                    "NOR"->"Νορβηγία",
                    "ZAF"->"Νότια Αφρική",
                    "SGS"->"Νήσοι Νότια Γεωργία και Νότιες Σάντουιτς",
                    "KOR"->"Νότια Κορέα",
                    "DMA"->"Ντομίνικα",
                    "ANT"->"Ολλανδικές Αντίλλες",
                    "OMN"->"Ομάν",
                    "FSM"->"Ομόσπονδα Κράτη της Μικρονησίας",
                    "HND"->"Ονδούρα",
                    "WLF"->"Ουάλις και Φουτούνα",
                    "HUN"->"Ουγγαρία",
                    "UGA"->"Ουγκάντα",
                    "UZB"->"Ουζμπεκιστάν",
                    "UKR"->"Ουκρανία",
                    "URY"->"Ουρουγουάη",
                    "PAK"->"Πακιστάν",
                    "PLW"->"Παλάου",
                    "PAN"->"Παναμάς",
                    "PNG"->"Παπούα - Νέα Γουϊνέα",
                    "PRY"->"Παραγουάη",
                    "VIR"->"Αμερικανικές Παρθένοι Νήσοι",
                    "MKD"->"ΠΓΔΜ",
                    "PER"->"Περού",
                    "POL"->"Πολωνία",
                    "PRT"->"Πορτογαλία",
                    "PRI"->"Πουέρτο Ρίκο",
                    "CPV"->"Πράσινο Ακρωτήρι",
                    "REU"->"Ρεϋνιόν",
                    "RWA"->"Ρουάντα",
                    "ROU"->"Ρουμανία",
                    "RUS"->"Ρωσία",
                    "WSM"->"Σαμόα",
                    "SHN"->"Νήσος Αγίας Ελένης",
                    "LCA"->"Σάντα Λουτσία",
                    "STP"->"Σάο Τομέ και Πρίνσιπε",
                    "SAU"->"Σαουδική Αραβία",
                    "SJM"->"Σβάλμπαρντ",
                    "SEN"->"Σενεγάλη",
                    "SRB"->"Σερβία",
                    "SYC"->"Σεϋχέλλες",
                    "SLE"->"Σιέρα Λεόνε",
                    "SGP"->"Σιγκαπούρη",
                    "SVK"->"Σλοβακία",
                    "SVN"->"Σλοβενία",
                    "SOM"->"Σομαλία",
                    "SWZ"->"Σουαζηλάνδη",
                    "SDN"->"Σουδάν",
                    "SWE"->"Σουηδία",
                    "SUR"->"Σουρινάμ",
                    "LKA"->"Σρι Λάνκα",
                    "SYR"->"Συρία",
                    "TWN"->"Ταϊβάν",
                    "THA"->"Ταϊλάνδη",
                    "TZA"->"Τανζανία",
                    "TJK"->"Τατζικιστάν",
                    "JAM"->"Τζαμάικα",
                    "JEY"->"Τζέρσεϋ",
                    "DJI"->"Τζιμπουτί",
                    "TON"->"Τόγκα",
                    "TGO"->"Τόγκο",
                    "TKL"->"Τοκελάου",
                    "TUV"->"Τουβαλού",
                    "TUR"->"Τουρκία",
                    "TKM"->"Τουρκμενιστάν",
                    "TCA"->"Τερκ και Κάικος",
                    "TTO"->"Τρινιντάντ και Τομπάγκο",
                    "TCD"->"Τσαντ",
                    "CZE"->"Τσεχία",
                    "TUN"->"Τυνησία",
                    "YEM"->"Υεμένη",
                    "PHL"->"Φιλιππίνες",
                    "FIN"->"Φινλανδία",
                    "FJI"->"Φίτζι",
                    "CHL"->"Χιλή",
                    "HKG"->"Χονγκ Κονγκ"
                )
            } // el end
            //en
            case _ =>
            {
                Locale.getISOCountries
                          .map(code => new Locale("en", code))
                          .map(locale => (locale.getISO3Country, locale.getDisplayCountry(Locale.US)))
                          .toMap
            }
        }//match end
    }

}