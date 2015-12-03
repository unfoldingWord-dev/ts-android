package com.door43.translationstudio.rendering;

import android.text.Editable;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.style.AlignmentSpan;
import android.text.style.BulletSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.TypefaceSpan;
import android.util.Log;

import com.door43.translationstudio.spannables.LinkSpan;
import com.door43.translationstudio.spannables.Span;

import org.eclipse.jgit.diff.Edit;
import org.xml.sax.XMLReader;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Vector;

/**
 * Some parts of this code are based on android.text.Html
 */
public class HtmlTagHandler implements Html.TagHandler {
    public static final String TAG = "HtmlTagHandler";
    private final Span.OnClickListener clickListener;
    private int mListItemCount = 0;
    private static final boolean DEBUG = true;
    private Vector<String> mListParents = new Vector<>();
    final HashMap<String, String> attributes = new HashMap<>();

    public HtmlTagHandler(Span.OnClickListener clickListener) {
        this.clickListener = clickListener;
    }

    private static class Code {
    }

    private static class Center {
    }

    private static class AppLink {
    }

    /**
     * http://stackoverflow.com/questions/6952243/how-to-get-an-attribute-from-an-xmlreader
     * @param xmlReader
     */
    private void processAttributes(final XMLReader xmlReader) {
        try {
            Field elementField = xmlReader.getClass().getDeclaredField("theNewElement");
            elementField.setAccessible(true);
            Object element = elementField.get(xmlReader);
            Field attsField = element.getClass().getDeclaredField("theAtts");
            attsField.setAccessible(true);
            Object atts = attsField.get(element);
            Field dataField = atts.getClass().getDeclaredField("data");
            dataField.setAccessible(true);
            String[] data = (String[])dataField.get(atts);
            Field lengthField = atts.getClass().getDeclaredField("length");
            lengthField.setAccessible(true);
            int len = (Integer)lengthField.get(atts);

            /**
             * MSH: Look for supported attributes and add to hash map.
             * This is as tight as things can get :)
             * The data index is "just" where the keys and values are stored.
             */
            for(int i = 0; i < len; i++)
                attributes.put(data[i * 5 + 1], data[i * 5 + 4]);
        }
        catch (Exception e) {
            Log.d(TAG, "Exception: " + e);
        }
    }

    @Override
    public void handleTag(final boolean opening, final String tag, Editable output, final XMLReader xmlReader) {
        processAttributes(xmlReader);
        if (opening) {
            // opening tag
            if (DEBUG) {
                Log.d(TAG, "opening, output: " + output.toString());
            }

            if (tag.equalsIgnoreCase("ul") || tag.equalsIgnoreCase("ol") || tag.equalsIgnoreCase("dd")) {
                mListParents.add(tag);
                mListItemCount = 0;
            } else if (tag.equalsIgnoreCase("code")) {
                start(output, new Code());
            } else if (tag.equalsIgnoreCase("center")) {
                start(output, new Center());
            } else if (tag.equalsIgnoreCase("app-link")) {
                start(output, new AppLink());
            }
        } else {
            // closing tag
            if (DEBUG) {
                Log.d(TAG, "closing, output: " + output.toString());
            }

            if (tag.equalsIgnoreCase("ul") || tag.equalsIgnoreCase("ol") || tag.equalsIgnoreCase("dd")) {
                mListParents.remove(tag);
                mListItemCount = 0;
            } else if (tag.equalsIgnoreCase("li")) {
                handleListTag(output);
            } else if (tag.equalsIgnoreCase("code")) {
                end(output, Code.class, new TypefaceSpan("monospace"), false);
            } else if (tag.equalsIgnoreCase("center")) {
                end(output, Center.class, new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), true);
            } else if (tag.equalsIgnoreCase("app-link")) {
                handleAppLinkTag(output);
            }
        }
    }

    /**
     * Mark the opening tag by using private classes
     *
     * @param output
     * @param mark
     */
    private void start(Editable output, Object mark) {
        int len = output.length();
        output.setSpan(mark, len, len, Spannable.SPAN_MARK_MARK);

        if (DEBUG) {
            Log.d(TAG, "len: " + len);
        }
    }

    private void end(Editable output, Class kind, Object repl, boolean paragraphStyle) {
        Object obj = getLast(output, kind);
        // start of the tag
        int where = output.getSpanStart(obj);
        // end of the tag
        int len = output.length();

        output.removeSpan(obj);

        if (where != len) {
            // paragraph styles like AlignmentSpan need to end with a new line!
            if (paragraphStyle) {
                output.append("\n");
                len++;
            }
            output.setSpan(repl, where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if (DEBUG) {
            Log.d(TAG, "where: " + where);
            Log.d(TAG, "len: " + len);
        }
    }

    /**
     * Get last marked position of a specific tag kind (private class)
     *
     * @param text
     * @param kind
     * @return
     */
    private Object getLast(Editable text, Class kind) {
        Object[] objs = text.getSpans(0, text.length(), kind);
        if (objs.length == 0) {
            return null;
        } else {
            for (int i = objs.length; i > 0; i--) {
                if (text.getSpanFlags(objs[i - 1]) == Spannable.SPAN_MARK_MARK) {
                    return objs[i - 1];
                }
            }
            return null;
        }
    }

    private void handleAppLinkTag(Editable output) {
        Object obj = getLast(output, AppLink.class);
        // start of the tag
        int where = output.getSpanStart(obj);
        // end of the tag
        int len = output.length();

        output.removeSpan(obj);

        CharSequence title = output.subSequence(where, len);
        LinkSpan span = new LinkSpan(title.toString(), attributes.get("href"), attributes.get("type"));
        span.setOnClickListener(this.clickListener);

        if(where != len) {
            output.replace(where, len, span.toCharSequence());
        }

        if (DEBUG) {
            Log.d(TAG, "where: " + where);
            Log.d(TAG, "len: " + len);
        }
    }

    private void handleListTag(Editable output) {
        if (mListParents.lastElement().equals("ul")) {
            output.append("\n");
            String[] split = output.toString().split("\n");

            int lastIndex = split.length - 1;
            int start = output.length() - split[lastIndex].length() - 1;
            output.setSpan(new BulletSpan(15 * mListParents.size()), start, output.length(), 0);
        } else if (mListParents.lastElement().equals("ol")) {
            mListItemCount++;

            output.append("\n");
            String[] split = output.toString().split("\n");

            int lastIndex = split.length - 1;
            int start = output.length() - split[lastIndex].length() - 1;
            output.insert(start, mListItemCount + ". ");
            output.setSpan(new LeadingMarginSpan.Standard(15 * mListParents.size()), start, output.length(), 0);
        }
    }
}