package com.door43.translationstudio.spannables;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by joel on 1/27/2015.
 * TODO: we need to provide support for rendering with a range of verses as well as provide accessor methods to the ranged verse numbers
 */
public class USFMVerseSpan extends VerseSpan {
    public static final String PATTERN = "\\\\v\\s(\\d+(-\\d+)?)\\s?";
    private int mStartVerseNumber = 0;
    private int mEndVerseNumber = 0;
    //    private int mVerseNumber = -1;
    private SpannableStringBuilder mSpannable;

    /**
     * Creates a new verse span of either a single verse or range of verses
     * @param verse
     */
    public USFMVerseSpan(String verse) {
        super(verse, "\\v "+verse+" ");
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
    public USFMVerseSpan(int verse) {
        super(verse+"", "\\v "+verse+" ");
        mStartVerseNumber = verse;
    }

    /**
     * Creates a verse span over a range of verses
     * @param startVerse
     * @param endVerse
     */
    public USFMVerseSpan(int startVerse, int endVerse) {
        super(startVerse+"-"+endVerse, "\\v "+startVerse+"-"+endVerse+" ");
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
            mSpannable.setSpan(new ForegroundColorSpan(App.context().getResources().getColor(R.color.gray)), 0, mSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return mSpannable;
    }

    /**
     * Parses a usfm string into a verse span
     * @param usfm
     * @return
     */
    public static USFMVerseSpan parseVerse(String usfm) {
        Pattern pattern = Pattern.compile(PATTERN);
        Matcher matcher = pattern.matcher(usfm);
        while(matcher.find()) {
            return new USFMVerseSpan(matcher.group(1));
        }
        return null;
    }

    /**
     * Returns the range of verses that a chunk of text spans
     *
     * @param text
     * @return int[0] if no verses, int[1] if one verse, int[2] if a range of verses
     */
    public static int[] getVerseRange(CharSequence text) {
        // locate verse range
        Pattern pattern = Pattern.compile(PATTERN);
        Matcher matcher = pattern.matcher(text);
        int numVerses = 0;
        int startVerse = 0;
        int endVerse = 0;
        USFMVerseSpan verse = null;
        while(matcher.find()) {
            verse = new USFMVerseSpan(matcher.group(1));

            if(numVerses == 0) {
                // first verse
                startVerse = verse.getStartVerseNumber();
                endVerse = verse.getEndVerseNumber();
            }
            numVerses ++;
        }
        if(verse != null) {
            if(verse.getEndVerseNumber() > 0) {
                endVerse = verse.getEndVerseNumber();
            } else {
                endVerse = verse.getStartVerseNumber();
            }
        }
        if(startVerse <= 0 || endVerse <= 0) {
            // no verse range
            return new int[0];
        } else if(startVerse == endVerse) {
            // single verse
            return new int[]{startVerse};
        } else {
            // verse range
            return new int[]{startVerse, endVerse};
        }
    }
}
