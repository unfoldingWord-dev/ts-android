package com.door43.translationstudio.spannables;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.AppContext;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Created by joel on 10/28/2014.
 */
public class USFMNoteSpan extends Span {
    private final CharSequence mNotes;
    private final CharSequence mPassage;
    private final String mCaller;
    private static final String DEFAULT_CALLER = "+";
    private String mStyle;
    private SpannableStringBuilder mSpannable;
    public static final String PATTERN = "\\\\f\\s(\\S)\\s(.+)\\\\f\\*";
    public static final String CHAR_PATTERN = "\\\\f([^\\*\\s]+)\\s([^\\\\]+)(?:\\\\f\\1\\*)?";

    /**
     * @param style the note style
     * @param caller the note caller
     * @param chars a list of char elements that make up the note
     */
    public USFMNoteSpan(String style, String caller, List<USFMChar> chars) {
        super();
        CharSequence spanTitle = "";
        CharSequence note = "";
        CharSequence quotation = "";
        CharSequence altQuotation = "";
        CharSequence passageText = "";
        for(USFMChar c:chars) {
            if(c.style.equals(USFMChar.STYLE_PASSAGE_TEXT)) {
                passageText = c.value;
            } else if(c.style.equals(USFMChar.STYLE_FOOTNOTE_QUOTATION)) {
                quotation = c.value;
            } else if(c.style.equals(USFMChar.STYLE_FOOTNOTE_ALT_QUOTATION)) {
                altQuotation = c.value;
            } else {
                // TODO: implement better. We may need to format the values
                note = TextUtils.concat(note, c.value);
            }
        }

        // set the span title
        if(!TextUtils.isEmpty(passageText)) {
            spanTitle = passageText;
        } else if(!TextUtils.isEmpty(quotation)) {
            spanTitle = quotation;
        }

        init(spanTitle, generateTag(style, caller, spanTitle, chars));

        mCaller = caller;
        mPassage = spanTitle;
        mNotes = TextUtils.concat(note, " ", altQuotation);
        mStyle = style;
    }

    @Override
    public SpannableStringBuilder render() {
        if(mSpannable == null) {
            mSpannable = super.render();
            // apply custom styles
            if(getHumanReadable().toString().isEmpty()) {
                Bitmap image = BitmapFactory.decodeResource(AppContext.context().getResources(), R.drawable.ic_description_black_24dp);
                BitmapDrawable background = new BitmapDrawable(AppContext.context().getResources(), image);
                background.setBounds(0, 0, background.getMinimumWidth(), background.getMinimumHeight());
                mSpannable.setSpan(new ImageSpan(background), 0, mSpannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                mSpannable.setSpan(new BackgroundColorSpan(AppContext.context().getResources().getColor(R.color.footnote_yellow)), 0, mSpannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                mSpannable.setSpan(new StyleSpan(Typeface.ITALIC), 0, mSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                mSpannable.setSpan(new ForegroundColorSpan(AppContext.context().getResources().getColor(R.color.dark_gray)), 0, mSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return mSpannable;
    }

    /**
     * Generates the passage note tag with additional attributes
     * @param style
     * @param caller
     * @param title
     * @param chars
     * @return
     */
    public static CharSequence generateTag(String style, String caller, CharSequence title, List<USFMChar> chars) {

        String tag = "\\f " + caller + " ";
        for(USFMChar c: chars) {
            switch(c.style) {
                case USFMChar.STYLE_FOOTNOTE_VERSE:
                    tag += "\\fv " + c.value + "\\fv*";
                    break;
                default:
                    tag += "\\" + c.style + " " + c.value + " ";
                    break;
            }
        }
        tag += "\\f*";
        return tag;
    }

    /**
     * Generates a custom Doku Wiki footnote tag.
     * TODO: I think this will just be used for footnotes, however if footnotes are to be treated normally we won't have the span text.
     * @return
     */
    public String generateDokuWikiTag() {
        return "((ref:\""+ mPassage +"\",note:\""+ mNotes +"\"))";
    }

    public String getCaller() {
        return mCaller;
    }

    /**
     * Returns the type of note this is
     * @return
     */
    public String getStyle() {
        return mStyle;
    }

    /**
     * Returns the notes regarding the passage
     * @return
     */
    public CharSequence getNotes() {
        return mNotes;
    }

    /**
     * Returns the text upon which the notes are made
     * @return
     */
    public CharSequence getPassage() {
        return mPassage;
    }

    /**
     * Generates a footnote span
     * @param note the note
     * @return
     */
    public static USFMNoteSpan generateFootnote(CharSequence note) {
        List<USFMChar> chars = new ArrayList<>();
        chars.add(new USFMChar(USFMChar.STYLE_FOOTNOTE_TEXT, note));
        return new USFMNoteSpan("f", DEFAULT_CALLER, chars);
    }

    /**
     * Generates a new note span from the enclosed text and returns it.
     * Don't forget to set the click listener!
     * we are using usfm for footnotes and our own variant for user notes
     * http://ubs-icap.org/chm/usfm/2.4/index.html
     * @param caller
     * @param noteText
     * @return
     */
    public static USFMNoteSpan parseNote(CharSequence caller, CharSequence noteText) {
        List<USFMChar> chars = new ArrayList<>();

        Pattern pattern = Pattern.compile(CHAR_PATTERN);
        Matcher matcher = pattern.matcher(noteText);
        int lastIndex = 0;
        CharSequence note = "";

        while(matcher.find()) {

            int start = matcher.start();
            if (start > lastIndex) {
                note = TextUtils.concat(note, noteText.subSequence(lastIndex, start));
            }

            chars.add (new USFMChar("f" + matcher.group(1),matcher.group(2)));
            lastIndex = matcher.end();
        }

        if(lastIndex < noteText.length()) { // if extra text, add it
            note = TextUtils.concat(note, noteText.subSequence(lastIndex, noteText.length()));
            chars.add (new USFMChar(USFMChar.STYLE_PASSAGE_TEXT,note));
        }
        return new USFMNoteSpan("f", caller.toString(), chars);
    }
}
