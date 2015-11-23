package com.door43.translationstudio.dialogs;


import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.door43.translationstudio.R;

/**
 * Created by blm on 11/23/15.
 */

public class tsAlertDialogBuilder extends AlertDialog.Builder {

    private final Context mContext;
    private TextView mTitle;
    private ImageView mIcon;
    private TextView mMessage;

    public tsAlertDialogBuilder(Context context) {
        super(context);
        mContext = context;

        View customTitle = View.inflate(mContext, R.layout.alert_dialog_title, null);
        mTitle = (TextView) customTitle.findViewById(R.id.alertTitle);
        mIcon = (ImageView) customTitle.findViewById(R.id.icon);
        setCustomTitle(customTitle);

        View customMessage = View.inflate(mContext, R.layout.alert_dialog_message, null);
        mMessage = (TextView) customMessage.findViewById(R.id.message);
        setView(customMessage);
    }

    @Override
    public tsAlertDialogBuilder setTitle(int textResId) {
        mTitle.setText(textResId);
        return this;
    }
    @Override
    public tsAlertDialogBuilder setTitle(CharSequence text) {
        mTitle.setText(text);
        return this;
    }

    @Override
    public tsAlertDialogBuilder setMessage(int textResId) {
        mMessage.setText(textResId);
        return this;
    }

    @Override
    public tsAlertDialogBuilder setMessage(CharSequence text) {
        mMessage.setText(text);
        return this;
    }

    @Override
    public tsAlertDialogBuilder setIcon(int drawableResId) {
        mIcon.setImageResource(drawableResId);
        return this;
    }

    @Override
    public tsAlertDialogBuilder setIcon(Drawable icon) {
        mIcon.setImageDrawable(icon);
        return this;
    }

}