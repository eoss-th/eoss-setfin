package com.th.eoss.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
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

public class SETIndexServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		
		Query q = new Query("Industry");

		// This returns weddingPhoto, babyPhoto, and dancePhoto,
		// but not campingPhoto, because tom is not an ancestor
		List<Entity> results =
		    datastore.prepare(q).asList(FetchOptions.Builder.withDefaults());	
		
		PrintWriter out = resp.getWriter();
		
		Set<String> sectors;
		for (Entity e:results) {
			out.print(e.getKey().getName());
			sectors = e.getProperties().keySet();
			for (String sector:sectors) {
				out.print("," + sector);				
			}
			out.println();
		}
	}

}
