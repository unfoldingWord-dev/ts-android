package com.door43.translationstudio.spannables;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.util.MainContext;

/**
 * Created by joel on 10/31/2014.
 */
public class FancySpan {
    private final FancySpan me = this;
    private final String mText;
    private final OnClickListener mClickListener;
    private final String mId;

    public FancySpan(String id, String text, OnClickListener clickListener) {
        mId = id;
        mText = text;
        mClickListener = clickListener;
    }

    @Override
    public String toString() {
        return mText;
    }

    /**
     * Generates the content of the span
     * @param backgroundResource
     * @return
     */
    protected SpannableStringBuilder generateSpan(int backgroundResource, int colorResource, int textSizeResource) {
        SpannableStringBuilder spannable = new SpannableStringBuilder(mText);
        if(spannable.length() > 0) {
            spannable.append(toString());
            BitmapDrawable bd = convertViewToDrawable(createFancyTextView(mText, backgroundResource, colorResource, textSizeResource));
            bd.setBounds(0, 0, bd.getIntrinsicWidth(), bd.getIntrinsicHeight());
            spannable.setSpan(new ImageSpan(bd), 0, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ClickableSpan clickSpan = new ClickableSpan() {
                @Override
                public void onClick(View view) {
                    mClickListener.onClick(view, mText, mId);
                }
            };
            spannable.setSpan(clickSpan, 0, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannable;
    }

    /**
     * Generates a fancy text view
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
        TextView tv = new TextView(MainContext.getContext());
        tv.setText(text);
        tv.setTextSize(MainContext.getContext().getResources().getDimension(fontSizeResource));
        tv.setTextColor(MainContext.getContext().getResources().getColor(textColorResource));
        tv.setBackgroundResource(backgroundResource);
        tv.setPadding(10, 5, 10, 5);
        return tv;
    }

    /**
     * Converts a view into a bitmap
     * @param view
     * @return
     */
    private static BitmapDrawable convertViewToDrawable(View view) {
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
        public void onClick(View view, String spanText, String spanId);
    }
}
