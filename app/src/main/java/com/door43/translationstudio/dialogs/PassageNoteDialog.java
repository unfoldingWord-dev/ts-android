package com.door43.translationstudio.dialogs;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

import com.door43.translationstudio.R;
import com.door43.translationstudio.spannables.NoteSpan;
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
        NoteSpan.NoteType noteType = NoteSpan.NoteType.values()[args.getInt("noteType")];

        // load values
        final EditText passageText = (EditText)v.findViewById(R.id.passageEditText);
        passageText.setText(args.getString("passage"));
        final EditText passageNoteText = (EditText)v.findViewById(R.id.passageNoteEditText);
        passageNoteText.setText(args.getString("note"));
        final Switch footnoteSwitch = (Switch)v.findViewById(R.id.passageIsFootnoteSwitch);
//        footnoteSwitch.setChecked(noteType == NoteSpan.NoteType.Footnote);
        // TODO: each project will need to have it's own settings about what features it has. Then we can show footnotes according to these settings.
        // for now since we only have one project we just disable all the footnotes.
        footnoteSwitch.setChecked(false);
        footnoteSwitch.setVisibility(View.GONE);

        // set up fonts
        passageNoteText.setTypeface(MainContext.getContext().getTranslationTypeface());
        passageText.setTypeface(MainContext.getContext().getTranslationTypeface());

        // hook up buttons
        Button cancelBtn = (Button)v.findViewById(R.id.cancelButton);
        Button deleteBtn = (Button)v.findViewById(R.id.deleteButton);
        Button okBtn = (Button)v.findViewById(R.id.okButton);

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NoteSpan.NoteType noteType = footnoteSwitch.isChecked() ? NoteSpan.NoteType.Footnote : NoteSpan.NoteType.UserNote;
                MainContext.getEventBus().post(new PassageNoteEvent(me, PassageNoteEvent.Status.CANCEL, passageText.getText().toString(), passageNoteText.getText().toString(), id, noteType));
            }
        });
        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NoteSpan.NoteType noteType = footnoteSwitch.isChecked() ? NoteSpan.NoteType.Footnote : NoteSpan.NoteType.UserNote;
                MainContext.getEventBus().post(new PassageNoteEvent(me, PassageNoteEvent.Status.DELETE, passageText.getText().toString(), passageNoteText.getText().toString(), id, noteType));
            }
        });
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // strip out invalid characters
                String safeNote = passageNoteText.getText().toString().replace('<', ' ').replace('>', ' ').replace('"', '\'').replace('(', ' ').replace(')', ' ');
                NoteSpan.NoteType noteType = footnoteSwitch.isChecked() ? NoteSpan.NoteType.Footnote : NoteSpan.NoteType.UserNote;
                MainContext.getEventBus().post(new PassageNoteEvent(me, PassageNoteEvent.Status.OK, passageText.getText().toString(), safeNote, id, noteType));
            }
        });
        return v;
    }
}
