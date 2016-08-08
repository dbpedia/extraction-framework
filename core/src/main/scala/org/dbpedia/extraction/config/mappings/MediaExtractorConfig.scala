package org.dbpedia.extraction.config.mappings

/**
  * Similar to the ImageExtractorConfig, this file adds Regex to match sound and video files
  * Mainly used by the MediaExtractor to extract all media files of Wikipedia articles
  *
  * @author Lukas Faber, Stephan Haarmann, Sebastian Serth
  * date 07.07.2016.
  */
object MediaExtractorConfig {
  // import from ImageExtractorConfig
  val NonFreeRegex = ImageExtractorConfig.NonFreeRegex
  val supportedLanguages = ImageExtractorConfig.supportedLanguages
  val ImageRegex = ImageExtractorConfig.ImageRegex
  val ImageLinkRegex = ImageExtractorConfig.ImageLinkRegex

  val SoundRegex = """(?i)[^\"/\*?<>|:]+\.(?:ogg|oga|ogx|flac|wav|midi?|kar|opus|spx)""".r
  val SoundLinkRegex = """(?i).*\.(?:ogg|oga|ogx|flac|wav|midi?|kar|opus|spx)""".r

  val VideoRegex = """(?i)[^\"/\*?<>|:]+\.(?:ogv|ogm|webm)""".r
  val VideoLinkRegex = """(?i).*\.(?:ogv|ogm|webm)""".r

  val MediaRegex = s"""($ImageRegex|$SoundRegex|$VideoRegex)""".r
  val MediaLinkRegex = s"""($ImageLinkRegex|$SoundLinkRegex|$VideoLinkRegex)""".r
}
