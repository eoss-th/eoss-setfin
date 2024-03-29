import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.extensions.appengine.auth.oauth2.AppIdentityCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.prediction.Prediction;
import com.google.api.services.prediction.PredictionScopes;
import com.google.api.services.prediction.model.Input;
import com.google.api.services.prediction.model.Input.InputInput;
import com.google.api.services.prediction.model.Insert;
import com.google.api.services.prediction.model.Insert2;
import com.google.api.services.prediction.model.Output;
import com.google.api.services.storage.StorageScopes;

import java.io.IOException;
import java.security.PrivateKey;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author Yaniv Inbar
 */
public class YahooPredictMain {

  /**
   * Be sure to specify the name of your application. If the application name is {@code null} or
   * blank, the application will log a warning. Suggested format is "MyCompany-ProductName/1.0".
   */
  private static final String APPLICATION_NAME = "YahooPrediction";

  /** Specify the Cloud Storage location of the training data. */
  static final String STORAGE_DATA_LOCATION = "eoss-setfin.appspot.com/INTUCH.csv";
  static final String MODEL_ID = "intuch";

  /**
   * Specify your Google Developers Console project ID, your service account's email address, and
   * the name of the P12 file you copied to src/main/resources/.
   */
  static final String PROJECT_ID = "eoss-setfin";
  static final String SERVICE_ACCT_EMAIL = "eoss-setfin@appspot.gserviceaccount.com";
  static final String SERVICE_ACCT_KEYFILE = "eoss-th.p12";

  /** Global instance of the HTTP transport. */
  private static HttpTransport httpTransport;

  /** Global instance of the JSON factory. */
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

  /** Authorizes the installed application to access user's protected data. */
  private static GoogleCredential authorize() throws Exception {
    return new GoogleCredential.Builder()
        .setTransport(httpTransport)
        .setJsonFactory(JSON_FACTORY)
        .setServiceAccountId(SERVICE_ACCT_EMAIL)
        .setServiceAccountPrivateKeyFromP12File(new File(
        		YahooPredictMain.class.getResource("/"+SERVICE_ACCT_KEYFILE).getFile()))
        .setServiceAccountScopes(Arrays.asList(PredictionScopes.PREDICTION,
                                               StorageScopes.DEVSTORAGE_READ_ONLY))
        .build();
  }
  
  private static AppIdentityCredential appengine() throws Exception {
	  AppIdentityCredential credential =
			    new AppIdentityCredential(Arrays.asList(PredictionScopes.PREDICTION,
                        StorageScopes.DEVSTORAGE_READ_ONLY));
	  return credential;
  }

  private static void run() throws Exception {
    httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    // authorization
//  HttpRequestInitializer credential = authorize();
    HttpRequestInitializer credential = appengine();
    Prediction prediction = new Prediction.Builder(
        httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
    //train(prediction);
    predict(prediction, 53.75, 51.25, 50.75, 52.25, 52.75);
    predict(prediction, 54.00, 54.25, 54.50, 55.00, 54.50);
  }

  private static void train(Prediction prediction) throws IOException {
    Insert trainingData = new Insert();
    trainingData.setId(MODEL_ID);
    trainingData.setStorageDataLocation(STORAGE_DATA_LOCATION);
    prediction.trainedmodels().insert(PROJECT_ID, trainingData).execute();
    System.out.println("Training started.");
    System.out.print("Waiting for training to complete");
    System.out.flush();

    int triesCounter = 0;
    Insert2 trainingModel;
    while (triesCounter < 100) {
      // NOTE: if model not found, it will throw an HttpResponseException with a 404 error
      try {
        HttpResponse response = prediction.trainedmodels().get(PROJECT_ID, MODEL_ID).executeUnparsed();
        if (response.getStatusCode() == 200) {
          trainingModel = response.parseAs(Insert2.class);
          String trainingStatus = trainingModel.getTrainingStatus();
          if (trainingStatus.equals("DONE")) {
            System.out.println();
            System.out.println("Training completed.");
            System.out.println(trainingModel.getModelInfo());
            return;
          }
        }
        response.ignore();
      } catch (HttpResponseException e) {
      }

      try {
        // 5 seconds times the tries counter
        Thread.sleep(5000 * (triesCounter + 1));
      } catch (InterruptedException e) {
        break;
      }
      System.out.print(".");
      System.out.flush();
      triesCounter++;
    }
    error("ERROR: training not completed.");
  }

  private static void error(String errorMessage) {
    System.err.println();
    System.err.println(errorMessage);
    System.exit(1);
  }

  private static void predict(Prediction prediction, Object...closes) throws IOException {
    Input input = new Input();
    InputInput inputInput = new InputInput();
    inputInput.setCsvInstance(Arrays.asList(closes));
    input.setInput(inputInput);
    Output output = prediction.trainedmodels().predict(PROJECT_ID, MODEL_ID, input).execute();
    System.out.println("Text: " + input);
    System.out.println("Predicted language: " + output.getOutputValue());
  }

  public static void main(String[] args) {
    try {
      run();
      // success!
      return;
    } catch (IOException e) {
      System.err.println(e.getMessage());
    } catch (Throwable t) {
      t.printStackTrace();
    }
    System.exit(1);
  }
}
