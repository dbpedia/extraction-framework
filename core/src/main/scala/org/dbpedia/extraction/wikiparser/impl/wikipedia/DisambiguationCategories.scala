package org.dbpedia.extraction.wikiparser.impl.wikipedia

import org.dbpedia.extraction.util.Language

/**
  * Disambiguation templates categories per language - used by the WikiDisambigReader to determine disambiguation templates
  * based on sameas links: https://en.wikipedia.org/w/api.php?action=query&prop=langlinks&lllimit=500&format=xml&titles=Category:Disambiguation_message_boxes
  */
object DisambiguationCategories {
  private val map = Map(
    "ba"->"Категория:Википедия:Күп мәғәнәлелек ҡалыптары",
    "bar"->"Kategorie:Vorlage:Begriffsklärung",
    "bn"->"বিষয়শ্রেণী:দ্ব্যর্থতা নিরসন বার্তা বাক্স",
    "br"->"Rummad:Patromoù disheñvelout",
    "ca"->"Categoria:Plantilles de desambiguació",
    "ce"->"Категори:Википеди:Масех маьӀнийн кепаш",
    "cs"->"Kategorie:Šablony:Rozcestníky",
    "da"->"Kategori:Flertydighedsskabeloner",
    "de"->"Kategorie:Vorlage:Begriffsklärung",
    "eo"->"Kategorio:Ŝablono apartigiloj",
    "en"->"Category:Disambiguation message boxes",
    "fa"->"رده:الگو:ابهام‌زدایی",
    "fr"->"Catégorie:Modèle de bandeau pour page d'homonymie",
    "gl"->"Categoría:Modelos de homónimos",
    "hr"->"Kategorija:Predlošci za višeznačne pojmove",
    "id"->"Kategori:Templat disambiguasi",
    "it"->"Categoria:Template di disambiguazione",
    "ja"->"Category:曖昧さ回避メッセージボックス",
    "jv"->"Kategori:Cithakan disambiguasi",
    "lt"->"Kategorija:Skaidymo šablonai",
    "map-bms"->"Kategori:Cithakan disambiguasi",
    "nl"->"Categorie:Wikipedia:Sjablonen doorverwijzing",
    "oc"->"Categoria:Bendèl per pagina d'omonimia",
    "pl"->"Kategoria:Szablony ujednoznacznień",
    "pt"->"Categoria:!Predefinições para desambiguações",
    "ro"->"Categorie:Formate de dezambiguizare",
    "roa-tara"->"Categoria:Template de disambiguazione",
    "ru"->"Категория:Шаблоны:Для неоднозначностей",
    "sd"->"زمرو:سلجهائپ پيغام خانا",
    "sh"->"Kategorija:Šabloni za višeznačne pojmove",
    "simple"->"Category:Disambiguation message boxes",
    "sk"->"Kategória:Šablóny na rozlíšenie významov",
    "sl"->"Kategorija:Sporočilna polja za razločitve",
    "stq"->"Kategorie:Foarloage:Begriepskloorenge",
    "su"->"Kategori:Citakan disambiguasi",
    "szl"->"Kategoryjo:Mustry uod ujednoznaczńyńůw",
    "tt"->"Төркем:Википедия:Күп мәгънәле мәкаләләр калыплары",
    "uk"->"Категорія:Шаблони-повідомлення для неоднозначностей",
    "vec"->"Categoria:Modełi de disanbiguasion",
    "vi"->"Thể loại:Hộp tin nhắn định hướng",
    "zh"->"Category:消歧義訊息模板"
  )

  def get(language: Language) = map.get(language.wikiCode)
}
