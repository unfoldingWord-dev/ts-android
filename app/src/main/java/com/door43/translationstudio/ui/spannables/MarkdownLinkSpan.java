package com.door43.translationstudio.ui.spannables;

import java.util.regex.Pattern;

/**
 * Created by joel on 2/24/17.
 */

public class MarkdownLinkSpan extends Span {
    public static final Pattern PATTERN = Pattern.compile("\\[\\[(((?!\\]).)*)\\]\\]");
    private final String mAddress;
    private final String mTitle;

    public MarkdownLinkSpan(String title, String address) {
        super(title, address);
        mTitle = title;
        mAddress = address;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getAddress() {
        return mAddress;
    }
}
