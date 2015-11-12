package com.door43.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

/**
 * Created by joel on 11/11/2015.
 */
public class LockableScrollView extends ScrollView {
    private boolean scrollable = true;

    public LockableScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    public LockableScrollView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public LockableScrollView(Context context) {
        super(context);
    }

    /**
     * Enables or disables the scrolling of this scroll view
     * @param enabled
     */
    public void setScrollingEnabled(boolean enabled) {
        this.scrollable = enabled;
    }

    /**
     * Checks if scrolling is enabled on this scroll view
     * @return
     */
    public boolean isScrollable() {
        return this.scrollable;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (scrollable) {
                    return super.onTouchEvent(ev);
                } else {
                    return scrollable;
                }
            default:
                return super.onTouchEvent(ev);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!scrollable) {
            return false;
        } else {
            return super.onInterceptTouchEvent(ev);
        }
    }
}
