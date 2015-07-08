package com.door43.translationstudio.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.tasks.ThreadableUI;
import com.door43.util.reporting.Logger;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This dialog display a list of all the error logs
 */
public class ErrorLogDialog  extends DialogFragment{

    private ErrorLogAdapter mAdapter;
    private ThreadableUI mThread;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle("Logs");
        View v = inflater.inflate(R.layout.dialog_error_log, container, false);

        ListView list = (ListView)v.findViewById(R.id.errorLogListView);
        mAdapter = new ErrorLogAdapter(AppContext.context());
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
                Logger.getLogFile().delete();
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

        final Handler handle = new Handler(Looper.getMainLooper());
        mThread = new ThreadableUI(getActivity()) {
            private int numLogs = 0;
            /**
             * updates the ui
             * @param log
             * @param details
             */
            private void saveLog(Logger.ErrorLog log, String details) {
                numLogs ++;
                log.setDetails(details.trim());

                // update the ui
                final Logger.ErrorLog staticLog = log;
                handle.post(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.addItem(staticLog);
                    }
                });
            }

            @Override
            public void onStop() {

            }

            @Override
            public void run() {
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(Logger.getLogFile())));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    Pattern pattern = Pattern.compile(Logger.PATTERN);
                    Logger.ErrorLog log = null;
                    while((line = br.readLine()) != null) {
                        if(isInterrupted()) break;
                        Matcher match = pattern.matcher(line);
                        if(match.find()) {
                            // save log
                            if(log != null) {
                                saveLog(log, sb.toString());
                                sb.setLength(0);
                            }
                            // start new log
                            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yy hh:mm a");
                            log = new Logger.ErrorLog(format.parse(match.group(1)), Logger.Level.getLevel(match.group(2)), match.group(3), match.group(5));
                        } else {
                            // build log details
                            sb.append(line);
                        }
                    }
                    // save the last log
                    if(log != null) {
                        saveLog(log, sb.toString());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onPostExecute() {
                if(numLogs == 0) {
                    AppContext.context().showToastMessage("There are no logs");
                    dismiss();
                }
            }
        };
        mThread.start();
    }
}
