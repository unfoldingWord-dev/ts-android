package com.door43.translationstudio.spannables;

import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.Html;
import android.util.Xml;
import android.view.View;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.util.MainContext;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * Resets the note counts.
     * This effectively puts the next note id back to 0
     */
    public static void reset() {
        mNumFootnotes = 0;
        mNumNotes = 0;
    }

    /**
     * Generates a new note span from the supplied xml and returns it
     * we are using usx for footnotes and our own variant for user notes
     * http://dbl.ubs-icap.org:8090/display/DBLDOCS/USX#USX-note(Footnote)
     * @param xmlText
     * @return
     */
    public static NoteSpan getInstanceFromXML(String xmlText) {
        XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(new StringReader(xmlText));
            parser.nextTag();
            return readXML(parser);
        } catch (XmlPullParserException e) {
            return null;
        } catch(IOException e) {
            return null;
        }
    }

    /**
     * Reads some xml to produce a new note span
     * @param parser
     * @return
     * @throws XmlPullParserException
     */
    private static NoteSpan readXML(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "note");

        // load attributes
        String style = parser.getAttributeValue("","style");
        NoteType noteType;
        if(style != null) {
            if(style.equals("f")) {
                noteType = NoteType.Footnote;
            } else {
                // by default everything is a user note
                noteType = NoteType.UserNote;
            }
        } else {
            noteType = NoteType.UserNote;
        }

        // load char's
        while(parser.nextTag() != XmlPullParser.END_TAG) {
            String name = parser.getName();
            if (name.equals("char")) {
                // TODO: load
                String charStyle = parser.getAttributeValue("","style");
                String charValue = parser.getText();
            }
        }

//        Pattern defPattern = Pattern.compile("def=\"(((?!\").)*)\"");
//
//
//        // extract type
//        String data = xmlText.substring(0, xmlText.length() - NoteSpan.REGEX_CLOSE_TAG.length());
//        Matcher defMatcher = defPattern.matcher(data);
//        String def = "";
//        if(defMatcher.find()) {
//            def = defMatcher.group(1);
//        }
//        final String definition = def;
//
//        // extract phrase
//        String[] pieces = data.split(NoteSpan.REGEX_OPEN_TAG);
//
//        // determine note type
//        Boolean isFootnote = false;
//        if(data.substring(0, data.length() - pieces[1].length()).contains("footnote")) {
//            isFootnote = true;
//        }
//        final NoteSpan.NoteType noteType = isFootnote ? NoteSpan.NoteType.Footnote : NoteSpan.NoteType.UserNote;
//
//        // build passage note
//        NoteSpan note = new NoteSpan(pieces[1], definition, noteType, new FancySpan.OnClickListener() {
//            @Override
//            public void onClick(View view, String spanText, String spanId) {
//                openPassageNoteDialog(spanText, definition, spanId, noteType);
//            }
//        });
//        if(definition.isEmpty()) {
//            needsUpdate = note;
//        }
        return null;
    }
}
