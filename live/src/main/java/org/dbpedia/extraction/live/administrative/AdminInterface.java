package org.dbpedia.extraction.live.administrative;

import org.eclipse.jetty.server.Server;

/**
 * Created by Andre Pereira on 26/06/2015.
 */

import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;

public class AdminInterface extends Thread{

    public void run() {
        Server server = new Server(8080);

        String rootPath = AdminInterface.class.getClassLoader().getResource(".").toString();
        System.out.println("PATH: " + rootPath);
        WebAppContext webapp = new WebAppContext(rootPath + "../../web", "");
        webapp.addServlet(new ServletHolder(new StatsServlet()), "/stats");
        webapp.addServlet(new ServletHolder(new ControlServlet()), "/control");
        server.setHandler(webapp);

        try {
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}