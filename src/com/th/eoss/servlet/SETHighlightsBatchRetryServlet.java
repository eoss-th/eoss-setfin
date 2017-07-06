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

public class SETHighlightsBatchRetryServlet extends HttpServlet {
		
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		
		try {
			
			Filter propertyFilter = new FilterPredicate("closes", FilterOperator.EQUAL, null);
			Query q = new Query("Symbol").setFilter(propertyFilter);
			List<Entity> results = datastore.prepare(q).asList(FetchOptions.Builder.withDefaults());
			
			for (Entity symbolEntity:results) {
				
				SETUpdater.updateHighlights(symbolEntity);
									
				datastore.put(symbolEntity);
			}
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}

}
