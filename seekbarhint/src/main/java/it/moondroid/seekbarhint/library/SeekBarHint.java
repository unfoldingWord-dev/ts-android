package it.moondroid.seekbarhint.library;


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

/**
 * 7/1/2016
 * added custom layout and X offset for popup
 */
public class SeekBarHint extends SeekBar implements SeekBar.OnSeekBarChangeListener {

    public static final String TAG = SeekBarHint.class.getSimpleName();
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
        public String onHintTextChanged(SeekBarHint seekBarHint, int progress);
    }

    public SeekBarHint(Context context) {
        super(context);
        init(context, null);
    }

    public SeekBarHint(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public SeekBarHint(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {

        setOnSeekBarChangeListener(this);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SeekBarHint);

        mPopupLayout = a.getResourceId(it.moondroid.seekbarhint.library.R.styleable.SeekBarHint_popupLayout, R.layout.popup);
        mPopupWidth = (int) a.getDimension(R.styleable.SeekBarHint_popupWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        mYLocationOffset = (int) a.getDimension(R.styleable.SeekBarHint_yOffset, 0);
        mXLocationOffset = (int) a.getDimension(R.styleable.SeekBarHint_xOffset, 0);
        mPopupStyle = a.getInt(R.styleable.SeekBarHint_popupStyle, POPUP_FIXED);

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
        mPopupTextView = (TextView) undoView.findViewById(R.id.text);

        initPopupText();

        mSeekbarRectangle = new Rect();
        this.getGlobalVisibleRect(mSeekbarRectangle);
        Log.d(TAG,"initHintPopup: Rect=" + mSeekbarRectangle);

        mPopup = new PopupWindow(undoView, mPopupWidth, ViewGroup.LayoutParams.WRAP_CONTENT, false);
        mPopup.setAnimationStyle(R.style.fade_animation);
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
            int yPosition = getYPosition();
            int xPosition = getXPosition(this);
            Log.d(TAG,"showPopup: show Hint at =" + xPosition + "," + yPosition);
            mPopup.showAtLocation(this, Gravity.LEFT | Gravity.BOTTOM, getXPosition(this), getYPosition());
        }
        if (mPopupStyle == POPUP_FIXED) {
            int yPosition = getYPosition();
            int xPosition = 0;
            Log.d(TAG,"showPopup: show Hint at =" + xPosition + "," + yPosition);
            mPopup.showAtLocation(this, Gravity.CENTER | Gravity.BOTTOM, 0, yPosition);
        }
    }

    private void getMeasurements() {
        mSeekbarRectangle = new Rect();
        this.getGlobalVisibleRect(mSeekbarRectangle);
        Log.d(TAG,"getMeasurements: Rect=" + mSeekbarRectangle);
    }

    private int getXPosition(SeekBar seekBar) {
//        int location[] = new int[2];
//        this.getLocationOnScreen(location);
//        float x = this.getX();
        int x = mSeekbarRectangle.left + mXLocationOffset + (int) getXOffset(seekBar);
        Log.d(TAG,"mXLocationOffset: " + mXLocationOffset);
        Log.d(TAG,"getXPosition: " + x);
        return x;
    }

    private int getYPosition() {
//        int y = mSeekbarRectangle.top + mYLocationOffset + this.getHeight();
        int y = (int) this.getY() + mYLocationOffset + this.getHeight();
        Log.d(TAG,"mYLocationOffset: " + mYLocationOffset);
        Log.d(TAG,"getHeight(): " + this.getHeight());
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
            popupText = mProgressChangeListener.onHintTextChanged(this, getProgress());
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
            int yPosition = getYPosition();
            int xPosition = getXPosition(seekBar);
            Log.d(TAG,"onProgressChanged: new Hint =" + xPosition + "," + yPosition);
            mPopup.update(xPosition, yPosition, -1, -1);
        }
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

    private float getXOffset(SeekBar seekBar) {
        int progress = seekBar.getProgress();
        Log.d(TAG,"getXOffset: progress=" + progress);
        int seekBarMax = seekBar.getMax();
        Log.d(TAG,"getXOffset: seekBarMax=" + seekBarMax);
        int seekBarThumbOffset = seekBar.getThumbOffset();
        Log.d(TAG,"getXOffset: seekBarThumbOffset=" + seekBarThumbOffset);
        float xPosition = (( progress * (float) (seekBar.getWidth() - 2 * seekBarThumbOffset)) / seekBarMax);
        Log.d(TAG,"getXOffset: xPosition=" + xPosition);
        float offset = seekBarThumbOffset;
        Log.d(TAG,"getXOffset: offset=" + offset);

        int textWidth = mPopupWidth;
        float textCenter = (textWidth / 2.0f);

        float newX = xPosition + offset + textCenter;
        Log.d(TAG,"getXOffset: newX=" + newX);
        return newX;
    }
}
