package com.th.eoss.servlet;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.th.eoss.util.SETIndex;

public class SETIndexUpdaterServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		SETIndex index = new SETIndex();
		Map<String, Map<String, Set<String>>> map = index.map();
		
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		
		for (String industry:map.keySet()) {
			Entity entity = new Entity("Industry", industry);
			
			int size = 0;
			Set<String> sectors = map.get(industry).keySet();
			for (String sector:sectors) {
				entity.setProperty(sector, map.get(industry).get(sector));
				size += map.get(industry).get(sector).size();
			}
			datastore.put(entity);
			System.out.println(industry + ":" + size);
		}
		
		resp.getWriter().println("ok");
	}

}
