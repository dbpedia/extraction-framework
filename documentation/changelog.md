# Changelog

## 2021.12.01
* Fix issue [#720](https://github.com/dbpedia/extraction-framework/issues/720). Fix ImageExtractorNew and remove producing images triples from Wikipedia pages that don't contain images. Add new pages to the minidump: [Borysthenia_goldfussiana](https://en.wikipedia.org/wiki/Borysthenia_goldfussiana) and [Ingoldiomyces](https://en.wikipedia.org/wiki/Ingoldiomyces)

## 2021.09.01
* Fix issue [#711](https://github.com/dbpedia/extraction-framework/issues/711). Remove the triple starting with `http://dbpedia.org/resource/`. Change `dct:subject` property to `dbo:mainArticleForCategory` (Commit [bb64db3](https://github.com/dbpedia/extraction-framework/commit/bb64db3402932acca65476b5f640437a38a0a3ad))
* Fix issue [#579](https://github.com/dbpedia/extraction-framework/issues/579). Added extraction of transcriptions by removing  `.noexcerpt` from `nif-remove-elements` list in the `nifextractionconfig.json` file. Added removing of broken brackets in dief-server (Commit [6800d82](https://github.com/dbpedia/extraction-framework/commit/6800d82732e84afac98334b0346246117055b2a9))
* Rename `AbstractExtractorWikipedia` to `HtmlAbstractExtractor` and `AbstractExtractor` to `PlainAbstractExtractor` ([PR #705](https://github.com/dbpedia/extraction-framework/pull/705))
* Fix issue [#693](https://github.com/dbpedia/extraction-framework/issues/693). Brackets that starts with `(;`, `(,` are removed during abstract extraction. Empty brackets like `()`, `( )` are also removed. `remove-broken-brackets-html-abstracts=true` is a property which enable removing those brackets (Commit [3335b62](https://github.com/dbpedia/extraction-framework/commit/3335b622742f418162c97376cd7c4d846755394b))