package com.door43.translationstudio.ui.legal;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;

import com.door43.translationstudio.ui.BaseActivity;

public class LegalDocumentActivity extends BaseActivity {

    public static final String ARG_RESOURCE = "arg_resource_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getIntent().getExtras();
        int resourceId = 0;
        if(args != null) {
            resourceId = args.getInt(ARG_RESOURCE, 0);
        }
        if(resourceId == 0) {
            finish();
            return;
        }

        LegalDocumentDialog dialog = new LegalDocumentDialog();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        dialog.setArguments(getIntent().getExtras());
        dialog.show(ft, "dialog");
    }
}
