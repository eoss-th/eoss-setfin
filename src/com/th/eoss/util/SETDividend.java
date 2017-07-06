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

public class SETDividend {

    public static final String URL = "http://www.set.or.th/set/companyrights.do?language=en&country=US&symbol=";

    public static final DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy hh:mm:ss", Locale.US);

    public String symbol;

    public String xd;
    
    public String value;

    public SETDividend(String symbol) {
        this.symbol = symbol;

        symbol = symbol.replace(" ", "+");
        symbol = symbol.replace("&", "%26");

        try {
            Document doc = Jsoup.connect(URL+symbol).get();
            Element tr = doc.select("td:containsOwn(XD)").first().parent();
            if (tr==null) return;
            Element body = tr.child(2).child(0).child(0);
            try {
                this.xd = tr.child(0).text();
                this.value = body.child(2).child(1).text();
            } catch (Exception e) {

            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("\t" + this.symbol);
        }
    }

    public String toString() {
        return String.format("%s\t%s\t%s",symbol,xd,value);
    }
    
    public static void main(String[]args) {
    	System.out.println(new SETDividend("CMR"));
    }

}
