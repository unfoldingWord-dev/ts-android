package com.door43.translationstudio.dialogs;

import android.app.DialogFragment;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.door43.translationstudio.MainActivity;
import com.door43.translationstudio.R;
import com.door43.translationstudio.spannables.PassageNoteSpan;
import com.door43.translationstudio.util.MainContext;
import com.door43.translationstudio.util.PassageNoteEvent;

/**
 * Created by joel on 11/3/2014.
 */
public class PassageNoteDialog extends DialogFragment {
    private PassageNoteDialog me = this;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int style = DialogFragment.STYLE_NORMAL, theme = 0;
        setStyle(style, theme);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.label_passage_note);
        View v = inflater.inflate(R.layout.dialog_passage_note, container, false);

        Bundle args = getArguments();

        // the span id
        final String id = args.getString("id");
        Boolean isFootnote = args.getBoolean("footnote");

        // load values
        final EditText passageText = (EditText)v.findViewById(R.id.passageEditText);
        passageText.setText(args.getString("passage"));
        final EditText passageNoteText = (EditText)v.findViewById(R.id.passageNoteEditText);
        passageNoteText.setText(args.getString("note"));
        final Switch footnoteSwitch = (Switch)v.findViewById(R.id.passageIsFootnoteSwitch);
        footnoteSwitch.setChecked(isFootnote);

        // hook up buttons
        Button cancelBtn = (Button)v.findViewById(R.id.cancelButton);
        Button deleteBtn = (Button)v.findViewById(R.id.deleteButton);
        Button okBtn = (Button)v.findViewById(R.id.okButton);

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainContext.getEventBus().post(new PassageNoteEvent(me, PassageNoteEvent.Status.CANCEL, passageText.getText().toString(), passageNoteText.getText().toString(), id, footnoteSwitch.isChecked()));
            }
        });
        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainContext.getEventBus().post(new PassageNoteEvent(me, PassageNoteEvent.Status.DELETE, passageText.getText().toString(), passageNoteText.getText().toString(), id, footnoteSwitch.isChecked()));
            }
        });
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // strip out invalid characters
                String safeNote = passageNoteText.getText().toString().replace('<', ' ').replace('>', ' ').replace('"', '\'').replace('(', ' ').replace(')', ' ');
                MainContext.getEventBus().post(new PassageNoteEvent(me, PassageNoteEvent.Status.OK, passageText.getText().toString(), safeNote, id, footnoteSwitch.isChecked()));
            }
        });
        return v;
    }
}
