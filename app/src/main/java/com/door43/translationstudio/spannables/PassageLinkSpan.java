package com.door43.translationstudio.spannables;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.AppContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by joel on 2/18/2015.
 */
public class PassageLinkSpan extends Span {
    // e.g. [[:en:bible:notes:gen:01:03|1:5]]
    public static final Pattern PATTERN = Pattern.compile("\\[\\[:(((?!\\]\\]).)*)\\|(((?!\\]\\]).)*)\\]\\]");//\\[\\[:((?!\\]\\])(.*)\\|(.*))\\]\\]");
    private static String mTitle;
    private static String mAddress;
    private SpannableStringBuilder mSpannable;
    private String mLanguageId;
    private String mProjectId;
    private String mChapterId;
    private String mFrameId;

    /**
     * Creates a new passage link
     * @param title the title of the link e.g. 1:5
     * @param address the address to the link e.g. en:bible:notes:gen:01:03
     */
    public PassageLinkSpan(String title, String address) {
        super(title, address);
        mTitle = title;
        mAddress = address;
        explodeAddress(address);
    }

    /**
     * Changes the title of the passage link
     * @param title
     */
    public void setTitle(String title) {
        setHumanReadable(title);
    }

    @Override
    public SpannableStringBuilder render() {
        if(mSpannable == null) {
            mSpannable = super.render();
            // apply custom styles
            mSpannable.setSpan(new ForegroundColorSpan(AppContext.context().getResources().getColor(R.color.accent)), 0, mSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return mSpannable;
    }

    /**
     * Returns the project id from the address
     * @return
     */
    public String getProjectId() {
        return mProjectId;
    }

    /**
     * Returns teh chapter id from the address
     * @return
     */
    public String getChapterId() {
        return mChapterId;
    }

    /**
     * Returns the frame id from the address
     * @return
     */
    public String getFrameId() {
        return mFrameId;
    }

    /**
     * Returns the language id from the address
     * @return
     */
    public String getLanguageId() {
        return mLanguageId;
    }

    /**
     * Breaks the address appart into it's components
     * @param address
     */
    private void explodeAddress(String address) {
        String[] parts = address.split(":");
        if(parts.length == 6 && parts[1].equals("bible")) {
            // example: en:bible:notes:gen:03:04
            mLanguageId = parts[0];

            mProjectId = parts[3];
            mChapterId = parts[4];
            mFrameId = parts[5];
        } else if(parts.length == 5 && parts[3].equals("frames")) {
            // example: en:obs:notes:frames:01-11
            String[] chapterFrame = parts[4].split("-");
            if(chapterFrame.length == 2) {
                mLanguageId = parts[0];
                mProjectId = parts[1];
                mChapterId = chapterFrame[0];
                mFrameId = chapterFrame[1];
            }

        } else {
            Logger.w(this.getClass().getName(), "invalid passage link address "+address);
        }
    }

//    /**
//     * Returns the human readable name of the link
//     * @param rawLink
//     * @return
//     */
//    public void parseLink(String rawLink) {
//        Matcher matcher = PATTERN.matcher(rawLink);
//        while(matcher.find()) {
//            mTitle = matcher.group(3);
//            mAddress = matcher.group(2);
//            explodeAddress(mAddress);
//        }
//    }

    /**
     * Returns the link title
     * @return
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Returns the link address
     * @return
     */
    public String getAddress() {
        return mAddress;
    }
}
