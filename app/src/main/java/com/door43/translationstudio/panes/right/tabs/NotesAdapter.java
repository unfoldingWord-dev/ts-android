package com.door43.translationstudio.panes.right.tabs;

import android.app.ProgressDialog;
import android.content.Context;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;

import com.door43.translationstudio.MainActivity;
import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Chapter;
import com.door43.translationstudio.projects.Frame;
import com.door43.translationstudio.projects.Navigator;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.Translation;
import com.door43.translationstudio.projects.TranslationNote;
import com.door43.translationstudio.rendering.LinkRenderer;
import com.door43.translationstudio.spannables.PassageLinkSpan;
import com.door43.translationstudio.spannables.Span;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.ClearableEditText;

import org.eclipse.jgit.diff.Edit;

/**
 * Created by joel on 5/7/2015.
 */
public class NotesAdapter extends BaseAdapter {

    private final Context mContext;
    private TranslationNote mTranslationNote;
    private boolean mRenderTranslations = false;

    public NotesAdapter(Context context) {
        mContext = context;
    }

    public void changeDataset(TranslationNote note) {
        mTranslationNote = note;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        if(mTranslationNote != null) {
            return mTranslationNote.getNotes().size();
        } else {
            return 0;
        }
    }

    @Override
    public TranslationNote.Note getItem(int position) {
        return mTranslationNote.getNotes().get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder holder = new ViewHolder();

        if(convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.fragment_pane_right_resources_note_item, null);
            holder.title = (TextView) v.findViewById(R.id.translationNoteReferenceText);
            holder.text = (TextView) v.findViewById(R.id.translationNoteText);
            holder.titleTranslation = (ClearableEditText) v.findViewById(R.id.translationNoteReferenceEditText);
            holder.textTranslation = (ClearableEditText) v.findViewById(R.id.translationNoteEditText);
            v.setTag(holder);
        } else {
            holder = (ViewHolder)v.getTag();
        }

        // render links
        // TODO: this should be placed in a task
        LinkRenderer renderingEngine = new LinkRenderer(new LinkRenderer.OnPreprocessLink() {
            @Override
            public boolean onPreprocess(PassageLinkSpan span) {
                return getLinkEndpoint(span) != null;
            }
        }, new Span.OnClickListener() {
            @Override
            public void onClick(View view, Span span, int start, int end) {
                PassageLinkSpan link = (PassageLinkSpan) span;
                final Frame f = getLinkEndpoint(link);
                if (f != null) {
                    final ProgressDialog dialog = new ProgressDialog(mContext);
                    dialog.setMessage(mContext.getResources().getString(R.string.loading_project_chapters));
                    dialog.show();
                    AppContext.navigator().open(f.getChapter().getProject(), new Navigator.OnSuccessListener() {
                        @Override
                        public void onSuccess() {
                            AppContext.navigator().open(f.getChapter());
                            AppContext.navigator().open(f);
                            dialog.dismiss();
                        }

                        @Override
                        public void onFailed() {
                            dialog.dismiss();
                        }
                    });
                    ((MainActivity) mContext).closeDrawers();
                }
            }
        });

        if(mRenderTranslations) {
            holder.title.setVisibility(View.GONE);
            holder.text.setVisibility(View.GONE);
            holder.titleTranslation.setVisibility(View.VISIBLE);
            holder.textTranslation.setVisibility(View.VISIBLE);

            if(getItem(position).getRefTranslation() != null) {
                holder.titleTranslation.setText(getItem(position).getRefTranslation().getText());
            }
            if(getItem(position).getTextTranslation() != null) {
                CharSequence translationText = renderingEngine.render(Html.fromHtml(getItem(position).getTextTranslation().getText()));
                holder.textTranslation.setText(translationText);
            }
        } else {
            holder.title.setVisibility(View.VISIBLE);
            holder.text.setVisibility(View.VISIBLE);
            holder.titleTranslation.setVisibility(View.GONE);
            holder.textTranslation.setVisibility(View.GONE);
        }

        // title
        holder.title.setText(getItem(position).getRef());
        holder.titleTranslation.setHint(getItem(position).getRef());

        // definition
        // TODO: we might want to use an HtmlTextView instead
        CharSequence text = renderingEngine.render(Html.fromHtml(getItem(position).getText()));
        holder.text.setText(text);
        holder.textTranslation.setHint(text);

        // make links clickable
        MovementMethod mmNotes = holder.text.getMovementMethod();
        if ((mmNotes == null) || !(mmNotes instanceof LinkMovementMethod)) {
            if (holder.text.getLinksClickable()) {
                holder.text.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
        MovementMethod mmNotesTranslation = holder.textTranslation.getMovementMethod();
        if ((mmNotes == null) || !(mmNotes instanceof LinkMovementMethod)) {
            if (holder.textTranslation.getLinksClickable()) {
                holder.textTranslation.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
        // TODO: apply the correct font


        // set up change listeners
        holder.textTranslation.clearTextChangedListeners();
        holder.textTranslation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                getItem(position).setDefinitionTranslation(s.toString());
                getItem(position).save();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        holder.titleTranslation.clearTextChangedListeners();
        holder.titleTranslation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                getItem(position).setReferenceTranslation(s.toString());
                getItem(position).save();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        return v;
    }

    /**
     * Returns the link endpoint
     * @deprecated
     * @param link
     * @return
     */
    private Frame getLinkEndpoint(PassageLinkSpan link) {
        Project p = AppContext.projectManager().getProject(link.getProjectId());
        if(p != null) {
            Chapter c = p.getChapter(link.getChapterId());
            if(c != null) {
                return c.getFrame(link.getFrameId());
            }
        }
        return null;
    }

    /**
     * Returns how the notes are rendered
     * @return
     */
    public boolean getRenderTranslations() {
        return mRenderTranslations;
    }

    /**
     * Sets whether the notes or their translations should be rendered
     * @param renderTranslations
     */
    public void setRenderTranslations(boolean renderTranslations) {
        mRenderTranslations = renderTranslations;
        notifyDataSetChanged();
    }

    private class ViewHolder {
        public TextView title;
        public TextView text;
        public ClearableEditText titleTranslation;
        public ClearableEditText textTranslation;
    }
}
