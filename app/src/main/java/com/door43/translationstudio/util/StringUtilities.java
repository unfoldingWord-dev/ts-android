package com.door43.translationstudio.util;

/**
 * Created by joel on 1/14/2015.
 */
public class StringUtilities {
    /**
     * Splits a string by delimiter into two pieces
     * @param string the string to split
     * @param delimiter
     * @return
     */
    public static String[] chunk(String string, String delimiter) {
        if(string == null || string.isEmpty()) {
            return new String[]{"", ""};
        }
        String[] pieces = string.split(delimiter, 2);
        if(pieces.length == 1) {
            pieces = new String[] {string, ""};
        }
        return pieces;
    }
}
