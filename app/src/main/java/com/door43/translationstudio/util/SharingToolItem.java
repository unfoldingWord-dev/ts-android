package com.door43.translationstudio.util;

/**
 * Defines a sharing tool option to be displayed in the sharing tools list
 */
public class SharingToolItem {
    private SharingToolAction mAction;
    private String mName;
    private int mIcon;

    public SharingToolItem(String name, int icon, SharingToolAction action) {
        mAction = action;
        mName = name;
        mIcon = icon;
    }

    public SharingToolAction getAction() {
        return mAction;
    }

    public String getName() {
        return mName;
    }

    public int getIcon() {
        return mIcon;
    }

    public interface SharingToolAction {
        public void run();
    }
}
