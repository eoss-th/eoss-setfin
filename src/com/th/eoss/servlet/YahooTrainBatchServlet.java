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
import com.th.eoss.gcp.predict.YahooPredictor;

public class YahooTrainBatchServlet extends HttpServlet {
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		
		String industry = req.getParameter("industry");
		
		Key key = KeyFactory.createKey("Industry", industry);
		try {
			Entity entity = datastore.get(key);
			
			Map<String, Object> map = entity.getProperties();
			Set<String> groups = map.keySet();
			for (String group:groups) {
				List<String> symbols = (List<String>) entity.getProperty(group);
				
				for (String symbol:symbols) {
					
					try {
						
						YahooPredictor predictor = new YahooPredictor(symbol);
						predictor.train();						
						
					} catch (Exception e) {
						e.printStackTrace();
					}
										
				}
				
			}
			
		} catch (EntityNotFoundException e) {
			throw new RuntimeException(e);
		}
		
	}

}
