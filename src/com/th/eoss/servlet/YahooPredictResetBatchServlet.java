package com.th.eoss.servlet;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.th.eoss.gcp.datastore.SETUpdater;
import com.th.eoss.gcp.predict.YahooPredictor;

public class YahooPredictResetBatchServlet extends HttpServlet {
		
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		
		try {
			
			Query q = new Query("Symbol");
			List<Entity> results = datastore.prepare(q).asList(FetchOptions.Builder.withDefaults());
			
			for (Entity setEntity:results) {
				
				setEntity.setProperty("MA5",  0);
				setEntity.setProperty("predict",  0);
				setEntity.setProperty("output",  "");
				setEntity.setProperty("winrate",  0);
				datastore.put(setEntity);				
				
			}
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}

}
