package com.th.eoss.servlet;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.th.eoss.exception.YahooHistoryException;
import com.th.eoss.exception.YahooPredictorException;
import com.th.eoss.gcp.predict.YahooPredictor;

public class YahooPredictBatchServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		String industry = req.getParameter("industry");
		//String s = req.getParameter("sector");

		Key key = KeyFactory.createKey("Industry", industry);
		try {
			Entity entity = datastore.get(key);

			Map<String, Object> map = entity.getProperties();
			Set<String> sectors = map.keySet();
			for (String sector:sectors) {

				List<String> symbols = (List<String>) entity.getProperty(sector);

				for (String symbol:symbols) {

					try {
						
						Key setKey = KeyFactory.createKey("Symbol", symbol);
						Entity setEntity;
						try {

							setEntity = datastore.get(setKey);

						} catch (EntityNotFoundException e) {
							setEntity = new Entity("Symbol", symbol);						
						}

						YahooPredictor predictor = new YahooPredictor(symbol);
						String outputs = predictor.predict(null);

						if (outputs.startsWith("MA5")) {
							
							String [] vals = outputs.split(",");

							Number ma5 = Double.parseDouble(vals[0].replace("MA5=", ""));
							String output = vals[1].replace("Output=", "");

							float winrate;
							try {
								String accuracy = vals[2].replace("Accuracy=", "");
								winrate = (float) (Double.parseDouble(accuracy)*100.0f);
								winrate = round(winrate);
							} catch (Exception e) {
								winrate = 0;
							}

							Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Bangkok"));
							int day = calendar.get(Calendar.DAY_OF_WEEK);

							float MA5 = round(ma5.floatValue());

							Long correctCount = (Long) setEntity.getProperty("correctCount");
							if (correctCount==null) correctCount = 0L;

							Long predictCount = (Long) setEntity.getProperty("predictCount");
							if (predictCount==null) predictCount = 0L;

							Double lastMA5 = (Double) setEntity.getProperty("MA5"+"-"+day);
							Double lastPredict = (Double) setEntity.getProperty("predict"+"-"+day);

							if ( lastMA5!=null && lastPredict!=null ) {
								if (
										(lastPredict.floatValue() > 0 && MA5 > lastMA5.floatValue()) ||
										(lastPredict.floatValue() < 0 && MA5 < lastMA5.floatValue())) {
									setEntity.setProperty("correctCount",  correctCount.intValue() + 1);
								}
							}
							setEntity.setProperty("MA5"+"-"+day,  MA5);
							setEntity.setProperty("predict"+"-"+day,  MA5 * (output.equals("Sell")?-1:1));

							setEntity.setProperty("MA5",  MA5);
							setEntity.setProperty("predict",  MA5 * (output.equals("Sell")?-1:1));
							setEntity.setProperty("output",  output);
							setEntity.setProperty("winrate",  winrate);
							setEntity.setProperty("predictCount",  predictCount.intValue() + 1);

							datastore.put(setEntity);

						} else {
							throw new YahooPredictorException(symbol+":"+outputs);
						}
					/*	
					} catch (YahooHistoryException e) {
						
					} catch (GoogleJsonResponseException e) {

					} catch (YahooPredictorException e) {
					*/	
					} catch (Exception e) {
						throw new RuntimeException(e);
					}

				}					
			}

		} catch (EntityNotFoundException e) {
			throw new RuntimeException(e);
		}

	}

	private float round (float num) {
		return Math.round(num * 100.0f)/100.0f;
	}


}
