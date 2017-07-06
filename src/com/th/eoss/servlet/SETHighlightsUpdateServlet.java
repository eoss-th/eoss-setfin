package com.th.eoss.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.th.eoss.gcp.datastore.SETUpdater;

public class SETHighlightsUpdateServlet extends HttpServlet {
		
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		
		String symbol = req.getParameter("symbol");
		
		try {
			Entity symbolEntity = SETUpdater.getOrCreate(datastore, symbol);
			
			SETUpdater.updateHighlights(symbolEntity);
			SETUpdater.updateDVD(symbolEntity);
			SETUpdater.updateProfile(symbolEntity);
			
			datastore.put(symbolEntity);
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}

}
