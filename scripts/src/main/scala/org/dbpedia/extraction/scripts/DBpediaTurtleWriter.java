package org.dbpedia.extraction.scripts;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: May 28, 2010
 * Time: 9:54:59 AM
 * This class is built upon TurtleWriter found in org.openrdf.rio because this TurtleWriter can only write
 * the literal objects between double quotes, and we want the numbers like FileSize and NumberOfTriples
 * to be written without quotes in order to be easily parsed
 */
import org.openrdf.rio.*;
import org.openrdf.rio.turtle.*;
import org.openrdf.model.*;


import java.io.IOException;
import java.io.OutputStream;


public class DBpediaTurtleWriter extends TurtleWriter
{
    public DBpediaTurtleWriter(OutputStream outOutputStream)
    {
      super(outOutputStream);
    }
    public void handleStatement(Statement st, boolean withQuotes)
                    throws RDFHandlerException {
                if (!writingStarted) {
                    throw new RuntimeException(
                            "Document writing has not yet been started");
                }

                Resource subj = st.getSubject();
                URI pred = st.getPredicate();
                Value obj = st.getObject();

                try {
                    if (subj.equals(lastWrittenSubject)) {
                        if (pred.equals(lastWrittenPredicate)) {
                            // Identical subject and predicate
                            writer.write(" , ");
                        } else {
                            // Identical subject, new predicate
                            writer.write(" ;");
                            writer.writeEOL();

                            // Write new predicate
                            writePredicate(pred);
                            writer.write(" ");
                            lastWrittenPredicate = pred;
                        }
                    } else {
                        // New subject
                        closePreviousStatement();

                        // Write new subject:
                        writer.writeEOL();
                        writeResource(subj);
                        writer.write(" ");
                        lastWrittenSubject = subj;

                        // Write new predicate
                        writePredicate(pred);
                        writer.write(" ");
                       lastWrittenPredicate = pred;

                       statementClosed = false;
                       writer.increaseIndentation();
                   }

                   writeValue(obj, withQuotes);

                  // Don't close the line just yet. Maybe the next
                  // statement has the same subject and/or predicate.
               } catch (IOException e) {
                    throw new RDFHandlerException(e);
               }
            }

    protected void writeValue(Value val, boolean withQuotes) throws IOException {
               if (val instanceof  Resource) {
                    writeResource((Resource) val);
               } else {
                   writeLiteral((Literal) val, withQuotes);
               }
            }

    protected void writeLiteral(Literal lit, boolean withQuotes) throws IOException {
                String label = lit.getLabel();

                if(!withQuotes)
                {
                   writer.write(TurtleUtil.encodeString(label));
                    return;
                }
                if (label.indexOf('\n') > 0 || label.indexOf('\r') > 0
                        || label.indexOf('\t') > 0) {
                    // Write label as long string
                    writer.write("\"\"\"");
                   writer.write(TurtleUtil.encodeLongString(label));
                    writer.write("\"\"\"");
                } else {
                   // Write label as normal string
                    writer.write("\"");
                   writer.write(TurtleUtil.encodeString(label));
                   writer.write("\"");
                }

               if (lit.getDatatype() != null) {
                   // Append the literal's datatype (possibly written as an abbreviated
                    // URI)
                    writer.write("^^");
                   writeURI(lit.getDatatype());
               } else if (lit.getLanguage() != null) {
                   // Append the literal's language
                    writer.write("@");
                   writer.write(lit.getLanguage());
                }
          }
}
