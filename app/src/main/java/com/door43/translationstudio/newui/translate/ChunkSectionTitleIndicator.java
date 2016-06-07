package com.door43.translationstudio.newui.translate;

import android.content.Context;
import android.util.AttributeSet;

import com.door43.translationstudio.R;

import xyz.danoz.recyclerviewfastscroller.sectionindicator.title.SectionTitleIndicator;

/**
 * Created by blm on 6/6/16.
 */
public class ChunkSectionTitleIndicator extends SectionTitleIndicator<String> {

    private static final int DEFAULT_TITLE_INDICATOR_LAYOUT = R.layout.chapter_indicator_with_title;

    public ChunkSectionTitleIndicator(Context context) {
        super(context);
    }

    public ChunkSectionTitleIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ChunkSectionTitleIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
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
    public void setSection(String section) {
//        setIndicatorTextColor(android.R.color.holo_green_dark);

        // Example of using a single character
//        setTitleText(colorGroup.getName().charAt(0) + "");

        // Example of using a longer string
        setTitleText(section);
    }
}