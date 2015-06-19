package com.door43.translationstudio.rendering;

import android.text.TextUtils;

import com.door43.translationstudio.projects.Frame;
import com.door43.translationstudio.projects.Term;
import com.door43.translationstudio.spannables.Span;
import com.door43.translationstudio.spannables.TermSpan;

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
    private final Boolean mHighlightTerms;
    private Vector<Boolean> mIndicies;

    /**
     * Creates a new key term renderer
     * @param frame the frame with key terms that will be rendered
     */
    public KeyTermRenderer(Frame frame, Span.OnClickListener clickListener, Boolean highlightTerms) {
        mClickListener = clickListener;
        mTerms = frame.getChapter().getProject().getTerms();
        mFrame = frame;
        mHighlightTerms = highlightTerms;
    }

    public CharSequence renderTerms(CharSequence in) {
        CharSequence out = "";
        Pattern pattern = Pattern.compile(TermSpan.PATTERN);
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
            if(isStopped()) return in;

            if(mHighlightTerms) {
                // add term
                TermSpan span = new TermSpan(matcher.group(1), matcher.group(1));
                span.setOnClickListener(mClickListener);
                out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), span.toCharSequence());
            } else {
                out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), matcher.group(1));
            }
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }

    @Override
    public CharSequence render(CharSequence in) {
        return renderTerms(preprocess(in));
    }

    /**
     *
     * @param in
     * @return
     */
    public CharSequence preprocess(CharSequence in) {
        // locate key terms
        CharSequence out = in;
        mIndicies = new Vector<>();
        mIndicies.setSize(out.length());
        for(Term t:mTerms) {
            if(isStopped()) return in;
            if(!t.getName().trim().isEmpty()) {
                out = locateKey(t.getName(), t, in, out);
            }
            for(String alias:t.getAliases()) {
                if(!alias.trim().isEmpty()) {
                    out = locateKey(alias, t, in, out);
                }
            }
        }
        return out;
    }

    private CharSequence locateKey(String termId, Term t, CharSequence in, CharSequence out) {
        Pattern p = Pattern.compile("\\b" + termId + "\\b"); // should we use case insensitive?
        // TRICKY: we need to run two matches at the same time in order to keep track of used indicies in the string
        Matcher matcherSourceText = p.matcher(in);
        Matcher matcherKeyedText = p.matcher(out);
        CharSequence buff = "";
        int lastIndex = 0;
        while (matcherSourceText.find() && matcherKeyedText.find()) {
            if(isStopped()) return in;
            // ensure the key term was found in an area of the string that does not overlap another key term.
            if(mIndicies.get(matcherSourceText.start()) == null && mIndicies.get(matcherSourceText.end()) == null) {
                // build the term
                String key = "<keyterm>" + matcherSourceText.group() + "</keyterm>";
                mFrame.addImportantTerm(t.getName());
                // lock indicies to prevent key term collisions
                for(int i = matcherSourceText.start(); i <= matcherSourceText.end(); i ++) {
                    if(isStopped()) return in;
                    mIndicies.set(i, true);
                }

                // insert the key into the keyedText
                buff = TextUtils.concat(buff, out.subSequence(lastIndex, matcherKeyedText.start()), key);
                lastIndex = matcherKeyedText.end();
            } else {
                // do nothing. this is a key collision
                // e.g. the key term "life" collided with "eternal life".
            }
        }
        buff = TextUtils.concat(buff, out.subSequence(lastIndex, out.length()));
        return buff;
    }
}
