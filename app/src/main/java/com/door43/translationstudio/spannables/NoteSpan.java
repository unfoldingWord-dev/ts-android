package com.door43.translationstudio.spannables;

import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.util.MainContext;

/**
 * Created by joel on 10/28/2014.
 */
public class NoteSpan extends FancySpan {
    private final String mNoteText;
    private int mId;
    private int mFootnoteId;
    private NoteType mNoteType;
    private static int mNumNotes = 0;
    private static int mNumFootnotes = 0;
    public static final String TAG = "note";
    public static final String REGEX_OPEN_TAG = "<"+TAG+" ((?!>).)*>";
    public static final String REGEX_CLOSE_TAG = "</"+TAG+">";

    /**
     * Identifies the note as a particular type
     */
    public static enum NoteType {
        UserNote,
        Footnote
    }

    public NoteSpan(String text, String noteText, NoteType noteType, OnClickListener clickListener) {
        super(mNumNotes + "", text, clickListener);
        mId = mNumNotes;
        mNumNotes++;
        if(noteType == NoteType.Footnote) {
            mNumFootnotes ++;
            mFootnoteId = mNumFootnotes;
        }
        mNoteText = noteText;
        mNoteType = noteType;
    }

    /**
     * Converts the footnote to a spannable char sequence
     * @return
     */
    public CharSequence toCharSequence() {
        Bundle attrs = new Bundle();
        attrs.putString("id", mId + "");
        if(mNoteType == NoteType.Footnote) {
            // load custom footnote layout
            TextView textView = (TextView) MainContext.getContext().getCurrentActivity().getLayoutInflater().inflate(R.layout.span_footnote, null);
            textView.setText(Html.fromHtml(toString() + "<sup>" + mFootnoteId + "</sup>"));
            textView.setTextSize(MainContext.getContext().getResources().getDimension(R.dimen.h5));
            BitmapDrawable bm = convertViewToDrawable(textView);
            return generateSpan(generateTag(toString(), mNoteText, mNoteType, attrs), bm);
        } else {
            return generateSpan(generateTag(toString(), mNoteText, mNoteType, attrs), R.drawable.span_light_blue_bubble, R.color.blue, R.dimen.h5);
        }
    }

    /**
     * Generates the passage note tag
     * @param title the passage title
     * @param definition the passage definition
     * @return
     */
    public static String generateTag(String title, String definition, NoteType noteType) {
        return generateTag(title, definition, noteType, new Bundle());
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
    public static String generateTag(String title, String definition, NoteType noteType, Bundle attrs) {
        String attributes = "";
        for(String key: attrs.keySet()) {
            attributes += String.format("%s=\"%s\" ", key, attrs.getString(key));
        }
        if(noteType == NoteType.Footnote) {
            attributes += "footnote ";
        }
        return "<"+TAG+" "+attributes+"def=\""+definition+"\">"+title+"</"+TAG+">";
    }

    public int getId() {
        return mId;
    }

    public NoteType getNoteType() {
        return mNoteType;
    }

    public static void reset() {
        mNumFootnotes = 0;
        mNumNotes = 0;
    }
}
