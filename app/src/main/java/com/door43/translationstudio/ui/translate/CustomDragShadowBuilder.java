package com.door43.translationstudio.ui.translate;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

/**
 * Created by joel on 10/4/2015.
 */
public class CustomDragShadowBuilder extends View.DragShadowBuilder {
    private Drawable shadow;

    private CustomDragShadowBuilder() {
        super();
    }

    public static View.DragShadowBuilder fromResource(Context context, int drawableId) {
        CustomDragShadowBuilder builder = new CustomDragShadowBuilder();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.shadow = context.getResources().getDrawable(drawableId, null);
        } else {
            builder.shadow = context.getResources().getDrawable(drawableId);
        }
        if (builder.shadow == null) {
            throw new NullPointerException("Drawable from id is null");
        }

        builder.shadow.setBounds(0, 0, builder.shadow.getMinimumWidth(), builder.shadow.getMinimumHeight());

        return builder;
    }

    public static View.DragShadowBuilder fromBitmap(Context context, Bitmap bmp) {
        if (bmp == null) {
            throw new IllegalArgumentException("Bitmap cannot be null");
        }

        CustomDragShadowBuilder builder = new CustomDragShadowBuilder();

        builder.shadow = new BitmapDrawable(context.getResources(), bmp);
        builder.shadow.setBounds(0, 0, builder.shadow.getMinimumWidth(), builder.shadow.getMinimumHeight());

        return builder;
    }

    @Override
    public void onDrawShadow(Canvas canvas) {
        shadow.draw(canvas);
    }

    @Override
    public void onProvideShadowMetrics(Point shadowSize, Point shadowTouchPoint) {
        shadowSize.x = shadow.getMinimumWidth();
        shadowSize.y = shadow.getMinimumHeight();

        shadowTouchPoint.x = (int)(shadowSize.x / 2);
        shadowTouchPoint.y = (int)(shadowSize.y + 36);
    }
}