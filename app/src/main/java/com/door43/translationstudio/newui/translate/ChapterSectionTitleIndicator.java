package com.door43.translationstudio.newui.translate;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import com.door43.translationstudio.R;

import xyz.danoz.recyclerviewfastscroller.sectionindicator.title.SectionTitleIndicator;

/**
 * Extends SectionTitleIndicator and tweaks to look better in ts-android.
 */
public class ChapterSectionTitleIndicator extends SectionTitleIndicator<String> {

    private static final int DEFAULT_TITLE_INDICATOR_LAYOUT = R.layout.chapter_indicator_with_title;
    public static final String TAG = ChapterSectionTitleIndicator.class.getSimpleName();

    public ChapterSectionTitleIndicator(Context context) {
        super(context);
    }

    public ChapterSectionTitleIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ChapterSectionTitleIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * @return the default layout for a section indicator with a title. This closely resembles the section indicator
     *         featured in Lollipop's Contact's application
     */
    @Override
    protected int getDefaultLayoutId() {
        return DEFAULT_TITLE_INDICATOR_LAYOUT;
    }

    @Override
    public void setProgress(float progress) {
        Log.d(TAG, "setProgress: progress=" + progress);
        super.setProgress(progress); // do default positioning

        float initialY = getY(); // get default positioning

        Log.d(TAG, "setProgress: initialY=" + initialY);
        int handleHeight = getHeight();
        Log.d(TAG, "setProgress: handleHeight=" + handleHeight);
        float yPositionFromScrollProgress = initialY - handleHeight / 4;
        Log.d(TAG, "setProgress: new yPositionFromScrollProgress=" + yPositionFromScrollProgress);
        setY(yPositionFromScrollProgress);
    }


    @Override
    public void setSection(String section) {
//        setIndicatorTextColor(android.R.color.holo_green_dark);

        // Example of using a single character
//        setTitleText(colorGroup.getName().charAt(0) + "");

        // Example of using a longer string
        setTitleText(section);
    }
}