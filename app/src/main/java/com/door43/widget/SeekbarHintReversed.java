package com.door43.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;

import it.moondroid.seekbarhint.library.SeekBarHint;

/**
 * This class provides a seekbar that is reversed. e.g. operates from right to left
 */
public class SeekbarHintReversed extends SeekBarHint {
    public SeekbarHintReversed(Context context) {
        super(context);
    }

    public SeekbarHintReversed(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SeekbarHintReversed(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    // TODO: 10/4/16 we should turn this into a generic seek bar that allows vertical and horizontal orientation and also allow switching direction of seek.
//
//    @Override
//    protected void onDraw(Canvas canvas) {
//        float px = this.getWidth() / 2.0f;
//        float py = this.getHeight() / 2.0f;
//
//        canvas.scale(-1, 1, px, py);
//
//        super.onDraw(canvas);
//    }
//
//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        event.setLocation(this.getWidth() - event.getX(), event.getY());
//
//        return super.onTouchEvent(event);
//    }
}
