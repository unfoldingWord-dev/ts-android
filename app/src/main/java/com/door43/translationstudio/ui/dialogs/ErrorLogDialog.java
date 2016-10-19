package com.door43.translationstudio.ui.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.unfoldingword.tools.logger.LogEntry;
import org.unfoldingword.tools.logger.Logger;
import com.door43.translationstudio.R;
import org.unfoldingword.tools.taskmanager.ThreadableUI;

import java.util.ArrayList;
import java.util.List;


/**
 * This dialog display a list of all the error logs
 */
public class ErrorLogDialog  extends DialogFragment{

    public static final String ARG_LOG_TEXT = "arg_log_text";
    private LogAdapter mAdapter;
    private ThreadableUI mThread;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle("Logs");
        View v = inflater.inflate(R.layout.dialog_error_log, container, false);

        ListView list = (ListView)v.findViewById(R.id.errorLogListView);
        mAdapter = new LogAdapter();
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String details = mAdapter.getItem(i).getDetails();
                if(ErrorLogDialog.this.getActivity() != null && details != null && !details.isEmpty()) {
                    Dialog dialog = new Dialog(ErrorLogDialog.this.getActivity());
                    TextView text = new TextView(ErrorLogDialog.this.getActivity());
                    text.setText(details);
                    text.setVerticalScrollBarEnabled(true);
                    text.setPadding(10, 0, 10, 0);
                    text.setMovementMethod(ScrollingMovementMethod.getInstance());
                    text.canScrollVertically(View.SCROLL_AXIS_VERTICAL);
                    dialog.setContentView(text);
                    dialog.setTitle("Log Details");
                    dialog.show();
                }
            }
        });

        // close
        Button dismissButton = (Button)v.findViewById(R.id.dismissButton);
        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        // empty log
        Button emtpyButton = (Button)v.findViewById(R.id.emptyLogButton);
        emtpyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Logger.flush();
                dismiss();
            }
        });

        init();

        return v;
    }

    /**
     * Loads the error logs from the disk
     */
    private void init() {
        if(mThread != null) {
            mThread.stop();
        }

        if(getActivity() == null) {
            dismiss();
            return;
        }

        mThread = new ThreadableUI(getActivity()) {
            private List<LogEntry> mLogs = new ArrayList<>();

            @Override
            public void onStop() {

            }

            @Override
            public void run() {
                mLogs = Logger.getLogEntries();
            }

            @Override
            public void onPostExecute() {
                if(mLogs.size() > 0) {
                    mAdapter.setItems(mLogs);
                } else {
                    Toast toast = Toast.makeText(getActivity(), "There are no logs", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.TOP, 0, 0);
                    toast.show();
                    dismiss();
                }
            }
        };
        mThread.start();
    }
}
