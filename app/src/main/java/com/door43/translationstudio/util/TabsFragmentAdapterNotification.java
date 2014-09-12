package com.door43.translationstudio.util;

/**
 * This interface allows us to notify Adapters that the dataset has been changed.
 * Adapters should reload their content when notified.
 */
public interface TabsFragmentAdapterNotification {
    public void NotifyAdapterDataSetChanged();
}
