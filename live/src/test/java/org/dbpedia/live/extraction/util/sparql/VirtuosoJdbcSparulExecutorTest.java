package org.dbpedia.live.extraction.util.sparql;

import com.hp.hpl.jena.rdf.model.Model;
import org.dbpedia.extraction.live.util.ModelUtil;
import org.dbpedia.extraction.live.util.sparql.ISparulExecutor;
import org.dbpedia.extraction.live.util.sparql.VirtuosoJdbcSparulExecutor;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Created by IntelliJ IDEA.
 * User: raven
 * Date: Sep 21, 2010
 * Time: 3:39:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class VirtuosoJdbcSparulExecutorTest {
    public static void main(String[] args)
        throws Exception
    {
        VirtuosoJdbcSparulExecutorTest test = new VirtuosoJdbcSparulExecutorTest();
        test.runTest();
    }

    public void runTest()
            throws Exception
    {
        String hostName = "localhost";
        String userName = "dba";
        String passWord = "dba";

        Class.forName("virtuoso.jdbc4.Driver").newInstance();
        String url = "jdbc:virtuoso://" + hostName + "/charset=UTF-8";
        //Connection connection = DriverManager.getConnection(url);
        Connection conn = DriverManager.getConnection(url, userName, passWord);


        ISparulExecutor graphDAO = new VirtuosoJdbcSparulExecutor(conn, "http://test.org");

        Model m = graphDAO.executeConstruct("Construct {?s ?p ?o . } From <http://test.org> {?s ?p ?o .}");


        System.out.println(ModelUtil.toString(m));
    }
}
