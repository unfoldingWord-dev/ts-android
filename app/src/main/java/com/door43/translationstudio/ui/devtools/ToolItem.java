package com.door43.translationstudio.ui.devtools;

/**
 * Tool items allow you to easily provide tools within a ListView
 */
public class ToolItem {
    private ToolAction mAction;
    private String mName;
    private String mDescription;
    private int mIcon;
    private Boolean mIsEnabled;
    private String mDisabledNotice;

    public ToolItem(String nameResource, String descriptionResource, int iconResource, ToolAction action) {
        mAction = action;
        mDescription = descriptionResource;
        mName = nameResource;
        mIcon = iconResource;
        mIsEnabled = true;
        mDisabledNotice = "";
    }

    /**
     *
     * @param nameResource The visible name of the tool
     * @param icon the tool image
     * @param action the action to be performed
     * @param enabled sets the tool as enabled or disabled. when disabled a notice will be displayed
     */
    public ToolItem(String nameResource, String descriptionResource, int icon, ToolAction action, Boolean enabled, String disabledNotice) {
        mAction = action;
        mDescription = descriptionResource;
        mName = nameResource;
        mIcon = icon;
        mIsEnabled = enabled;
        mDisabledNotice = disabledNotice;
    }

    /**
     * Returns the action to be performed
     * @return
     */
    public ToolAction getAction() {
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
     * Returns the tool description
     * @return
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     * Returns the image resource id
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
    public String getDisabledNotice() {
        return mDisabledNotice;
    }

    public interface ToolAction {
        public void run();
    }
}
