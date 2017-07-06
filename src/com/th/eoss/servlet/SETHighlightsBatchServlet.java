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
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.th.eoss.gcp.datastore.SETUpdater;

public class SETHighlightsBatchServlet extends HttpServlet {
		
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		
		String industry = req.getParameter("industry");
		String s = req.getParameter("sector");
		
		Key key = KeyFactory.createKey("Industry", industry);
		try {
			Entity entity = datastore.get(key);
			
			Map<String, Object> map = entity.getProperties();
			Set<String> sectors = map.keySet();
			for (String sector:sectors) {
				
				if ( s!=null && s.equals(sector) ) {
					List<String> symbols = (List<String>) entity.getProperty(sector);
					for (String symbol:symbols) {
						
						Entity symbolEntity = SETUpdater.getOrCreate(datastore, symbol);
						
						SETUpdater.updateSector(symbolEntity, industry, sector);
						SETUpdater.updateHighlights(symbolEntity);
						SETUpdater.updateDVD(symbolEntity);
						SETUpdater.updateProfile(symbolEntity);
						
						datastore.put(symbolEntity);
						
					}
					
				}
				
			}
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}

}
