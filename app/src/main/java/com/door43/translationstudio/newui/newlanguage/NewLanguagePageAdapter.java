package com.door43.translationstudio.newui.newlanguage;

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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.NewLanguagePage;
import com.door43.translationstudio.core.NewLanguageQuestion;
import com.door43.widget.ViewUtil;

import java.util.ArrayList;
import java.util.List;


/**
 * Handles the rendering of the questions in NewLanguageActivity
 */
public class NewLanguagePageAdapter extends RecyclerView.Adapter<NewLanguagePageAdapter.ViewHolder> {

    public static final String TAG = NewLanguagePageAdapter.class.getSimpleName();
    private static final int TYPE_BOOLEAN = 1;
    private static final int TYPE_STRING = 2;
    private final Activity context;
    private NewLanguagePage page;
    private List<ViewHolder> viewHolders = new ArrayList<>();
    private OnEventListener onEventListener = null;
    private int lastPosition = -1;
    private boolean animateRight = true;

    public NewLanguagePageAdapter(Activity context) {
        this.context = context;
    }

    /**
     * Sets the questionnaire page that should be displayed
     * @param page
     * @param animateRight indicates the direction of the question animations
     */
    public void setPage(NewLanguagePage page, boolean animateRight) {
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

    private void setAnimation(View viewToAnimate, int position) {
        // If the bound view wasn't previously displayed on screen, it's animated
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
        if(page.getQuestion(position).type == NewLanguageQuestion.InputType.Boolean) {
            return TYPE_BOOLEAN;
        } else if(page.getQuestion(position).type == NewLanguageQuestion.InputType.String) {
            return TYPE_STRING;
        }
        return -1;
    }

    public void onBindBooleanQuestion(BooleanViewHolder holder, int position) {
        final NewLanguageQuestion question = page.getQuestion(position);
        holder.question.setText(question.question);
        holder.question.setHint(question.helpText);
        holder.radioButtonYes.setOnCheckedChangeListener(null);
        holder.radioButtonNo.setOnCheckedChangeListener(null);

        String answerString = getQuestionAnswer(question);
        boolean answer = Boolean.parseBoolean(answerString);

        // check radio buttons if an answer has been given
        if(answerString != null && !answerString.isEmpty()) {
            holder.radioButtonYes.setChecked(answer);
            holder.radioButtonNo.setChecked(!answer);
        } else {
            holder.radioButtonYes.setChecked(false);
            holder.radioButtonNo.setChecked(false);
        }

        if(isQuestionEnabled(question)) {
            holder.enable();
        } else {
            holder.disable();
        }

        holder.radioButtonYes.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    saveAnswer(question, "true");
                }
                reload();
            }
        });
        holder.radioButtonNo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    saveAnswer(question, "false");
                }
                reload();
            }
        });
    }

    public void onBindStringQuestion(StringViewHolder holder, int position) {
        final NewLanguageQuestion question = page.getQuestion(position);
        holder.question.setText(question.question);
        holder.answer.setHint(question.helpText);
        holder.answer.removeTextChangedListener(holder.textWatcher);

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
    private boolean saveAnswer(NewLanguageQuestion question, String answer) {
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
     * Re-evaluates the state of each question
     */
    private void reload() {
        Handler hand = new Handler(Looper.getMainLooper());
        hand.post(new Runnable() {
            @Override
            public void run() {
                for(ViewHolder vh:viewHolders) {
                    NewLanguageQuestion question = page.getQuestion(vh.currentPosition);
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
    private boolean isQuestionEnabled(NewLanguageQuestion question) {
        if(question != null) {
            return question.reliantQuestionId < 0
                    || (isAnswerAffirmative(page.getQuestionById(question.reliantQuestionId))
                    && isQuestionEnabled(page.getQuestionById(question.reliantQuestionId)));
        }
        return false;
    }

    /**
     * Checks if the question has been answered in the affirmative.
     * This means it was either yes for boolean questions or text was entered for string questions
     * @param question
     * @return
     */
    private boolean isAnswerAffirmative(NewLanguageQuestion question) {
        if(question != null) {
            String answer = getQuestionAnswer(question);
            if(answer != null && !answer.isEmpty()) {
                if (question.type == NewLanguageQuestion.InputType.Boolean) {
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
    private String getQuestionAnswer(NewLanguageQuestion question) {
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

    public static abstract class ViewHolder extends RecyclerView.ViewHolder {

        protected final CardView card;
        public int currentPosition = -1;

        public ViewHolder(View v) {
            super(v);
            this.card = (CardView)v.findViewById(R.id.card);
        }

        abstract void enable();
        abstract void disable();

        public void clearAnimation() {
            this.card.clearAnimation();
        }
    }

    public static class BooleanViewHolder extends ViewHolder {
        private final TextView question;
        private final RadioButton radioButtonYes;
        private final RadioButton radioButtonNo;
        private final Context context;

        public BooleanViewHolder(final Context context, View v) {
            super(v);

            this.context = context;
            this.question = (TextView)v.findViewById(R.id.label);
            this.radioButtonYes = (RadioButton)v.findViewById(R.id.radio_button_yes);
            this.radioButtonNo = (RadioButton)v.findViewById(R.id.radio_button_no);


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
//            radioButtonNo.setFocusable(true);
//            radioButtonNo.setFocusableInTouchMode(true);
            radioButtonYes.setEnabled(true);
//            radioButtonYes.setFocusable(true);
//            radioButtonYes.setFocusableInTouchMode(true);
        }

        @Override
        public void disable() {
            question.setTextColor(context.getResources().getColor(R.color.dark_disabled_text));
            radioButtonNo.setEnabled(false);
//            radioButtonNo.setFocusable(false);
//            radioButtonNo.setFocusableInTouchMode(false);
            radioButtonYes.setEnabled(false);
//            radioButtonYes.setFocusable(false);
//            radioButtonYes.setFocusableInTouchMode(false);
        }
    }

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
            answer.setHintTextColor(context.getResources().getColor(R.color.half_transparent));
            answer.setFocusable(true);
            answer.setFocusableInTouchMode(true);
        }

        @Override
        public void disable() {
            question.setTextColor(context.getResources().getColor(R.color.dark_disabled_text));
            question.setFocusable(false);
            question.setFocusableInTouchMode(false);
            answer.setEnabled(false);
            answer.setTextColor(context.getResources().getColor(R.color.dark_disabled_text));
            answer.setHintTextColor(context.getResources().getColor(R.color.transparent));
            answer.setFocusable(false);
            answer.setFocusableInTouchMode(false);
        }
    }

    public interface OnEventListener {
        String onGetAnswer(NewLanguageQuestion question);
        void onAnswerChanged(NewLanguageQuestion question, String answer);
    }
}
