package com.th.eoss.servlet;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.th.eoss.util.SETDividend;
import com.th.eoss.util.SETSummary;

public class SETSummaryBatchServlet extends HttpServlet {
		
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		
		String industry = req.getParameter("industry");
		
		Key key = KeyFactory.createKey("Industry", industry);
		try {
			Entity entity = datastore.get(key);
			
			Map<String, Object> map = entity.getProperties();
			Set<String> sectors = map.keySet();
			for (String sector:sectors) {
				List<String> symbols = (List<String>) entity.getProperty(sector);
				
				for (String symbol:symbols) {
					SETSummary set = new SETSummary(symbol);
					SETDividend dvd = new SETDividend(symbol);
					
					key = KeyFactory.createKey("Symbol", symbol);
					
					Entity symbolEntity = datastore.get(key);
					
					if (symbolEntity==null) {
						symbolEntity = new Entity("Symbol", symbol);			
					}
					symbolEntity.setProperty("industry", industry);
					symbolEntity.setProperty("sector", sector);
					symbolEntity.setProperty("date", set.date);
					symbolEntity.setProperty("dvd", dvd.value);
					symbolEntity.setProperty("xd", dvd.xd);
					for (String prop:SETSummary.PROPERTY_NAMES) {
						symbolEntity.setProperty(prop, set.getValue(prop));
					}
					
					datastore.put(symbolEntity);
				}
				
			}
			
		} catch (EntityNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
