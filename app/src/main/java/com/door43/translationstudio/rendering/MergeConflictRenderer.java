package com.door43.translationstudio.rendering;

import android.text.TextUtils;
import com.door43.translationstudio.spannables.MergeConflictSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by joel on 2/16/2016.
 */
public class MergeConflictRenderer extends RenderingEngine {

    @Override
    public CharSequence render(CharSequence in) {
        CharSequence out = in;

        out = renderConflict(in);

        return out;
    }

    public CharSequence renderConflict(CharSequence in) {
        CharSequence out = "";

        Pattern pattern = Pattern.compile(MergeConflictSpan.PATTERN, Pattern.DOTALL | Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(in);
        int lastIndex = 0;
        while(matcher.find()) {
            if(isStopped()) return in;
            MergeConflictSpan span = new MergeConflictSpan(matcher.group(0), matcher.group(1), matcher.group(2));
            // TODO: 2/16/2016 set up click listener so the user can perform the merge
            span.setOnClickListener(null);
            if(span != null) {
                out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.start()), span.render());
            } else {
                out = TextUtils.concat(out, in.subSequence(lastIndex, matcher.end()));
            }
            lastIndex = matcher.end();
        }
        out = TextUtils.concat(out, in.subSequence(lastIndex, in.length()));
        return out;
    }
}
