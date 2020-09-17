According to https://github.com/dbpedia/extraction-framework/pull/35/#issuecomment-16187074 the current design is the following

![](https://camo.githubusercontent.com/c274b58393dc2aafc799f023d84de5d670d232e3/68747470733a2f2f662e636c6f75642e6769746875622e636f6d2f6173736574732f3630373436382f3336333238362f31663864613632632d613166662d313165322d393963332d6262353133366163636330372e706e67)

In order to create a new Extractor you need to extend the `Extractor[T]` trait and in particular:

* `WikiPageExtractor` when you want to use only the page metadata. e.g. `RedirectExtractor`, `LabelExtractor', ... 
* `PageNodeExtractor` when you want to work with the Wikitext AST (most common case)
* `JsonNodeExtractor` when you want to work with Wikidata pages

Examples of Extractors can be found in the `org/dbpedia/extraction/mappings` package in `core` module.

If you want to test your new extractor you can do it in two ways:

* for a full dump extraction you can add `.MyNewExtractor` in the extraction property files in `dump` module
* add your extractor in the `server.default.properties` in the `server` module and start the mapping server with  `../run server`. Open `http://localhost:{PORT}/server/extraction/{LANG}/` and try your extractor on a specific page. (You can also run this from your IDE and debug)
