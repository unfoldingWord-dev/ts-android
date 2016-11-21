package com.door43.translationstudio.ui.spannables;

import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.view.View;
import android.widget.TextView;

import com.door43.widget.LongClickableSpan;

/**
 * Created by joel on 1/28/2015.
 */
public abstract class Span {
    private CharSequence mHumanReadable;
    private CharSequence mMachineReadable;
    private OnClickListener mClickListener;
    private Bundle extras;

    /**
     * Creates a new empty span.
     * This is useful for classes that extends this class because they may need to perform
     * some proccesing before fully initializing.
     * You should manualy call init() if using this constructor
     */
    public Span() {
        init("", "");
    }

    /**
     * Creates a new span
     * @param humanReadable the human readable title of the span
     * @param machineReadable the machine readable definition of the span
     */
    Span(CharSequence humanReadable, CharSequence machineReadable) {
        init(humanReadable, machineReadable);
    }

    public Bundle getExtras() {
        return this.extras;
    }

    public void setExtras(Bundle extras) {
        this.extras = extras;
    }

    /**
     * Initializes the span
     * @param humanReadable
     * @param machineReadable
     */
    protected void init(CharSequence humanReadable, CharSequence machineReadable) {
        mHumanReadable = humanReadable;
        mMachineReadable = machineReadable;
    }

    /**
     * Sets the click listener on this span
     * @param listener
     */
    public void setOnClickListener(OnClickListener listener) {
        mClickListener = listener;
    }

    protected void setHumanReadable(String text) {
        mHumanReadable = text;
    }

    /**
     * Generates the span and hooks up the click listener.
     * @return
     */
    public SpannableStringBuilder render() {
        SpannableStringBuilder spannable;
        if(mHumanReadable != null && !mHumanReadable.toString().isEmpty()) {
            spannable = new SpannableStringBuilder(mHumanReadable);
        } else {
            spannable = new SpannableStringBuilder(mMachineReadable);
        }
        if (spannable.length() > 0) {
            spannable.setSpan(new SpannedString(mMachineReadable), 0, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            LongClickableSpan clickSpan = new LongClickableSpan() {
                @Override
                public void onLongClick(View view) {
                    if(mClickListener != null) {
                        TextView tv = (TextView)view;
                        Spanned s = (Spanned)tv.getText();
                        int start = s.getSpanStart(this);
                        int end = s.getSpanEnd(this);
                        mClickListener.onLongClick(view, Span.this, start, end);
                    }
                }

                @Override
                public void onClick(View view) {
                    if (mClickListener != null) {
                        TextView tv = (TextView)view;
                        Spanned s = (Spanned)tv.getText();
                        int start = s.getSpanStart(this);
                        int end = s.getSpanEnd(this);
                        mClickListener.onClick(view, Span.this, start, end);
                    }
                }
            };
            spannable.setSpan(clickSpan, 0, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannable;
    }

    /**
     * Returns the machine readable source of this span
     * @return
     */
    public CharSequence getMachineReadable() {
        return mMachineReadable;
    }

    /**
     * Returns the human readable title of this span
     * @return
     */
    public CharSequence getHumanReadable() {
        return mHumanReadable;
    }

    /**
     * Returns the span as a CharSequence
     * @return
     */
    public CharSequence toCharSequence() {
        return render();
    }

    /**
     * Custom click listener when span is clicked
     */
    public interface OnClickListener {
        void onClick(View view, Span span, int start, int end);
        void onLongClick(View view, Span span, int start, int end);
    }
}
