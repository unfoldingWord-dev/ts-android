package com.door43.translationstudio.spannables;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;

import java.util.regex.Pattern;

/**
 * Created by joel on 12/2/2015.
 */
public class TranslationAcademyLinkSpan extends Span {
    // e.g. [[en:ta:vol1:translate:translate_unknown|How to Translate Unknowns]]
    public static final Pattern PATTERN = Pattern.compile("\\[\\[((?!\\]\\])([-a-zA-Z0-9]+:ta.*)(\\|(.*))?)\\]\\]");
    private final String mTitle;
    private final String mAddress;
    private SpannableStringBuilder mSpannable;
    private String sourceLanguageSlug;
    private String taVolume;
    private String taId;
    private String taManual;

    /**
     * Creates a new translation academy link
     * @param title the title of the link (may be null)
     * @param address the address to the link e.g. en:ta:vol2:translate:figs_euphemism
     */
    public TranslationAcademyLinkSpan(String title, String address) {
        super(title, address);
        mTitle = title;
        mAddress = address;
        explodeAddress(address);
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
     * Changes the title of the passage link
     * @param title
     */
    public void setTitle(String title) {
        setHumanReadable(title);
        mSpannable = null;
    }

    /**
     * Breaks the address appart into it's components
     * @param address
     */
    private void explodeAddress(String address) {
        String[] parts = address.split(":");
        if(parts.length == 5) {
            // example: en:ta:vol2:translate:figs_euphemism
            sourceLanguageSlug = parts[0];
            taVolume = parts[2];
            taManual = parts[3];
            taId = parts[4];
        } else {
            Logger.w(this.getClass().getName(), "invalid translation academy link address " + address);
        }
    }
}
