package com.th.eoss.gcp.datastore;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Key;

import com.th.eoss.util.SETDividend;
import com.th.eoss.util.SETHighlights;
import com.th.eoss.util.SETProfile;

public class SETUpdater {
	
	public static Entity getOrCreate(DatastoreService datastore, String symbol) {
		
		Entity symbolEntity;
		try {
			
			Key key = KeyFactory.createKey("Symbol", symbol);
			symbolEntity = datastore.get(key);
			
		} catch (EntityNotFoundException e) {
			symbolEntity = new Entity("Symbol", symbol);
		}
		
		return symbolEntity;
	}

	public static void updateSector(Entity symbolEntity, String industry, String sector) {
		symbolEntity.setProperty("industry", industry);
		symbolEntity.setProperty("sector", sector);
	}
	
	public static Entity updateHighlights(Entity symbolEntity) {
		
		String symbol = symbolEntity.getKey().getName();
		
		SETHighlights set = new SETHighlights(symbol);
		
		symbolEntity.setProperty("date", set.date);
		symbolEntity.setProperty("closes", set.financeCloses);
		
		for (String prop:SETHighlights.PROPERTIES_NAMES) {
			symbolEntity.setProperty(prop, set.getValues(prop));
		}						
							
		for (String prop:SETHighlights.PROPERTY_NAMES) {
			symbolEntity.setProperty(prop, set.getValue(prop));
		}
							
		return symbolEntity;
	}

	public static Entity updateDVD(Entity symbolEntity) {
		
		String symbol = symbolEntity.getKey().getName();
		
		SETDividend dvd = new SETDividend(symbol);
		symbolEntity.setProperty("dvd", dvd.value);
		symbolEntity.setProperty("xd", dvd.xd);
				
		return symbolEntity;
	}
	
	public static Entity updateProfile(Entity symbolEntity) {
		
		String symbol = symbolEntity.getKey().getName();
		
		SETProfile profile = new SETProfile(symbol);
		symbolEntity.setProperty("name", profile.name);
		symbolEntity.setProperty("website", profile.website);
		symbolEntity.setProperty("dvdPolicy", profile.dvdPolicy);
		
		return symbolEntity;
	}

}
