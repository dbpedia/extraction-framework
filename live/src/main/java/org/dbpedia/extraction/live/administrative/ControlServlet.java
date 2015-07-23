package org.dbpedia.extraction.live.administrative;

import org.dbpedia.extraction.live.main.Main;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/**
 * Created by Andre Pereira on 27/06/2015.
 */
public class ControlServlet extends HttpServlet {
    public ControlServlet(){}
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        String password = request.getParameter("password");
        String type = request.getParameter("type");
        boolean result = false;

        String path = getServletContext().getRealPath("/") + "/../adminPassword.txt";
        String passw = new Scanner(new File(path)).nextLine();

        if(!password.equals(passw)) result = false;
        else {
            System.out.println(Main.state);
            if (type.equals("start")) {
                if(!Main.state.equals("running") && !Main.state.equals("starting")) {
                    Main.state = "starting";
                    Main.initLive();
                    Main.startLive();
                    result = true;
                }
            } else if (type.equals("stop")) {
                if(!Main.state.equals("stopped")) {
                    Main.stopLive();
                    result = true;
                }
            }
        }

        response.getWriter().println("" + result);
    }
}
