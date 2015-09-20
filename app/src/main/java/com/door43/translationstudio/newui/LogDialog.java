package com.door43.translationstudio.newui;

/**
 * Created by joel on 9/19/2015.
 */

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.dialogs.LogAdapter;

import java.util.List;

/**
 * This dialog display a list of all the error logs
 */
public class LogDialog extends DialogFragment {

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_log, container, false);

        List<Logger.Entry> logs = Logger.getLogEntries();
        if(logs.size() > 0) {
            ListView list = (ListView) v.findViewById(R.id.log_list);
            final LogAdapter mAdapter = new LogAdapter();
            mAdapter.setItems(logs);
            list.setAdapter(mAdapter);
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    String details = mAdapter.getItem(i).getDetails();
                    if (LogDialog.this.getActivity() != null && details != null && !details.isEmpty()) {
                        Dialog dialog = new Dialog(LogDialog.this.getActivity());
                        TextView text = new TextView(LogDialog.this.getActivity());
                        text.setText(details);
                        text.setVerticalScrollBarEnabled(true);
                        text.setPadding(10, 0, 10, 0);
                        text.setMovementMethod(ScrollingMovementMethod.getInstance());
                        text.canScrollVertically(View.SCROLL_AXIS_VERTICAL);
                        dialog.setContentView(text);
                        dialog.requestWindowFeature(STYLE_NO_TITLE);
                        dialog.show();
                    }
                }
            });

            // close
            Button dismissButton = (Button) v.findViewById(R.id.dismiss_button);
            dismissButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dismiss();
                }
            });

        } else {
            // there are no logs
            dismiss();
        }
        return v;
    }

}