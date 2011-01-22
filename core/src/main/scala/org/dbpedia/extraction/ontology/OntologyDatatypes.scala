package org.dbpedia.extraction.ontology

import datatypes._

/**
 * Loads the ontology datatypes.
 */
// TODO: this is a hack. Data types should be configured in configuration files.
object OntologyDatatypes
{
	def load() : List[Datatype] =
	{
        var types = List[Datatype]()

	    types ::= new Datatype("xsd:date")
	    types ::= new Datatype("xsd:time")
	    types ::= new Datatype("xsd:dateTime")
	    types ::= new Datatype("xsd:gYear")
	    types ::= new Datatype("xsd:gYearMonth")
	    types ::= new Datatype("xsd:gMonth")
	    types ::= new Datatype("xsd:gMonthDay")
	    types ::= new Datatype("xsd:gDay")
	    types ::= new Datatype("xsd:boolean")
	    types ::= new Datatype("xsd:string")
	    types ::= new Datatype("xsd:integer")
        types ::= new Datatype("xsd:positiveInteger")    // >0
        types ::= new Datatype("xsd:nonNegativeInteger") // >=0
        types ::= new Datatype("xsd:nonPositiveInteger") // <=0
        types ::= new Datatype("xsd:negativeInteger")    // <0        
	    types ::= new Datatype("xsd:double")
	    types ::= new Datatype("xsd:float")
        types ::= new Datatype("xsd:anyURI")
        
	    val fuelType = new EnumerationDatatype("fuelType")
        fuelType.addLiteral("diesel")
        fuelType.addLiteral("petrol")
        types ::= fuelType
        
	    //Engine Configurations (Source: http://en.wikipedia.org/wiki/Template:Piston_engine_configurations)
	    val engineConfiguration = new EnumerationDatatype("engineConfiguration");
	    
	    engineConfiguration.addLiteral("straight-two"  , List("I2" , "inline 2" , "inline twin", "parallel twin"));
	    engineConfiguration.addLiteral("straight-three", List("I3" , "inline 3" , "inline-three"));
	    engineConfiguration.addLiteral("straight-four" , List("I4" , "inline 4" , "inline-four"));
	    engineConfiguration.addLiteral("straight-five" , List("I5" , "inline 5" , "inline-five"));
	    engineConfiguration.addLiteral("straight-six"  , List("I6" , "inline 6" , "inline-six"));
	    engineConfiguration.addLiteral("straight-eight", List("I8" , "inline 8" , "inline-eight"));
	    engineConfiguration.addLiteral("straight-nine" , List("I9" , "inline 9" , "inline-nine"));
	    engineConfiguration.addLiteral("straight-10"   , List("I10", "inline 10", "inline-10"));
	    engineConfiguration.addLiteral("straight-12"   , List("I12", "inline 12", "inline-12"));
	    engineConfiguration.addLiteral("straight-14"   , List("I14", "inline 14", "inline-14"));
	
	    //TODO add all alternatives
	    engineConfiguration.addLiteral("flat-twin", List("F2", "H2"));
	    engineConfiguration.addLiteral("F4");
	    engineConfiguration.addLiteral("F6");
	    engineConfiguration.addLiteral("F8");
	    engineConfiguration.addLiteral("F10");
	    engineConfiguration.addLiteral("F12");
	    engineConfiguration.addLiteral("F16");
	
	    engineConfiguration.addLiteral("V2");
	    engineConfiguration.addLiteral("V4");
	    engineConfiguration.addLiteral("V5");
	    engineConfiguration.addLiteral("V6");
	    engineConfiguration.addLiteral("V8");
	    engineConfiguration.addLiteral("V10");
	    engineConfiguration.addLiteral("V12");
	    engineConfiguration.addLiteral("V16");
	    engineConfiguration.addLiteral("V20");
	    engineConfiguration.addLiteral("V24");
	
	    engineConfiguration.addLiteral("W8");
	    engineConfiguration.addLiteral("W12");
	    engineConfiguration.addLiteral("W16");
	    engineConfiguration.addLiteral("W18");
	
	    engineConfiguration.addLiteral("H");
	    engineConfiguration.addLiteral("U");
	    engineConfiguration.addLiteral("Square four");
	    engineConfiguration.addLiteral("VR");//TODO add VR6 etc. ?
	    engineConfiguration.addLiteral("Opposed");
	    engineConfiguration.addLiteral("X");
	    engineConfiguration.addLiteral("Single");
	    engineConfiguration.addLiteral("Radial");
	    engineConfiguration.addLiteral("Rotary");
	    engineConfiguration.addLiteral("Deltic");
	    engineConfiguration.addLiteral("Pistonless");
	    engineConfiguration.addLiteral("Wankel");
	
	    types ::= engineConfiguration
	
	    //Valvetrain
	    val valvetrain = new EnumerationDatatype("valvetrain");
	
	    valvetrain.addLiteral("SOHC");
	    valvetrain.addLiteral("DOHC");
	    valvetrain.addLiteral("L-head", List("L-block", "flathead", "sidevalve"));
	    valvetrain.addLiteral("F-head");
	    valvetrain.addLiteral("I-head", List("OHV"));
	    valvetrain.addLiteral("Camless");
	
	    types ::= valvetrain
        
	    //Units
        val builder = new UnitBuilder
	    
	    builder.addDimension("Area");
	    builder.addUnit(new StandardUnitDatatype("squareMetre", Set("square metre","m2","m²")));
	    builder.addUnit(new FactorUnitDatatype("squareMillimetre", Set("mm2","mm²","square millimetre"), 1.0E-6));
	    builder.addUnit(new FactorUnitDatatype("squareCentimetre", Set("cm2","cm²","square centimetre"), 0.0001));
	    builder.addUnit(new FactorUnitDatatype("squareDecimetre", Set("dm2","square decimetre"), 0.01));
	    builder.addUnit(new FactorUnitDatatype("squareDecametre", Set("dam2","square decametre"), 0.1));
	    builder.addUnit(new FactorUnitDatatype("squareHectometre", Set("hm2","square hectometre"), 10000.0));
	    builder.addUnit(new FactorUnitDatatype("squareKilometre", Set("km²","km2","square kilometre","km\u00B2"), 1000000.0));
	    builder.addUnit(new FactorUnitDatatype("hectare", Set("ha","hectare"), 10000.0));
	    builder.addUnit(new FactorUnitDatatype("squareInch", Set("sqin","square inch"), 0.00064516));
	    builder.addUnit(new FactorUnitDatatype("squareFoot", Set("sqft","ft2","ft²","square foot"), 0.09290304));
	    builder.addUnit(new FactorUnitDatatype("squareYard", Set("sqyd","square yard"), 0.83612736));
	    builder.addUnit(new FactorUnitDatatype("acre", Set("acre","acres"), 4046.564224));
	    builder.addUnit(new FactorUnitDatatype("squareMile", Set("sqmi","mi2","mi²","square mile"), 2589988.110336));
	    builder.addUnit(new FactorUnitDatatype("squareNauticalMile", Set("sqnmi","nmi2","square nautical mile"), 3429904.0));
	    types :::= builder.build
	    
	    builder.addDimension("Currency");
	    builder.addUnit(new InconvertibleUnitDatatype("usDollar", Set("US","USD","Dollar","US dollar", "$")));
	    builder.addUnit(new InconvertibleUnitDatatype("euro", Set("€","EUR","Euro")));
	    builder.addUnit(new InconvertibleUnitDatatype("poundSterling", Set("GBP","British Pound","£","Pound sterling")));
	    builder.addUnit(new InconvertibleUnitDatatype("japaneseYen", Set("¥","yen","JPY","Japanese yen")));
	    builder.addUnit(new InconvertibleUnitDatatype("russianRouble", Set("RUR","RUB","Russian rouble")));
	    builder.addUnit(new InconvertibleUnitDatatype("unitedArabEmiratesDirham", Set("AED","United Arab Emirates dirham")));
	    builder.addUnit(new InconvertibleUnitDatatype("afghanAfghani", Set("AFN","Afghani")));
	    builder.addUnit(new InconvertibleUnitDatatype("albanianLek", Set("ALL","Lek")));
	    builder.addUnit(new InconvertibleUnitDatatype("armenianDram", Set("AMD","Armenian dram")));
	    builder.addUnit(new InconvertibleUnitDatatype("netherlandsAntilleanGuilder", Set("ANG","Netherlands Antillean guilder")));
	    builder.addUnit(new InconvertibleUnitDatatype("angolanKwanza", Set("AOA","Kwanza")));
	    builder.addUnit(new InconvertibleUnitDatatype("argentinePeso", Set("ARS","Argentine peso")));
	    builder.addUnit(new InconvertibleUnitDatatype("australianDollar", Set("AUD","Australian dollar")));
	    builder.addUnit(new InconvertibleUnitDatatype("arubanGuilder", Set("AWG","Aruban guilder")));
	    builder.addUnit(new InconvertibleUnitDatatype("bosniaAndHerzegovinaConvertibleMarks", Set("BAM","Convertible marks")));
	    builder.addUnit(new InconvertibleUnitDatatype("barbadosDollar", Set("BBD","Barbados dollar")));
	    builder.addUnit(new InconvertibleUnitDatatype("bangladeshiTaka", Set("BDT","Bangladeshi taka")));
	    builder.addUnit(new InconvertibleUnitDatatype("bulgarianLev", Set("BGN","Bulgarian lev")));
	    builder.addUnit(new InconvertibleUnitDatatype("bahrainiDinar", Set("BHD","Bahraini dinar")));
	    builder.addUnit(new InconvertibleUnitDatatype("burundianFranc", Set("BIF","Burundian franc")));
	    builder.addUnit(new InconvertibleUnitDatatype("bermudianDollar", Set("BMD","Bermudian dollar")));
	    builder.addUnit(new InconvertibleUnitDatatype("bruneiDollar", Set("BND","Brunei dollar")));
	    builder.addUnit(new InconvertibleUnitDatatype("bolivianBoliviano", Set("BOB","Boliviano")));
	    builder.addUnit(new InconvertibleUnitDatatype("brazilianReal", Set("BRL","Brazilian real")));
	    builder.addUnit(new InconvertibleUnitDatatype("bahamianDollar", Set("BSD","Bahamian dollar")));
	    builder.addUnit(new InconvertibleUnitDatatype("bhutaneseNgultrum", Set("BTN","Ngultrum")));
	    builder.addUnit(new InconvertibleUnitDatatype("botswanaPula", Set("BWP","Pula")));
	    builder.addUnit(new InconvertibleUnitDatatype("belarussianRuble", Set("BYR","Belarussian ruble")));
	    builder.addUnit(new InconvertibleUnitDatatype("belizeDollar", Set("BZD","Belize dollar")));
	    builder.addUnit(new InconvertibleUnitDatatype("canadianDollar", Set("CAD","Canadian dollar")));
	    builder.addUnit(new InconvertibleUnitDatatype("congoleseFranc", Set("CDF","Franc Congolais")));
	    builder.addUnit(new InconvertibleUnitDatatype("swissFranc", Set("CHF","Swiss franc")));
	    builder.addUnit(new InconvertibleUnitDatatype("chileanPeso", Set("CLP","Chilean peso")));
	    builder.addUnit(new InconvertibleUnitDatatype("renminbi", Set("CNY","Renminbi")));
	    builder.addUnit(new InconvertibleUnitDatatype("colombianPeso", Set("COP","Colombian peso")));
	    // ??? builder.addUnit(new InconvertibleUnitDatatype("unidadDeValorReal", Set("COU","Unidad de Valor Real")));
	    builder.addUnit(new InconvertibleUnitDatatype("costaRicanColon", Set("CRC","Costa Rican colon","Costa Rican colón")));
	    builder.addUnit(new InconvertibleUnitDatatype("cubanPeso", Set("CUP","Cuban peso")));
	    builder.addUnit(new InconvertibleUnitDatatype("capeVerdeEscudo", Set("CVE","Cape Verde escudo")));
	    builder.addUnit(new InconvertibleUnitDatatype("czechKoruna", Set("CZK","Czech koruna")));
	    builder.addUnit(new InconvertibleUnitDatatype("djiboutianFranc", Set("DJF","Djibouti franc")));
	    builder.addUnit(new InconvertibleUnitDatatype("danishKrone", Set("DKK","Danish krone")));
	    builder.addUnit(new InconvertibleUnitDatatype("dominicanPeso", Set("DOP","Dominican peso")));
	    builder.addUnit(new InconvertibleUnitDatatype("algerianDinar", Set("DZD","Algerian dinar")));
	    builder.addUnit(new InconvertibleUnitDatatype("estonianKroon", Set("EEK","Kroon")));
	    builder.addUnit(new InconvertibleUnitDatatype("egyptianPound", Set("EGP","Egyptian pound")));
	    builder.addUnit(new InconvertibleUnitDatatype("eritreanNakfa", Set("ERN","Nakfa")));
	    builder.addUnit(new InconvertibleUnitDatatype("ethiopianBirr", Set("ETB","Ethiopian birr")));
	    builder.addUnit(new InconvertibleUnitDatatype("fijiDollar", Set("FJD","Fiji dollar")));
	    builder.addUnit(new InconvertibleUnitDatatype("falklandIslandsPound", Set("FKP","Falkland Islands pound")));
	    builder.addUnit(new InconvertibleUnitDatatype("georgianLari", Set("GEL","Lari")));
	    builder.addUnit(new InconvertibleUnitDatatype("ghanaianCedi", Set("GHS","Cedi")));
	    builder.addUnit(new InconvertibleUnitDatatype("gibraltarPound", Set("GIP","Gibraltar pound")));
	    builder.addUnit(new InconvertibleUnitDatatype("gambianDalasi", Set("GMD","Dalasi")));
	    builder.addUnit(new InconvertibleUnitDatatype("guineaFranc", Set("GNF","Guinea franc")));
	    builder.addUnit(new InconvertibleUnitDatatype("guatemalanQuetzal", Set("GTQ","Quetzal")));
	    builder.addUnit(new InconvertibleUnitDatatype("guyanaDollar", Set("GYD","Guyana dollar")));
	    builder.addUnit(new InconvertibleUnitDatatype("hongKongDollar", Set("HKD","Hong Kong dollar")));
	    builder.addUnit(new InconvertibleUnitDatatype("honduranLempira", Set("HNL","Lempira")));
	    builder.addUnit(new InconvertibleUnitDatatype("croatianKuna", Set("HRK","Croatian kuna")));
	    builder.addUnit(new InconvertibleUnitDatatype("haitiGourde", Set("HTG","Haiti gourde")));
	    builder.addUnit(new InconvertibleUnitDatatype("hungarianForint", Set("HUF","Forint")));
	    builder.addUnit(new InconvertibleUnitDatatype("indonesianRupiah", Set("IDR","Rupiah")));
	    builder.addUnit(new InconvertibleUnitDatatype("israeliNewSheqel", Set("ILS","Israeli new sheqel")));
	    builder.addUnit(new InconvertibleUnitDatatype("indianRupee", Set("INR","Indian rupee")));
	    builder.addUnit(new InconvertibleUnitDatatype("iraqiDinar", Set("IQD","Iraqi dinar")));
	    builder.addUnit(new InconvertibleUnitDatatype("iranianRial", Set("IRR","Iranian rial")));
	    builder.addUnit(new InconvertibleUnitDatatype("icelandKrona", Set("ISK","Iceland krona")));
	    builder.addUnit(new InconvertibleUnitDatatype("jamaicanDollar", Set("JMD","Jamaican dollar")));
	    builder.addUnit(new InconvertibleUnitDatatype("jordanianDinar", Set("JOD","Jordanian dinar")));
	    builder.addUnit(new InconvertibleUnitDatatype("kenyanShilling", Set("KES","Kenyan shilling")));
	    builder.addUnit(new InconvertibleUnitDatatype("kyrgyzstaniSom", Set("KGS","Som")));
	    builder.addUnit(new InconvertibleUnitDatatype("uzbekistanSom", Set("UZS","Uzbekistan som")));
	    builder.addUnit(new InconvertibleUnitDatatype("cambodianRiel", Set("KHR","Riel")));
	    builder.addUnit(new InconvertibleUnitDatatype("comorianFranc", Set("KMF","Comoro franc")));
	    builder.addUnit(new InconvertibleUnitDatatype("northKoreanWon", Set("KPW","North Korean won")));
	    builder.addUnit(new InconvertibleUnitDatatype("southKoreanWon", Set("KRW","South Korean won")));
	    builder.addUnit(new InconvertibleUnitDatatype("kuwaitiDinar", Set("KWD","Kuwaiti dinar")));
	    builder.addUnit(new InconvertibleUnitDatatype("caymanIslandsDollar", Set("KYD","Cayman Islands dollar")));
	    builder.addUnit(new InconvertibleUnitDatatype("kazakhstaniTenge", Set("KZT","Tenge")));
	    builder.addUnit(new InconvertibleUnitDatatype("laoKip", Set("LAK","Kip")));
	    builder.addUnit(new InconvertibleUnitDatatype("lebanesePound", Set("LBP","Lebanese pound")));
	    builder.addUnit(new InconvertibleUnitDatatype("sriLankanRupee", Set("LKR","Sri Lanka rupee")));
	    builder.addUnit(new InconvertibleUnitDatatype("liberianDollar", Set("LRD","Liberian dollar")));
	    builder.addUnit(new InconvertibleUnitDatatype("lesothoLoti", Set("LSL","Loti")));
	    builder.addUnit(new InconvertibleUnitDatatype("lithuanianLitas", Set("LTL","Lithuanian litas")));
	    builder.addUnit(new InconvertibleUnitDatatype("latvianLats", Set("LVL","Latvian lats")));
	    builder.addUnit(new InconvertibleUnitDatatype("libyanDinar", Set("LYD","Libyan dinar")));
	    builder.addUnit(new InconvertibleUnitDatatype("moroccanDirham", Set("MAD","Moroccan dirham")));
	    builder.addUnit(new InconvertibleUnitDatatype("moldovanLeu", Set("MDL","Moldovan leu")));
	    builder.addUnit(new InconvertibleUnitDatatype("malagasyAriary", Set("MGA","Malagasy ariary")));
	    builder.addUnit(new InconvertibleUnitDatatype("macedonianDenar", Set("MKD","Denar")));
	    builder.addUnit(new InconvertibleUnitDatatype("myanmaKyat", Set("MMK","Kyat")));
	    builder.addUnit(new InconvertibleUnitDatatype("mongolianTögrög", Set("MNT","Tugrik")));
	    builder.addUnit(new InconvertibleUnitDatatype("macanesePataca", Set("MOP","Pataca")));
	    builder.addUnit(new InconvertibleUnitDatatype("mauritanianOuguiya", Set("MRO","Ouguiya")));
	    builder.addUnit(new InconvertibleUnitDatatype("mauritianRupee", Set("MUR","Mauritius rupee")));
	    builder.addUnit(new InconvertibleUnitDatatype("maldivianRufiyaa", Set("MVR","Rufiyaa")));
	    builder.addUnit(new InconvertibleUnitDatatype("malawianKwacha", Set("MWK","malawian kwacha"))); // TODO: "kwacha" is also used, but clashes with zambianKwacha.
	    builder.addUnit(new InconvertibleUnitDatatype("zambianKwacha", Set("ZMK","zambian kwacha"))); // TODO: "kwacha" is also used, but clashes with malawianKwacha.
	    builder.addUnit(new InconvertibleUnitDatatype("mexicanPeso", Set("MXN","Mexican peso")));
	    builder.addUnit(new InconvertibleUnitDatatype("malaysianRinggit", Set("MYR","Malaysian ringgit")));
	    builder.addUnit(new InconvertibleUnitDatatype("mozambicanMetical", Set("MZN","Metical")));
	    builder.addUnit(new InconvertibleUnitDatatype("namibianDollar", Set("NAD","Namibian dollar")));
	    builder.addUnit(new InconvertibleUnitDatatype("nigerianNaira", Set("NGN","Naira","naira")));
	    builder.addUnit(new InconvertibleUnitDatatype("nicaraguanCórdoba", Set("NIO","Cordoba oro", "C")));
	    builder.addUnit(new InconvertibleUnitDatatype("norwegianKrone", Set("NOK","Norwegian krone")));
	    builder.addUnit(new InconvertibleUnitDatatype("nepaleseRupee", Set("NPR","Nepalese rupee")));
	    builder.addUnit(new InconvertibleUnitDatatype("newZealandDollar", Set("NZD","New Zealand dollar")));
	    builder.addUnit(new InconvertibleUnitDatatype("omaniRial", Set("OMR","Rial Omani", "Omani rial")));
	    builder.addUnit(new InconvertibleUnitDatatype("panamanianBalboa", Set("PAB","Balboa")));
	    builder.addUnit(new InconvertibleUnitDatatype("peruvianNuevoSol", Set("PEN","Nuevo sol")));
	    builder.addUnit(new InconvertibleUnitDatatype("papuaNewGuineanKina", Set("PGK","Kina")));
	    builder.addUnit(new InconvertibleUnitDatatype("philippinePeso", Set("PHP","Philippine peso")));
	    builder.addUnit(new InconvertibleUnitDatatype("pakistaniRupee", Set("PKR","Pakistan rupee")));
	    builder.addUnit(new InconvertibleUnitDatatype("polishZłoty", Set("PLN","Zloty", "Złoty")));
	    builder.addUnit(new InconvertibleUnitDatatype("paraguayanGuarani", Set("PYG","Guarani")));
	    builder.addUnit(new InconvertibleUnitDatatype("qatariRial", Set("QAR","Qatari rial")));
	    builder.addUnit(new InconvertibleUnitDatatype("romanianNewLeu", Set("RON","Romanian new leu")));
	    builder.addUnit(new InconvertibleUnitDatatype("serbianDinar", Set("RSD","Serbian dinar")));
	    builder.addUnit(new InconvertibleUnitDatatype("rwandaFranc", Set("RWF","Rwanda franc")));
	    builder.addUnit(new InconvertibleUnitDatatype("saudiRiyal", Set("SAR","Saudi riyal")));
	    builder.addUnit(new InconvertibleUnitDatatype("solomonIslandsDollar", Set("SBD","Solomon Islands dollar")));
	    builder.addUnit(new InconvertibleUnitDatatype("seychellesRupee", Set("SCR","Seychelles rupee")));
	    builder.addUnit(new InconvertibleUnitDatatype("sudanesePound", Set("SDG","Sudanese pound")));
	    builder.addUnit(new InconvertibleUnitDatatype("swedishKrona", Set("SEK","kr","Swedish krona")));
	    builder.addUnit(new InconvertibleUnitDatatype("singaporeDollar", Set("SGD","Singapore dollar")));
	    builder.addUnit(new InconvertibleUnitDatatype("saintHelenaPound", Set("SHP","Saint Helena pound")));
	    builder.addUnit(new InconvertibleUnitDatatype("slovakKoruna", Set("SKK","Slovak koruna")));
	    builder.addUnit(new InconvertibleUnitDatatype("sierraLeoneanLeone", Set("SLL","Leone")));
	    builder.addUnit(new InconvertibleUnitDatatype("somaliShilling", Set("SOS","Somali shilling")));
	    builder.addUnit(new InconvertibleUnitDatatype("surinamDollar", Set("SRD","Surinam dollar")));
	    builder.addUnit(new InconvertibleUnitDatatype("sãoToméAndPríncipeDobra", Set("STD","Dobra")));
	    builder.addUnit(new InconvertibleUnitDatatype("syrianPound", Set("SYP","Syrian pound")));
	    builder.addUnit(new InconvertibleUnitDatatype("swaziLilangeni", Set("SZL","Lilangeni")));
	    builder.addUnit(new InconvertibleUnitDatatype("thaiBaht", Set("THB","Baht")));
	    builder.addUnit(new InconvertibleUnitDatatype("tajikistaniSomoni", Set("TJS","Somoni")));
	    builder.addUnit(new InconvertibleUnitDatatype("turkmenistaniManat", Set("TMT","turkmenistani manat"))); // old code: TMM
	    builder.addUnit(new InconvertibleUnitDatatype("azerbaijaniManat", Set("AZN","azerbaijani manat","azerbaijanian manat")));
	    builder.addUnit(new InconvertibleUnitDatatype("tunisianDinar", Set("TND","Tunisian dinar")));
	    builder.addUnit(new InconvertibleUnitDatatype("tonganPaanga", Set("TOP","Paanga"))); // correct: "Tongan Paʻanga"
	    builder.addUnit(new InconvertibleUnitDatatype("turkishLira", Set("TRY","turkish lira")));
	    builder.addUnit(new InconvertibleUnitDatatype("trinidadAndTobagoDollar", Set("TTD","Trinidad and Tobago dollar")));
	    builder.addUnit(new InconvertibleUnitDatatype("newTaiwanDollar", Set("TWD","New Taiwan dollar")));
	    builder.addUnit(new InconvertibleUnitDatatype("tanzanianShilling", Set("TZS","Tanzanian shilling")));
	    builder.addUnit(new InconvertibleUnitDatatype("ukrainianHryvnia", Set("UAH","Hryvnia")));
	    builder.addUnit(new InconvertibleUnitDatatype("ugandaShilling", Set("UGX","Uganda shilling")));
	    builder.addUnit(new InconvertibleUnitDatatype("uruguayanPeso", Set("UYU","Peso Uruguayo")));
	    builder.addUnit(new InconvertibleUnitDatatype("venezuelanBolívar", Set("VEF","Venezuelan bolívar fuerte")));
	    builder.addUnit(new InconvertibleUnitDatatype("vanuatuVatu", Set("VUV","Vatu")));
	    builder.addUnit(new InconvertibleUnitDatatype("samoanTala", Set("WST","Samoan tala")));
	    builder.addUnit(new InconvertibleUnitDatatype("centralAfricanCfaFranc", Set("XAF","CFA franc BEAC")));
	    builder.addUnit(new InconvertibleUnitDatatype("eastCaribbeanDollar", Set("XCD","East Caribbean dollar")));
	    builder.addUnit(new InconvertibleUnitDatatype("westAfricanCfaFranc", Set("XOF","CFA Franc BCEAO")));
	    builder.addUnit(new InconvertibleUnitDatatype("cfpFranc", Set("XPF","CFP franc")));
	    builder.addUnit(new InconvertibleUnitDatatype("yemeniRial", Set("YER","Yemeni rial")));
	    builder.addUnit(new InconvertibleUnitDatatype("southAfricanRand", Set("ZAR","South African rand")));
	    builder.addUnit(new InconvertibleUnitDatatype("zimbabweanDollar", Set("ZWD","Zimbabwe dollar")));
	    types :::= builder.build
	
	    builder.addDimension("Density");
	    builder.addUnit(new StandardUnitDatatype("kilogramPerCubicMetre", Set("kg·m−3","kg/m³","kg/m3","kg·m","kilogram per cubic metre")));
	    builder.addUnit(new FactorUnitDatatype("kilogramPerLitre", Set("kg/l","kg/L","kilogram per litre"), 1000.0));
	    builder.addUnit(new FactorUnitDatatype("gramPerCubicCentimetre", Set("g/cc","g/cm3","g/cm³","gram per cubic centimetre"), 1000.0));
	    builder.addUnit(new FactorUnitDatatype("gramPerMillilitre", Set("g/ml","g/mL","gram per millilitre"), 1000.0));
	    types :::= builder.build

	    builder.addDimension("Energy");
	    builder.addUnit(new StandardUnitDatatype("joule", Set("J","joule")));
	    builder.addUnit(new FactorUnitDatatype("kilojoule", Set("kJ","kilojoule"), 1000.0));
	    builder.addUnit(new FactorUnitDatatype("erg", Set("erg"), 1.0E-7));
	    builder.addUnit(new FactorUnitDatatype("milliwattHour", Set("mWh","milliwatt-hour"), 3.6));
	    builder.addUnit(new FactorUnitDatatype("wattHour", Set("Wh","watt-hour"), 3600.0));
	    builder.addUnit(new FactorUnitDatatype("kilowattHour", Set("kWh","kilowatt-hour"), 3600000.0));
	    builder.addUnit(new FactorUnitDatatype("megawattHour", Set("MWh","megawatt-hour"), 3600000000.0));
	    builder.addUnit(new FactorUnitDatatype("gigawattHour", Set("GWh","gigawatt-hour"), 3600000000000.0));
	    builder.addUnit(new FactorUnitDatatype("terawattHour", Set("TWh","terawatt-hour"), 3600000000000000.0));
	    // builder.addUnit(new FactorUnitDatatype("electronVolt", Set("eV","electron volt"), "missing conversion factor"));
	    builder.addUnit(new FactorUnitDatatype("millicalorie", Set("mcal","millicalorie"), 0.0041868));
	    builder.addUnit(new FactorUnitDatatype("calorie", Set("cal","calorie"), 4.1868));
	    builder.addUnit(new FactorUnitDatatype("kilocalorie", Set("kcal","kilocalorie"), 4186.8));
	    builder.addUnit(new FactorUnitDatatype("megacalorie", Set("Mcal","megacalorie"), 4186800.0));
	    builder.addUnit(new FactorUnitDatatype("inchPound", Set("inlb","inch-pound"), 0.11298482902));
	    builder.addUnit(new FactorUnitDatatype("footPound", Set("ftlb","foot-pound"), 1.3558179483));
	    types :::= builder.build

	    builder.addDimension("FlowRate");
	    builder.addUnit(new StandardUnitDatatype("cubicMetrePerSecond", Set("m\u00B3/s","m³/s","cubic metre per second")));
	    builder.addUnit(new FactorUnitDatatype("cubicFeetPerSecond", Set("ft\u00B3/s","ft³/s","cuft/s","cubic feet per second"), 0.028316846593));
	    builder.addUnit(new FactorUnitDatatype("cubicMetrePerYear", Set("m\u00B3/y","m³/y","cubic metre per year"), 3.1709791983765E-8));
	    builder.addUnit(new FactorUnitDatatype("cubicFeetPerYear", Set("ft\u00B3/y","ft³/y","cubic feet per year"), 8.9792131512047E-10));
	    types :::= builder.build

	    builder.addDimension("Force");
	    builder.addUnit(new StandardUnitDatatype("newton", Set("N","newton")));
	    builder.addUnit(new FactorUnitDatatype("nanonewton", Set("nN","nanonewton"), 1.0E-9));
	    builder.addUnit(new FactorUnitDatatype("millinewton", Set("mN","millinewton"), 0.001));
	    builder.addUnit(new FactorUnitDatatype("kilonewton", Set("kN","kilonewton"), 1000.0));
	    builder.addUnit(new FactorUnitDatatype("meganewton", Set("MN","meganewton"), 1000000.0));
	    builder.addUnit(new FactorUnitDatatype("giganewton", Set("GN","giganewton"), 1000000000.0));
	    builder.addUnit(new FactorUnitDatatype("tonneForce", Set("tf","t-f","tonne-force"), 9806.65));
	    builder.addUnit(new FactorUnitDatatype("megapond", Set("Mp","megapond"), 9806.65));
	    builder.addUnit(new FactorUnitDatatype("kilogramForce", Set("kgf","kg-f","kilogram-force"), 9.80665));
	    builder.addUnit(new FactorUnitDatatype("kilopond", Set("kp","kilopond"), 9.80665));
	    builder.addUnit(new FactorUnitDatatype("gramForce", Set("gf","g-f","gram-force"), 0.00980665));
	    builder.addUnit(new FactorUnitDatatype("pond", Set("p","pond"), 0.00980665));
	    builder.addUnit(new FactorUnitDatatype("milligramForce", Set("mgf","mg-f","milligram-force"), 0.00980665));
	    builder.addUnit(new FactorUnitDatatype("millipond", Set("mp","millipond"), 0.00980665));
	    builder.addUnit(new FactorUnitDatatype("poundal", Set("pdl","poundal"), 0.1383));
	    types :::= builder.build

	    builder.addDimension("Frequency");
	    builder.addUnit(new StandardUnitDatatype("hertz", Set("Hz","hertz")));
	    builder.addUnit(new FactorUnitDatatype("millihertz", Set("mHz","millihertz"), 0.001));
	    builder.addUnit(new FactorUnitDatatype("kilohertz", Set("kHz","kilohertz"), 1000.0));
	    builder.addUnit(new FactorUnitDatatype("megahertz", Set("MHz","megahertz"), 1000000.0));
	    builder.addUnit(new FactorUnitDatatype("gigahertz", Set("GHz","gigahertz"), 1000000000.0));
        builder.addUnit(new FactorUnitDatatype("terahertz", Set("THz","terahertz"), 1000000000000.0));
	    types :::= builder.build

	    builder.addDimension("FuelEfficiency");
	    builder.addUnit(new StandardUnitDatatype("kilometresPerLitre", Set("km/l","km/L","kilometres per litre")));
	    // builder.addUnit(new FactorUnitDatatype("litresPerKilometre", Set("l/km","L/km","litres per kilometre"), "missing conversion factor"));
	    // builder.addUnit(new FactorUnitDatatype("milesPerImperialGallon", Set("mpgimp","miles per imperial gallon"), "missing conversion factor"));
	    // builder.addUnit(new FactorUnitDatatype("milesPerUsGallon", Set("mpgus","miles per US gallon"), "missing conversion factor"));
	    // builder.addUnit(new FactorUnitDatatype("imperialGallonsPerMile", Set("impgal/mi","imperial gallons per mile"), "missing conversion factor"));
	    // builder.addUnit(new FactorUnitDatatype("usGallonsPerMile", Set("usgal/mi","US gallons per mile"), "missing conversion factor"));
	    types :::= builder.build

	    builder.addDimension("InformationUnit");
	    builder.addUnit(new StandardUnitDatatype("byte", Set("B","byte")));
	    builder.addUnit(new FactorUnitDatatype("bit", Set("bit"), 0.125));
	    builder.addUnit(new FactorUnitDatatype("kilobit", Set("kbit","kilobit"), 128.0));
	    builder.addUnit(new FactorUnitDatatype("megabit", Set("Mbit","megabit"), 131072.0));
	    builder.addUnit(new FactorUnitDatatype("kilobyte", Set("kB","kilobyte"), 1000.0));
	    builder.addUnit(new FactorUnitDatatype("megabyte", Set("MB","megabyte"), 1000000.0));
	    builder.addUnit(new FactorUnitDatatype("gigabyte", Set("GB","gigabyte"), 1000000000.0));
	    builder.addUnit(new FactorUnitDatatype("terabyte", Set("TB","terabyte"), 1000000000000.0));
	    types :::= builder.build

	    builder.addDimension("Length");
	    builder.addUnit(new StandardUnitDatatype("metre", Set("m","meter","metres","metre")));
	    builder.addUnit(new FactorUnitDatatype("nanometre", Set("nm","nanometre"), 1.0E-9));
	    builder.addUnit(new FactorUnitDatatype("micrometre", Set("µm","micrometre"), 1.0E-6));
	    builder.addUnit(new FactorUnitDatatype("millimetre", Set("mm","millimetre"), 0.001));
	    builder.addUnit(new FactorUnitDatatype("centimetre", Set("cm","centimetre"), 0.01));
	    builder.addUnit(new FactorUnitDatatype("decimetre", Set("dm","decimetre"), 0.1));
	    builder.addUnit(new FactorUnitDatatype("decametre", Set("dam","decametre"), 10.0));
	    builder.addUnit(new FactorUnitDatatype("hectometre", Set("hm","hectometre"), 100.0));
	    builder.addUnit(new FactorUnitDatatype("kilometre", Set("km","kilometre"), 1000.0));
	    builder.addUnit(new FactorUnitDatatype("megametre", Set("Mm","megametre"), 1000000.0));
	    builder.addUnit(new FactorUnitDatatype("gigametre", Set("Gm","gigametre"), 1000000000.0));
	    builder.addUnit(new FactorUnitDatatype("inch", Set("in","inch","''"), 0.0254));
	    builder.addUnit(new FactorUnitDatatype("hand", Set("hand"), 0.1016));
	    builder.addUnit(new FactorUnitDatatype("foot", Set("ft","feet","foot"), 0.3048));
	    builder.addUnit(new FactorUnitDatatype("yard", Set("yd","yard"), 0.9144));
	    builder.addUnit(new FactorUnitDatatype("fathom", Set("fathom"), 1.8288));
	    builder.addUnit(new FactorUnitDatatype("rod", Set("rd","perch","pole","rod"), 5.0292));
	    builder.addUnit(new FactorUnitDatatype("chain", Set("chain"), 20.1168));
	    builder.addUnit(new FactorUnitDatatype("furlong", Set("furlong"), 201.168));
	    builder.addUnit(new FactorUnitDatatype("mile", Set("mi","miles","mile"), 1609.344));
	    builder.addUnit(new FactorUnitDatatype("nautialMile", Set("nmi","nautial mile"), 1852.01));
	    builder.addUnit(new FactorUnitDatatype("astronomicalUnit", Set("AU","astronomical unit"), 149597870691.0));
	    builder.addUnit(new FactorUnitDatatype("lightYear", Set("ly","light-year"), 9460730472580800.0));
	    builder.addUnit(new FactorUnitDatatype("kilolightYear", Set("kly","kilolight-year"), 9.4607304725808E+18));
	    types :::= builder.build

	    builder.addDimension("Mass");
	    builder.addUnit(new StandardUnitDatatype("gram", Set("g","gram")));
	    builder.addUnit(new FactorUnitDatatype("milligram", Set("mg","milligram"), 0.001));
	    builder.addUnit(new FactorUnitDatatype("kilogram", Set("kg","kilogram"), 1000.0));
	    builder.addUnit(new FactorUnitDatatype("tonne", Set("t","tonne"), 1000000.0));
	    builder.addUnit(new FactorUnitDatatype("stone", Set("st","stone"), 6350.29318));
	    builder.addUnit(new FactorUnitDatatype("pound", Set("lb","lbs","lbm","pound"), 453.6));
	    builder.addUnit(new FactorUnitDatatype("ounce", Set("oz","ounce"), 28.35));
	    builder.addUnit(new FactorUnitDatatype("grain", Set("gr","grain"), 0.0648));
	    builder.addUnit(new FactorUnitDatatype("carat", Set("carat"), 0.2));
	    // builder.addUnit(new FactorUnitDatatype("atomicMassUnit", Set("Da","u","atomic mass unit"), "missing conversion factor"));
	    types :::= builder.build

	    builder.addDimension("PopulationDensity");
	    builder.addUnit(new StandardUnitDatatype("inhabitantsPerSquareKilometre", Set("PD/sqkm","/sqkm","per square kilometre","inhabitants per square kilometre")));
	    // builder.addUnit(new FactorUnitDatatype("inhabitantsPerHectare", Set("PD/ha","/ha","per hectare","inhabitants per hectare"), "missing conversion factor"));
	    builder.addUnit(new FactorUnitDatatype("inhabitantsPerSquareMile", Set("PD/sqmi","/sqmi","per square mile","inhabitants per square mile"), 1.0 / 2.589988110336));
	    // builder.addUnit(new FactorUnitDatatype("inhabitantsPerAcre", Set("PD/acre","/acre","per acre","inhabitants per acre"), "missing conversion factor"));
	    types :::= builder.build

	    builder.addDimension("Power");
	    builder.addUnit(new StandardUnitDatatype("watt", Set("W","watt")));
	    builder.addUnit(new FactorUnitDatatype("kilowatt", Set("kW","kilowatt"), 1000.0));
	    builder.addUnit(new FactorUnitDatatype("milliwatt", Set("mW","milliwatt"), 0.001));
	    builder.addUnit(new FactorUnitDatatype("megawatt", Set("MW","megawatt"), 1000000.0));
	    builder.addUnit(new FactorUnitDatatype("gigawatt", Set("GW","gigawatt"), 1000000000.0));
	    builder.addUnit(new FactorUnitDatatype("horsepower", Set("hp","horsepower"), 745.72218));
	    builder.addUnit(new FactorUnitDatatype("pferdestaerke", Set("PS","pferdestaerke"), 735.49875));
	    builder.addUnit(new FactorUnitDatatype("brake horsepower", Set("bhp","brake horsepower"), 745.7));
	    types :::= builder.build

	    builder.addDimension("Pressure");
	    builder.addUnit(new StandardUnitDatatype("pascal", Set("Pa","pascal")));
	    builder.addUnit(new FactorUnitDatatype("millipascal", Set("mPa","millipascal"), 0.001));
	    builder.addUnit(new FactorUnitDatatype("hectopascal", Set("hPa","hectopascal"), 0.01));
	    builder.addUnit(new FactorUnitDatatype("kilopascal", Set("kPa","kilopascal"), 1000.0));
	    builder.addUnit(new FactorUnitDatatype("megapascal", Set("MPa","megapascal"), 1000000.0));
	    builder.addUnit(new FactorUnitDatatype("millibar", Set("mbar","mb","millibar"), 100.0));
	    builder.addUnit(new FactorUnitDatatype("decibar", Set("dbar","decibar"), 10000.0));
	    builder.addUnit(new FactorUnitDatatype("bar", Set("bar"), 100000.0));
	    builder.addUnit(new FactorUnitDatatype("standardAtmosphere", Set("atm","standard atmosphere"), 101325.0));
	    builder.addUnit(new FactorUnitDatatype("poundPerSquareInch", Set("psi","pound per square inch"), 6894.7572932));
	    types :::= builder.build

	    builder.addDimension("Speed");
	    builder.addUnit(new StandardUnitDatatype("kilometrePerHour", Set("km/h","kmh","kilometre per hour")));
	    builder.addUnit(new FactorUnitDatatype("metrePerSecond", Set("m/s","ms","metre per second"), 3.6));
	    builder.addUnit(new FactorUnitDatatype("kilometrePerSecond", Set("km/s","kilometre per second"), 3600.0));
	    builder.addUnit(new FactorUnitDatatype("milePerHour", Set("mph","mi/h","mile per hour"), 1.60934));
	    builder.addUnit(new FactorUnitDatatype("footPerSecond", Set("ft/s","foot per second"), 0.0003048333333));
	    builder.addUnit(new FactorUnitDatatype("footPerMinute", Set("ft/min","foot per minute"), 0.01829));
	    builder.addUnit(new FactorUnitDatatype("knot", Set("kn","knot"), 1.852));
	    types :::= builder.build

	    builder.addDimension("Temperature");
	    builder.addUnit(new StandardUnitDatatype("kelvin", Set("K","kelvin")));
      builder.addUnit(new FactorUnitDatatype("degreeCelsius", Set("°C","degree celsius","C","Celsius"), 1.0, 273.15));
	    builder.addUnit(new FactorUnitDatatype("degreeFahrenheit", Set("°F","F","Fahrenheit","degree fahrenheit"), 5.0 / 9.0, 459.67));
	    builder.addUnit(new FactorUnitDatatype("degreeRankine", Set("°R","R","degree rankine"), 5.0 / 9.0, 0));
	    types :::= builder.build

	    builder.addDimension("Time");
	    builder.addUnit(new StandardUnitDatatype("second", Set("s","sec","secs","second","seconds")));
	    builder.addUnit(new FactorUnitDatatype("minute", Set("m","min","min.","mins","minute","minutes"), 60.0));
	    builder.addUnit(new FactorUnitDatatype("hour", Set("h","hr","hr.","hour","hours","std"), 3600.0));
	    builder.addUnit(new FactorUnitDatatype("day", Set("d","days","day"), 86400.0));
	    types :::= builder.build

	    builder.addDimension("Torque");
	    builder.addUnit(new StandardUnitDatatype("newtonMetre", Set("Nm","N.m","N·m", "newton metre")));
	    builder.addUnit(new FactorUnitDatatype("newtonMillimetre", Set("Nmm","newton millimetre"), 0.001));
	    builder.addUnit(new FactorUnitDatatype("newtonCentimetre", Set("Ncm","newton centimetre"), 0.001));
	    builder.addUnit(new FactorUnitDatatype("poundFoot", Set("ftlb-f", "ftlbf", "lbft"), 1.3558179483314));
	    types :::= builder.build

	    builder.addDimension("Volume");
	    builder.addUnit(new StandardUnitDatatype("cubicMetre", Set("m3","m³","cubic metre")));
	    builder.addUnit(new FactorUnitDatatype("cubicMillimetre", Set("mm3","mm³","cubic millimetre"), 1.0E-9));
	    builder.addUnit(new FactorUnitDatatype("cubicCentimetre", Set("cm3","cm³","cc","cubic centimetre"), 1.0E-6));
	    builder.addUnit(new FactorUnitDatatype("cubicDecimetre", Set("dm3","dm³","cubic decimetre"), 0.001));
	    builder.addUnit(new FactorUnitDatatype("cubicDecametre", Set("dam3","cubic decametre"), 1000.0));
	    builder.addUnit(new FactorUnitDatatype("cubicHectometre", Set("hm3","hm³","cubic hectometre"), 1000000.0));
	    builder.addUnit(new FactorUnitDatatype("cubicKilometre", Set("km3","km³","cubic kilometre"), 1000000000.0));
	    builder.addUnit(new FactorUnitDatatype("microlitre", Set("ul","uL","microlitre"), 1.0E-9));
	    builder.addUnit(new FactorUnitDatatype("millilitre", Set("ml","mL","millilitre"), 1.0E-6));
	    builder.addUnit(new FactorUnitDatatype("centilitre", Set("cl","cL","centilitre"), 1.0E-5));
	    builder.addUnit(new FactorUnitDatatype("decilitre", Set("dl","dL","decilitre"), 0.0001));
	    builder.addUnit(new FactorUnitDatatype("litre", Set("l","L","litre"), 0.001));
	    // builder.addUnit(new FactorUnitDatatype("decalitre", Set("dal","daL","decalitre"), "missing conversion factor"));
	    builder.addUnit(new FactorUnitDatatype("hectolitre", Set("hl","hL","hectolitre"), 0.1));
	    builder.addUnit(new FactorUnitDatatype("kilolitre", Set("kl","kL","kilolitre"), 1.0));
	    builder.addUnit(new FactorUnitDatatype("megalitre", Set("Ml","ML","megalitre"), 1000.0));
	    builder.addUnit(new FactorUnitDatatype("gigalitre", Set("Gl","GL","gigalitre"), 1000000.0));
	    builder.addUnit(new FactorUnitDatatype("cubicMile", Set("cumi","mi3","mi³","cubic mile"), 4168181825.4406));
	    builder.addUnit(new FactorUnitDatatype("cubicYard", Set("cuyd","yd3","cubic yard"), 0.764692));
	    builder.addUnit(new FactorUnitDatatype("cubicFoot", Set("cuft","ft3","ft³","cubic foot"), 0.0283219));
	    builder.addUnit(new FactorUnitDatatype("cubicInch", Set("cuin","in3","in³","cubic inch"), 1.639E-5));
	    builder.addUnit(new FactorUnitDatatype("imperialBarrel", Set("impbl","imperial barrel"), 0.163659));
	    builder.addUnit(new FactorUnitDatatype("usBarrel", Set("usbl","us barrel"), 0.11924));
	    builder.addUnit(new FactorUnitDatatype("imperialBarrelOil", Set("impbbl","imperial barrel oil"), 0.159113));
	    builder.addUnit(new FactorUnitDatatype("usBarrelOil", Set("usbbl","us barrel oil"), 0.158987));
	    builder.addUnit(new FactorUnitDatatype("imperialGallon", Set("impgal","imperial gallon"), 0.00454609));
	    builder.addUnit(new FactorUnitDatatype("usGallon", Set("usgal","USgal","us gallon"), 0.00378541178));
	    types :::= builder.build

	    builder.addDimension("LinearMassDensity");
	    builder.addUnit(new StandardUnitDatatype("gramPerKilometre", Set("g/km")));
	    types :::= builder.build
	    
	    return types
	}
	
	private class UnitBuilder
	{
	    private var currentDimension : String = null;
	    private var currentUnits : List[UnitDatatype] = List.empty
	    
	    def addDimension(name : String)
	    {
	    	currentDimension = name
	    	currentUnits = List.empty
	    }
	    
	    def addUnit(unit : UnitDatatype)
	    {
	        currentUnits = unit :: currentUnits
	    }
	    
	    def build = new DimensionDatatype(currentDimension, currentUnits.reverse) :: currentUnits
	}
}
