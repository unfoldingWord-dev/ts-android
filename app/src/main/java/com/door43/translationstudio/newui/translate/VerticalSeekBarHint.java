package com.door43.translationstudio.newui.translate;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;

import org.unfoldingword.tools.logger.Logger;

/**
 * 7/1/2016
 * added X offset for popup
 */
public class VerticalSeekBarHint extends com.door43.widget.VerticalSeekBar implements SeekBar.OnSeekBarChangeListener {

    public static final String TAG = VerticalSeekBarHint.class.getSimpleName();
    private int mPopupLayout;
    private int mPopupWidth;
    private int mPopupStyle;
    public static final int POPUP_FIXED = 1;
    public static final int POPUP_FOLLOW = 0;

    private PopupWindow mPopup;
    private TextView mPopupTextView;
    private int mXLocationOffset;
    private int mYLocationOffset;

    private OnSeekBarChangeListener mInternalListener;
    private OnSeekBarChangeListener mExternalListener;

    private OnSeekBarHintProgressChangeListener mProgressChangeListener;

    public interface OnSeekBarHintProgressChangeListener {
        public String onHintTextChanged(VerticalSeekBarHint seekBarHint, int progress);
    }

    public VerticalSeekBarHint(Context context) {
        super(context);
        init(context, null);
    }

    public VerticalSeekBarHint(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public VerticalSeekBarHint(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {

        TypedArray a = context.obtainStyledAttributes(attrs, it.moondroid.seekbarhint.library.R.styleable.SeekBarHint);

        mPopupLayout = a.getResourceId(it.moondroid.seekbarhint.library.R.styleable.SeekBarHint_popupLayout, it.moondroid.seekbarhint.library.R.layout.popup);
        mPopupWidth = (int) a.getDimension(it.moondroid.seekbarhint.library.R.styleable.SeekBarHint_popupWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        mYLocationOffset = (int) a.getDimension(it.moondroid.seekbarhint.library.R.styleable.SeekBarHint_yOffset, 0);
        mXLocationOffset = (int) a.getDimension(it.moondroid.seekbarhint.library.R.styleable.SeekBarHint_xOffset, 0);
        mPopupStyle = a.getInt(it.moondroid.seekbarhint.library.R.styleable.SeekBarHint_popupStyle, POPUP_FIXED);

        a.recycle();

        setOnSeekBarChangeListener(this);

        initHintPopup();
    }

    public void setPopupStyle(int style) {
        mPopupStyle = style;
    }

    public int getPopupStyle() {
        return mPopupStyle;
    }

    private void initHintPopup() {
        String popupText = null;

        if (mProgressChangeListener != null) {
            popupText = mProgressChangeListener.onHintTextChanged(this, getProgress());
        }

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View undoView = inflater.inflate(mPopupLayout, null);
        mPopupTextView = (TextView) undoView.findViewById(it.moondroid.seekbarhint.library.R.id.text);
        mPopupTextView.setText(popupText != null ? popupText : String.valueOf(getProgress()));

        mPopup = new PopupWindow(undoView, mPopupWidth, ViewGroup.LayoutParams.WRAP_CONTENT, false);

        mPopup.setAnimationStyle(it.moondroid.seekbarhint.library.R.style.fade_animation);

    }

    private void showPopup() {

        if (mPopupStyle == POPUP_FOLLOW) {
            mPopup.showAtLocation(this, Gravity.LEFT | Gravity.BOTTOM, getXPosition(), getYPosition(this));
        }
        if (mPopupStyle == POPUP_FIXED) {
            mPopup.showAtLocation(this, Gravity.CENTER | Gravity.BOTTOM, 0, getYPosition(this));
        }
    }

    private int getXPosition() {
        int textWidth = mPopupWidth;
        float textCenter = (textWidth / 2.0f);
        int x = (int) (this.getX() + textCenter + mXLocationOffset + this.getWidth());
        return x;
    }

    private int getYPosition(SeekBar seekBar) {
        int y = (int) (this.getY() + mYLocationOffset + (int) getYOffset(seekBar));
        return y;
    }

    private void hidePopup() {
        if (mPopup.isShowing()) {
            mPopup.dismiss();
        }
    }

    public void setHintView(View view) {
        //TODO
        //initHintPopup();
    }

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        if (mInternalListener == null) {
            mInternalListener = l;
            super.setOnSeekBarChangeListener(l);
        } else {
            mExternalListener = l;
        }
    }

    public void setOnProgressChangeListener(OnSeekBarHintProgressChangeListener l) {
        mProgressChangeListener = l;
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
        String popupText = null;
        int newProgress = invertProgress(seekBar, progress);

        if (mProgressChangeListener != null) {
            popupText = mProgressChangeListener.onHintTextChanged(this, newProgress);
        }

        if (mExternalListener != null) {
            mExternalListener.onProgressChanged(seekBar, newProgress, b);
        }

        mPopupTextView.setText(popupText != null ? popupText : String.valueOf(newProgress));

        if (mPopupStyle == POPUP_FOLLOW) {
            int x = getXPosition();
            int y = getYPosition(seekBar);
            mPopup.update(x, y, -1, -1);
            Logger.i(TAG,"(x,y)= " + x + "," + y);
        }

    }

    private int invertProgress(SeekBar seekBar, int progress) {
        int newProgress = seekBar.getMax() - progress;
        return limitProgress(seekBar, newProgress);
    }

    private int limitProgress(SeekBar seekBar, int progress) {
        int max = seekBar.getMax();
        if(progress > max) {
            progress = max;
        }
        if(progress < 0) {
            progress = 0;
        }
        return progress;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if (mExternalListener != null) {
            mExternalListener.onStartTrackingTouch(seekBar);
        }

        showPopup();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mExternalListener != null) {
            mExternalListener.onStopTrackingTouch(seekBar);
        }

        hidePopup();
    }

    private float getYOffset(SeekBar seekBar) {
        float progress = (float) limitProgress( seekBar, seekBar.getProgress());
        int seekBarHeight = seekBar.getHeight();
        int seekBarThumbOffset = seekBar.getThumbOffset();
        float maxScale = (float) (seekBarHeight - 2 * seekBarThumbOffset);
        float position = (progress * maxScale / seekBar.getMax());
        float offset = seekBarThumbOffset;

        float newY = position + offset;
        return newY;
    }
}

