package org.dbpedia.extraction.config.dataparser


object GeoCoordinateParserConfig
{
    //make them language-specifig? might be redundant
    val coordTemplateNames = Set("coord", "coor dms", "coor dm", "coor", "location", "geocoordinate", "coords")
                                 //"coor title dms", "coor title d", "coor title dm", "coorheader",
                                 //"coor at dm", "coor at dms", "coor at d", "coor d/new", "coor dm/new",
                                 //"coor dms/new", "coor dec", "coor/new", "coor dms/archive001",
                                 //"coord/conversion", "coord/templates", "location dec"
}