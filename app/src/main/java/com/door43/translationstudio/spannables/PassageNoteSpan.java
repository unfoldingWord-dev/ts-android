package com.door43.translationstudio.spannables;

import android.os.Bundle;

import com.door43.translationstudio.R;

/**
 * Created by joel on 10/28/2014.
 */
public class PassageNoteSpan extends FancySpan {
    private final String mDefinition;
    private int mId;
    private static int mNumSpans = 0;
    public static final String TAG = "passagenote";
    public static final String REGEX_OPEN_TAG = "<"+TAG+" ((?!>).)*>";
    public static final String REGEX_CLOSE_TAG = "</"+TAG+">";

    public PassageNoteSpan(String text, String definition, OnClickListener clickListener) {
        super(mNumSpans+"", text, clickListener);
        mId = mNumSpans;
        mNumSpans ++;
        mDefinition = definition.replace("\"", "'");
    }

    /**
     * Converts the footnote to a spannable char sequence
     * @return
     */
    public CharSequence toCharSequence() {
        Bundle attrs = new Bundle();
        attrs.putString("id", mId + "");
        return generateSpan(generateTag(toString(), mDefinition, attrs), R.drawable.light_blue_bubble, R.color.blue, R.dimen.h5);
    }

    /**
     * Generates the passage note tag
     * @param title the passage title
     * @param definition the passage definition
     * @return
     */
    public static String generateTag(String title, String definition) {
        return generateTag(title, definition, new Bundle());
    }

    /**
     * Generates the regular expression used to select the opening tag of a span by id
     * @param id
     * @return
     */
    public static String regexOpenTagById(String id) {
        return "<"+TAG+" ((?!>).)*id=\""+id+"\"((?!>).)*>";
    }

    /**
     * Generates the passage note tag with additional attributes
     * @param title
     * @param definition
     * @param attrs
     * @return
     */
    public static String generateTag(String title, String definition, Bundle attrs) {
        String attributes = "";
        for(String key: attrs.keySet()) {
            attributes += String.format("%s=\"%s\" ", key, attrs.getString(key));
        }
        return "<"+TAG+" "+attributes+"def=\""+definition+"\">"+title+"</"+TAG+">";
    }

    public int getId() {
        return mId;
    }

    public boolean isFootnote() {
        return false;
    }
}
