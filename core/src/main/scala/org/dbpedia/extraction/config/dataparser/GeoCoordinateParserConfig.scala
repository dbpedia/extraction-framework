package org.dbpedia.extraction.config.dataparser


object GeoCoordinateParserConfig
{
    //make them language-specifig? might be redundant
    val coordTemplateNames = Set("coord", "coor dms", "coor dm", "coor", "location", "geocoordinate", "coords", "coordenadas") 
                                 //"coor title dms", "coor title d", "coor title dm", "coorheader",
                                 //"coor at dm", "coor at dms", "coor at d", "coor d/new", "coor dm/new",
                                 //"coor dms/new", "coor dec", "coor/new", "coor dms/archive001",
                                 //"coord/conversion", "coord/templates", "location dec"
    
    //map latitude letters used in languages to the ones used in English ("E" for East and "W" for West) 
    val longitudeLetterMap = Map(
        "de" -> Map("E" -> "E", "O" -> "E", "W" -> "W"),
        "en" -> Map("E" -> "E", "W" -> "W"),
        "fr" -> Map("E" -> "E", "O" -> "W", "W" -> "W")
    )

    //map longitude letters used in languages to the ones used in English ("N" for North and "S" for South)
    val latitudeLetterMap = Map(
        "en" -> Map("N" -> "N", "S" -> "S")
    )
    
}
