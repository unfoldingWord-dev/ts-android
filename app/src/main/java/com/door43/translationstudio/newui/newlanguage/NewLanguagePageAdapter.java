package com.door43.translationstudio.newui.newlanguage;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.NewLanguagePage;
import com.door43.translationstudio.core.NewLanguageQuestion;


/**
 * Handles the rendering of the questions in NewLanguageActivity
 */
public class NewLanguagePageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final String TAG = NewLanguagePageAdapter.class.getSimpleName();
    private static final int TYPE_BOOLEAN = 1;
    private static final int TYPE_STRING = 2;
    private final Context context;
    private int mFocusedPosition = -1;
    private int mSelection = -1;
    private NewLanguagePage page;

    public NewLanguagePageAdapter(Context context) {
        this.context = context;
    }

    /**
     * Sets the questionnaire page that should be displayed
     * @param page
     */
    public void setPage(NewLanguagePage page) {
        this.page = page;
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        RecyclerView.ViewHolder vh;
        switch (viewType) {
            case TYPE_BOOLEAN:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_new_language_boolean_question, parent, false);
                vh = new BooleanViewHolder(context, v);
                break;
            case TYPE_STRING:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_new_language_text_question, parent, false);
                vh = new StringViewHolder(context, v);
                break;
            default:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_new_language_text_question, parent, false);
                vh = new StringViewHolder(context, v);
                break;
        }
        return vh;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case TYPE_BOOLEAN:
                onBindBooleanQuestion((BooleanViewHolder)holder, position);
                break;
            case TYPE_STRING:
                onBindStringQuestion((StringViewHolder)holder, position);
                break;
            default:
                onBindStringQuestion((StringViewHolder)holder, position);
                break;
        }
    }

    @Override
    public int getItemCount() {
        if(page != null) {
            return page.getNumQuestions();
        }
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        if(page.getQuestion(position).type == NewLanguageQuestion.InputType.Boolean) {
            return TYPE_BOOLEAN;
        } else if(page.getQuestion(position).type == NewLanguageQuestion.InputType.String) {
            return TYPE_STRING;
        }
        return -1;
    }

    public void onBindBooleanQuestion(BooleanViewHolder holder, int position) {
        NewLanguageQuestion question = page.getQuestion(position);
        holder.question.setText(question.question);
        holder.question.setHint(question.helpText);

        boolean answer = Boolean.parseBoolean(getQuestionAnswer(question));

        holder.radioButtonYes.setChecked(answer);
        holder.radioButtonNo.setChecked(!answer);

        if(question.reliantQuestionId > 0 && isAnswerAffirmative(page.getQuestionById(question.reliantQuestionId))) {
            holder.disable();
        } else {
            holder.enable();
        }

        holder.radioButtonYes.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO: save
                reload();
            }
        });
        holder.radioButtonNo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO: save
                reload();
            }
        });
    }

    public void onBindStringQuestion(StringViewHolder holder, int position) {
        NewLanguageQuestion question = page.getQuestion(position);
        holder.question.setText(question.question);
        holder.question.setHint(question.helpText);

        String answer = getQuestionAnswer(question);
        holder.answer.setText(answer);

        if(question.reliantQuestionId > 0 && isAnswerAffirmative(page.getQuestionById(question.reliantQuestionId))) {
            holder.disable();
        } else {
            holder.enable();
        }

        holder.answer.removeTextChangedListener(holder.textWatcher);
        holder.textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // TODO: 6/1/16 save answer
                reload();
            }
        };
        holder.answer.addTextChangedListener(holder.textWatcher);
    }

    private void reload() {
        // TODO: 6/1/16 notify callback that answers have changed
        notifyDataSetChanged();
    }

    /**
     * Checks if the question has been answered in the affirmative.
     * This means it was either yes for boolean questions or text was entered for string questions
     * @param question
     * @return
     */
    public boolean isAnswerAffirmative(NewLanguageQuestion question) {
        if(question != null) {
            // TODO: 6/1/16 get answer for question and check if it has been answered in the affirmative

        }
        return true;
    }

    /**
     * Returns the recorded answer to a question
     * @param question
     * @return
     */
    public String getQuestionAnswer(NewLanguageQuestion question) {
        if(question != null) {
            // TODO: 6/1/16 get the answer to the question
        }
        return "";
    }


    public int getFocusedPosition() {
        return mFocusedPosition;
    }

    public int getSelection() {
        return mSelection;
    }

    public static class BooleanViewHolder extends RecyclerView.ViewHolder {
        private final TextView question;
        private final RadioButton radioButtonYes;
        private final RadioButton radioButtonNo;
        private final Context context;

        public BooleanViewHolder(Context context, View v) {
            super(v);

            this.context = context;
            this.question = (TextView)v.findViewById(R.id.label);
            this.radioButtonYes = (RadioButton)v.findViewById(R.id.radio_button_yes);
            this.radioButtonNo = (RadioButton)v.findViewById(R.id.radio_button_no);
        }

        public void enable() {
            question.setTextColor(context.getResources().getColor(R.color.dark_primary_text));
            radioButtonNo.setEnabled(true);
            radioButtonNo.setFocusable(true);
            radioButtonNo.setFocusableInTouchMode(true);
            radioButtonYes.setEnabled(true);
            radioButtonYes.setFocusable(true);
            radioButtonYes.setFocusableInTouchMode(true);
        }

        public void disable() {
            question.setTextColor(context.getResources().getColor(R.color.dark_disabled_text));
            radioButtonNo.setEnabled(false);
            radioButtonNo.setFocusable(false);
            radioButtonNo.setFocusableInTouchMode(false);
            radioButtonYes.setEnabled(false);
            radioButtonYes.setFocusable(false);
            radioButtonYes.setFocusableInTouchMode(false);
        }
    }

    public static class StringViewHolder extends RecyclerView.ViewHolder {
        private final EditText answer;
        private final TextView question;
        private final Context context;
        public TextWatcher textWatcher = null;

        public StringViewHolder(Context context, View v) {
            super(v);

            this.context = context;
            this.question = (TextView)v.findViewById(R.id.label);
            this.answer = (EditText)v.findViewById(R.id.edit_text);
        }

        public void enable() {
            question.setTextColor(context.getResources().getColor(R.color.dark_primary_text));
            question.setFocusable(true);
            question.setFocusableInTouchMode(true);
            answer.setEnabled(true);
            answer.setTextColor(context.getResources().getColor(R.color.dark_primary_text));
            answer.setHintTextColor(context.getResources().getColor(R.color.half_transparent));
        }

        public void disable() {
            question.setTextColor(context.getResources().getColor(R.color.dark_disabled_text));
            question.setFocusable(false);
            question.setFocusableInTouchMode(false);
            answer.setEnabled(false);
            answer.setTextColor(context.getResources().getColor(R.color.dark_disabled_text));
            answer.setHintTextColor(context.getResources().getColor(R.color.transparent));
        }
    }
}
