package com.door43.translationstudio.spannables;

import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;

/**
 * Created by joel on 12/2/2015.
 */
public class LinkSpan extends Span {

    private final String title;
    private final String address;
    private final String type;
    private SpannableStringBuilder spannable;

    public LinkSpan(String title, String address, String type) {
        super(title, address);
        this.title = title;
        this.address = address;
        this.type = type;
    }

    /**
     * Changes the title of the link
     * @param title
     */
    public void setTitle(String title) {
        setHumanReadable(title);
    }

    @Override
    public SpannableStringBuilder render() {
        if(this.spannable == null) {
            this.spannable = super.render();
            // apply custom styles
            this.spannable.setSpan(new ForegroundColorSpan(AppContext.context().getResources().getColor(R.color.accent)), 0, this.spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return this.spannable;
    }

    /**
     * Returns the link title
     * @return
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * Returns the link address
     * @return
     */
    public String getAddress() {
        return this.address;
    }

    /**
     * Returns the link type
     * @return
     */
    public String getType() {
        return this.type;
    }
}
