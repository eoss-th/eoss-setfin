package com.th.eoss.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Stack;

import com.th.eoss.exception.YahooHistoryException;

/**
 * Created by wisarut on 28/9/2559.
 */

public class YahooHistory {
    public static final String YAHOO_URL = "https://ichart.finance.yahoo.com/table.csv?&a=01&b=19&c=2010&s=";
    public String symbol;

    public static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public static class HiLo {
        public String date;
        public float open;
        public float high;
        public float low;
        public float close;
        public long volume;

        public HiLo(String date, float open, float high, float low, float close, long volume) {
            this.date = date;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
        }

        public String toString() {
            return String.format("%s %.2f %.2f %.2f %.2f %d", date, open, high, low, close, volume);
        }
    }

    public HiLo [] hilos;
    public float [] closes;

    public YahooHistory (String symbol) throws Exception {
        this.symbol = symbol;
        BufferedReader br = null;
        try {
            symbol = symbol.replace("&","&amp;");
            symbol = symbol.replace(" ","%20");
            symbol += ".BK";
            br = new BufferedReader(new InputStreamReader(new URL(YAHOO_URL+symbol).openStream()));
            String line;
            Stack<String> stack = new Stack<String> ();
            while ((line=br.readLine())!=null) {
                stack.push(line);
            }

            hilos = new HiLo[stack.size()-1];

            String [] tokens;
            for ( int i=0; i<hilos.length; i++ ) {
                line = stack.pop();
                tokens = line.split(",");
                hilos [i] = new HiLo (tokens[0], Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]), Float.parseFloat(tokens[3]), Float.parseFloat(tokens[4]), Long.parseLong(tokens[5]));
            }

            closes = new float[hilos.length];

            for ( int i=0; i<closes.length; i++ ) {
                closes[i] = hilos [i].close;
            }

        } catch (Exception e) {
        	e.printStackTrace();
            throw new YahooHistoryException(symbol);
        } finally {
            if (br!=null) {
               try { br.close(); } catch (Exception e) { }
            }
        }
    }

    public void updateLast(HiLo hilo) {

        if (hilos!=null && hilo.date !=null && hilo.date.equals(hilos[hilos.length-1].date)) {
            //Update
            hilos[hilos.length-1] = hilo;

        } else {
            //Append
            int newLength = hilos==null?1:hilos.length + 1;
            HiLo [] oldHilos = hilos;
            float [] oldCloses = closes;

            hilos = new HiLo[newLength];
            closes = new float[newLength];

            if (oldHilos!=null) {
                for (int i=0; i<oldHilos.length; i++) {
                    hilos[i] = oldHilos[i];
                    closes[i] = oldCloses[i];
                }
            }

            hilos[newLength-1] = hilo;
            closes[newLength-1] = hilo.close;
        }

    }
    
    public Object [][] getLabelTrainingData (int days) {

        float [][] maResult = getTrainingData(days);
        
        Object [][] result = new Object[maResult.length][days+1];
        
        int i=0;
        float nextMA, ma;
        String label;
        for (float[]row:maResult) {
        	
        	nextMA = row[0];
        	ma = 0;
        	for (int j=1;j<row.length;j++) {
        		ma += row[j];
        		result[i][j] = row[j]; 
        	}
        	ma /= days;
        	
        	if (nextMA>ma) {
        		label = "Buy";
        	} else if (nextMA<ma) {
        		label = "Sell";
        	} else {
        		label = "Hold";
        	}
        	result[i][0] = label;
        	
        	i++;
        }

        return result;
    }
    
    public Object [][] getLabelTrainingDataWithWeekNumber (int days) {

    	Number [][] maResult = getTrainingDataWithWeekNumber(days);
        
        Object [][] result = new Object[maResult.length][days+2];
        
        int i=0;
        float nextMA, ma;
        String label;
        for (Number[]row:maResult) {
        	
        	nextMA = row[0].floatValue();
        	ma = 0;
        	for (int j=1;j<row.length;j++) {
        		
        		if (j<row.length-1)
        			ma += row[j].floatValue();
        		
        		result[i][j] = row[j]; 
        	}
        	ma /= days;
        	
        	if (nextMA>ma) {
        		label = "Buy";
        	} else if (nextMA<ma) {
        		label = "Sell";
        	} else {
        		label = "Hold";
        	}
        	result[i][0] = label;
        	
        	i++;
        }

        return result;
    }    
        
    public float [][] getTrainingData (int days) {

        List<Float> rows;
        List<List<Float>> data = new ArrayList<>();

        float totalPriceXDays, maXDays;
        int j;
        for (int i=closes.length-1; i>=(2*days-1); i--) {
            rows = new ArrayList<>();
            totalPriceXDays = 0;
            for (j = i;j>=i-(days-1);j--) {
                totalPriceXDays += closes[j];
            }
            maXDays = totalPriceXDays / days;
            rows.add(round(maXDays));
            for (;j>=i-(2*days-1);j--) {
                rows.add(round(closes[j]));
            }
            data.add(rows);
        }

        float [][] result = new float[data.size()][days+1];

        for (int i=0; i<result.length; i++) {
            for (int k=0; k<result[i].length; k++) {
                result[i][k] = data.get(i).get(k);
            }
        }

        return result;
    }
    
    public int getWeekNumber(String dateString) {    	
    	try {
    	Date date = dateFormat.parse(dateString);
    	Calendar c = Calendar.getInstance(Locale.US);
    	c.setTime(date);
    	return c.get(Calendar.WEEK_OF_YEAR);
    	} catch (Exception e) {
    		
    	}
    	return 0;
    }
    
    public Number [][] getTrainingDataWithWeekNumber (int days) {

        List<Number> rows;
        List<List<Number>> data = new ArrayList<>();

        float totalPriceXDays, maXDays;
        int j, week;
        for (int i=closes.length-1; i>=(2*days-1); i--) {
            rows = new ArrayList<>();
            totalPriceXDays = 0;
            for (j = i;j>=i-(days-1);j--) {
                totalPriceXDays += closes[j];
            }
            maXDays = totalPriceXDays / days;            
            rows.add(round(maXDays));
            for (;j>=i-(2*days-1);j--) {
                rows.add(round(closes[j]));
            }
            rows.add(getWeekNumber(hilos[j+1].date));
            data.add(rows);
        }

        Number [][] result = new Number[data.size()][days+2];

        for (int i=0; i<result.length; i++) {
            for (int k=0; k<result[i].length; k++) {
                result[i][k] = data.get(i).get(k);
            }
        }

        return result;
    }
        
    private float round (float num) {
    	return Math.round(num * 100.0)/100.0f;
    }
    
    public static void main (String [] args) throws Exception {
    	YahooHistory his = new YahooHistory("2S");
    	Number [][] data0 = his.getTrainingDataWithWeekNumber(5);
    	
    	for (Number[]row:data0) {
    		for (Number d:row) {
    			System.out.print(d);
    			System.out.print("\t\t\t");
    		}
    		System.out.println();
    	}
    	
    	System.out.println();
    	System.out.println();
    	
    	Object [][] data = his.getLabelTrainingDataWithWeekNumber(5);
    	
    	for (Object[]row:data) {
    		for (Object d:row) {
    			System.out.print(d);
    			System.out.print("\t\t\t");
    		}
    		System.out.println();
    	}
    }
}
