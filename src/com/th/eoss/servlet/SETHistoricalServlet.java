package com.th.eoss.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.th.eoss.gcp.datastore.SETUpdater;
import com.th.eoss.util.SETHighlights;

public class SETHistoricalServlet extends HttpServlet {
	
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		resp.setContentType("text/csv");
		resp.setCharacterEncoding("UTF-8");
	    resp.setHeader("Content-Disposition", "attachment; filename=\"SETHistorical.csv\"");
	    
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		
		String symbol = req.getParameter("s");
	    		
		Key key = KeyFactory.createKey("Symbol", symbol);
		
		PrintWriter out = resp.getWriter();
		
		try {
			Entity symbolEntity = datastore.get(key);
			List<String> closes = (List<String>) symbolEntity.getProperty("closes");
			
			if (closes==null) {
				SETUpdater.updateHighlights(symbolEntity);
				datastore.put(symbolEntity);
			}
			
			csv(out, symbolEntity);
			
		} catch (Exception e) {
			e.printStackTrace(out);
		} finally {
			out.flush();
			out.close();			
		}
		
	}

	private void csv(PrintWriter out, Entity symbolEntity) {
		StringBuilder line = new StringBuilder("\"As of Date\",");
		
		for (String name:SETHighlights.PROPERTIES_NAMES) {
			line.append("\"" + name + "\",");
		}
		
		out.println(line);
					
		List<List<Double>> valList = new ArrayList<>();
		
		List<String> closes = (List<String>) symbolEntity.getProperty("closes");	
		
		for (int i=0;i<SETHighlights.PROPERTIES_NAMES.length;i++) {
			valList.add((List<Double>) symbolEntity.getProperty(SETHighlights.PROPERTIES_NAMES[i]));
		}
		
		String close,closeDate, lastYear;
		String [] closeTokens, atTokens;
		for (int i=0;i<closes.size();i++) {
			line = new StringBuilder();
			closeDate = "";
			close = closes.get(i);
			if (close!=null) {
				closeTokens = close.split("/");
				if (closeTokens.length>0) {
					if (i<closes.size()-1) {
						closeDate = closeTokens[closeTokens.length-1];
					} else {
						atTokens = close.split(" ");
						lastYear = closeTokens[closeTokens.length-1];
						//closeDate = atTokens[0] + " " + lastYear.substring(lastYear.length()-2);
						closeDate = atTokens[0] + " " + lastYear;
					}					
				}
			}
			line.append(closeDate+",");
			
			for (int j=0;j<SETHighlights.PROPERTIES_NAMES.length;j++) {
				line.append(round(valList.get(j).get(i).doubleValue()));
				if (j<SETHighlights.PROPERTIES_NAMES.length-1)
					line.append(",");
			}
			
			out.println(line);
		}
	}

    private double round (double num) {
    	return Math.round(num * 100.0)/100.0;
    }
    
	private String formatAsOfDate(Object dateString) {
		try {
			return dateFormat.format(SETHighlights.asOfDateFormat.parse((String) dateString));
		} catch (Exception e) {
		}
		return "";
	}
	
	private String formatXDDate(Object dateString) {
		try {
			return dateFormat.format(SETHighlights.xdDateFormat.parse((String) dateString));
		} catch (Exception e) {
		}
		return "";
	}

}