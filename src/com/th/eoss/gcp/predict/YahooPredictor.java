package com.th.eoss.gcp.predict;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.api.client.googleapis.extensions.appengine.auth.oauth2.AppIdentityCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.prediction.Prediction;
import com.google.api.services.prediction.Prediction.Trainedmodels.Delete;
import com.google.api.services.prediction.PredictionScopes;
import com.google.api.services.prediction.model.Input;
import com.google.api.services.prediction.model.Insert;
import com.google.api.services.prediction.model.Insert2;
import com.google.api.services.prediction.model.Output;
import com.google.api.services.prediction.model.Input.InputInput;
import com.google.api.services.storage.StorageScopes;
import com.google.appengine.tools.cloudstorage.GcsFileMetadata;
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsInputChannel;
import com.google.appengine.tools.cloudstorage.GcsOutputChannel;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
import com.google.appengine.tools.cloudstorage.RetryParams;
import com.th.eoss.exception.YahooPredictorException;
import com.th.eoss.util.YahooHistory;

public class YahooPredictor {
	
	/**
	 * This is where backoff parameters are configured. Here it is aggressively retrying with
	 * backoff, up to 10 times but taking no more that 15 seconds total to do so.
	 */
	private final GcsService gcsService = GcsServiceFactory.createGcsService(new RetryParams.Builder()
			.initialRetryDelayMillis(10)
			.retryMaxAttempts(10)
			.totalRetryPeriodMillis(15000)
			.build());
	
	static final String APPLICATION_NAME = "YahooPrediction";
	static final String PROJECT_ID = "eoss-setfin";
	static final String STORAGE_DATA_LOCATION = "eoss-setfin.appspot.com";

	/** Global instance of the JSON factory. */
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();


	/**Used below to determine the size of chucks to read in. Should be > 1kb and < 10MB */
	private static final int BUFFER_SIZE = 2 * 1024 * 1024;
	
	private String symbol;
	
	private YahooHistory yahoo;
	
	private GcsFileOptions instance;
	
	public YahooPredictor (String symbol) throws Exception {
		
		this.symbol = symbol;
		
		yahoo = new YahooHistory(symbol);
		
		instance = GcsFileOptions.getDefaultInstance();
		
	}
	
	public void train() throws Exception {
		GcsFilename file = new GcsFilename(STORAGE_DATA_LOCATION, symbol+".csv");
		
		String trainingCSV = createCSV(yahoo.getLabelTrainingDataWithWeekNumber(5));
		
		InputStream stream = new ByteArrayInputStream(trainingCSV.getBytes(StandardCharsets.UTF_8));
		
		GcsOutputChannel outputChannel = gcsService.createOrReplace(file, instance);
		copy(stream, Channels.newOutputStream(outputChannel));
		
		Prediction prediction = createPrediction();
		
		prediction.trainedmodels().delete(PROJECT_ID, symbol);
		
		train(prediction, symbol, "csv");
	}
	
	public void _trainDiff() throws Exception {
		GcsFilename file = new GcsFilename(STORAGE_DATA_LOCATION, symbol+".csv");
		GcsFilename fileDiff = new GcsFilename(STORAGE_DATA_LOCATION, symbol+".diff.csv");
		
		GcsFileMetadata meta = gcsService.getMetadata(file);
		GcsFileMetadata metaDiff = gcsService.getMetadata(fileDiff);
		
		String trainingCSV = createCSV(yahoo.getTrainingData(5));
		
		InputStream stream = new ByteArrayInputStream(trainingCSV.getBytes(StandardCharsets.UTF_8));
		
		GcsOutputChannel outputChannel = gcsService.createOrReplace(file, instance);
		copy(stream, Channels.newOutputStream(outputChannel));
		
		Prediction prediction = createPrediction();
		
		boolean skipTrain = false;
		if (meta!=null) {
			
			GcsInputChannel inputChannel = gcsService.openReadChannel(file, 0);
			BufferedReader br = new BufferedReader(new InputStreamReader(Channels.newInputStream(inputChannel)));
			String line = br.readLine();
			
			int index = trainingCSV.indexOf(line);
			if (index!=0) {
				trainingCSV = trainingCSV.substring(0, index);
			} else {
				skipTrain = true;					
			}
		} else if (metaDiff==null) {
			prediction.trainedmodels().delete(PROJECT_ID, symbol);
		}

		if (!skipTrain) {
			stream = new ByteArrayInputStream(trainingCSV.getBytes(StandardCharsets.UTF_8));
			outputChannel = gcsService.createOrReplace(fileDiff, instance);
			copy(stream, Channels.newOutputStream(outputChannel));
			
			train(prediction, symbol, "diff.csv");				
		}
	}	

	private String createCSV(float [][] trainingData) {
		StringBuilder sb = new StringBuilder();
		for (float[]row:trainingData) {
			int i=0;
			for (float d:row) {
				sb.append(String.format("%.2f",d));
				if (i<5)
					sb.append(",");
				i++;
			}
			sb.append(System.lineSeparator());
		}
		return sb.toString();
	}

	private String createCSV(Object [][] trainingData) {
		StringBuilder sb = new StringBuilder();
		for (Object[]row:trainingData) {
			int i=0;
			for (Object d:row) {
				sb.append(d);
				if (i<6)
					sb.append(",");
				i++;
			}
			sb.append(System.lineSeparator());
		}
		return sb.toString();
	}
	
	private void copy(InputStream input, OutputStream output) throws IOException {
		try {
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead = input.read(buffer);
			while (bytesRead != -1) {
				output.write(buffer, 0, bytesRead);
				bytesRead = input.read(buffer);
			}
		} finally {
			input.close();
			output.close();
		}
	}	

	private static Prediction createPrediction() throws Exception {
		HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		HttpRequestInitializer credential = new AppIdentityCredential(Arrays.asList(PredictionScopes.PREDICTION,
				StorageScopes.DEVSTORAGE_READ_ONLY));
		return new Prediction.Builder(
				httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();	
	}
	
	private void train(Prediction prediction, String symbol, String extension) throws IOException {
		
		Insert trainingData = new Insert();
		trainingData.setId(symbol);
		trainingData.setStorageDataLocation(STORAGE_DATA_LOCATION + "/" + symbol + "." + extension);
		prediction.trainedmodels().insert(PROJECT_ID, trainingData).execute();
		
	}
	
	public String _predict (String inputs) throws Exception {
		Prediction prediction = createPrediction();
		HttpResponse response = prediction.trainedmodels().get(PROJECT_ID, symbol).executeUnparsed();
		if (response.getStatusCode() == 200) {
			Insert2 trainingModel = response.parseAs(Insert2.class);
			String trainingStatus = trainingModel.getTrainingStatus();
			if (trainingStatus.equals("DONE")) {
				
			    Input input = new Input();
			    InputInput inputInput = new InputInput();
			    
				List values = new ArrayList();
				if (inputs!=null) {
					String [] inputArray = inputs.split(",");
					
					try {
						
						for (int i=0;i<5;i++) {
							values.add(Float.parseFloat(inputArray[i]));
						}
						
					    inputInput.setCsvInstance(values);
					    
					} catch (Exception e) {
						throw e;
					}
					
				} else {
					//YahooHistory yahoo = new YahooHistory(symbol);
					for (int i=yahoo.closes.length-1;i>=yahoo.closes.length-5;i--) {
						values.add(yahoo.closes[i]);
					}
				}
				
				float totalPrice, mean;
				totalPrice = 0;
				
			    StringBuilder sb = new StringBuilder();
				for (Object o:values) {
					totalPrice += (Float) o;
					sb.append(o+",");
				}
				
				mean = totalPrice / values.size();
				
			    inputInput.setCsvInstance(values);
			    input.setInput(inputInput);
			    Output output = prediction.trainedmodels().predict(PROJECT_ID, symbol, input).execute();
			    
			    float last = (float) values.get(0);
			    float predict = Float.parseFloat(output.getOutputValue());
			    float chg = (predict - last) * 100 / last;
				return String.format("Last=%.2f,Predict=%.2f,Chg=%.2f%s,MA5=%.2f,Inputs=%s", last, predict, chg, "%", mean, sb.toString());
			    
			} else {
				return trainingStatus;
			}
		}
		
		String statusMsg = response.getStatusMessage();
		response.ignore();
		return statusMsg;
	}
	
	public static Insert2 getTraningModel(String symbol) throws Exception {
		Prediction prediction = createPrediction();
		HttpResponse response = prediction.trainedmodels().get(PROJECT_ID, symbol).executeUnparsed();
		if (response.getStatusCode() == 200) {
			Insert2 trainingModel = response.parseAs(Insert2.class);
			return trainingModel;
		}
		throw new Exception("Status Code:" + response.getStatusCode());
	}
	
	public String predict (String inputs) throws Exception {
		
	    try {
	    	
		Prediction prediction = createPrediction();
		HttpResponse response = prediction.trainedmodels().get(PROJECT_ID, symbol).executeUnparsed();
		
		if (response.getStatusCode() == 200) {
			
			Insert2 trainingModel = response.parseAs(Insert2.class);
			String trainingStatus = trainingModel.getTrainingStatus();
			if (trainingStatus.equals("DONE")) {
				
			    Input input = new Input();
			    InputInput inputInput = new InputInput();
			    
				List values = new ArrayList();
				if (inputs!=null) {
					String [] inputArray = inputs.split(",");
					
					try {
						
						for (int i=0;i<5;i++) {
							values.add(Float.parseFloat(inputArray[i]));
						}
						
					    inputInput.setCsvInstance(values);
					    
					} catch (Exception e) {
						throw e;
					}
					
				} else {
					//YahooHistory yahoo = new YahooHistory(symbol);
					int i;
					for (i=yahoo.closes.length-1;i>=yahoo.closes.length-5;i--) {
						values.add(yahoo.closes[i]);
						
					}
					//Add Week number at the last column
					values.add(yahoo.getWeekNumber(yahoo.hilos[i+1].date));
				}
				
				float totalPrice, mean;
				totalPrice = 0;
				
				for (int i=0;i<5;i++) {
					totalPrice += (Float) values.get(i);
				}
				
				mean = totalPrice / 5;
				
			    inputInput.setCsvInstance(values);
			    input.setInput(inputInput);
			    
				Output output = prediction.trainedmodels().predict(PROJECT_ID, symbol, input).execute();
				    
			    String classify = output.getOutputLabel();
				    
			    String accuracy = trainingModel.getModelInfo().getClassificationAccuracy();
				    
				return String.format("MA5=%.2f,Output=%s,Accuracy=%s", mean, classify, accuracy);				
			    
			} else {
				return trainingStatus;
			}
		}
		
		String statusMsg = response.getStatusMessage();
		response.ignore();
		return statusMsg;
		
	    } catch (Exception e) {
	    	throw new YahooPredictorException(e);
	    }
	}	
	
	public String accuracy(int limit) throws Exception {
		
		//StringBuilder csvResult = new StringBuilder("Last,NextMA5,PredictMA5");
		float [][] data = yahoo.getTrainingData(5);
		int total = 0;
		int score = 0;
		
		Prediction prediction = createPrediction();
		HttpResponse response = prediction.trainedmodels().get(PROJECT_ID, symbol).executeUnparsed();
		if (response.getStatusCode() == 200) {
			Insert2 trainingModel = response.parseAs(Insert2.class);
			String trainingStatus = trainingModel.getTrainingStatus();
			
			if (trainingStatus.equals("DONE")) {
				
			    Input input;
			    InputInput inputInput;
			    
				List values;
				float realOutput, predict, totalInput, mean;
				
				for (float[]row:data) {
					
					values = new ArrayList();
					
					totalInput = 0;
					for (int i=0;i<row.length;i++) {
						if (i>0) {
							values.add(row[i]);
							totalInput += row[i];
						} 
					}
					
					realOutput = row[0];
					
					mean = totalInput / 5;
															
					inputInput = new InputInput();
					inputInput.setCsvInstance(values);
					input = new Input();
				    input.setInput(inputInput);
				    
				    try {
					    Output output = prediction.trainedmodels().predict(PROJECT_ID, symbol, input).execute();				    
					    predict = Float.parseFloat(output.getOutputValue());
					    
					    if ( 
					    		( predict > mean && realOutput > mean ) ||
					    		( predict < mean && realOutput < mean ) ) {
					    	score ++;
					    }
					    
					    					    					    
				    } catch (Exception e) {
				    	//throw new RuntimeException("Oops:" + total + ":" + values.toString());
				    	break;
				    }
				    
				    total ++;
				    
				    if (total >= limit) break;
				}
				
			}
		}
		
		//return csvResult.toString();
		return new DecimalFormat("0.00").format(score * 100f / total) + "%/" + total;
	}	
	
}
