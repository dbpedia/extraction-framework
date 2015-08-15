package org.dbpedia.extraction.live.administrative;

import org.dbpedia.extraction.live.statistics.StatisticsData;
import org.dbpedia.extraction.live.statistics.StatisticsResult;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by Andre Pereira on 27/06/2015.
 */
public class StatsServlet extends HttpServlet {
    public StatsServlet(){}
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        response.setContentType("text/html");
        response.setHeader("Content-Type", "application/octet-stream; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        StatisticsResult stats = StatisticsData.getResults();
        if(stats != null)
            response.getWriter().println(stats.toString());
        else
            response.getWriter().println("null");
    }
}
