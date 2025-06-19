package org.dbpedia.extraction.config.dataparser

object GeoCoordinateParserConfig {
  // Comprehensive set of coordinate template names used across different Wikipedia languages
  val coordTemplateNames = Set(
    // Basic coordinate templates
    "coord", "coor", "coords", "coordinate", "coordinates", "geocoordinate", "geo",

    // DMS (Degrees, Minutes, Seconds) templates
    "coor dms", "coor dm", "coor d", "coord dms", "coord dm", "coord d",
    "coor title dms", "coor title d", "coor title dm", "coorheader",
    "coor at dm", "coor at dms", "coor at d",
    "coor d/new", "coor dm/new", "coor dms/new", "coor dec", "coor/new",
    "coor dms/archive001", "coord/conversion", "coord/templates",

    // Location templates
    "location", "location dec", "location dms", "location dm",
    "geobox", "infobox settlement", "settlement", "city",

    // Multilingual coordinate templates - FIXED: Added missing templates
    "coordenadas", "koordinaten", "koord", "współrzędne", "координаты",
    "coordonnées", "coordinaten", // Added French and Dutch templates
    "तालुका निर्देशांक", "निर्देशांक", "অবস্থান", "स्थान",

    // Additional format templates
    "point", "position", "lat", "lon", "latitude", "longitude",
    "geo-lat", "geo-lon", "geo-dec", "geo-dms"
  )

  // Map longitude letters used in different languages to English ("E" for East and "W" for West)
  val longitudeLetterMap = Map(
    "am" -> Map("E" -> "E", "W" -> "W", "ምስራቅ" -> "E", "ምዕራብ" -> "W"),
    "ar" -> Map("E" -> "E", "W" -> "W", "ق" -> "E", "غ" -> "W", "شرق" -> "E", "غرب" -> "W"),
    "bg" -> Map("E" -> "E", "W" -> "W", "И" -> "E", "З" -> "W", "изток" -> "E", "запад" -> "W"),
    "cs" -> Map("E" -> "E", "W" -> "W", "V" -> "E", "Z" -> "W", "východ" -> "E", "západ" -> "W"),
    "de" -> Map("E" -> "E", "O" -> "E", "W" -> "W", "Ost" -> "E", "West" -> "W"),
    "en" -> Map("E" -> "E", "W" -> "W", "East" -> "E", "West" -> "W"),
    "es" -> Map("E" -> "E", "W" -> "W", "O" -> "W", "Este" -> "E", "Oeste" -> "W"),
    "fr" -> Map("E" -> "E", "O" -> "E", "W" -> "W", "Est" -> "E", "Ouest" -> "W"),
    "hi" -> Map("E" -> "E", "W" -> "W", "पू" -> "E", "प" -> "W", "पूर्व" -> "E", "पश्चिम" -> "W"),
    "it" -> Map("E" -> "E", "W" -> "W", "O" -> "W", "Est" -> "E", "Ovest" -> "W"),
    "ja" -> Map("E" -> "E", "W" -> "W", "東" -> "E", "西" -> "W"),
    "ko" -> Map("E" -> "E", "W" -> "W", "동" -> "E", "서" -> "W", "東" -> "E", "西" -> "W"),
    "mk" -> Map("E" -> "E", "W" -> "W", "И" -> "E", "З" -> "W"),
    "nl" -> Map("E" -> "E", "W" -> "W", "O" -> "E", "Oost" -> "E", "West" -> "W"),
    "pl" -> Map("E" -> "E", "W" -> "W", "wsch" -> "E", "zach" -> "W", "wschód" -> "E", "zachód" -> "W"),
    "pt" -> Map("E" -> "E", "W" -> "W", "O" -> "W", "Leste" -> "E", "Oeste" -> "W"),
    "ru" -> Map("E" -> "E", "W" -> "W", "В" -> "E", "З" -> "W", "восток" -> "E", "запад" -> "W"),
    "zh" -> Map("E" -> "E", "W" -> "W", "东" -> "E", "西" -> "W", "東" -> "E", "西" -> "W")
  )

  // Map latitude letters used in different languages to English ("N" for North and "S" for South)
  val latitudeLetterMap = Map(
    "am" -> Map("N" -> "N", "S" -> "S", "ሰሜን" -> "N", "ደቡብ" -> "S"),
    "ar" -> Map("N" -> "N", "S" -> "S", "ش" -> "N", "ج" -> "S", "شمال" -> "N", "جنوب" -> "S"),
    "bg" -> Map("N" -> "N", "S" -> "S", "С" -> "N", "Ю" -> "S", "север" -> "N", "юг" -> "S"),
    "cs" -> Map("N" -> "N", "S" -> "S", "sever" -> "N", "jih" -> "S"),
    "de" -> Map("N" -> "N", "S" -> "S", "Nord" -> "N", "Süd" -> "S"),
    "en" -> Map("N" -> "N", "S" -> "S", "North" -> "N", "South" -> "S"),
    "es" -> Map("N" -> "N", "S" -> "S", "Norte" -> "N", "Sur" -> "S"),
    "fr" -> Map("N" -> "N", "S" -> "S", "Nord" -> "N", "Sud" -> "S"),
    "hi" -> Map("N" -> "N", "S" -> "S", "उ" -> "N", "द" -> "S", "उत्तर" -> "N", "दक्षिण" -> "S"),
    "it" -> Map("N" -> "N", "S" -> "S", "Nord" -> "N", "Sud" -> "S"),
    "ja" -> Map("N" -> "N", "S" -> "S", "北" -> "N", "南" -> "S"),
    "ko" -> Map("N" -> "N", "S" -> "S", "북" -> "N", "남" -> "S", "北" -> "N", "南" -> "S"),
    "mk" -> Map("N" -> "N", "S" -> "S", "С" -> "N", "Ј" -> "S"),
    "nl" -> Map("N" -> "N", "S" -> "S", "Noord" -> "N", "Zuid" -> "S"),
    "pl" -> Map("N" -> "N", "S" -> "S", "płn" -> "N", "płd" -> "S", "północ" -> "N", "południe" -> "S"),
    "pt" -> Map("N" -> "N", "S" -> "S", "Norte" -> "N", "Sul" -> "S"),
    "ru" -> Map("N" -> "N", "S" -> "S", "С" -> "N", "Ю" -> "S", "север" -> "N", "юг" -> "S"),
    "zh" -> Map("N" -> "N", "S" -> "S", "北" -> "N", "南" -> "S")
  )

  // Common parameter names for coordinate templates across different formats
  val coordinateParameterNames = Map(
    // Single coordinate string parameters
    "coordinates" -> Set("1", "coord", "coordinates", "coordinate", "coords", "location"),

    // Separate latitude/longitude parameters
    "latitude" -> Set("lat", "latitude", "lat_d", "lat_degrees", "latd", "NS", "latitud", "纬度", "широта"),
    "longitude" -> Set("lon", "long", "longitude", "lon_d", "lon_degrees", "lond", "EW", "longitud", "经度", "долгота"),

    // DMS (Degrees, Minutes, Seconds) parameters
    "lat_degrees" -> Set("lat_d", "latd", "lat_deg", "latitude_deg", "lat_degrees", "NS_deg"),
    "lat_minutes" -> Set("lat_m", "latm", "lat_min", "latitude_min", "lat_minutes", "NS_min"),
    "lat_seconds" -> Set("lat_s", "lats", "lat_sec", "latitude_sec", "lat_seconds", "NS_sec"),
    "lat_direction" -> Set("lat_NS", "NS", "latNS", "lat_dir", "latitude_dir", "lat_direction"),

    "lon_degrees" -> Set("lon_d", "lond", "lon_deg", "longitude_deg", "lon_degrees", "EW_deg"),
    "lon_minutes" -> Set("lon_m", "lonm", "lon_min", "longitude_min", "lon_minutes", "EW_min"),
    "lon_seconds" -> Set("lon_s", "lons", "lon_sec", "longitude_sec", "lon_seconds", "EW_sec"),
    "lon_direction" -> Set("lon_EW", "EW", "lonEW", "lon_dir", "longitude_dir", "lon_direction"),

    // Alternative DMS parameter names
    "degrees" -> Set("degrees", "deg", "d", "°"),
    "minutes" -> Set("minutes", "min", "m", "'", "′"),
    "seconds" -> Set("seconds", "sec", "s", "\"", "″"),
    "direction" -> Set("direction", "dir", "hemisphere", "hem")
  )

  // Coordinate format patterns for parsing
  val coordinatePatterns = List(
    // Decimal degrees: 40.7128, -74.0060
    """^(-?\d+\.?\d*)\s*[,\s]\s*(-?\d+\.?\d*)$""".r,

    // DMS format: 40°42'46"N 74°00'22"W
    """^(\d+)°(\d+)'(\d+(?:\.\d+)?)"([NS])\s+(\d+)°(\d+)'(\d+(?:\.\d+)?)"([EW])$""".r,

    // DM format: 40°42.77'N 74°00.37'W
    """^(\d+)°(\d+(?:\.\d+)?)'([NS])\s+(\d+)°(\d+(?:\.\d+)?)'([EW])$""".r,

    // D format: 40.7128°N 74.0060°W
    """^(\d+(?:\.\d+)?)°([NS])\s+(\d+(?:\.\d+)?)°([EW])$""".r,

    // Space-separated: 40.7128 -74.0060
    """^(-?\d+\.?\d*)\s+(-?\d+\.?\d*)$""".r,

    // Semicolon-separated: 40.7128;-74.0060
    """^(-?\d+\.?\d*);(-?\d+\.?\d*)$""".r
  )
}
