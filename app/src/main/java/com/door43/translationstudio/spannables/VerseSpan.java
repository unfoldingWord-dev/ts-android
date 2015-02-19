package com.door43.translationstudio.spannables;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;

import com.door43.translationstudio.R;
import com.door43.translationstudio.util.AppContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by joel on 1/27/2015.
 * TODO: we need to provide support for rendering with a range of verses as well as provide accessor methods to the rangeed verse numbers
 */
public class VerseSpan extends Span {
    public static final String PATTERN = "<verse\\s+number=\"(\\d+(-\\d+)?)\"\\s+style=\"v\"\\s*/>";
    private int mStartVerseNumber = 0;
    private int mEndVerseNumber = 0;
//    private int mVerseNumber = -1;
    private SpannableStringBuilder mSpannable;

    /**
     * Creates a new verse span of either a single verse or range of verses
     * @param verse
     */
    public VerseSpan(String verse) {
        super(verse, "<verse number=\""+verse+"\" style=\"v\" />");
        String[] verses = verse.split("-");
        if(verses.length == 2) {
            // range of verses
            mStartVerseNumber = Integer.parseInt(verses[0]);
            mEndVerseNumber = Integer.parseInt(verses[1]);
        } else {
            // single verse
            mStartVerseNumber = Integer.parseInt(verse);
        }
    }

    /**
     * Creates a new verse span
     * @param verse
     */
    public VerseSpan(int verse) {
        super(verse+"", "<verse number=\""+verse+"\" style=\"v\" />");
        mStartVerseNumber = verse;
    }

    /**
     * Creates a verse span over a range of verses
     * @param startVerse
     * @param endVerse
     */
    public VerseSpan(int startVerse, int endVerse) {
        super(startVerse+"-"+endVerse, "<verse number=\""+startVerse+"-"+endVerse+"\" style=\"v\" />");
        mStartVerseNumber = startVerse;
        mEndVerseNumber = endVerse;
    }

    /**
     * Returns the start verse number
     * @return
     */
    public int getStartVerseNumber() {
        return mStartVerseNumber;
    }

    /**
     * Returns the end verse number
     * @return
     */
    public int getEndVerseNumber() {
        return mEndVerseNumber;
    }

    /**
     * Generates the spannable.
     * This provides caching so we can look up the span in the text later
     * @return
     */
    @Override
    public SpannableStringBuilder render() {
        if(mSpannable == null) {
            mSpannable = super.render();
            // apply custom styles
            mSpannable.setSpan(new RelativeSizeSpan(0.8f), 0, mSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            mSpannable.setSpan(new ForegroundColorSpan(AppContext.context().getResources().getColor(R.color.gray)), 0, mSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return mSpannable;
    }

    /**
     * Parses a usx string into a verse span
     * @param usx
     * @return
     */
    public static VerseSpan parseVerse(String usx) {
        Pattern pattern = Pattern.compile(PATTERN);
        Matcher matcher = pattern.matcher(usx);
        while(matcher.find()) {
            return new VerseSpan(matcher.group(1));
        }
        return null;
    }
}
