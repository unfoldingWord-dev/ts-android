package com.door43.translationstudio.rendering;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.projects.Frame;
import com.door43.translationstudio.projects.Term;
import com.door43.translationstudio.spannables.FancySpan;
import com.door43.translationstudio.spannables.Span;
import com.door43.translationstudio.spannables.TermSpan;
import com.door43.translationstudio.util.Logger;

import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class renders
 */
public class KeyTermRenderer extends RenderingEngine {
    private final List<Term> mTerms;
    private final Frame mFrame;
    private final Span.OnClickListener mClickListener;

    /**
     * Creates a new key term renderer
     * @param frame the frame with key terms that will be rendered
     */
    public KeyTermRenderer(Frame frame, Span.OnClickListener clickListener) {
        mClickListener = clickListener;
        mTerms = frame.getChapter().getProject().getTerms();
        mFrame = frame;
    }

    @Override
    public CharSequence render(CharSequence in) {
        // locate key terms
        CharSequence keyedText = in;
        Vector<Boolean> indicies = new Vector<Boolean>();
        indicies.setSize(keyedText.length());
        for(Term t:mTerms) {
            if(isStopped()) return in;
            StringBuffer buf = new StringBuffer();
            Pattern p = Pattern.compile("\\b" + t.getName() + "\\b");
            // TRICKY: we need to run two matches at the same time in order to keep track of used indicies in the string
            Matcher matcherSourceText = p.matcher(in);
            Matcher matcherKeyedText = p.matcher(keyedText);

            while (matcherSourceText.find() && matcherKeyedText.find()) {
                if(isStopped()) return in;
                // ensure the key term was found in an area of the string that does not overlap another key term.
                if(indicies.get(matcherSourceText.start()) == null && indicies.get(matcherSourceText.end()) == null) {
                    // build important terms list.
                    mFrame.addImportantTerm(matcherSourceText.group());
                    // build the term
                    String key = "<a>" + matcherSourceText.group() + "</a>";
                    // lock indicies to prevent key term collisions
                    for(int i = matcherSourceText.start(); i <= matcherSourceText.end(); i ++) {
                        if(isStopped()) return in;
                        indicies.set(i, true);
                    }

                    // insert the key into the keyedText
                    matcherKeyedText.appendReplacement(buf, key);
                } else {
                    // do nothing. this is a key collision
                    // e.g. the key term "life" collided with "eternal life".
                }
            }
            matcherKeyedText.appendTail(buf);
            keyedText = buf.toString();
        }

        // convert links into spans
        CharSequence out = "";
        if(keyedText != null) {
            // TODO: will converting the keyed text to string cause any problems with already generated spans?
            String[] pieces = keyedText.toString().split("<a>");
            out = pieces[0];
            for (int i = 1; i < pieces.length; i++) {
                if(isStopped()) return in;
                // get closing anchor
                String[] linkChunks = pieces[i].split("</a>");
                TermSpan term = new TermSpan(linkChunks[0], linkChunks[0]);
                term.setOnClickListener(mClickListener);
                out = TextUtils.concat(out, term.toCharSequence());
                try {
                    out = TextUtils.concat(out, linkChunks[1]);
                } catch (Exception e) {
                    Logger.e(this.getClass().getName(), "failed to concat string", e);
                }
            }
        }
        return out;
    }
}
