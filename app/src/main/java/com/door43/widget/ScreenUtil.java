package com.door43.widget;

import android.content.Context;
import android.util.DisplayMetrics;

/**
 * Created by joel on 8/5/2015.
 */
public class ScreenUtil {

    /**
     * Converts dp to pixels
     * @param context
     * @param dp
     * @return
     */
    public static int dpToPx(Context context, float dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

    /**
     * Converts pixels to dp
     * @param context
     * @param px
     * @return
     */
    public float pxToDp(Context context, int px) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT);
    }
}
