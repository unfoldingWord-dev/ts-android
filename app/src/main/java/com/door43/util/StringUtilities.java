package com.door43.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.Editable;
import android.text.SpannedString;
import android.util.Pair;

/**
 * Created by joel on 1/14/2015.
 */
public class StringUtilities {

    /**
     * Returns a string formatted as an integer (removes the leading 0's
     * Otherwise it returns the original value
     * @param value the string to format
     * @return the number formatted string
     */
    public static String formatNumber(String value) {
        try {
            return Integer.parseInt(value) + "";
        } catch (Exception e) {}
        return value;
    }

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

    /**
     * Copies the text to the clipboard
     * @param text
     */
    public static void copyToClipboard(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("simple text", text);
        clipboard.setPrimaryClip(clip);
    }

    /**
     * Expands a selection to include spans
     * @param text
     * @param start
     * @param end
     * @return the pair of start and end values
     */
    public static Pair<Integer, Integer> expandSelectionForSpans(Editable text, int start, int end) {
        // make sure we don't cut any spans in half
        SpannedString[] spans = text.getSpans(start, end, SpannedString.class);
        for(SpannedString s :  spans) {
            int spanStart = text.getSpanStart(s);
            int spanEnd = text.getSpanEnd(s);
            if(spanStart < start) {
                start = spanStart;
            }
            if(spanEnd > end) {
                end = spanEnd;
            }
        }
        return new Pair(start, end);
    }

    public static String ltrim(String str, char target) {
        if (str.length() > 0 && str.charAt(str.length()-1)==target) {
            str = str.substring(0, str.length()-1);
        }
        return str;
    }
}
