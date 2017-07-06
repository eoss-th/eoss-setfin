package com.th.eoss.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.nio.charset.Charset;
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

public class LottoHistory {

    public static final String URL = "http://www.myhora.com/หวย/สถิติหวย-ย้อนหลัง-20-ปี.aspx?mode=year-range&value=20";
    
    private List<String> twoDigits = new ArrayList<>();

    public LottoHistory() {
        try {
            Document doc = Jsoup.connect(URLEncoder.encode(URL, Charset.defaultCharset().name())).get();
            
            Element table = doc.select("table[id='dl_lottery_stats_list']").first();
            Elements trs = table.child(0).children();
            
            Element div;
            for (int i=0;i<trs.size();i++) {
            	div = trs.get(i).child(0);
            	System.out.println(div.child(7).text());
            }
            //Element tr = doc.select("strong:containsOwn(EV (MB.))").first().parent().parent().nextElementSibling();
                
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[]args) {
    	new LottoHistory();
    }

}
