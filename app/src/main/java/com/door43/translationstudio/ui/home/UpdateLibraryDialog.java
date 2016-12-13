package com.door43.translationstudio.ui.home;

import android.app.DialogFragment;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.door43.util.EventBuffer;
import com.door43.translationstudio.R;

/**
 * Created by joel on 10/6/16.
 */

public class UpdateLibraryDialog extends DialogFragment implements EventBuffer.OnEventTalker {
    public static final int EVENT_UPDATE_LANGUAGES = 1;
    public static final int EVENT_UPDATE_SOURCE = 2;
    public static final int EVENT_UPDATE_ALL = 3;
    public static final int EVENT_SELECT_DOWNLOAD_SOURCES = 4;
    public static final int EVENT_UPDATE_APP = 5;
    public static final String TAG = "update-library-dialog";
    private EventBuffer eventBuffer = new EventBuffer();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.dialog_update_library, container, false);

        v.findViewById(R.id.infoButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://help.door43.org/en/knowledgebase/9-translationstudio/docs/5-update-options"));
                startActivity(browserIntent);
            }
        });

        v.findViewById(R.id.update_languages).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eventBuffer.write(UpdateLibraryDialog.this, EVENT_UPDATE_LANGUAGES, null);
            }
        });
        v.findViewById(R.id.download_sources).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eventBuffer.write(UpdateLibraryDialog.this, EVENT_SELECT_DOWNLOAD_SOURCES, null);
            }
        });
        v.findViewById(R.id.update_source).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eventBuffer.write(UpdateLibraryDialog.this, EVENT_UPDATE_SOURCE, null);
            }
        });

        TextView updateAppLabel = (TextView) v.findViewById(R.id.update_app_text);
        updateAppLabel.setPaintFlags(updateAppLabel.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        v.findViewById(R.id.update_app).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eventBuffer.write(UpdateLibraryDialog.this, EVENT_UPDATE_APP, null);
            }
        });
        v.findViewById(R.id.dismiss_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        return v;
    }

    @Override
    public void onDestroy() {
        eventBuffer.removeAllListeners();
        super.onDestroy();
    }

    @Override
    public EventBuffer getEventBuffer() {
        return eventBuffer;
    }
}
