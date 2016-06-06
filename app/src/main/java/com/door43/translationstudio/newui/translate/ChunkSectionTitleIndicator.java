package com.door43.translationstudio.newui.translate;

import android.content.Context;
import android.util.AttributeSet;

import xyz.danoz.recyclerviewfastscroller.sectionindicator.title.SectionTitleIndicator;

/**
 * Created by blm on 6/6/16.
 */
public class ChunkSectionTitleIndicator extends SectionTitleIndicator<String> {

    public ChunkSectionTitleIndicator(Context context) {
        super(context);
    }

    public ChunkSectionTitleIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ChunkSectionTitleIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setSection(String section) {
        setIndicatorTextColor(android.R.color.holo_green_dark);

        // Example of using a single character
//        setTitleText(colorGroup.getName().charAt(0) + "");

        // Example of using a longer string
        setTitleText(section);
    }
}