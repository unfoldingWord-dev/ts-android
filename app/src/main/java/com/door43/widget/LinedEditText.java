package com.door43.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.ViewParent;
import android.widget.EditText;

/**
 * Created by blm on 11/25/15.
 */

public class LinedEditText extends EditText {
    private Rect mRect;
    private Paint mPaint;
    private boolean mEnableLines = false;
    static public int mRelativeOffset = 8;

    // we need this constructor for LayoutInflater
    public LinedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);

        mRect = new Rect();
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(0xFFC4E7FF); // same color as in GIF
    }

    @Override
    protected void onDraw(Canvas canvas) {

        LinedLinearLayout parent = getLinedParent();
        if(null != parent) { // if on top of LinedLinearLayout, draw lines on it

          parent.setEditText(this);
        }
        else // if not paired, draw line on edittext if enabled
        if(mEnableLines) {

            int count = getLineCount();

            Rect r = mRect;

            Rect bounds = canvas.getClipBounds();
            int bottom = bounds.bottom;

            int lineHeight = (int) getLineHeight();
            int offset = lineHeight / mRelativeOffset; // offset so that text is above line

            int position = 0;

            for (int i = 0; i < 100; i++) {  // 100 is just here for a sanity limit to max number of lines

                if (i < count) {
                    position = getLineBounds(i, r) + offset;
                } else { // keep drawing below last text line
                    position += lineHeight;
                }

                if (position > bottom) { // done when we have filled the view
                    break;
                }

                canvas.drawLine(r.left, position, r.right, position, mPaint);
            }
        }

        super.onDraw(canvas);
    }

    public int getYlocation() {

        // get view position on screen
        int[] l = new int[2];
        this.getLocationOnScreen(l);
//        int viewX = l[0];
        int viewY = l[1];
        return viewY;
    }

    public int getDistanceBetweenLines() {

        int lineHeight = (int) getLineHeight();
        return lineHeight;
    }

    public int getLinePosition() {

//        int offset = lineHeight / mRelativeOffset; // offset so that text is above line

        Rect r = mRect;
        int position = getLineBounds(0, r);
        return position;
    }

    public boolean isEnableLines() {
        LinedLinearLayout parent = getLinedParent();
        if(null != parent) {
            return parent.isEnableLines();
        }

        return mEnableLines;
    }

    public void setEnableLines(boolean mEnableLines) {
        LinedLinearLayout parent = getLinedParent();
        if(null != parent) {
            parent.setEnableLines(mEnableLines);
        }

        this.mEnableLines = mEnableLines;
        this.invalidate();
    }

    private LinedLinearLayout getLinedParent() {
        ViewParent parent = this.getParent();

        for(int i = 0; i < 2; i++) { // maximum levels

            if(null == parent) {
                break;
            }

            boolean paired = (parent instanceof LinedLinearLayout);

            if (paired) {
                return (LinedLinearLayout) parent;
            }

            parent = parent.getParent(); // try moving up
        }
        return null;
    }

}