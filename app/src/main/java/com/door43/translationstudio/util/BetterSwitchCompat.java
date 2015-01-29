package com.door43.translationstudio.util;

import android.content.Context;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.widget.CompoundButton;

/**
 * Created by joel on 1/28/2015.
 */
public class BetterSwitchCompat extends SwitchCompat {
    private OnCheckedChangeListener mListener;

    public BetterSwitchCompat(Context context) {
        super(context);
    }

    public BetterSwitchCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BetterSwitchCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener listener){
        if(mListener == null) {
            mListener = listener;
        }
        super.setOnCheckedChangeListener(listener);
    }

    /**
     * Sets the check state without triggering any events
     * @param checked
     */
    public void silentlySetChecked(boolean checked) {
        toggleListener(false);
        super.setChecked(checked);
        toggleListener(true);
    }

    /**
     * Turns the listener on and off
     * @param on
     */
    private void toggleListener(boolean on){
        if(on){
            super.setOnCheckedChangeListener(mListener);
        } else {
            super.setOnCheckedChangeListener(null);
        }
    }
}
