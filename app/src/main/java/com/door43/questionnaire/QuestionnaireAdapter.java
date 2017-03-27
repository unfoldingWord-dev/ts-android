package com.door43.questionnaire;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.widget.ViewUtil;

import org.unfoldingword.door43client.models.Question;

import java.util.ArrayList;
import java.util.List;


/**
 * Handles the rendering of the questions in a questionnaire
 */
public class QuestionnaireAdapter extends RecyclerView.Adapter<QuestionnaireAdapter.ViewHolder> {

    public static final String TAG = QuestionnaireAdapter.class.getSimpleName();
    private static final int TYPE_BOOLEAN = 1;
    private static final int TYPE_STRING = 2;
    private final Activity context;
    private QuestionnairePage page;
    private List<ViewHolder> viewHolders = new ArrayList<>();
    private OnEventListener onEventListener = null;
    private int lastPosition = -1;
    private boolean animateRight = true;

    public QuestionnaireAdapter(Activity context) {
        this.context = context;
    }

    /**
     * Sets the questionnaire page that should be displayed
     * @param page
     * @param animateRight indicates the direction of the question animations
     */
    public void setPage(QuestionnairePage page, boolean animateRight) {
        this.page = page;
        lastPosition = -1;
        this.animateRight = animateRight;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        ViewHolder vh;
        switch (viewType) {
            case TYPE_BOOLEAN:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_questionnaire_boolean_question, parent, false);
                vh = new BooleanViewHolder(context, v);
                break;
            case TYPE_STRING:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_questionnaire_text_question, parent, false);
                vh = new StringViewHolder(context, v);
                break;
            default:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_questionnaire_text_question, parent, false);
                vh = new StringViewHolder(context, v);
                break;
        }
        viewHolders.add(vh);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.currentPosition = position;
        setAnimation(holder.card, position);
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
    public void onViewDetachedFromWindow(final ViewHolder holder) {
        holder.clearAnimation();
    }

    /**
     * Animates a view the first time it appears on the screen
     * @param viewToAnimate
     * @param position
     */
    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            Animation animation;
            if(animateRight) {
                animation = AnimationUtils.loadAnimation(context, R.anim.slide_in_right);
            } else {
                animation = AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left);
            }
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
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
        if(page.getQuestion(position).inputType == Question.InputType.Boolean) {
            return TYPE_BOOLEAN;
        } else if(page.getQuestion(position).inputType == Question.InputType.String) {
            return TYPE_STRING;
        }
        return -1;
    }

    /**
     * Handles binding to boolean questions
     * @param holder
     * @param position
     */
    public void onBindBooleanQuestion(BooleanViewHolder holder, int position) {
        final Question question = page.getQuestion(position);
        holder.question.setText(question.text);
        holder.question.setHint(question.help);
        holder.radioGroup.setOnCheckedChangeListener(null);

        holder.setRequired(question.isRequired);

        String answerString = getQuestionAnswer(question);
        boolean answer = Boolean.parseBoolean(answerString);

        holder.radioGroup.clearCheck();

        // provide answer if given
        if(answerString != null && !answerString.isEmpty()) {
            holder.radioButtonYes.setChecked(answer);
            holder.radioButtonNo.setChecked(!answer);
        }

        if(isQuestionEnabled(question)) {
            holder.enable();
        } else {
            holder.disable();
        }

        holder.radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId == R.id.radio_button_yes) {
                    saveAnswer(question, "true");
                } else {
                    saveAnswer(question, "false");
                }
                reload();
            }
        });
    }

    /**
     * Handles binding to string questions
     * @param holder
     * @param position
     */
    public void onBindStringQuestion(StringViewHolder holder, int position) {
        final Question question = page.getQuestion(position);
        holder.question.setText(question.text);
        holder.answer.setHint(question.help);
        holder.answer.removeTextChangedListener(holder.textWatcher);

        holder.setRequired(question.isRequired);

        String answer = getQuestionAnswer(question);
        holder.answer.setText(answer);

        if(isQuestionEnabled(question)) {
            holder.enable();
        } else {
            holder.disable();
        }

        holder.textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String answer = getQuestionAnswer(question);
                boolean wasAnswered = answer != null && !answer.isEmpty();
                boolean isAnswered = !s.toString().isEmpty();
                saveAnswer(question, s.toString());
                if(wasAnswered != isAnswered) {
                    reload();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
        holder.answer.addTextChangedListener(holder.textWatcher);
    }

    /**
     * Saves the answer
     * @param question
     * @param answer
     */
    private boolean saveAnswer(Question question, String answer) {
        if(onEventListener != null) {
            onEventListener.onAnswerChanged(question, answer);
            return true;
        } else {
            Snackbar snack = Snackbar.make(context.findViewById(android.R.id.content), context.getResources().getString(R.string.answer_not_saved), Snackbar.LENGTH_LONG);
            ViewUtil.setSnackBarTextColor(snack, context.getResources().getColor(R.color.light_primary_text));
            snack.show();
            return false;
        }
    }

    /**
     * Re-evaluates the enabled state of each question
     */
    private void reload() {
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                for(ViewHolder vh:viewHolders) {
                    Question question = page.getQuestion(vh.currentPosition);
                    if(isQuestionEnabled(question)) {
                        vh.enable();
                    } else {
                        vh.disable();
                    }
                }
            }
        });
    }

    /**
     * Checks if the question dependencies are satisfied for it to be enabled
     * @param question
     * @return
     */
    private boolean isQuestionEnabled(Question question) {
        if(question != null) {
            return question.dependsOn <= 0
                    || (isAnswerAffirmative(page.getQuestionById(question.dependsOn))
                    && isQuestionEnabled(page.getQuestionById(question.dependsOn)));
        }
        return false;
    }

    /**
     * Checks if the question has been answered in the affirmative.
     * This means it was either yes for boolean questions or text was entered for string questions
     * @param question
     * @return
     */
    private boolean isAnswerAffirmative(Question question) {
        if(question != null) {
            String answer = getQuestionAnswer(question);
            if(answer != null && !answer.isEmpty()) {
                if (question.inputType == Question.InputType.Boolean) {
                    return Boolean.parseBoolean(answer);
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the recorded answer to a question
     * @param question
     * @return
     */
    private String getQuestionAnswer(Question question) {
        if(question != null && onEventListener != null) {
            return onEventListener.onGetAnswer(question);
        }
        return "";
    }

    /**
     * Sets the listener that will be called when certain ui events happen
     * @param onEventListener
     */
    public void setOnEventListener(OnEventListener onEventListener) {
        this.onEventListener = onEventListener;
    }

    /**
     * An abstract view holder for questions
     */
    public static abstract class ViewHolder extends RecyclerView.ViewHolder {

        protected final CardView card;
        protected final TextView requiredView;
        public int currentPosition = -1;

        public ViewHolder(View v) {
            super(v);
            this.card = (CardView)v.findViewById(R.id.card);
            this.requiredView = (TextView)v.findViewById(R.id.required);
        }

        abstract void enable();
        abstract void disable();

        public void clearAnimation() {
            this.card.clearAnimation();
        }
        public void setRequired(boolean required) {
            if(required) {
                this.requiredView.setVisibility(View.VISIBLE);
            } else {
                this.requiredView.setVisibility(View.GONE);
            }
        }
    }

    /**
     * A view holder for boolean questions
     */
    public static class BooleanViewHolder extends ViewHolder {
        private final TextView question;
        private final RadioButton radioButtonYes;
        private final RadioButton radioButtonNo;
        private final Context context;
        private final RadioGroup radioGroup;

        public BooleanViewHolder(final Context context, View v) {
            super(v);

            this.context = context;
            this.question = (TextView)v.findViewById(R.id.label);
            this.radioButtonYes = (RadioButton)v.findViewById(R.id.radio_button_yes);
            this.radioButtonNo = (RadioButton)v.findViewById(R.id.radio_button_no);
            this.radioGroup = (RadioGroup)v.findViewById(R.id.radio_group);

            this.card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    View focusedView = ((View)v.getParent()).findFocus();
                    if(focusedView != null) {
                        focusedView.clearFocus();
                        InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                    }
                }
            });
        }

        @Override
        public void enable() {
            question.setTextColor(context.getResources().getColor(R.color.dark_primary_text));
            radioButtonNo.setEnabled(true);
            radioButtonYes.setEnabled(true);
            requiredView.setTextColor(context.getResources().getColor(R.color.red));
        }

        @Override
        public void disable() {
            question.setTextColor(context.getResources().getColor(R.color.dark_disabled_text));
            radioButtonNo.setEnabled(false);
            radioButtonYes.setEnabled(false);
            requiredView.setTextColor(context.getResources().getColor(R.color.light_red));
        }
    }

    /**
     * A view holder for string questions
     */
    public static class StringViewHolder extends ViewHolder {
        private final EditText answer;
        private final TextView question;
        private final Context context;
        public TextWatcher textWatcher = null;

        public StringViewHolder(final Context context, View v) {
            super(v);

            this.context = context;
            this.question = (TextView)v.findViewById(R.id.label);
            this.answer = (EditText)v.findViewById(R.id.edit_text);

            this.card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(answer.isFocusable()) {
                        View focusedView = ((View)v.getParent()).findFocus();
                        if(focusedView != null) {
                            focusedView.clearFocus();
                        }
                        answer.requestFocus();
                        InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(answer, InputMethodManager.SHOW_FORCED);
                    }
                }
            });
        }

        @Override
        public void enable() {
            question.setTextColor(context.getResources().getColor(R.color.dark_primary_text));
            question.setFocusable(true);
            question.setFocusableInTouchMode(true);
            answer.setEnabled(true);
            answer.setTextColor(context.getResources().getColor(R.color.dark_primary_text));
            answer.setHintTextColor(context.getResources().getColor(R.color.transparent));
            answer.setFocusable(true);
            answer.setFocusableInTouchMode(true);
            requiredView.setTextColor(context.getResources().getColor(R.color.red));
        }

        @Override
        public void disable() {
            question.setTextColor(context.getResources().getColor(R.color.dark_disabled_text));
            question.setFocusable(false);
            question.setFocusableInTouchMode(false);
            answer.setEnabled(false);
            answer.setTextColor(context.getResources().getColor(R.color.dark_disabled_text));
            answer.setHintTextColor(context.getResources().getColor(R.color.half_transparent));
            answer.setFocusable(false);
            answer.setFocusableInTouchMode(false);
            requiredView.setTextColor(context.getResources().getColor(R.color.light_red));
        }
    }

    /**
     * The interface by which answers are sent/received
     */
    public interface OnEventListener {
        String onGetAnswer(Question question);
        void onAnswerChanged(Question question, String answer);
    }
}
