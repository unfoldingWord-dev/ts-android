package com.door43.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.door43.widget.LinedEditText;

/**
 * Created by blm on 12/7/2015.
 * LinearLayout with drawn lines
 */
public class LinedLinearLayout extends LinearLayout {
    private Rect mRect;
    private Paint mPaint;
    private boolean mEnableLines = false;
    private int mLineHeight = 0;
    private int mYOffset = -1;
    private int mFirstLineY = -1;
    private LinedEditText mEditText = null;

    public LinedLinearLayout(Context context) {
        super(context);
        drawInit();
    }

    public LinedLinearLayout (Context context, AttributeSet attrs) {
        super(context, attrs);
        drawInit();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        boolean var = true;

        if (mEnableLines) {
            if (null != mEditText) {

                // get view position on screen
                int[] l = new int[2];
                this.getLocationOnScreen(l);
//            int viewX = l[0];
                int viewY = l[1];

                Rect bounds = canvas.getClipBounds();
                int bottom = bounds.bottom;

                int relativeY = mEditText.getYlocation() - viewY;
                int lineHeight = mEditText.getDistanceBetweenLines();
                int offset = lineHeight / LinedEditText.mRelativeOffset; // offset so that text is above line
                int position = mEditText.getLinePosition() + relativeY + offset;

                for (int i = 0; i < 100; i++) {

                    if (position > bottom) {
                        break;
                    }

                    canvas.drawLine(bounds.left, position, bounds.right, position, mPaint);

                    position += lineHeight;
                }
            }
        }

        super.onDraw(canvas);
    }

    public boolean isEnableLines() {
        return mEnableLines;
    }

    public void setEnableLines(boolean mEnableLines) {
        this.mEnableLines = mEnableLines;
        this.invalidate();
    }

    private void drawInit() {
        mRect = new Rect();
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(0xFFC4E7FF); // same color as in GIF
    }

    public void setEditText(final LinedEditText mEditText) {
        this.mEditText = mEditText;
    }
}
