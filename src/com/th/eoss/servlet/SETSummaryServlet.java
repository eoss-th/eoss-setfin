package com.th.eoss.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.th.eoss.util.CalendarUtil;
import com.th.eoss.util.SETSummary;

public class SETSummaryServlet extends HttpServlet {
	
	private static Map<String, String> paramMap = new HashMap<>();
	
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    
	static {
		paramMap.put("industry", "industry");
		paramMap.put("sector", "sector");
		paramMap.put("cap", "Market Cap");
		paramMap.put("ev", "EV");
		paramMap.put("eda", "EBITDA");
		paramMap.put("evda", "EV/EBITDA");
		paramMap.put("roa", "ROA");
		paramMap.put("roe", "ROE");
		paramMap.put("mar", "Margin");
		paramMap.put("marg", "Margin Growth %");
		paramMap.put("pbv", "P/BV");
		paramMap.put("pe", "P/E");
		paramMap.put("eps", "EPS");
		paramMap.put("epsg", "EPS Growth %");
		paramMap.put("cr", "CR");
		paramMap.put("crg", "CR Growth %");
		paramMap.put("de", "D/E");
		paramMap.put("deg", "D/E Growth %");
		paramMap.put("dvd %", "DVD %");
		paramMap.put("dvd", "dvd");
		paramMap.put("xd", "xd");
		paramMap.put("dvdg", "DVD Growth %");
		paramMap.put("last", "Last");
		paramMap.put("predict", "predict");	
		paramMap.put("predictChg", "predictChg");	
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		resp.setContentType("text/csv");
	    resp.setHeader("Content-Disposition", "attachment; filename=\"SETSummary.csv\"");
	    
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		
		Query q = new Query("Symbol");
		
		String industry = req.getParameter("industry");
		
		if (industry!=null) {
			q.setFilter(new FilterPredicate(paramMap.get("industry"), FilterOperator.EQUAL, industry));
		}
		
		List<Entity> results = datastore.prepare(q).asList(FetchOptions.Builder.withDefaults());
		
		PrintWriter out = resp.getWriter();
		
		StringBuilder line = new StringBuilder("\"Industry\",\"Sector\",\"Symbol\",\"As of Date\",");
		for (String name:SETSummary.PROPERTY_NAMES) {
			line.append("\"" + name + "\",");
		}
				
		line.append("\"DVD\",");
		line.append("\"XD\",");
		line.append("\"Predict MA of " + CalendarUtil.asOfWeek() + "\",");
		line.append("\"Predict Chg %\"");
		out.println(line);
		
		for (Entity e:results) {
			line = new StringBuilder();
			line.append(e.getProperty("industry") + ",");
			line.append(e.getProperty("sector") + ",");
			line.append(e.getKey().getName() + ",");
			line.append(formatDate(e.getProperty("date")) + ",");
			
			for (String name:SETSummary.PROPERTY_NAMES) {
				line.append(e.getProperty(name) + ",");
			}
			
			line.append(e.getProperty("dvd") + ",");
			line.append(formatDate(e.getProperty("xd")) + ",");
			line.append(e.getProperty("predict") + ",");
			line.append(e.getProperty("predictChg"));
			out.println(line);
		}
			    
		out.flush();
		out.close();
	}

	private void applyQuery(HttpServletRequest req, Query q) {
		Enumeration em = req.getParameterNames();
		Set<String> paramNames = paramMap.keySet();
		String value;
		Object val;
		FilterOperator opt;
		List<FilterPredicate> filters = new ArrayList<>();
		
		for (String name:paramNames) {
			
			value = req.getParameter(name);
			
			if (value==null) {
				opt = FilterOperator.LESS_THAN;
				val = Float.MAX_VALUE;
				
			} else if (value.startsWith("ge")) {
				
				opt = FilterOperator.GREATER_THAN_OR_EQUAL;
				val = Float.parseFloat(value.replace("ge", ""));
				
			} else if (value.startsWith("g")) {
				
				opt = FilterOperator.GREATER_THAN;
				val = Float.parseFloat(value.replace("g", ""));
				
			} else if (value.startsWith("le")) {
				
				opt = FilterOperator.LESS_THAN_OR_EQUAL;
				val = Float.parseFloat(value.replace("le", ""));
				
			} else if (value.startsWith("l")) {
				
				opt = FilterOperator.LESS_THAN;
				val = Float.parseFloat(value.replace("l", ""));
				
			} else if (value.startsWith("eq")) {
				
				opt = FilterOperator.EQUAL;
				val = Float.parseFloat(value.replace("eq", ""));
				
			} else if (value.startsWith("ne")) {
				
				opt = FilterOperator.NOT_EQUAL;
				val = Float.parseFloat(value.replace("ne", ""));
				
			} else {
				opt = FilterOperator.GREATER_THAN_OR_EQUAL;
				val = value;
			}
			
			filters.add(new FilterPredicate(paramMap.get(name), opt, val));
			
		}
		
		
		
		q.setFilter(CompositeFilterOperator.and(filters.toArray(new FilterPredicate[filters.size()])));
	}
	
	private String formatDate(Object dateString) {
		try {
			return dateFormat.format(SETSummary.dateFormat.parse((String) dateString));
		} catch (Exception e) {
		}
		return "";
	}

}
