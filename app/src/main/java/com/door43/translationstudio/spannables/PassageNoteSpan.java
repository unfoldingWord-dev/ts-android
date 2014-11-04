package com.door43.translationstudio.spannables;

import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.util.MainContext;

/**
 * Created by joel on 10/28/2014.
 */
public class PassageNoteSpan extends FancySpan {
    private final String mDefinition;
    private int mId;
    private int mFootnoteId;
    private Boolean mIsFootnote;
    private static int mNumSpans = 0;
    private static int mNumFootnotes = 0;
    public static final String TAG = "passagenote";
    public static final String REGEX_OPEN_TAG = "<"+TAG+" ((?!>).)*>";
    public static final String REGEX_CLOSE_TAG = "</"+TAG+">";

    public PassageNoteSpan(String text, String definition, Boolean isFootnote, OnClickListener clickListener) {
        super(mNumSpans+"", text, clickListener);
        mId = mNumSpans;
        mNumSpans ++;
        if(isFootnote) {
            mNumFootnotes ++;
            mFootnoteId = mNumFootnotes;
        }
        mDefinition = definition.replace("\"", "'");
        mIsFootnote = isFootnote;
    }

    /**
     * Converts the footnote to a spannable char sequence
     * @return
     */
    public CharSequence toCharSequence() {
        Bundle attrs = new Bundle();
        attrs.putString("id", mId + "");
        if(mIsFootnote) {
            // load custom footnote layout
            TextView textView = (TextView) MainContext.getContext().getCurrentActivity().getLayoutInflater().inflate(R.layout.span_footnote, null);
            textView.setText(Html.fromHtml(toString() + "<sup>" + mFootnoteId + "</sup>"));
            textView.setTextSize(MainContext.getContext().getResources().getDimension(R.dimen.h5));
            BitmapDrawable bm = convertViewToDrawable(textView);
            return generateSpan(generateTag(toString(), mDefinition, mIsFootnote, attrs), bm);
        } else {
            return generateSpan(generateTag(toString(), mDefinition, mIsFootnote, attrs), R.drawable.span_light_blue_bubble, R.color.blue, R.dimen.h5);
        }
    }

    /**
     * Generates the passage note tag
     * @param title the passage title
     * @param definition the passage definition
     * @return
     */
    public static String generateTag(String title, String definition, Boolean isFootnote) {
        return generateTag(title, definition, isFootnote, new Bundle());
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
    public static String generateTag(String title, String definition, Boolean isFootnote, Bundle attrs) {
        String attributes = "";
        for(String key: attrs.keySet()) {
            attributes += String.format("%s=\"%s\" ", key, attrs.getString(key));
        }
        if(isFootnote) {
            attributes += "footnote ";
        }
        return "<"+TAG+" "+attributes+"def=\""+definition+"\">"+title+"</"+TAG+">";
    }

    public int getId() {
        return mId;
    }

    public boolean isFootnote() {
        return false;
    }

    public static void reset() {
        mNumFootnotes = 0;
        mNumSpans = 0;
    }
}
