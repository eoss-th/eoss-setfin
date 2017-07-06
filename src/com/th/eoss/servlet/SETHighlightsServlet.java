package com.th.eoss.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.th.eoss.util.CalendarUtil;
import com.th.eoss.util.SETHighlights;

public class SETHighlightsServlet extends HttpServlet {
	
	private static Map<String, String> paramMap = new HashMap<>();
	
	static {
		paramMap.put("eag", "E/A Growth %");
		paramMap.put("rg", "Revenue Growth %");
		paramMap.put("ng", "Net Growth %");
		paramMap.put("eg", "EPS Growth %");
		paramMap.put("roeg", "ROE Growth %");
		paramMap.put("mg", "Margin Growth %");
		paramMap.put("dvdg", "DVD Growth %");
		paramMap.put("eps", "EPS");
		paramMap.put("roa", "ROA");
		paramMap.put("roe", "ROE");
		paramMap.put("mar", "Margin");
		paramMap.put("last", "Last");
		paramMap.put("pe", "P/E");
		paramMap.put("pbv", "P/BV");
		paramMap.put("dvd", "DVD %");
		paramMap.put("me", "Market Cap:Estimated E");
		paramMap.put("pchg", "predictChg");
	}
	
	//http://eoss-setfin.appspot.com/csv?eag=%3E0&rg=%3E0&ng=%3E0&eg=%3E0&roeg=%3E0&mg=%3E0&dvdg=%3E0&eps=%3E0&roa=%3E0&roe=%3E0&mar=%3E0&pe=%3C20&dvd=%3E0&pchg=%3E0
	//http://eoss-setfin.appspot.com/csv?eag=%3E0&rg=%3E0&ng=%3E0&eg=%3E0&roeg=%3E0&mg=%3E0&dvdg=%3E0&eps=%3E0&roa=%3E0&roe=%3E0&mar=%3E0&pe=%3C10&dvd=%3E0&pchg=%3E0
	
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		resp.setContentType("text/csv");
		resp.setCharacterEncoding("UTF-8");
	    resp.setHeader("Content-Disposition", "attachment; filename=\"SETHighlights.csv\"");
	    
	    Map<String, String> paramValue = new HashMap<>();
	    
	    Set<String> paramNames = paramMap.keySet();
	    
	    String value;
	    for (String paramName:paramNames) {
	    	value = req.getParameter(paramName);
	    	if (value!=null)
	    		paramValue.put(paramMap.get(paramName), value);
	    }
	    
	    String setOperator = req.getParameter("setOperator");
	    
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		
		Query q = new Query("Symbol");
		
		String industry = req.getParameter("industry");
		
		if (industry!=null) {
			q.setFilter(new FilterPredicate(paramMap.get("industry"), FilterOperator.EQUAL, industry));
		}
		
		List<Entity> results = datastore.prepare(q).asList(FetchOptions.Builder.withDefaults());
		
		PrintWriter out = resp.getWriter();
		
		StringBuilder line = new StringBuilder("\"Industry\",\"Sector\",\"Symbol\",\"As of Date\",");
		for (String name:SETHighlights.PROPERTY_NAMES) {
			line.append("\"" + name + "\",");
		}
				
		if (setOperator!=null) {
			line.append("\"DVD\",");
			line.append("\"XD\",");
			line.append("\"Predict MA of " + CalendarUtil.asOfWeek() + "\",");
			line.append("\"Predict Chg %\",");
			line.append("\"Name\",");
			line.append("\"Website\",");
			line.append("\"DVD Policy\"");			
		} else {
			line.append("\"DVD\",");
			line.append("\"XD\",");
			line.append("\"Predict MA of " + CalendarUtil.asOfWeek() + "\",");
			line.append("\"Predict Chg %\"");			
		}
		out.println(line);
		
		String paramVal;
		Object entityVal;
		float val;
		boolean skip;
		Set<String> params = paramValue.keySet();
		String companyName, policy;
		for (Entity e:results) {
			
			skip = false;
			for (String name:params) {
				
				entityVal = e.getProperty(name);
				if (entityVal==null) {
					skip=true;
					break;
				}
				
				try {
					val = Float.parseFloat(entityVal.toString());
				} catch (Exception ne) {
					skip=true;
					break;						
				}
				
				paramVal = paramValue.get(name);
				
				if (paramVal.startsWith(">=")) {
					if (!(val>=Float.parseFloat(paramVal.replace(">=", "")))) {
						skip=true;
						break;
					}
				} else if (paramVal.startsWith(">")) {
					if (!(val>Float.parseFloat(paramVal.replace(">", "")))) {
						skip=true;
						break;
					}
				} else if (paramVal.startsWith("<=")) {
					if (!(val<=Float.parseFloat(paramVal.replace("<=", "")))) {
						skip=true;
						break;
					}
				} else if (paramVal.startsWith("<")) {
					if (!(val<Float.parseFloat(paramVal.replace("<", "")))) {
						skip=true;
						break;
					}
				}
				
			}
			
			if (skip) continue;
			
			line = new StringBuilder();
			line.append(e.getProperty("industry") + ",");
			line.append(e.getProperty("sector") + ",");
			line.append(e.getKey().getName() + ",");
			line.append(formatAsOfDate(e.getProperty("date")) + ",");
						
			for (String name:SETHighlights.PROPERTY_NAMES) {				
				line.append(e.getProperty(name) + ",");
			}
			
			
			if (setOperator!=null) {
				line.append(e.getProperty("dvd") + ",");
				line.append(formatXDDate2(e.getProperty("xd")) + ",");
				line.append(e.getProperty("predict") + ",");
				line.append(e.getProperty("predictChg") + ",");
				if (e.getProperty("name")!=null) {
					companyName = e.getProperty("name").toString().replace(',', ' ');
				} else {
					companyName = "";
				}
				line.append(companyName + ",");
				
				line.append(e.getProperty("website") + ",");
				
				if (e.getProperty("dvdPolicy")!=null) {
					policy = e.getProperty("dvdPolicy").toString().replace(',', ' ');
				} else {
					policy = "";
				}
				line.append(policy);				
			} else {
				line.append(e.getProperty("dvd") + ",");
				line.append(formatXDDate(e.getProperty("xd")) + ",");
				line.append(e.getProperty("predict") + ",");
				//line.append(e.getProperty("predictChg"));				
				line.append(e.getProperty("winrate"));
			}
			out.println(line);
		}
			    
		out.flush();
		out.close();
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

	private String formatXDDate2(Object dateString) {
		
		try {
			Object xd = SETHighlights.xdDateFormat.parse((String) dateString);
			return dateString.toString();
		} catch (Exception e) {
		}
		return "";
	}
}
