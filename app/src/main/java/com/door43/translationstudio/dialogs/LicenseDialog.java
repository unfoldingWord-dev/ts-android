package com.door43.translationstudio.dialogs;

import android.app.DialogFragment;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.util.FileUtilities;
import com.door43.translationstudio.util.MainContext;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by joel on 9/29/2014.
 */
public class LicenseDialog extends DialogFragment {
    private final LicenseDialog me = this;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int style = DialogFragment.STYLE_NO_TITLE, theme = 0;
        setStyle(style, theme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_license, container, false);
        TextView licenseText = (TextView) v.findViewById(R.id.license_text);

        // load the license html
        try {
            InputStream is = MainContext.getContext().getAssets().open("license.html");
            String licenseString = FileUtilities.convertStreamToString(is);
            // display license text
            licenseText.setText(Html.fromHtml(licenseString));
        } catch (IOException e) {
            e.printStackTrace();
            licenseText.setText(Html.fromHtml("Failed to load the license file"));
        } catch (Exception e) {
            e.printStackTrace();
            licenseText.setText(Html.fromHtml("Failed to parse the license file"));
        }

        // enable button
        Button dismissBtn = (Button)v.findViewById(R.id.dismiss_license_btn);
        dismissBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                me.dismiss();
            }
        });
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        // safety check
        if (getDialog() == null) {
            return;
        }

        getDialog().getWindow().setLayout(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT);

    }
}
