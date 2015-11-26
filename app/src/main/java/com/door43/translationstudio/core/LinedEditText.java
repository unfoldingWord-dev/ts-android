package com.door43.translationstudio.core;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.EditText;

/**
 * Created by blm on 11/25/15.
 */

public class LinedEditText extends EditText {
    private Rect mRect;
    private Paint mPaint;
    public boolean mEnableLines;

    // we need this constructor for LayoutInflater
    public LinedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);

        mRect = new Rect();
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(0xFFC4E7FF); // same color as in GIF
        mEnableLines = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if(mEnableLines) {
            int count = getLineCount();
            Rect r = mRect;
            Paint paint = mPaint;
            int lineHeight = (int) getLineHeight();
            int offset = lineHeight / 8; // offset so that text is above line

            for (int i = 0; i < count; i++) {
                int baseline = getLineBounds(i, r) + offset;
                canvas.drawLine(r.left, baseline, r.right, baseline, paint);
            }
        }

        super.onDraw(canvas);
    }
}