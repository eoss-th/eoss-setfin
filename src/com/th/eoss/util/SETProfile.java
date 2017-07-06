package com.th.eoss.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by wisarut on 28/9/2559.
 */

public class SETProfile {

    public static final String URL = "http://www.set.or.th/set/companyprofile.do?language=en&country=US&symbol=";

    public static final DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy hh:mm:ss", Locale.US);

    public String symbol;

    public String website;
    
    public String name;
    
    public String dvdPolicy;

    public SETProfile(String symbol) {
        this.symbol = symbol;

        symbol = symbol.replace(" ", "+");
        symbol = symbol.replace("&", "%26");

        try {
            Document doc = Jsoup.connect(URL+symbol).get();
            
            try {
                Element element = doc.select("strong:containsOwn(Company Name)").first().parent().nextElementSibling();
                name = element.text();
            } catch (Exception e) {
            	name = "";
            }

            try {
                Element element = doc.select("strong:containsOwn(Website)").first().parent().nextElementSibling();
                website = element.text().replace("http://", "");
            } catch (Exception e) {
            	website = "";
            }
            
            try {
                Element element = doc.select("strong:containsOwn(Dividend Policy)").first().parent().nextElementSibling();
                dvdPolicy = element.text();
            } catch (Exception e) {
            	dvdPolicy = "";
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("\t" + this.symbol);
        }
    }

    public String toString() {
        return String.format("%s\t%s\t%s\t%s",symbol,name,website,dvdPolicy);
    }
    
    public static void main(String[]args) {
    	System.out.println(new SETProfile("MSC"));
    	System.out.println(new SETProfile("INTUCH"));
    	System.out.println(new SETProfile("THIP"));
    	System.out.println(new SETProfile("TASCO"));
    	System.out.println(new SETProfile("AAV"));
    }

}
