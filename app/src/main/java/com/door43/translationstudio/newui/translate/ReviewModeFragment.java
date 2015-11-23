package com.door43.translationstudio.newui.translate;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.CheckingQuestion;
import com.door43.translationstudio.core.Frame;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.SourceLanguage;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TranslationNote;
import com.door43.translationstudio.core.TranslationWord;
import com.door43.translationstudio.core.Typography;
import com.door43.translationstudio.rendering.LinkRenderer;
import com.door43.translationstudio.spannables.PassageLinkSpan;
import com.door43.translationstudio.spannables.Span;
import com.door43.translationstudio.AppContext;
import com.door43.widget.ViewUtil;

import org.apmem.tools.layouts.FlowLayout;
import org.sufficientlysecure.htmltextview.HtmlTextView;

/**
 * Created by joel on 9/8/2015.
 */
public class ReviewModeFragment extends ViewModeFragment {

    private static final String STATE_RESOURCES_OPEN = "state_resources_open";
    private static final String STATE_RESOURCES_DRAWER_OPEN = "state_resources_drawer_open";
    private static final String STATE_WORD_ID = "state_word_id";
    private static final String STATE_NOTE_ID = "state_note_id";
    private static final String STATE_CHAPTER_ID = "state_chapter_id";
    private static final String STATE_FRAME_ID = "state_frame_id";
    private static final String STATE_QUESTION_ID = "state_question_id";
    private boolean mResourcesOpen = false;
    private boolean mResourcesDrawerOpen = false;
    private CardView mResourcesDrawer;
    private ScrollView mScrollingResourcesDrawerContent;
    private Button mCloseResourcesDrawerButton;
    private String mTranslationWordId;
    private String mTranslationNoteId;
    private String mFrameId;
    private String mChapterId;
    private String mCheckingQuestionId;
    private LinearLayout mResourcesDrawerContent;

    @Override
    ViewModeAdapter generateAdapter(Activity activity, String targetTranslationId, String sourceTranslationId, String chapterId, String frameId, Bundle extras) {
        return new ReviewModeAdapter(activity, targetTranslationId, sourceTranslationId, chapterId, frameId, mResourcesOpen);
    }

    @Override
    protected void onPrepareView(final View rootView) {
        mResourcesDrawer = (CardView)rootView.findViewById(R.id.resources_drawer_card);
        mScrollingResourcesDrawerContent = (ScrollView)rootView.findViewById(R.id.scrolling_resources_drawer_content);
        mResourcesDrawerContent = (LinearLayout)rootView.findViewById(R.id.resources_drawer_content);
        mCloseResourcesDrawerButton = (Button)rootView.findViewById(R.id.close_resources_drawer_btn);
        mCloseResourcesDrawerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeResourcesDrawer();
            }
        });

        // open the drawer on rotate
        if(mResourcesDrawerOpen && mResourcesOpen) {
            ViewTreeObserver viewTreeObserver = rootView.getViewTreeObserver();
            if(viewTreeObserver.isAlive()) {
                viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        } else {
                            rootView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        }
                        ReviewModeAdapter.ViewHolder sample = (ReviewModeAdapter.ViewHolder) getViewHolderSample();
                        if (sample != null) {
                            if (mTranslationNoteId != null) {
                                onTranslationNoteClick(mChapterId, mFrameId, mTranslationNoteId, sample.getResourceCardWidth());
                            } else if (mTranslationWordId != null) {
                                onTranslationWordClick(mTranslationWordId, sample.getResourceCardWidth());
                            } else if (mCheckingQuestionId != null) {
                                onCheckingQuestionClick(mChapterId, mFrameId, mCheckingQuestionId, sample.getResourceCardWidth());
                            }
                        }
                    }
                });
            }
        }
        closeResourcesDrawer();
    }

    @Override
    protected void onRightSwipe(MotionEvent e1, MotionEvent e2) {
        if(mResourcesDrawerOpen) {
            closeResourcesDrawer();
        } else {
            if (getAdapter() != null) {
                ((ReviewModeAdapter) getAdapter()).closeResources();
            }
        }
    }

    @Override
    protected void onLeftSwipe(MotionEvent e1, MotionEvent e2) {
        if(getAdapter() != null) {
            ((ReviewModeAdapter)getAdapter()).openResources();
        }
    }

    private void openResourcesDrawer(int width) {
        mResourcesDrawerOpen = true;
        ViewGroup.LayoutParams params = mResourcesDrawer.getLayoutParams();
        params.width = width;
        mResourcesDrawer.setLayoutParams(params);
        // TODO: animate in
    }

    private void closeResourcesDrawer() {
        mResourcesDrawerOpen = false;
        ViewGroup.LayoutParams params = mResourcesDrawer.getLayoutParams();
        params.width = 0;
        mResourcesDrawer.setLayoutParams(params);
        // TODO: animate
    }

    @Override
    public void onTranslationWordClick(String translationWordId, int width) {
        renderTranslationWord(translationWordId);
        openResourcesDrawer(width);
    }

    @Override
    public void onTranslationNoteClick(String chapterId, String frameId, String translatioNoteId, int width) {
        renderTranslationNote(chapterId, frameId, translatioNoteId);
        openResourcesDrawer(width);
    }

    @Override
    public void onCheckingQuestionClick(String chapterId, String frameId, String checkingQuestionId, int width) {
        renderCheckingQuestion(chapterId, frameId, checkingQuestionId);
        openResourcesDrawer(width);
    }

    /**
     * Prepares the resources drawer with the translation words index
     */
    private void renderTranslationWordsIndex() {
        if(mScrollingResourcesDrawerContent != null) {
            mScrollingResourcesDrawerContent.setVisibility(View.GONE);
        }
        if(mResourcesDrawerContent != null) {
            mResourcesDrawerContent.setVisibility(View.VISIBLE);
            mCloseResourcesDrawerButton.setText(getActivity().getResources().getString(R.string.translation_words_index));
            Library library = AppContext.getLibrary();
            ListView list = (ListView) getActivity().getLayoutInflater().inflate(R.layout.fragment_words_index_list, null);
            mResourcesDrawerContent.removeAllViews();
            mResourcesDrawerContent.addView(list);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), R.layout.list_clickable_text);
            TranslationWord[] words = library.getTranslationWords(getSourceTranslation());
            if(words.length == 0) {
                words = library.getTranslationWords(library.getDefaultSourceTranslation(getSourceTranslation().projectSlug, "en"));
            }
            for(TranslationWord word:words) {
                adapter.add(word.getTerm());
            }
            final TranslationWord[] staticWords = words;
            list.setAdapter(adapter);
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    TranslationWord word = staticWords[position];
                    renderTranslationWord(word.getId());
                }
            });
        }
    }

    /**
     * Prepares the resources drawer with the translation word
     * @param translationWordId
     */
    private void renderTranslationWord(String translationWordId) {
        mTranslationWordId = translationWordId;
        mTranslationNoteId = null;

        Library library = AppContext.getLibrary();
        SourceTranslation sourceTranslation = getSourceTranslation();
        SourceLanguage sourceLanguage = library.getSourceLanguage(sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug);
        TranslationWord word = getPreferredWord(sourceTranslation, translationWordId);
        if(mResourcesDrawerContent != null) {
            mResourcesDrawerContent.setVisibility(View.GONE);
        }
        if(mScrollingResourcesDrawerContent != null && word != null) {
            mScrollingResourcesDrawerContent.setVisibility(View.VISIBLE);
            mScrollingResourcesDrawerContent.scrollTo(0, 0);
            LinearLayout view = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.fragment_resources_word, null);

            mCloseResourcesDrawerButton.setText(word.getTerm());

            TextView descriptionTitle = (TextView)view.findViewById(R.id.description_title);
            HtmlTextView descriptionView = (HtmlTextView)view.findViewById(R.id.description);
            TextView seeAlsoTitle = (TextView)view.findViewById(R.id.see_also_title);
            FlowLayout seeAlsoView = (FlowLayout)view.findViewById(R.id.see_also);
            LinearLayout examplesView = (LinearLayout)view.findViewById(R.id.examples);
            TextView examplesTitle = (TextView)view.findViewById(R.id.examples_title);
            Button indexButton = (Button)view.findViewById(R.id.wordsIndex);

            indexButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    renderTranslationWordsIndex();
                }
            });

            descriptionTitle.setText(word.getDefinitionTitle());
            Typography.formatTitle(getActivity(), descriptionTitle, sourceLanguage.getId(), sourceLanguage.getDirection());
            descriptionView.setHtmlFromString(word.getDefinition(), true);
            Typography.formatSub(getActivity(), descriptionView, sourceLanguage.getId(), sourceLanguage.getDirection());

            seeAlsoView.removeAllViews();
            for(int i = 0; i < word.getSeeAlso().length; i ++) {
                final TranslationWord relatedWord = getPreferredWord(sourceTranslation, word.getSeeAlso()[i]);
                if(relatedWord != null) {
                    Button button = new Button(new ContextThemeWrapper(getActivity(), R.style.Widget_Button_Tag), null, R.style.Widget_Button_Tag);
                    button.setText(relatedWord.getTerm());
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onTranslationWordClick(relatedWord.getId(), mResourcesDrawer.getLayoutParams().width);
                        }
                    });
                    Typography.formatSub(getActivity(), button, sourceLanguage.getId(), sourceLanguage.getDirection());
                    seeAlsoView.addView(button);
                }
            }
            if(word.getSeeAlso().length > 0) {
                seeAlsoTitle.setVisibility(View.VISIBLE);
            } else {
                seeAlsoTitle.setVisibility(View.GONE);
            }
            Typography.formatTitle(getActivity(), seeAlsoTitle, sourceLanguage.getId(), sourceLanguage.getDirection());

            examplesView.removeAllViews();
            for(final TranslationWord.Example example:word.getExamples()) {
                LinearLayout exampleView = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.fragment_resources_example_item, null);
                TextView referenceView = (TextView)exampleView.findViewById(R.id.reference);
                HtmlTextView passageView = (HtmlTextView)exampleView.findViewById(R.id.passage);
                Frame frame = library.getFrame(sourceTranslation, example.getChapterId(), example.getFrameId());
                referenceView.setText(sourceTranslation.getProjectTitle() + " " + Integer.parseInt(example.getChapterId()) + ":" + frame.getTitle());
                passageView.setHtmlFromString(example.getPassage(), true);
                exampleView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        scrollToFrame(example.getChapterId(), example.getFrameId());
                    }
                });
                Typography.formatSub(getActivity(), referenceView, sourceLanguage.getId(), sourceLanguage.getDirection());
                Typography.formatSub(getActivity(), passageView, sourceLanguage.getId(), sourceLanguage.getDirection());
                examplesView.addView(exampleView);
            }
            if(word.getExamples().length > 0) {
                examplesTitle.setVisibility(View.VISIBLE);
            } else {
                examplesTitle.setVisibility(View.GONE);
            }
            Typography.formatTitle(getActivity(), examplesTitle, sourceLanguage.getId(), sourceLanguage.getDirection());

            mScrollingResourcesDrawerContent.removeAllViews();
            mScrollingResourcesDrawerContent.addView(view);
        }
    }

    /**
     * Prepares the resources drawer with the translation note
     * @param noteId
     */
    private void renderTranslationNote(String chapterId, String frameId, String noteId) {
        mTranslationWordId = null;
        mTranslationNoteId = noteId;
        mFrameId = frameId;
        mChapterId = chapterId;

        final Library library = AppContext.getLibrary();
        final SourceTranslation sourceTranslation = getSourceTranslation();
        TranslationNote note = getPreferredNote(sourceTranslation, chapterId, frameId, noteId);
        if(mResourcesDrawerContent != null) {
            mResourcesDrawerContent.setVisibility(View.GONE);
        }
        if(mScrollingResourcesDrawerContent != null && note != null) {
            mScrollingResourcesDrawerContent.setVisibility(View.VISIBLE);
            mScrollingResourcesDrawerContent.scrollTo(0, 0);
            LinearLayout view = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.fragment_resources_note, null);

            mCloseResourcesDrawerButton.setText(note.getTitle());

            TextView title = (TextView)view.findViewById(R.id.title);
            TextView description = (TextView)view.findViewById(R.id.description);

            LinkRenderer renderer = new LinkRenderer(new LinkRenderer.OnPreprocessLink() {
                @Override
                public boolean onPreprocess(PassageLinkSpan span) {
                    Frame frame = library.getFrame(sourceTranslation, span.getChapterId(), span.getFrameId());
                    String title = sourceTranslation.getProjectTitle() + " " + Integer.parseInt(span.getChapterId()) + ":" + frame.getTitle();
                    span.setTitle(title);
                    return library.getFrame(sourceTranslation, span.getChapterId(), span.getFrameId()) != null;
                }
            }, new Span.OnClickListener() {
                @Override
                public void onClick(View view, Span span, int start, int end) {
                    PassageLinkSpan link = (PassageLinkSpan)span;
                    scrollToFrame(link.getChapterId(), link.getFrameId());
                }

                @Override
                public void onLongClick(View view, Span span, int start, int end) {

                }
            });

            title.setText(note.getTitle());
            SourceLanguage sourceLanguage = library.getSourceLanguage(sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug);
            Typography.format(getActivity(), title, sourceLanguage.getId(), sourceLanguage.getDirection());
            description.setText(renderer.render(Html.fromHtml(note.getBody())));
            Typography.formatSub(getActivity(), description, sourceLanguage.getId(), sourceLanguage.getDirection());
            ViewUtil.makeLinksClickable(description);

            mScrollingResourcesDrawerContent.removeAllViews();
            mScrollingResourcesDrawerContent.addView(view);
        }
    }

    /**
     * Prepares the resources drawer with the checking question
     * @param questionId
     */
    private void renderCheckingQuestion(String chapterId, String frameId, String questionId) {
        mTranslationWordId = null;
        mTranslationNoteId = null;
        mCheckingQuestionId = questionId;
        mFrameId = frameId;
        mChapterId = chapterId;

        final Library library = AppContext.getLibrary();
        SourceTranslation sourceTranslation = getSourceTranslation();
        CheckingQuestion question = getPreferredQuestion(sourceTranslation, chapterId, frameId, questionId);
        SourceLanguage sourceLanguage = library.getSourceLanguage(sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug);
        if(mResourcesDrawerContent != null && question != null) {
            mResourcesDrawerContent.setVisibility(View.GONE);
        }
        if(mScrollingResourcesDrawerContent != null) {
            mScrollingResourcesDrawerContent.setVisibility(View.VISIBLE);
            mScrollingResourcesDrawerContent.scrollTo(0, 0);
            LinearLayout view = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.fragment_resources_question, null);

            mCloseResourcesDrawerButton.setText(question.getQuestion());

            TextView questionTitle = (TextView)view.findViewById(R.id.question_title);
            Typography.formatTitle(getActivity(), questionTitle, sourceLanguage.getId(), sourceLanguage.direction);
            TextView questionView = (TextView)view.findViewById(R.id.question);
            TextView answerTitle = (TextView)view.findViewById(R.id.answer_title);
            Typography.formatTitle(getActivity(), answerTitle, sourceLanguage.getId(), sourceLanguage.direction);
            TextView answerView = (TextView)view.findViewById(R.id.answer);
            TextView referencesTitle = (TextView)view.findViewById(R.id.references_title);
            Typography.formatTitle(getActivity(), referencesTitle, sourceLanguage.getId(), sourceLanguage.direction);
            LinearLayout referencesLayout = (LinearLayout)view.findViewById(R.id.references);

            referencesLayout.removeAllViews();
            for(final CheckingQuestion.Reference reference:question.getReferences()) {
                TextView referenceView = (TextView) getActivity().getLayoutInflater().inflate(R.layout.fragment_resources_reference_item, null);
                Frame frame = library.getFrame(sourceTranslation, reference.getChapterId(), reference.getFrameId());
                referenceView.setText(sourceTranslation.getProjectTitle() + " " + Integer.parseInt(reference.getChapterId()) + ":" + frame.getTitle());
                Typography.formatSub(getActivity(), referenceView, sourceLanguage.getId(), sourceLanguage.direction);
                referenceView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        scrollToFrame(reference.getChapterId(), reference.getFrameId());
                    }
                });
                referencesLayout.addView(referenceView);
            }

            questionView.setText(question.getQuestion());
            Typography.formatSub(getActivity(), questionView, sourceLanguage.getId(), sourceLanguage.getDirection());
            answerView.setText(question.getAnswer());
            Typography.formatSub(getActivity(), answerView, sourceLanguage.getId(), sourceLanguage.getDirection());

            mScrollingResourcesDrawerContent.removeAllViews();
            mScrollingResourcesDrawerContent.addView(view);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null) {
            mResourcesOpen = savedInstanceState.getBoolean(STATE_RESOURCES_OPEN, false);
            mResourcesDrawerOpen = savedInstanceState.getBoolean(STATE_RESOURCES_DRAWER_OPEN, false);

            if(savedInstanceState.containsKey(STATE_NOTE_ID)) {
                mTranslationNoteId = savedInstanceState.getString(STATE_NOTE_ID);
                mChapterId = savedInstanceState.getString(STATE_CHAPTER_ID);
                mFrameId = savedInstanceState.getString(STATE_FRAME_ID);
            } else if(savedInstanceState.containsKey(STATE_WORD_ID)) {
                mTranslationWordId = savedInstanceState.getString(STATE_WORD_ID);
            } else if(savedInstanceState.containsKey(STATE_QUESTION_ID)) {
                mCheckingQuestionId = savedInstanceState.getString(STATE_QUESTION_ID);
                mChapterId = savedInstanceState.getString(STATE_CHAPTER_ID);
                mFrameId = savedInstanceState.getString(STATE_FRAME_ID);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        out.putBoolean(STATE_RESOURCES_OPEN, ((ReviewModeAdapter) getAdapter()).isResourcesOpen());
        out.putBoolean(STATE_RESOURCES_DRAWER_OPEN, mResourcesDrawerOpen);
        if(mTranslationWordId != null) {
            out.putString(STATE_WORD_ID, mTranslationWordId);
        } else {
            out.remove(STATE_WORD_ID);
        }
        if(mTranslationNoteId != null) {
            out.putString(STATE_NOTE_ID, mTranslationNoteId);
            out.putString(STATE_CHAPTER_ID, mChapterId);
            out.putString(STATE_FRAME_ID, mFrameId);
        } else {
            out.remove(STATE_NOTE_ID);
        }
        if(mCheckingQuestionId != null) {
            out.putString(STATE_QUESTION_ID, mCheckingQuestionId);
            out.putString(STATE_CHAPTER_ID, mChapterId);
            out.putString(STATE_FRAME_ID, mFrameId);
        } else {
            out.remove(STATE_QUESTION_ID);
        }
        super.onSaveInstanceState(out);
    }

    /**
     * Returns the preferred translation notes.
     * if none exist in the source language it will return the english version
     * @param chapterId
     * @param frameId
     * @return
     */
    private static TranslationNote getPreferredNote(SourceTranslation sourceTranslation, String chapterId, String frameId, String noteId) {
        Library library = AppContext.getLibrary();
        TranslationNote note = library.getTranslationNote(sourceTranslation, chapterId, frameId, noteId);
        if(note == null && !sourceTranslation.sourceLanguageSlug.equals("en")) {
            SourceTranslation defaultSourceTranslation = library.getDefaultSourceTranslation(sourceTranslation.projectSlug, "en");
            note = library.getTranslationNote(defaultSourceTranslation, chapterId, frameId, noteId);
        }
        return note;
    }

    /**
     * Returns the preferred translation words.
     * if none exist in the source language it will return the english version
     * @param sourceTranslation
     * @return
     */
    private static TranslationWord getPreferredWord(SourceTranslation sourceTranslation, String wordId) {
        Library library = AppContext.getLibrary();
        TranslationWord word = library.getTranslationWord(sourceTranslation, wordId);
        if(word == null && !sourceTranslation.sourceLanguageSlug.equals("en")) {
            SourceTranslation defaultSourceTranslation = library.getDefaultSourceTranslation(sourceTranslation.projectSlug, "en");
            word = library.getTranslationWord(defaultSourceTranslation, wordId);
        }
        return word;
    }

    /**
     * Returns the preferred checking question.
     * if none exist in the source language it will return the english version
     * @param sourceTranslation
     * @param chapterId
     * @param frameId
     * @return
     */
    private static CheckingQuestion getPreferredQuestion(SourceTranslation sourceTranslation, String chapterId, String frameId, String questionId) {
        Library library = AppContext.getLibrary();
        CheckingQuestion question = library.getCheckingQuestion(sourceTranslation, chapterId, frameId, questionId);
        if(question == null && !sourceTranslation.sourceLanguageSlug.equals("en")) {
            SourceTranslation defaultSourceTranslation = library.getDefaultSourceTranslation(sourceTranslation.projectSlug, "en");
            question = library.getCheckingQuestion(defaultSourceTranslation, chapterId, frameId, questionId);
        }
        return question;
    }
}
