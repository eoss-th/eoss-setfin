package com.th.eoss.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by wisarut on 28/9/2559.
 */

public class SETSummary {

	public static final String [] PROPERTY_NAMES = {
			"Market Cap",
			"EV",
			"EBITDA",
			"EV/EBITDA",
			"ROA",
			"ROE",
			"Margin",
			"Margin Growth %",
			"P/BV",
			"P/E",
			"EPS",
			"EPS Growth %",
			"CR",
			"CR Growth %",
			"D/E",
			"D/E Growth %",
			"DVD %",
			"DVD Growth %",
			"Last"
	}; 
	
    public static final String URL = "http://www.set.or.th/set/factsheet.do?language=en&country=US&symbol=";

    public static final NumberFormat numberFormat = new DecimalFormat("#,##0.00");

    public static final DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.US);

    private final Map<String, Number> valueMap = new HashMap<>();

    public String symbol;

    public String date;

    public String dvdText;

    public Date payDate;

    public SETQuote quote;

    public YahooHistory yahooHistory;

    public SETSummary(String symbol) {
        this.symbol = symbol;

        symbol = symbol.replace(" ", "+");
        symbol = symbol.replace("&", "%26");

        try {
            Document doc = Jsoup.connect(URL+symbol).get();
            Element date = doc.select("td[style='background-color:#000000; color:#FFFFFF; font-weight:bold;']").first();
            this.date = trimDate(date.text().replace("Data as of", ""));

            try {
                Element tr = doc.select("strong:containsOwn(EV (MB.))").first().parent().parent().nextElementSibling();
                try { valueMap.put("Market Cap", numberFormat.parse(trim(tr.child(5).text())).floatValue()); } catch (Exception e) { }
                try { valueMap.put("EV", numberFormat.parse(trim(tr.child(6).text())).floatValue()); } catch (Exception e) { }
                try { valueMap.put("EBITDA", numberFormat.parse(trim(tr.child(7).text())).floatValue()); } catch (Exception e) { }
                try { valueMap.put("EV/EBITDA", numberFormat.parse(trim(tr.child(8).text())).floatValue()); } catch (Exception e) { }
            } catch (Exception e) {
            }

            float last = getRowValue(doc, "Price (B./share)", 1);
            valueMap.put("Last", last);
            valueMap.put("Cost", last);
            valueMap.put("P/BV", getRowValue(doc, "P/BV (X)", 1));
            valueMap.put("P/E", getRowValue(doc, "P/E (X)", 1));

            valueMap.put("EPS", last / valueMap.get("P/E").floatValue());
            valueMap.put("EPS-1", getRowValue(doc, "Price (B./share)", 2) / getRowValue(doc, "P/E (X)", 2));
            valueMap.put("EPS-2", getRowValue(doc, "Price (B./share)", 3) / getRowValue(doc, "P/E (X)", 3));
            valueMap.put("EPS Growth %", getAverageGrowth("EPS"));

            valueMap.put("DVD", getRowValue(doc, "Dividend Yield (%)", 1));
            valueMap.put("DVD-1", getRowValue(doc, "Dividend Yield (%)", 2));
            valueMap.put("DVD-2", getRowValue(doc, "Dividend Yield (%)", 3));
            valueMap.put("DVD %", valueMap.get("DVD").floatValue());
            valueMap.put("DVD Growth %", getAverageGrowth("DVD"));

            valueMap.put("CR", getRowValue(doc, "Current Ratio (X)", 1));
            valueMap.put("CR-1", getRowValue(doc, "Current Ratio (X)", 2));
            valueMap.put("CR-2", getRowValue(doc, "Current Ratio (X)", 3));
            valueMap.put("CR-3", getRowValue(doc, "Current Ratio (X)", 4));
            valueMap.put("CR Growth %", getAverageGrowth("CR"));

            valueMap.put("ROE", getRowValue(doc, "ROE (%)", 1));
            valueMap.put("ROA", getRowValue(doc, "ROA (%)", 1));

            valueMap.put("D/E", getRowValue(doc, "D/E (X)", 1));
            valueMap.put("D/E-1", getRowValue(doc, "D/E (X)", 2));
            valueMap.put("D/E-2", getRowValue(doc, "D/E (X)", 3));
            valueMap.put("D/E-3", getRowValue(doc, "D/E (X)", 4));
            valueMap.put("D/E Growth %", getAverageGrowth("D/E"));

            valueMap.put("Margin", getRowValue(doc, "Net Profit Margin (%)", 1));
            valueMap.put("Margin-1", getRowValue(doc, "Net Profit Margin (%)", 2));
            valueMap.put("Margin-2", getRowValue(doc, "Net Profit Margin (%)", 3));
            valueMap.put("Margin-3", getRowValue(doc, "Net Profit Margin (%)", 4));
            valueMap.put("Margin Growth %", getAverageGrowth("Margin"));

            valueMap.put("Growth", getRowValue(doc, "Net Profit Growth", 1));

            try {
                Element tr = doc.select("strong:containsOwn(Dividend/Share)").first().parent().parent().nextElementSibling();

                try {
                    this.dvdText = trim(tr.child(1).text());
                } catch (Exception e) {

                }
                try {
                    this.payDate = dateFormat.parse(trim(tr.child(3).text()));
                    valueMap.put("Pay Date", (float) payDate.getTime());
                } catch (Exception e) {

                }

            } catch (Exception e) {
            }


            //
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private float getRowValue(Document doc, String rowTitle, int column) {
        String val = "";
        try {
            Element tr = doc.select("td:containsOwn(" + rowTitle + ")").first().parent();
            val = trim(tr.child(column).text());
            return numberFormat.parse(val).floatValue();
        } catch (NumberFormatException e) {
            System.out.println (symbol+":"+val+":"+rowTitle);
        } catch (Exception e) {
        	//throw new RuntimeException(e);
        }
        return 0.0f;
    }
    
    public Number getValue(String valueName) {
    	
    	Number num = valueMap.get(valueName);
    	if (num!=null&&num.floatValue()!=Float.POSITIVE_INFINITY)
    		return Math.round(num.floatValue() * 100.0) / 100.0;
    	
    	return null;
    }

    private String format(String valueName) {
        if (valueMap.get(valueName)!=null) {
            return numberFormat.format(valueMap.get(valueName));
        }
        return "";
    }

    private String format(Date date) {
        try {
            return dateFormat.format(date);
        } catch (Exception e) {

        }
        return "";
    }

    public float getFloatValue(String valueName) {
        try {
            return valueMap.get(valueName).floatValue();
        } catch (Exception e) {
        }
        return 0;
    }

    public long getLongValue(String valueName) {
        try {
            return valueMap.get(valueName).longValue();
        } catch (Exception e) {
        }
        return 0;
    }
    
    public String getValueFormatted (String valueName) {
    	return format(valueName);
    }

    public String [][] getValuesFormatted() {
        return new String[][]{
                {format("Market Cap"), format("EV"), format("EBITDA"), format("EV/EBITDA")},
                {format("ROA"), format("ROE"), format("Margin"), format("Margin Growth %")},
                {format("P/BV"), format("P/E"), format("EPS"), format("EPS Growth %")},
                {format("CR"), format("CR Growth %"), format("D/E"), format("D/E Growth %")},
                {format(payDate), dvdText, format("DVD %"), format("DVD Growth %")},
                {format("Cost"), format("Last"), format("Chg %"), format("Volume")}
        };
    }

    public static String trimDate(String text) {
    	
    	text = text.trim();
    	StringBuilder sb = new StringBuilder();
    	char ch;
    	for (int i=0;i<text.length();i++) {
    		ch = text.charAt(i);
    		if (Character.isAlphabetic(ch) || Character.isDigit(ch) || ch == ' ') {
    			sb.append(ch);
    		}
    	}
    	return sb.toString();
    }
    
    public static String trim(String text) {
    	return text.trim();
    }

    public String toString() {
        return String.format("%s %s", symbol, date);
    }

    public void updateQuote(SETQuote quote) {
        this.quote = quote;

        valueMap.put("Last", quote.last);
        valueMap.put("Chg %", quote.chg);

        if (yahooHistory!=null) {
            try {
                Date date = SETQuote.dateFormat.parse(quote.date);
                String yahooFormattedDate = YahooHistory.dateFormat.format(date);
                yahooHistory.updateLast(new YahooHistory.HiLo(yahooFormattedDate, quote.open, quote.high, quote.low, quote.last, quote.volume));
            } catch (Exception e) {
            }
        }
    }

    private float getAverageGrowth (String valueName) {

    	float growth = 0;
    	
    	float [] numbers = null, values = null;
        if (valueMap.get(valueName+"-3")!=null) {
            numbers = new float [] {0, 1, 2, 3};
            values = new float [] {
                    valueMap.get(valueName+"-3").floatValue(),
                    valueMap.get(valueName+"-2").floatValue(),
                    valueMap.get(valueName+"-1").floatValue(),
                    valueMap.get(valueName).floatValue()
            };
        } else if (valueMap.get(valueName+"-2")!=null) {
            numbers = new float [] {0, 1, 2};
            values = new float []{
                    valueMap.get(valueName + "-2").floatValue(),
                    valueMap.get(valueName + "-1").floatValue(),
                    valueMap.get(valueName).floatValue()
            };
        }

        if (numbers!=null&&values!=null) {
        	LinearRegression lr = new LinearRegression(numbers, values);
        	float y1 = (float) lr.predict(1);
        	float y2 = (float) lr.predict(2);
        	growth = ((y2 - y1) / y1) * 100;
        	growth = (float) (Math.round(growth * 100.0)/100.0);
        }        

        return growth;
    }
}
