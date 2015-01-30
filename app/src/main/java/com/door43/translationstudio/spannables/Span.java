package com.door43.translationstudio.spannables;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

/**
 * Created by joel on 1/28/2015.
 */
public abstract class Span {
    private final String mHumanReadable;
    private final String mMachineReadable;
    private OnClickListener mListener;

    /**
     * Creates a new span
     * @param humanReadable
     * @param machineReadable
     */
    public Span(String humanReadable, String machineReadable) {
        mHumanReadable = humanReadable;
        mMachineReadable = machineReadable;
    }

    public void setOnClickListener(OnClickListener listener) {
        mListener = listener;
    }

    /**
     * Generates the span and hooks up the click listener.
     * @return
     */
    public SpannableStringBuilder generateSpan() {
        SpannableStringBuilder spannable = new SpannableStringBuilder(mHumanReadable);
        if (spannable.length() > 0) {
            spannable.setSpan(new SpannedString(mMachineReadable), 0, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (mListener != null) {
                ClickableSpan clickSpan = new ClickableSpan() {
                    @Override
                    public void onClick(View view) {
                        if (mListener != null) {
                            TextView tv = (TextView)view;
                            Spanned s = (Spanned)tv.getText();
                            int start = s.getSpanStart(this);
                            int end = s.getSpanEnd(this);
                            mListener.onClick(view, Span.this, start, end);
                        }
                    }
                };
                spannable.setSpan(clickSpan, 0, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return spannable;
    }

    /**
     * Returns the machine readable source of this span
     * @return
     */
    public String getMachineReadable() {
        return mMachineReadable;
    }

    /**
     * Returns the span as a CharSequence
     * @return
     */
    public CharSequence toCharSequence() {
        return generateSpan();
    }

    /**
     * Custom click listener when span is clicked
     */
    public static interface OnClickListener {
        public void onClick(View view, Span span, int start, int end);
    }
}
