package com.door43.translationstudio.util;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * This class allows us to control what happens when the soft keyboard is shown/hidden over the layout.
 */
public class AdjustingLayout extends LinearLayout {
    private OnSoftKeyboardListener onSoftKeyboardListener;

    public AdjustingLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AdjustingLayout(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        if (onSoftKeyboardListener != null) {
            final int newSpec = MeasureSpec.getSize(heightMeasureSpec);
            final int oldSpec = getMeasuredHeight();
            if (oldSpec > newSpec){
                onSoftKeyboardListener.onShown();
            } else {
                onSoftKeyboardListener.onHidden();
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public final void setOnSoftKeyboardListener(final OnSoftKeyboardListener listener) {
        this.onSoftKeyboardListener = listener;
    }

    public interface OnSoftKeyboardListener {
        public void onShown();
        public void onHidden();
    }
}
