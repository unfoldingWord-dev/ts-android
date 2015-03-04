package com.door43.translationstudio.rendering;

import android.text.TextUtils;

import com.door43.translationstudio.projects.Frame;
import com.door43.translationstudio.projects.Term;
import com.door43.translationstudio.spannables.NoteSpan;
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

    /**
     * Creates a new key term renderer
     * @param frame the frame with key terms that will be rendered
     */
    public KeyTermRenderer(Frame frame, Span.OnClickListener clickListener) {
        mClickListener = clickListener;
        mTerms = frame.getChapter().getProject().getTerms();
        mFrame = frame;
    }

    public CharSequence renderTerm(String termId, Term term, CharSequence in) {
        CharSequence out = "";
        Pattern pattern = Pattern.compile("\\b" + termId + "\\b", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while (matcher.find()) {
            if (isStopped()) return in;
            mFrame.addImportantTerm(term.getName());
            TermSpan span = new TermSpan(matcher.group(), matcher.group());
            span.setOnClickListener(mClickListener);

            out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), span.toCharSequence());
            lastIndex = matcher.end();
        }
        return TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
    }

    @Override
    public CharSequence render(CharSequence in) {
        CharSequence out = in;
        for(Term t:mTerms) {
            if(isStopped()) return in;
            out = renderTerm(t.getName(), t, out);
            for(String alias:t.getAliases()) {
                out = renderTerm(alias, t, out);
            }
        }
        return out;
    }
}
