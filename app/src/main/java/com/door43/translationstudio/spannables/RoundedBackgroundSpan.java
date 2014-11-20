package com.door43.translationstudio.spannables;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.style.ReplacementSpan;

public class RoundedBackgroundSpan extends ReplacementSpan
{
    private final int mBackgroundColorResource;
    private final int mTypefaceColorResource;

    public RoundedBackgroundSpan(int backgroundColorResource, int typeFaceColorResource) {
        mBackgroundColorResource = backgroundColorResource;
        mTypefaceColorResource = typeFaceColorResource;
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        return Math.round(measureText(paint, text, start, end));
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint)
    {
        RectF rect = new RectF(x, top + 2, x + measureText(paint, text, start, end), bottom);
        paint.setColor(mBackgroundColorResource);
        canvas.drawRoundRect(rect, 20, 20, paint);
        paint.setColor(mTypefaceColorResource);
        canvas.drawText(text, start, end, x, y, paint);
    }

    private float measureText(Paint paint, CharSequence text, int start, int end) {
        return paint.measureText(text, start, end);
    }
}