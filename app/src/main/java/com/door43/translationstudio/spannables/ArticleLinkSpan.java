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
public class ArticleLinkSpan extends Span {
    // e.g. [[en:ta:vol1:translate:translate_unknown|How to Translate Unknowns]]
    public static final Pattern ADDRESS_PATTERN = Pattern.compile("\\[\\[(([-a-zA-Z0-9]+:ta:[-\\_a-z0-9]+:[-\\_a-z0-9]+:[-\\_a-z0-9]+)(\\|(((?!\\]\\]).)+))?)\\]\\]");
    // e.g <a href="/en/ta/vol1/translate/figs_intro" title="en:ta:vol1:translate:figs_intro">Figures of Speech</a>
    public static final Pattern LINK_PATTERN = Pattern.compile("<a(((?!<\\/a>).)*)href=\"\\/?([-a-zA-Z0-9]+\\/ta\\/[-\\_a-z0-9]+\\/[-\\_a-z0-9]+\\/[-\\_a-z0-9]+)\\/?\"(((?!<\\/a>).)*)>\\s*(((?!<\\/a>).)*)\\s*<\\/a>");
    private final String title;
    private final String address;
    private SpannableStringBuilder mSpannable;
    private String sourceLanguageSlug;
    private String volume;
    private String id;
    private String manual;

    /**
     *
     * @param title
     * @param sourceLanguageSlug
     * @param volume
     * @param manual
     * @param id
     */
    protected ArticleLinkSpan(String title, String sourceLanguageSlug, String volume, String manual, String id) {
        this.title = title;
        this.address = buildAddress(sourceLanguageSlug, volume, manual, id);
        this.sourceLanguageSlug = sourceLanguageSlug;
        this.volume = volume;
        this.manual = manual;
        this.id = id;
        init(this.title, address);
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

    private String buildAddress(String sourceLanguageSlug, String volume, String manual, String id) {
        // example: en:ta:vol2:translate:figs_euphemism
        return sourceLanguageSlug + ":ta:" + volume + ":" + manual + ":" + id;
    }

    /**
     * Changes the title of the passage link
     * @param title
     */
    public void setTitle(String title) {
        setHumanReadable(title);
        mSpannable = null;
    }

    public static ArticleLinkSpan parse(String address) {
        return parse("", address);
    }

    public static ArticleLinkSpan parse(String title, String address) {
        String[] parts = address.split(":");
        if(parts.length == 5) {
            // example: en:ta:vol2:translate:figs_euphemism
            String sourceLanguageSlug = parts[0];
            String taVolume = parts[2];
            String taManual = parts[3];
            String taId = parts[4];
            return new ArticleLinkSpan(title, sourceLanguageSlug, taVolume, taManual, taId);
        } else {
            Logger.w(ArticleLinkSpan.class.getName(), "invalid translation academy link address " + address);
        }
        return null;
    }

    public String getSourceLanguageSlug() {
        return sourceLanguageSlug;
    }

    public String getVolume() {
        return volume;
    }

    public String getId() {
        return id;
    }

    public String getManual() {
        return manual;
    }
}
