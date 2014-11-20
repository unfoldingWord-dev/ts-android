package com.door43.translationstudio.spannables;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.SpannedString;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.util.MainContext;

/**
 * Created by joel on 10/31/2014.
 */
public abstract class FancySpan {
    private final FancySpan me = this;
    private final String mSpanText;
    private OnClickListener mClickListener;
    private final String mSpanId;
    protected static Typeface mTypeface;
    protected static float mTypefaceSize = 0;

    public FancySpan(String spanId, String spanText, OnClickListener clickListener) {
        mSpanId = spanId;
        mSpanText = spanText;
        mClickListener = clickListener;
    }

    @Override
    public String toString() {
        return mSpanText;
    }

    /**
     * Returns the text that spanned
     * @return
     */
    public String getSpanText() {
        return mSpanText;
    }

    /**
     * Returns the id of the span.
     * This id is used to identify an individual span within the text.
     * @return
     */
    public String getSpanId() {
        return mSpanId;
    }

    /**
     * Specifies the type face to use for all spans
     * @param typeface
     */
    public static void setGlobalTypeface(Typeface typeface) {
        mTypeface = typeface;
    }


    /**
     * Specifies the typeface size to use for all spans. This overides the size given by inherited classes
     * @param typefaceSize
     */
    public static void setGlobalTypefaceSize(int typefaceSize) {
        // TRICKY: this is a hack to get the span font size to match normal text size. This seems to be the magic ratio.
        mTypefaceSize = (float)(typefaceSize * 1.5);
    }

    /**
     * Generates the content of the span
     * @return
     */
    protected SpannableStringBuilder generateSpan() {
        return generateSpan(mSpanText);
    }

    /**
     * Generates the content of the span
     * @param textReplacement the text to be inserted into the raw text (not visible) This is useful for storing information that should be persited to the disk.
     * @return
     */
    private SpannableStringBuilder generateSpan(String textReplacement) {
        SpannableStringBuilder spannable = new SpannableStringBuilder(textReplacement);
        if(spannable.length() > 0) {
            spannable.setSpan(new SpannedString(textReplacement), 0, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ClickableSpan clickSpan = new ClickableSpan() {
                @Override
                public void onClick(View view) {
                    if(mClickListener != null) {
                        mClickListener.onClick(view, me);
                    }
                }
            };
            spannable.setSpan(clickSpan, 0, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannable;
    }

    /**
     * Generates a spannable that has a bubble background
     * @param backgroundColorResourceId
     * @param textColorResourceId
     * @return
     */
    protected SpannableStringBuilder generateBubbleSpan(int backgroundColorResourceId, int textColorResourceId) {
        SpannableStringBuilder spannable = generateSpan(mSpanText);
        // TODO: there has to be a better way than using the global context class.
        spannable.setSpan(new RoundedBackgroundSpan(MainContext.getContext().getResources().getColor(backgroundColorResourceId), MainContext.getContext().getResources().getColor(textColorResourceId)), 0, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannable;
    }

    /**
     * Specifies the listener to be called upon a click
     * @param clickListener
     */
    public void setOnClickListener(OnClickListener clickListener) {
        mClickListener = clickListener;
    }

    /**
     * Generates the content of the span
     * @deprecated
     * @param textReplacement
     * @param background
     * @return
     */
    protected SpannableStringBuilder generateImageSpan(String textReplacement, BitmapDrawable background) {
        SpannableStringBuilder spannable = new SpannableStringBuilder(textReplacement);
        if(spannable.length() > 0) {
            background.setBounds(0, 0, background.getIntrinsicWidth(), background.getIntrinsicHeight());
            spannable.setSpan(new ImageSpan(background), 0, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ClickableSpan clickSpan = new ClickableSpan() {
                @Override
                public void onClick(View view) {
                    if(mClickListener != null) {
                        mClickListener.onClick(view, me);
                    }
                }
            };
            spannable.setSpan(clickSpan, 0, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannable;
    }

    /**
     * Generates a fancy text view
     * @deprecated
     * @param text
     * @param backgroundResource
     * @return
     */
    private static TextView createFancyTextView(String text, int backgroundResource) {
        return createFancyTextView(text, backgroundResource, R.color.dark_gray, R.dimen.h5);
    }

    /**
     * Generates a fancy text view
     * @param text
     * @param backgroundResource
     * @param textColorResource
     * @param fontSizeResource
     * @return
     */
    private static TextView createFancyTextView(String text, int backgroundResource, int textColorResource, int fontSizeResource) {
        TextView tv = (TextView) MainContext.getContext().getCurrentActivity().getLayoutInflater().inflate(R.layout.span_plain, null);
        tv.setText(text);
        tv.setTextSize(MainContext.getContext().getResources().getDimension(fontSizeResource));
        tv.setTextColor(MainContext.getContext().getResources().getColor(textColorResource));
        tv.setBackgroundResource(backgroundResource);
        if(mTypeface != null) {
            tv.setTypeface(mTypeface);
        }
        if(mTypefaceSize > 0) {
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, mTypefaceSize);
        }
        return tv;
    }

    /**
     * Converts a view into a bitmap
     * @param view
     * @return
     */
    protected static BitmapDrawable convertViewToDrawable(View view) {
        int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(spec, spec);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        Bitmap b = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.translate(-view.getScrollX(), -view.getScrollY());
        view.draw(c);
        view.setDrawingCacheEnabled(true);
        Bitmap cacheBmp = view.getDrawingCache();
        Bitmap viewBmp = cacheBmp.copy(Bitmap.Config.ARGB_8888, true);
        view.destroyDrawingCache();
        return new BitmapDrawable(viewBmp);
    }

    /**
     * Custom click listener when span is clicked
     */
    public static interface OnClickListener {
        public void onClick(View view, FancySpan span);
    }
}
