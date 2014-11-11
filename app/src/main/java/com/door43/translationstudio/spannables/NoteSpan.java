package com.door43.translationstudio.spannables;

import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.util.Xml;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.util.MainContext;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
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
public class NoteSpan extends FancySpan {
    private final String mNoteText;
    private int mFootnoteId;
    private NoteType mNoteType;
    private static int mNumNotes = 0;
    private static int mNumFootnotes = 0;
    private static final String ROOT_TAG = "note";
    private static final String CHILDREN_TAG = "char";
    private static final String FOOTNOTE_STYLE = "f";
    private static final String USERNOTE_STYLE = "u";
    private static final String NOTE_TEXT_STYLE = "ft";
    private static final String NOTE_PASSAGE_STYLE = "pt";

    // use these two expressions to identify starting and ending tags.
    // This is most useful when avoiding note collisions.
    public static final String REGEX_OPEN_TAG = "<"+ ROOT_TAG +" ((?!>).)*>";
    public static final String REGEX_CLOSE_TAG = "</"+ ROOT_TAG +">";

    // use this if you just want to find any old note
    public static final String REGEX_NOTE = REGEX_OPEN_TAG + "((?!" + REGEX_CLOSE_TAG + ").)*" + REGEX_CLOSE_TAG;

    /**
     * A custom enum to identify the note type.
     * This also stores the style value use to identify the note in xml.
     */
    public static enum NoteType {
        UserNote(USERNOTE_STYLE),
        Footnote(FOOTNOTE_STYLE);

        private final String mText;
        private NoteType(final String text) {
            mText = text;
        }

        @Override
        public String toString() {
            return mText;
        }
    }

    public NoteSpan(String spanText, String noteText, NoteType noteType) {
        super(mNumNotes + "", spanText, null);
        mNumNotes++;
        if(noteType == NoteType.Footnote) {
            mNumFootnotes ++;
            mFootnoteId = mNumFootnotes;
        }
        mNoteText = noteText;
        mNoteType = noteType;
    }

    public NoteSpan(String spanText, String noteText, NoteType noteType, OnClickListener clickListener) {
        super(mNumNotes + "", spanText, clickListener);
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
        attrs.putString("id", getSpanId());
        TextView textView;
        BitmapDrawable bm;
        CharSequence span;
        switch(mNoteType) {
            case Footnote:
                // load custom footnote layout
                textView = (TextView) MainContext.getContext().getCurrentActivity().getLayoutInflater().inflate(R.layout.span_footnote, null);
                textView.setText(Html.fromHtml(toString() + "<sup>" + mFootnoteId + "</sup>"));
                textView.setTextSize(MainContext.getContext().getResources().getDimension(R.dimen.h5));
                if(mTypeface != null) {
                    textView.setTypeface(mTypeface);
                }
                bm = convertViewToDrawable(textView);
                span = generateSpan(generateTag(toString(), mNoteText, mNoteType, attrs), bm);
                break;
            case UserNote:
            default:
                // load custom user note layout
                textView = (TextView) MainContext.getContext().getCurrentActivity().getLayoutInflater().inflate(R.layout.span_usernote, null);
                textView.setText(toString());
                textView.setTextSize(MainContext.getContext().getResources().getDimension(R.dimen.h5));
                if(mTypeface != null) {
                    textView.setTypeface(mTypeface);
                }
                bm = convertViewToDrawable(textView);
                span = generateSpan(generateTag(toString(), mNoteText, mNoteType, attrs), bm);
        }
        return span;
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
     * Generates the regular expression used to select a note by id
     * @param id
     * @return
     */
    public static String regexNoteById(String id) {
        return "<" + ROOT_TAG + " ((?!>).)*id=\"" + id + "\"((?!>).)*>((?!" + REGEX_CLOSE_TAG + ").)*" + REGEX_CLOSE_TAG;
    }

    /**
     * Generates the passage note tag with additional attributes
     * @param spanText
     * @param noteText
     * @param attrs
     * @return
     */
    public static String generateTag(String spanText, String noteText, NoteType noteType, Bundle attrs) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            return spanText;
        }
        Document document = db.newDocument();

        // build root
        Element rootElement = document.createElement(ROOT_TAG);
        for(String key: attrs.keySet()) {
            rootElement.setAttribute(key, attrs.getString(key));
        }
        rootElement.setAttribute("style", noteType.toString());
        document.appendChild(rootElement);

        // add note
        Element noteElement = document.createElement(CHILDREN_TAG);
        noteElement.setAttribute("style", NOTE_TEXT_STYLE);
        // TRICKY: spannables render incorrectly when there are newlines within the content.
        noteElement.setTextContent(noteText.replace("\n", "\\n"));
        rootElement.appendChild(noteElement);

        // add user note data
        if(noteType == NoteType.UserNote) {
            Element userNoteElement = document.createElement(CHILDREN_TAG);
            userNoteElement.setAttribute("style", NOTE_PASSAGE_STYLE);
            userNoteElement.setTextContent(spanText);
            rootElement.appendChild(userNoteElement);
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
            return spanText;
        } catch (TransformerException e) {
            e.printStackTrace();
        }

        String tag = output.toString();
        if(noteType == NoteType.Footnote) {
            tag = spanText + tag;
        }
        return tag;
    }

    /**
     * Generates a custom Doku Wiki footnote tag.
     * TODO: I think this will just be used for footnotes, however if footnotes are to be treated normally we won't have the span text.
     * @return
     */
    public String generateDokuWikiTag() {
        return "((ref:\""+getSpanText()+"\",note:\""+mNoteText+"\"))";
    }

    /**
     * Returns the type of note this is
     * @return
     */
    public NoteType getNoteType() {
        return mNoteType;
    }

    /**
     * Returns the actual note text
     * @return
     */
    public String getNoteText() {
        return mNoteText;
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
     * Generates a new note span from the supplied xml and returns it.
     * Don't forget to set the click listener!
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
        NoteType noteType;
        String noteText = "";
        String notePassage = ""; // just for user notes

        parser.require(XmlPullParser.START_TAG, null, ROOT_TAG);

        // load attributes
        String style = parser.getAttributeValue("","style");
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

        int eventType = parser.getEventType();
        parser.nextTag();

        // load char's
        while(eventType != XmlPullParser.END_DOCUMENT) {
            if(eventType == XmlPullParser.START_TAG){
                parser.require(XmlPullParser.START_TAG, null, CHILDREN_TAG);
                String charStyle = parser.getAttributeValue("","style");
                String charText = parser.nextText();

                if(charStyle.equals(NOTE_TEXT_STYLE)) {
                    // TRICKY: add back the newlines that were removed to preserve spannable rendering
                    noteText = charText.replace("\\n", "\n");
                } else if(charStyle.equals(NOTE_PASSAGE_STYLE)) {
                    notePassage = charText;
                }
            }
            eventType = parser.next();
        }

        return new NoteSpan(notePassage, noteText, noteType);
    }
}
