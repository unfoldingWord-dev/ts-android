package com.door43.translationstudio.translations;

import com.door43.delegate.DelegateResponse;

/**
 * Created by joel on 9/29/2014.
 */
public class TranslationSyncResponse implements DelegateResponse {
    private boolean mSuccess;

    /**
     *
     * @param success true if the sync was successful
     */
    public TranslationSyncResponse(Boolean success) {
        mSuccess = success;
    }

    /**
     * Check if the sync was successful
     * @return
     */
    public boolean isSuccess() {
        return mSuccess;
    }
}
