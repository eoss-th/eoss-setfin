package com.th.eoss.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by wisarut on 28/9/2559.
 */

public class SETHighlights {
	
	public static final String [] PROPERTIES_NAMES = {
			"Assets",
			"Liabilities",
			"Equity",
			"Paid-up Capital",
			"Revenue",
			"Net Profit",
			"EPS (Baht)",
			"ROA(%)",
			"ROE(%)",
			"Net Profit Margin(%)",
			"Last Price(Baht)",
			"Dvd. Yield(%)"
	};
	

	public static final String [] PROPERTY_NAMES = {
			"E/A Growth %",
			"Revenue Growth %",
			"Net Growth %",
			"EPS Growth %",
			"ROE Growth %",
			"Margin Growth %",
			"DVD Growth %",
			"EPS",
			"ROA",
			"ROE",
			"Margin",
			"Last",
			"P/E",
			"P/BV",
			"DVD %",
			"Market Cap:Estimated E",
			"Estimated Asset",
			"Estimated Equity",
			"Estimated Revenue",
			"Estimated Net"
	}; 
	
    public static final String URL = "http://www.set.or.th/set/companyhighlight.do?language=en&country=US&symbol=";

    public static final NumberFormat numberFormat = new DecimalFormat("#,##0.00");

    public static final DateFormat asOfDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
    
    public static final DateFormat xdDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.US);

    private final Map<String, Number> valueMap = new HashMap<>();
    
    private final Map<String, List<Float>> valuesMap = new HashMap<>();

    public String symbol;

    public String date;
    
    public List<String> financeCloses;

    public SETHighlights(String symbol) {
        this.symbol = symbol;

        symbol = symbol.replace(" ", "+");
        symbol = symbol.replace("&", "%26");

        try {
            Document doc = Jsoup.connect(URL+symbol).get();
            Element table = doc.select("table[class='table table-hover table-info']").first();

            Element headerTR = table.child(1).child(0);
                        
            Elements elements = headerTR.children();
            if (elements.size()>1) {
                financeCloses = new ArrayList<>();
                
                String val;
                for (int i=1; i<elements.size(); i++) {
                	val = elements.get(i).text();
                	if (!val.equals(" "))
                		financeCloses.add(val);
                } 	
            }
            
            String financeCloseDate;
            try {
            	financeCloseDate = financeCloses.get(financeCloses.size()-1);
            } catch (Exception e) {
            	financeCloseDate = "";
            }
            
            float multiplier;
            
            if (financeCloseDate.startsWith("Q1")) {
            	multiplier = 4;
            } else if (financeCloseDate.startsWith("Q2")) {
            	multiplier = 2;
            } else if (financeCloseDate.startsWith("Q3")) {
            	multiplier = 1.33f;
            } else {
            	multiplier = 1;
            }            
            
            Element statTR = table.child(3).child(0);
            
            Element asOfElement = statTR.children().last();
            date = asOfElement.text();
            
            float [] assets = getFloatValues(table, "Assets");
            
            float [] liabilities = getFloatValues(table, "Liabilities");
            float [] equity = getFloatValues(table, "Equity");
            float [] paidUpCapital = getFloatValues(table, "Paid-up Capital");
            float [] revenue = getFloatValues(table, "Revenue");
            float [] netProfit = getFloatValues(table, "Net Profit");
            float [] eps = getFloatValues(table, "EPS (Baht)");
            float [] roa = getFloatValues(table, "ROA(%)");
            float [] roe = getFloatValues(table, "ROE(%)");
            float [] margin = getFloatValues(table, "Net Profit Margin(%)");
            
            float [] lastPrice  = getFloatValues(table, "Last Price(Baht)");
            float [] marketCap = getFloatValues(table, "Market Cap.");            
            //String [] fsDate = getStringValues(table, "F/S Period (As of date)");
            float [] pe = getFloatValues(table, "P/E");
            float [] pbv = getFloatValues(table, "P/BV");
            float [] bookValue = getFloatValues(table, "Book Value per share (Baht)");
            float [] prices = getFloatValues(table, "Last Price(Baht)");
            float [] dvdYield = getFloatValues(table, "Dvd. Yield(%)");
            
            //Captures from Screen
            valuesMap.put(PROPERTIES_NAMES[0], toList(assets));
            valuesMap.put(PROPERTIES_NAMES[1], toList(liabilities));
            valuesMap.put(PROPERTIES_NAMES[2], toList(equity));
            valuesMap.put(PROPERTIES_NAMES[3], toList(paidUpCapital));
            valuesMap.put(PROPERTIES_NAMES[4], toList(revenue));
            valuesMap.put(PROPERTIES_NAMES[5], toList(netProfit));
            valuesMap.put(PROPERTIES_NAMES[6], toList(eps));
            valuesMap.put(PROPERTIES_NAMES[7], toList(roa));
            valuesMap.put(PROPERTIES_NAMES[8], toList(roe));
            valuesMap.put(PROPERTIES_NAMES[9], toList(margin));
            valuesMap.put(PROPERTIES_NAMES[10], toList(prices));            
            valuesMap.put(PROPERTIES_NAMES[11], toList(dvdYield));
            
            float prevEquity = round(netProfit[0] * 200 / roe[0] - equity[0]);
            
            float [] adjustedEquity = new float[equity.length+1];
            
            adjustedEquity[0] = prevEquity;
    		adjustedEquity[1] = equity[0];
            
    		float addedE;
            for (int i=0;i<equity.length;i++) {
            	if (i==0) continue;
            	addedE = paidUpCapital[i] - paidUpCapital[i-1];
            	if (addedE < 0)
            		addedE = 0;
            	if (equity[i] > addedE)
            		adjustedEquity[i+1] = equity[i] - addedE;
            }
            
            estimate(assets);
            
            estimate(equity);
            
            estimate(adjustedEquity);
            
            estimate(multiplier, revenue);
            
            estimate(multiplier, netProfit);
            
            estimate(multiplier, eps);
            
            if (equity.length>=2) 
            	roe[roe.length-1] = round(netProfit[netProfit.length-1] * 200 / (equity [equity.length-1] + equity [ equity.length-2]));
            
            margin[margin.length-1] = round(netProfit[netProfit.length-1] * 100 / revenue[revenue.length-1]);
            
            /*
             * May be no need to adjust for EA
            float [] ea = new float [adjustedEquity.length];
            
            ea [0] = round(adjustedEquity[0] / assets [0]);
            
            for (int i=1;i<ea.length;i++) {
            	ea[i] = round(adjustedEquity[i] / assets[i-1]);
            }
            */
            
            float [] ea = new float [equity.length];
            
            for (int i=0;i<ea.length;i++) {
            	ea[i] = round(equity[i] / assets[i]);
            }
            
            float eaGrowth = getAvgGrowth(ea);
            
            float revenueGrowth = getAvgGrowth(revenue);
            
            float netProfitGrowth = getAvgGrowth(netProfit);
            
            float epsGrowth = getAvgGrowth(eps);
            
            float roeGrowth = getAvgGrowth(roe);
            
            float marginGrowth = getAvgGrowth(margin);
            
            float dvdGrowth = getAvgGrowth(dvdYield);
            
            valueMap.put(PROPERTY_NAMES[0], eaGrowth);
            valueMap.put(PROPERTY_NAMES[1], revenueGrowth);
            valueMap.put(PROPERTY_NAMES[2], netProfitGrowth);
            valueMap.put(PROPERTY_NAMES[3], epsGrowth);
            valueMap.put(PROPERTY_NAMES[4], roeGrowth);
            valueMap.put(PROPERTY_NAMES[5], marginGrowth);
            valueMap.put(PROPERTY_NAMES[6], dvdGrowth);
            valueMap.put(PROPERTY_NAMES[7], eps[eps.length-1]);
            valueMap.put(PROPERTY_NAMES[8], roa[roa.length-1]);
            valueMap.put(PROPERTY_NAMES[9], roe[roe.length-1]);
            valueMap.put(PROPERTY_NAMES[10], margin[margin.length-1]);
            valueMap.put(PROPERTY_NAMES[11], lastPrice[lastPrice.length-1]);
            valueMap.put(PROPERTY_NAMES[12], pe[pe.length-1]);
            valueMap.put(PROPERTY_NAMES[13], pbv[pbv.length-1]);
            valueMap.put(PROPERTY_NAMES[14], dvdYield[dvdYield.length-1]);
            valueMap.put(PROPERTY_NAMES[15], marketCap[marketCap.length-1]/adjustedEquity[adjustedEquity.length-1]);
            valueMap.put(PROPERTY_NAMES[16], assets[assets.length-1]);
            valueMap.put(PROPERTY_NAMES[17], adjustedEquity[adjustedEquity.length-1]);
            valueMap.put(PROPERTY_NAMES[18], revenue[revenue.length-1]);
            valueMap.put(PROPERTY_NAMES[19], netProfit[netProfit.length-1]);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private List<Float> toList(float[]values) {
    	List<Float> list = new ArrayList<>();
    	if (values!=null) {
    		for (float v:values) {
    			list.add(round(v));
    		}
    	}
    	return list;
    }
    
    private void estimate(float [] values) {
    	if (values.length < 2) return;
    	values [values.length-1] = (values [values.length-1] + values [values.length-2]) / 2.0f;
    }
    
    private void estimate(float multiplier, float [] values) {
    	values [values.length-1] = round (values [values.length-1] * multiplier);    	
    }        
    
    private void print (List<Float> values) {
    	if (values==null) return;
    	float [] vals = new float[values.size()];
    	for (int i=0;i<vals.length;i++) {
    		vals[i] = values.get(i);
    	}
    	print (vals);
    }
    
    private void print (float[]values) {
        for (float v:values) {
        	System.out.print(v+"\t");
        }
    	System.out.println();
    }

    private void print (String[]values) {
        for (String v:values) {
        	System.out.print(v+"\t");
        }
    	System.out.println();
    }
    
    private void print2 (List<String> values) {
        for (String v:values) {
        	System.out.print(v+"\t");
        }
    	System.out.println();
    }
    
    private void print (float value) {
    	System.out.println(value);
    }
    
    private float [] getFloatValues(Element element, String rowTitle) {
        try {
            Element tr = element.select("td:containsOwn(" + rowTitle + ")").first().parent();
            int size = tr.children().size();
            
            float [] values = new float[size-1];
            
            String val;
            for (int i=0;i<values.length;i++) {
                val = trim(tr.child(i+1).text());
                try {
            	values[i] = numberFormat.parse(val).floatValue();
                } catch (Exception e) {
                	//Last is empty
                	if ( i==values.length-1 && i>0 )
                		values[i] = -1;
                }
            }
            
            //Resize
            if (values[values.length-1]==-1) {
            	float [] resizedValues = new float[values.length-1];
            	for (int i=0;i<resizedValues.length;i++) {
            		resizedValues[i] = values[i];
            	}
            	return resizedValues;
            }
            
            return values;
            
        } catch (Exception e) {
        	//e.printStackTrace();
        }
        return null;
    }
    
    private String [] getStringValues(Element element, String rowTitle) {
        try {
            Element tr = element.select(":containsOwn(" + rowTitle + ")").first().parent();
            int size = tr.children().size();
            
            String [] values = new String[size-1];
            
            for (int i=0;i<values.length;i++) {
            	values[i] = trim(tr.child(i+1).text());
            }
            return values;
            
        } catch (Exception e) {
        	e.printStackTrace();
        }
        return null;
    }
    
    public List<Float> getValues(String valueName) {    	
    	return valuesMap.get(valueName);
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

    private float getAvgGrowth (float[]values) {
    	
    	if (values.length < 2) return 0;

    	float growth = 0;
    	float [] numbers = new float[values.length];
    	float [] vals = new float[values.length];
    	
    	float totalMinus = 0;
    	for (int i=0;i<values.length;i++) {
    		if (values[i]<0)
    			totalMinus += Math.abs(values[i]);
    	}
    	
    	for (int i=0;i<values.length;i++) {
    		vals[i] = values[i] + totalMinus;
    	}
    	
    	for (int i=0;i<numbers.length;i++) {
    		numbers[i] = i;
    	}
    	
    	LinearRegression lr = new LinearRegression(numbers, vals);
    	double y1 = lr.predict(values.length-2);
    	double y2 = lr.predict(values.length-1);
    	
    	growth = (float) (((y2 - y1)/y1)*100);
    	
    	return growth;
    }
    
    
    private float round (float num) {
    	return (float) (Math.round(num * 100.0)/100.0);
    }
    
    public static void main(String [] args) {
    	
    	new SETHighlights("BTNC");
    	/*
    	new SETHighlights("THIP");
    	new SETHighlights("SPCG");
    	new SETHighlights("MILL");
    	new SETHighlights("TWPC");
    	new SETHighlights("TASCO");
    	new SETHighlights("JAS");
    	new SETHighlights("AMATA");
    	new SETHighlights("ANAN");
    	new SETHighlights("AP");    	
    	new SETHighlights("INSURE");
    	new SETHighlights("FER");
    	new SETHighlights("WIIK");*/
    }
}
