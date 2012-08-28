package org.dbpedia.extraction.scripts

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: May 30, 2010
 * Time: 2:05:11 PM
 * This class holds the full information describing a Linkset e.g. subjectDataset, and linkPredicate.
 */


class LinkSetInfo()
{
  var DatasetName : String = "";
  var SubjectDataset : String = "";
  var ObjectDataset : String = "";
  var LinkPredicate : String = "";

  def this(datsetName : String, subjectDataset : String, objectDataset :String, linkPrediacte : String)=
    {
      this();
      DatasetName = datsetName;
      LinkPredicate = linkPrediacte;
      SubjectDataset = subjectDataset;
      ObjectDataset = objectDataset;
    }

  override def toString = "Dataset name = " + DatasetName + ",\nSubject dataset = " + SubjectDataset +
                          ",\nObject dataset = " + ObjectDataset + ",\nLink predicate = " + LinkPredicate + "\n";
}