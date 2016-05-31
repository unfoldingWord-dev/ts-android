package com.door43.translationstudio.util;

import java.util.Comparator;

/**
 * Created by joel on 5/20/16.
 */
public class NumericStringComparator implements Comparator<String> {

    @Override
    public int compare(String lhs, String rhs) {
        int num1 = coerceInt(lhs);
        int num2 = coerceInt(rhs);
        return num1 - num2;
    }

    private int coerceInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
