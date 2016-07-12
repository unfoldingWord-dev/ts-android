package com.door43.translationstudio.newui.translate;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
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
 * modified SeekBarHint to work with VerticalSeekBar
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
    private Rect mSeekbarRectangle;

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

        setOnSeekBarChangeListener(this);

        TypedArray a = context.obtainStyledAttributes(attrs, it.moondroid.seekbarhint.library.R.styleable.SeekBarHint);

        mPopupLayout = a.getResourceId(it.moondroid.seekbarhint.library.R.styleable.SeekBarHint_popupLayout, it.moondroid.seekbarhint.library.R.layout.popup);
        mPopupWidth = (int) a.getDimension(it.moondroid.seekbarhint.library.R.styleable.SeekBarHint_popupWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        mYLocationOffset = (int) a.getDimension(it.moondroid.seekbarhint.library.R.styleable.SeekBarHint_yOffset, 0);
        mXLocationOffset = (int) a.getDimension(it.moondroid.seekbarhint.library.R.styleable.SeekBarHint_xOffset, 0);
        mPopupStyle = a.getInt(it.moondroid.seekbarhint.library.R.styleable.SeekBarHint_popupStyle, POPUP_FIXED);

        a.recycle();
        initHintPopup();
    }

    public void setPopupStyle(int style) {
        mPopupStyle = style;
    }

    public int getPopupStyle() {
        return mPopupStyle;
    }

    private void initHintPopup() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View undoView = inflater.inflate(mPopupLayout, null);
        mPopupTextView = (TextView) undoView.findViewById(it.moondroid.seekbarhint.library.R.id.text);

        initPopupText();

        mSeekbarRectangle = new Rect();
        this.getGlobalVisibleRect(mSeekbarRectangle);
        Log.d(TAG,"initHintPopup: Rect=" + mSeekbarRectangle);

        mPopup = new PopupWindow(undoView, mPopupWidth, ViewGroup.LayoutParams.WRAP_CONTENT, false);
        mPopup.setAnimationStyle(it.moondroid.seekbarhint.library.R.style.fade_animation);
    }

    private void initPopupText() {
        String popupText = null;
        if (mProgressChangeListener != null) {
            popupText = mProgressChangeListener.onHintTextChanged(this, getProgress());
        }
        if(popupText == null) {
            popupText = String.valueOf(getProgress());
        }
        mPopupTextView.setText( popupText );
        Log.d(TAG,"initPopupText: popupText=" + popupText);
    }

    private void showPopup() {
        getMeasurements();
        initPopupText();

        if (mPopupStyle == POPUP_FOLLOW) {
            int xPosition = getXPosition();
            int yPosition = getYPosition(this);
            Log.d(TAG,"showPopup: show Hint at =" + xPosition + "," + yPosition);
            mPopup.showAtLocation(this, Gravity.LEFT | Gravity.BOTTOM, xPosition, yPosition);
        }
        if (mPopupStyle == POPUP_FIXED) {
            int xPosition = getXPosition();
            int yPosition = 0;
            Log.d(TAG,"showPopup: show Hint at =" + xPosition + "," + yPosition);
            mPopup.showAtLocation(this, Gravity.LEFT | Gravity.CENTER, xPosition, yPosition);
        }
    }

    private void getMeasurements() {
        mSeekbarRectangle = new Rect();
        this.getGlobalVisibleRect(mSeekbarRectangle);
        Log.d(TAG,"getMeasurements: Rect=" + mSeekbarRectangle);
    }

    private int getXPosition() {
        int textWidth = mPopupWidth;
        float textCenter = (textWidth / 2.0f);
        int x = (int) (this.getX() + textCenter + mXLocationOffset + this.getWidth());
        Log.d(TAG,"mXLocationOffset: " + mXLocationOffset);
        Log.d(TAG,"getWidth(): " + this.getWidth());
        Log.d(TAG,"getXPosition: " + x);
        return x;
    }

    private int getYPosition(SeekBar seekBar) {
//        int y = (int) (this.getY() + mYLocationOffset + (int) getYOffset(seekBar));
        int y = mSeekbarRectangle.top + mYLocationOffset + (int) getYOffset(seekBar);
        Log.d(TAG,"mYLocationOffset: " + mYLocationOffset);
        Log.d(TAG,"getYPosition: " + y);
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

        if (mProgressChangeListener != null) {
            popupText = mProgressChangeListener.onHintTextChanged(this, progress);
        }

        if (mExternalListener != null) {
            mExternalListener.onProgressChanged(seekBar, progress, b);
        }

        if(popupText == null) {
            popupText = String.valueOf(getProgress());
        }
        mPopupTextView.setText( popupText );
        Log.d(TAG,"onProgressChanged: popupText=" + popupText);

        if (mPopupStyle == POPUP_FOLLOW) {
            getMeasurements();
            int x = getXPosition();
            int y = getYPosition(seekBar);
            mPopup.update(x, y, -1, -1);
            Logger.i(TAG,"onProgressChanged: new Hint =" + x + "," + y);
        }
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
        Log.d(TAG,"getYOffset: progress=" + progress);
        int seekBarMax = seekBar.getMax();
        Log.d(TAG,"getYOffset: seekBarMax=" + seekBarMax);
        int seekBarHeight = seekBar.getHeight();
        int seekBarThumbOffset = seekBar.getThumbOffset();
        Log.d(TAG,"getYOffset: seekBarThumbOffset=" + seekBarThumbOffset);
        float maxScale = (float) (seekBarHeight - 2 * seekBarThumbOffset);
        float position = (progress * maxScale / seekBarMax);
        Log.d(TAG,"getYOffset: position=" + position);
        float offset = seekBarThumbOffset;
        Log.d(TAG,"getYOffset: offset=" + offset);

        int height = mPopup.getHeight();
        int center = height / 2;

        float newY = position + offset + center;
        Log.d(TAG,"getYOffset: newY=" + newY);
        return newY;
    }
}

