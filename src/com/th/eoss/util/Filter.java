package com.th.eoss.util;

/**
 * Created by wisarut on 4/10/2559.
 */

public class Filter {
    public boolean isValid (float value, float filterValue) {
        return true;
    }

    public static class LowerOrEqualThanFilter extends Filter {
        public boolean isValid (float value, float filterValue) {
            return value <= filterValue;
        }
    }

    public static class HigherOrEqualThanFilter extends Filter {
        public boolean isValid (float value, float filterValue) {
            return value >= filterValue;
        }
    }
}

