package org.dbpedia.extraction.live.administrative;

import org.dbpedia.extraction.live.main.Main;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by Andre Pereira on 27/06/2015.
 */
public class ControlServlet extends HttpServlet {
    public ControlServlet(){}
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
        response.setContentType("text/html");
        String password = request.getParameter("password");
        String type = request.getParameter("type");
        boolean result = false;
        String message = "";

        if(!AdminAuthentication.authenticate(password))
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        else {
            response.setStatus(HttpServletResponse.SC_OK);
            if (type.equals("start")) {
                if(Main.state.equals("stopped")) {
                    Main.state = "starting";
                    Main.initLive();
                    Main.startLive();
                    result = true;
                    message = "DBpedia Live has been started!";
                }else message = "DBpedia Live is already running";
            } else if (type.equals("stop")) {
                if(Main.state.equals("running")) {
                    Main.stopLive();
                    result = true;
                    message = "DBpedia Live was stopped!";
                }else if(Main.state.equals("stopped")){
                    message = "DBpedia Live is already stopped!";
                }else message = "DBpedia Live can't be stopped at this moment!";
            }
            response.getWriter().println("{\"result\":" + result + ", \"message\": \"" + message + "\"}");
        }

    }
}
