package com.th.eoss.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.*;
/**
 * Created by wisarut on 28/9/2559.
 */

public class SETIndex {

    public static final String URL = "http://www.set.or.th/dat/eod/listedcompany/static/listedCompanies_en_US.xls";

    private final Map<String, Map<String, Set<String>>> map;
    
    public SETIndex() {
        map = new TreeMap<String, Map<String, Set<String>>>();
        try {
            Document doc = Jsoup.connect(URL).ignoreContentType(true).get();
            Elements elements = doc.select("table tr");
            String symbol, industry, sector;
            for (int i=2;i<elements.size();i++) {
                symbol = elements.get(i).child(0).text().trim();
                industry = elements.get(i).child(3).text().trim();
                sector = elements.get(i).child(4).text().trim();

                Map<String, Set<String>> dict = map.get(industry);
                if (dict==null) {
                    dict = new TreeMap<String, Set<String>> ();
                    map.put(industry, dict);
                }

                Set<String> list = dict.get(sector);
                if (list==null) {
                    list = new TreeSet<String> ();
                    dict.put(sector, list);
                }

                list.add(symbol);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Map<String, Map<String, Set<String>>> map() {
        return map;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Set<String> industries = map.keySet();
        for (String i:industries) {
            sb.append(i);
            Set<String> sectors = map.get(i).keySet();
            for (String s:sectors) {
                sb.append("\n\t"+s);
                Set<String> list = map.get(i).get(s);
                for (String sym:list) {
                    sb.append("\n\t\t" + sym);
                }
            }
        }
        return sb.toString();
    }
}
