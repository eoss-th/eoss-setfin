package com.th.eoss.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wisarut on 4/10/2559.
 */

public class Mean {

    private float total;
    private int count;

    private static Map<String, Mean> meanMap = new HashMap<>();

    public static void add(String name, float value) {
        Mean mean = meanMap.get(name);
        if ( mean==null )
            mean = new Mean();
        mean.add(value);
        meanMap.put(name, mean);
    }

    public static Mean mean(String name) {
        return meanMap.get(name);
    }

    public void add(float value) {
        if (value>=0) {
            this.total += value;
            this.count ++;
        }
    }

    public float value () {
        return total/count;
    }
}