package com.door43.translationstudio.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.door43.translationstudio.R;
import com.door43.translationstudio.util.MainContext;
import com.door43.translationstudio.util.ModalDialog;

/**
 * Created by joel on 10/15/2014.
 */
public class ExportSDOptions extends ModalDialog {
    ExportSDOptions me = this;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_export_sd_options, container, false);

        // TODO: hook up options

        return v;
    }
}
