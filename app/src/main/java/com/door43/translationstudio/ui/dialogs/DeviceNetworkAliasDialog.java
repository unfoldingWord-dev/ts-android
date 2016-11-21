package com.door43.translationstudio.ui.dialogs;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;

/**
 * Created by joel on 11/25/2015.
 */
public class DeviceNetworkAliasDialog extends DialogFragment {

    private OnDismissListener listener = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_device_network_alias, container, false);

        final EditText deviceName = (EditText)v.findViewById(R.id.device_name);
        deviceName.setText(App.getDeviceNetworkAlias());

        Button cancelButton = (Button)v.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        Button confirmButton = (Button)v.findViewById(R.id.confirm_button);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                App.setDeviceNetworkAlias(deviceName.getText().toString());
                dismiss();
            }
        });

        return v;
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        if(listener != null) {
            listener.onDismiss();
        }
        super.onDismiss(dialogInterface);
    }

    public void setOnDismissListener(OnDismissListener listener) {
        this.listener = listener;
    }

    public interface OnDismissListener {
        void onDismiss();
    }

    public void onDestroy() {
        listener = null;
        super.onDestroy();
    }
}
