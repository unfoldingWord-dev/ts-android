package com.door43.translationstudio.util;

/**
 * Defines a sharing tool option to be displayed in the sharing tools list
 */
public class SharingToolItem {
    private SharingToolAction mAction;
    private String mName;
    private int mIcon;
    private Boolean mIsEnabled;
    private int mDisabledNotice;

    public SharingToolItem(String name, int icon, SharingToolAction action) {
        mAction = action;
        mName = name;
        mIcon = icon;
        mIsEnabled = true;
        mDisabledNotice = 0;
    }

    /**
     *
     * @param name The visible name of the tool
     * @param icon the tool icon
     * @param action the action to be performed
     * @param enabled sets the tool as enabled or disabled. when disabled a notice will be displayed
     */
    public SharingToolItem(String name, int icon, SharingToolAction action, Boolean enabled, int disabledNotice) {
        mAction = action;
        mName = name;
        mIcon = icon;
        mIsEnabled = enabled;
        mDisabledNotice = disabledNotice;
    }

    /**
     * Returns the action to be performed
     * @return
     */
    public SharingToolAction getAction() {
        return mAction;
    }

    /**
     * Returns the tool name
     * @return
     */
    public String getName() {
        return mName;
    }

    /**
     * Returns the icon resource id
     * @return
     */
    public int getIcon() {
        return mIcon;
    }

    /**
     * Checks if the tool is enabled
     * @return
     */
    public boolean isEnabled() {
        return mIsEnabled;
    }

    /**
     * Returns the notice to be shown when the tool is disabled
     * @return
     */
    public int getDisabledNotice() {
        return mDisabledNotice;
    }

    public interface SharingToolAction {
        public void run();
    }
}
