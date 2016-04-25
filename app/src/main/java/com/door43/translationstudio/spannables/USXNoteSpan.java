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
import android.util.Xml;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.AppContext;

import org.apache.commons.io.input.CharSequenceReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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
 * Class to create NoteSpans from USX format text
 */
public class USXNoteSpan extends NoteSpan {
    private final CharSequence mNotes;
    private final CharSequence mPassage;
    private final String mCaller;
    private List<USXChar> mChars;
    private static final String DEFAULT_CALLER = "+";
    private String mStyle;
    private SpannableStringBuilder mSpannable;
    public static final String PATTERN = "<note ((?!>).)*>((?!</note>).)*</note>";

    /**
     * @param style the note style
     * @param caller the note caller
     * @param chars a list of char elements that make up the note
     */
    public USXNoteSpan(String style, String caller, List<USXChar> chars) {
        super();
        CharSequence spanTitle = "";
        CharSequence note = "";
        CharSequence quotation = "";
        CharSequence altQuotation = "";
        CharSequence passageText = "";
        for(USXChar c:chars) {
            if(c.style.equals(USXChar.STYLE_PASSAGE_TEXT)) {
                passageText = c.value;
            } else if(c.style.equals(USXChar.STYLE_FOOTNOTE_QUOTATION)) {
                quotation = c.value;
            } else if(c.style.equals(USXChar.STYLE_FOOTNOTE_ALT_QUOTATION)) {
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

        mChars = chars;
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
    public static CharSequence generateTag(String style, String caller, CharSequence title, List<USXChar> chars) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            return title;
        }
        Document document = db.newDocument();

        // build root
        Element rootElement = document.createElement("note");
        rootElement.setAttribute("style", style);
        rootElement.setAttribute("caller", caller);
        document.appendChild(rootElement);

        // add chars
        for(USXChar c:chars) {
            Element element = document.createElement("char");
            element.setAttribute("style", c.style);
            element.setTextContent(c.value.toString().replace("\n", "\\n"));
            rootElement.appendChild(element);
        }

        // generate
        DOMSource domSource = new DOMSource(document.getDocumentElement());
        OutputStream output = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(output);

        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = factory.newTransformer();
            Properties outFormat = new Properties();
            outFormat.setProperty(OutputKeys.INDENT, "no");
            outFormat.setProperty(OutputKeys.METHOD, "xml");
            outFormat.setProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            outFormat.setProperty(OutputKeys.VERSION, "1.0");
            outFormat.setProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperties(outFormat);
            transformer.transform(domSource, result);
        } catch (TransformerConfigurationException e) {
            return title;
        } catch (TransformerException e) {
            Logger.e(USXNoteSpan.class.getName(), "failed to transform the the span text", e);
        }

        String tag = output.toString();
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

    /**
     * returns the caller
     * @return
     */
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
    public static USXNoteSpan generateFootnote(CharSequence note) {
        List<USXChar> chars = new ArrayList<>();
        chars.add(new USXChar(USXChar.STYLE_FOOTNOTE_TEXT, note));
        return new USXNoteSpan("f", DEFAULT_CALLER, chars);
    }

    /**
     * Generates a new note span from the supplied xml and returns it.
     * Don't forget to set the click listener!
     * we are using usx for footnotes and our own variant for user notes
     * http://dbl.ubs-icap.org:8090/display/DBLDOCS/USX#USX-note(Footnote)
     * @param usx
     * @return
     */
    public static USXNoteSpan parseNote(CharSequence usx) {
        XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(new CharSequenceReader(usx));
            parser.nextTag();
            return readXML(parser);
        } catch (XmlPullParserException e) {
            Logger.e(USXNoteSpan.class.getName(), "Failed to parse note", e);
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
    private static USXNoteSpan readXML(XmlPullParser parser) throws XmlPullParserException, IOException {
//        NoteType noteType;
//        String notes = "";
//        String passageText = "";

        parser.require(XmlPullParser.START_TAG, null, "note");

        // load attributes
        String style = parser.getAttributeValue("","style");

        String caller = parser.getAttributeValue("", "caller");
        if(caller == null) {
            caller = DEFAULT_CALLER;
        }

        int eventType = parser.getEventType();
        parser.nextTag();

        // load char's
//        CharSequence note = "";
        List<USXChar> chars = new ArrayList<>();
        while(eventType != XmlPullParser.END_DOCUMENT) {
            if(eventType == XmlPullParser.START_TAG){
                parser.require(XmlPullParser.START_TAG, null, "char");
                String charStyle = parser.getAttributeValue("", "style");
                String charText = parser.nextText();

                chars.add(new USXChar(charStyle, charText));
            }
            eventType = parser.next();
        }

        return new USXNoteSpan(style, caller.trim(), chars);
    }

    public List<USXChar> getChars() {
        return mChars;
    }
}
