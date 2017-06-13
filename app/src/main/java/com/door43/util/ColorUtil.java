package com.door43.util;

import android.content.Context;
import android.os.Build;

/**
 * Created by joel on 2/24/17.
 */
public class ColorUtil {

    /**
     * Returns a color with backwards compatibility for deprecated methods
     * @param context
     * @return
     */
    public static int getColor(Context context, int resource_id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.getResources().getColor(resource_id, context.getTheme());
        } else {
            return context.getResources().getColor(resource_id);
        }
    }

}
