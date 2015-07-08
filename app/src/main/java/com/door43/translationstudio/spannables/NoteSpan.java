package com.door43.translationstudio.spannables;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Xml;

import com.door43.translationstudio.R;
import com.door43.util.reporting.Logger;
import com.door43.translationstudio.util.AppContext;

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
 * Created by joel on 10/28/2014.
 */
public class NoteSpan extends Span {
    private final CharSequence mNotes;
    private final CharSequence mPassage;
    private final String mCaller;
    private static final String DEFAULT_CALLER = "+";
    private String mStyle;
    private SpannableStringBuilder mSpannable;
    public static final String PATTERN = "<note ((?!>).)*>((?!</note>).)*</note>";

    // custom usx styles
    public static final String STYLE_USERNOTE = "u";

    /**
     * @param style the note style
     * @param caller the note caller
     * @param chars a list of char elements that make up the note
     */
    public NoteSpan(String style, String caller, List<Char> chars) {
        super();
        CharSequence spanTitle = "";
        CharSequence note = "";
        CharSequence quotation = "";
        CharSequence altQuotation = "";
        CharSequence passageText = "";
        for(Char c:chars) {
            if(c.style.equals(Char.STYLE_PASSAGE_TEXT)) {
                passageText = c.value;
            } else if(c.style.equals(Char.STYLE_FOOTNOTE_QUOTATION)) {
                quotation = c.value;
            } else if(c.style.equals(Char.STYLE_FOOTNOTE_ALT_QUOTATION)) {
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
        } else if(!TextUtils.isEmpty(altQuotation)) {
            spanTitle = altQuotation;
        } else {
            spanTitle = "["+note+"]";
        }

        init(spanTitle, generateTag(style, caller, spanTitle, chars));

        mCaller = caller;
        mPassage = spanTitle;
        mNotes = note;
        mStyle = style;
    }

    @Override
    public SpannableStringBuilder render() {
        if(mSpannable == null) {
            mSpannable = super.render();
            // apply custom styles
            mSpannable.setSpan(new BackgroundColorSpan(AppContext.context().getResources().getColor(R.color.footnote_yellow)), 0, mSpannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mSpannable.setSpan(new StyleSpan(Typeface.ITALIC), 0, mSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            mSpannable.setSpan(new ForegroundColorSpan(AppContext.context().getResources().getColor(R.color.dark_gray)), 0, mSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
    public static CharSequence generateTag(String style, String caller, CharSequence title, List<Char> chars) {
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
//        Element noteElement = document.createElement(TAG_CHAR);
//        noteElement.setAttribute("style", NOTE_TEXT_STYLE);

        for(Char c:chars) {
            Element element = document.createElement("char");
            element.setAttribute("style", c.style);
            element.setTextContent(c.value.toString().replace("\n", "\\n"));
            rootElement.appendChild(element);
        }
        // TRICKY: spannables render incorrectly when there are newlines within the content.
//        noteElement.setTextContent(notes.replace("\n", "\\n"));
//        rootElement.appendChild(noteElement);

        // add user note data
//        if(type == NoteType.UserNote) {
//            Element userNoteElement = document.createElement(TAG_CHAR);
//            userNoteElement.setAttribute("style", STYLE_CHAR_PASSAGE_TEXT);
//            userNoteElement.setTextContent(title);
//            rootElement.appendChild(userNoteElement);
//        }

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
            Logger.e(NoteSpan.class.getName(), "failed to transform the the span text", e);
        }

        String tag = output.toString();
        if(style.equals("f")) {
            tag = title + tag;
        }
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
     * Generates a new user note span
     * @param passageText the passage on which the node is made
     * @param note the note
     * @return
     */
    public static NoteSpan generateUserNote(CharSequence passageText, CharSequence note) {
        List<Char> chars = new ArrayList<>();
        chars.add(new Char(Char.STYLE_PASSAGE_TEXT, passageText));
        chars.add(new Char(Char.STYLE_FOOTNOTE_TEXT, note));
        return new NoteSpan(STYLE_USERNOTE, DEFAULT_CALLER, chars);
    }

    /**
     * Generates a new note span from the supplied xml and returns it.
     * Don't forget to set the click listener!
     * we are using usx for footnotes and our own variant for user notes
     * http://dbl.ubs-icap.org:8090/display/DBLDOCS/USX#USX-note(Footnote)
     * @param usx
     * @return
     */
    public static NoteSpan parseNote(CharSequence usx) {
        XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(new CharSequenceReader(usx));
            parser.nextTag();
            return readXML(parser);
        } catch (XmlPullParserException e) {
            Logger.e(NoteSpan.class.getName(), "Failed to parse note", e);
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
//        NoteType noteType;
//        String notes = "";
//        String passageText = "";

        parser.require(XmlPullParser.START_TAG, null, "note");

        // load attributes
        String style = parser.getAttributeValue("","style");
//        if(style != null) {
//            if(style.equals(STYLE_FOOTNOTE)) {
//                noteType = NoteType.Footnote;
//            } else {
//                // by default everything is a user note
//                noteType = NoteType.UserNote;
//            }
//        } else {
//            noteType = NoteType.UserNote;
//        }

        String caller = parser.getAttributeValue("", "caller");
        if(caller == null) {
            caller = DEFAULT_CALLER;
        }

        int eventType = parser.getEventType();
        parser.nextTag();

        // load char's
//        CharSequence note = "";
        List<Char> chars = new ArrayList<>();
        while(eventType != XmlPullParser.END_DOCUMENT) {
            if(eventType == XmlPullParser.START_TAG){
                parser.require(XmlPullParser.START_TAG, null, "char");
                String charStyle = parser.getAttributeValue("", "style");
                String charText = parser.nextText();

                chars.add(new Char(charStyle, charText));
//                if(charStyle.equals(NOTE_TEXT_STYLE)) {
//                    // TRICKY: add back the newlines that were removed to preserve spannable rendering
//                    TextUtils.concat(note, charText.replace("\\n", "\n"));
//                } else if(charStyle.equals(STYLE_CHAR_PASSAGE_TEXT)) {
//                    passageText = charText;
//                }
//
//                if(charStyle.equals(STYLE_FOOTNOTE)) {
//                    passageText = "footnote";
//                }
            }
            eventType = parser.next();
        }

        return new NoteSpan(style, caller.trim(), chars);
    }
}
