package com.door43.translationstudio.util;

/**
 * Event sent when an asyncronous task finishes
 */
public class AsyncTaskResultEvent {
    private String mResult;

    public AsyncTaskResultEvent(String result) {
        mResult = result;
    }

    /**
     * Returns the task result
     * @return
     */
    public String getResult() {
        return mResult;
    }
}
