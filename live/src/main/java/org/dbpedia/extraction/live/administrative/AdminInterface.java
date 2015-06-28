package org.dbpedia.extraction.live.administrative;

import org.eclipse.jetty.server.Server;

/**
 * Created by Andre Pereira on 26/06/2015.
 */

import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;

public class AdminInterface {

    public void start() throws InterruptedException {

        Server server = new Server(8080);

        String rootPath = AdminInterface.class.getClassLoader().getResource(".").toString();
        System.out.println("PATH: " + rootPath);
        WebAppContext webapp = new WebAppContext(rootPath + "../../web", "");
        webapp.addServlet(new ServletHolder(new TestServlet("Hello World from TestServlet!")), "/hello");
        server.setHandler(webapp);


        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        server.join();

    }

}