package org.dbpedia.extraction.dataparser

import _root_.org.dbpedia.extraction.sources.WikiPage
import _root_.org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.util.Language

class BooleanParserTest extends FlatSpec with ShouldMatchers
{
    "BooleanParser" should "return true" in
    {
        parse("true") should equal (Some(true))
        parse("yes") should equal (Some(true))
    }

    it should "return false" in
    {
        parse("false") should equal (Some(false))
        parse("no") should equal (Some(false))
    }

    it should "return None" in
    {
        parse("foo") should equal (None)
        parse("The Beatles song 'Yesterday'") should equal (None)
        parse("MONOTONE") should equal (None)
    }

    private val parser = WikiParser()

    private def parse(input : String) : Option[Boolean] =
    {
        val page = new WikiPage(WikiTitle.parse("TestPage", Language.Default), null, 0, 0, "1970-01-01T00:00:00Z", input)
        
        BooleanParser.parse(parser(page))
    }
}
