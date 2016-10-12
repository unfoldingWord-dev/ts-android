package com.door43.translationstudio.newui.translate;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.CheckingQuestion;
import com.door43.translationstudio.core.Frame;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TranslationArticle;
import com.door43.translationstudio.core.TranslationFormat;
import com.door43.translationstudio.core.TranslationNote;
import com.door43.translationstudio.core.TranslationWord;
import com.door43.translationstudio.core.Typography;
import com.door43.translationstudio.rendering.HtmlRenderer;
import com.door43.translationstudio.rendering.LinkToHtmlRenderer;
import com.door43.translationstudio.spannables.ArticleLinkSpan;
import com.door43.translationstudio.spannables.LinkSpan;
import com.door43.translationstudio.spannables.PassageLinkSpan;
import com.door43.translationstudio.spannables.Span;

import org.apmem.tools.layouts.FlowLayout;
import org.sufficientlysecure.htmltextview.HtmlTextView;
import org.sufficientlysecure.htmltextview.LocalLinkMovementMethod;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.models.SourceLanguage;
import org.unfoldingword.resourcecontainer.ContainerTools;
import org.unfoldingword.resourcecontainer.Link;
import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.taskmanager.ManagedTask;
import org.unfoldingword.tools.taskmanager.TaskManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private static final String TASK_ID_OPEN_HELP_SOURCE = "open-help-containers";
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
    private static List<ResourceContainer> helpfulContainers = null;

    @Override
    ViewModeAdapter generateAdapter(Activity activity, String targetTranslationId, String chapterId, String frameId, Bundle extras) {
        return new ReviewModeAdapter(activity, targetTranslationId, chapterId, frameId, mResourcesOpen);
    }

    /**
     * Resets the static variables
     */
    public static void reset() {
        helpfulContainers = null;
    }

    @Override
    public void onTaskFinished(final ManagedTask task) {
        super.onTaskFinished(task);
        if(task.getTaskId().equals(TASK_ID_OPEN_HELP_SOURCE)) {
            Handler hand  = new Handler(Looper.getMainLooper());
            hand.post(new Runnable() {
                @Override
                public void run() {
                    if(getAdapter() != null) {
                        ((ReviewModeAdapter)getAdapter()).setHelpContainers(helpfulContainers);
                    }
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(helpfulContainers == null && mSourceContainer != null) {
            // load the helpful containers
            if(getAdapter() != null) ((ReviewModeAdapter)getAdapter()).setHelpContainers(null);
            ManagedTask task = new ManagedTask() {
                @Override
                public void start() {
                    if(!mSourceContainer.config.containsKey("content")) {
                        helpfulContainers = new ArrayList<>();
                        return;
                    }
                    Map contentConfig = (Map<String, Object>)mSourceContainer.config.get("content");
                    for(String chapterSlug:(List<String>)contentConfig.keySet()) {
                        Map chapterConfig = (Map<String, Object>)contentConfig.get(chapterSlug);
                        for(String chunkSlug:(List<String>)chapterConfig.keySet()) {
                            Map chunkConfig = (Map<String, Object>)chapterConfig.get(chunkSlug);
                            for(String helpSlug:(List<String>)chunkConfig.keySet()) {
                                List<String> links = (List<String>)chunkConfig.get(helpSlug);
                                for(String link:links) {
                                    try {
                                        Link l = ContainerTools.parseLink(link);
                                        // TODO: 10/11/16 get link info and locate resource container for loading
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                    // TODO: 10/11/16 identify the needed containers
                    // TODO: 10/11/16 attempt to open containers
                    // TODO: 10/11/16 add containers to list
                }
            };
            task.addOnFinishedListener(this);
            TaskManager.addTask(task, TASK_ID_OPEN_HELP_SOURCE);
        } else if(getAdapter() != null){
            ((ReviewModeAdapter)getAdapter()).setHelpContainers(helpfulContainers);
        }
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
    public void onTranslationArticleClick(String volume, String manual, String slug, int width) {
        renderTranslationArticle(volume, manual, slug);
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
//            mCloseResourcesDrawerButton.setText(getActivity().getResources().getString(R.string.translation_words_index));
            Door43Client library = App.getLibrary();
            ListView list = (ListView) getActivity().getLayoutInflater().inflate(R.layout.fragment_words_index_list, null);
            mResourcesDrawerContent.removeAllViews();
            mResourcesDrawerContent.addView(list);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), R.layout.list_clickable_text);
//            TranslationWord[] words = library.getTranslationWords(getSelectedResourceContainer());
//            if(words.length == 0) {
//                words = library.getTranslationWords(library.getDefaultSourceTranslation(getSelectedResourceContainer().projectSlug, "en"));
//            }
//            for(TranslationWord word:words) {
//                adapter.add(word.getTerm());
//            }
            final TranslationWord[] staticWords = new TranslationWord[0];// words;
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
     * Prepares the resources drawer with the translation article
     * @param volume
     * @param manual
     * @param slug
     */
    private void renderTranslationArticle(String volume, String manual, String slug) {
        TranslationArticle article = getPreferredTranslationArticle(getSelectedResourceContainer(), volume, manual, slug);
        if(mResourcesDrawerContent != null) {
            mResourcesDrawerContent.setVisibility(View.GONE);
        }
        if(mScrollingResourcesDrawerContent != null && article != null) {
//            mCloseResourcesDrawerButton.setText(article.getTitle());
            mScrollingResourcesDrawerContent.setVisibility(View.VISIBLE);
            mScrollingResourcesDrawerContent.scrollTo(0, 0);
            WebView view = (WebView) getActivity().getLayoutInflater().inflate(R.layout.fragment_resources_article, null);

//            TextView title = (TextView)view.findViewById(R.id.title);
//            TextView descriptionView = (TextView)view.findViewById(R.id.description);

            final ResourceContainer resourceContainer = getSelectedResourceContainer();
            LinkToHtmlRenderer renderer = new LinkToHtmlRenderer(new LinkToHtmlRenderer.OnPreprocessLink() {
                @Override
                public boolean onPreprocess(Span span) {
                    if(span instanceof ArticleLinkSpan) {
                        ArticleLinkSpan link = ((ArticleLinkSpan)span);
                        TranslationArticle article = getPreferredTranslationArticle(resourceContainer, link.getVolume(), link.getManual(), link.getId());
                        if(article != null) {
                            link.setTitle(article.getTitle());
                        } else {
                            return false;
                        }
                    } else if(span instanceof PassageLinkSpan) {
                        PassageLinkSpan link = (PassageLinkSpan)span;
                        String text = resourceContainer.readChunk(link.getChapterId(), link.getFrameId());

//                        Frame frame = library.getFrame(resourceContainer, link.getChapterId(), link.getFrameId());
                        String title = resourceContainer.readChunk("front", "title") + " " + Integer.parseInt(link.getChapterId()) + ":" + Frame.parseVerseTitle(text, TranslationFormat.parse(resourceContainer.contentMimeType));
                        link.setTitle(title);
                        return !resourceContainer.readChunk(link.getChapterId(), link.getFrameId()).isEmpty();
                    }
                    return true;
                }
            });
//            , new Span.OnClickListener() {
//                @Override
//                public void onClick(View view, Span span, int start, int end) {
//                    if(((LinkSpan)span).getType().equals("ta")) {
//                        String url = span.getMachineReadable().toString();
//                        ArticleLinkSpan link = ArticleLinkSpan.parse(url);
//                        if(link != null) {
//                            onTranslationArticleClick(link.getVolume(), link.getManual(), link.getId(), mResourcesDrawer.getLayoutParams().width);
//                        }
//                    } else if(((LinkSpan)span).getType().equals("p")) {
//                        PassageLinkSpan link = (PassageLinkSpan) span;
//                        scrollToFrame(link.getChapterId(), link.getFrameId());
//                    }
//                }
//
//                @Override
//                public void onLongClick(View view, Span span, int start, int end) {
//
//                }
//            });

//            title.setText(article.getTitle());
//            SourceLanguage sourceLanguage = library.getSourceLanguage(sourceTranslation.projectSlug, sourceTranslation.sourceLanguageSlug);
//            Typography.format(getActivity(), title, sourceLanguage.getId(), sourceLanguage.getDirection());

//            descriptionView.setText(renderer.render(article.getBody()));
//            Typography.formatSub(getActivity(), descriptionView, sourceLanguage.getId(), sourceLanguage.getDirection());
//            descriptionView.setMovementMethod(LocalLinkMovementMethod.getInstance());

            view.setWebViewClient(new LinkToHtmlRenderer.CustomWebViewClient() {
                @Override
                public void onOverriddenLinkClick(WebView view, String url, Span span) {
                    if (span instanceof ArticleLinkSpan) {
                        ArticleLinkSpan link = (ArticleLinkSpan) span;
                        onTranslationArticleClick(link.getVolume(), link.getManual(), link.getId(), mResourcesDrawer.getLayoutParams().width);
                    } else if (span instanceof PassageLinkSpan) {
                        PassageLinkSpan link = (PassageLinkSpan) span;
                        scrollToFrame(link.getChapterId(), link.getFrameId());
                    }
                }

                @Override
                public void onLinkClick(WebView view, String url) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                }
            });
            view.loadData(Typography.getStyle(getActivity())
                    + renderer.render(article.getBody()).toString(), "text/html", "utf-8");

            mScrollingResourcesDrawerContent.removeAllViews();
            mScrollingResourcesDrawerContent.addView(view);
        }

    }

    /**
     * Prepares the resources drawer with the translation word
     * @param translationWordId
     */
    private void renderTranslationWord(String translationWordId) {
        mTranslationWordId = translationWordId;
        mTranslationNoteId = null;

        final Door43Client library = App.getLibrary();
        final ResourceContainer rc = getSelectedResourceContainer();
//        SourceLanguage sourceLanguage = library.index().getSourceLanguage(sourceTranslation.sourceLanguageSlug);
        TranslationWord word = null;//getPreferredWord(sourceTranslation, translationWordId);
        if(mResourcesDrawerContent != null) {
            mResourcesDrawerContent.setVisibility(View.GONE);
        }
        if(mScrollingResourcesDrawerContent != null && word != null) {
            mScrollingResourcesDrawerContent.setVisibility(View.VISIBLE);
            mScrollingResourcesDrawerContent.scrollTo(0, 0);
            LinearLayout view = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.fragment_resources_word, null);

//            mCloseResourcesDrawerButton.setText(word.getTerm());

            TextView wordTitle = (TextView)view.findViewById(R.id.word_title);
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

            wordTitle.setText(word.getTerm());
            descriptionTitle.setText(word.getDefinitionTitle());
            Typography.formatTitle(getActivity(), descriptionTitle, rc.language.slug, rc.language.direction);
            HtmlRenderer renderer = new HtmlRenderer(new HtmlRenderer.OnPreprocessLink() {
                @Override
                public boolean onPreprocess(Span span) {
                    if(span instanceof ArticleLinkSpan) {
                        ArticleLinkSpan link = ((ArticleLinkSpan)span);
                        TranslationArticle article = getPreferredTranslationArticle(rc, link.getVolume(), link.getManual(), link.getId());
                        if(article != null) {
                            link.setTitle(article.getTitle());
                        } else {
                            return false;
                        }
                    } else if(span instanceof PassageLinkSpan) {
//                        PassageLinkSpan link = (PassageLinkSpan)span;
//                        Frame frame = library.getFrame(rc, link.getChapterId(), link.getFrameId());
//                        String title = rc.getProjectTitle() + " " + Integer.parseInt(link.getChapterId()) + ":" + frame.getTitle();
//                        link.setTitle(title);
//                        return library.getFrame(rc, link.getChapterId(), link.getFrameId()) != null;
                        return false;
                    }
                    return true;
                }
            }, new Span.OnClickListener() {
                @Override
                public void onClick(View view, Span span, int start, int end) {
                    if(((LinkSpan)span).getType().equals("ta")) {
                        String url = span.getMachineReadable().toString();
                        ArticleLinkSpan link = ArticleLinkSpan.parse(url);
                        if(link != null) {
                            onTranslationArticleClick(link.getVolume(), link.getManual(), link.getId(), mResourcesDrawer.getLayoutParams().width);
                        }
                    } else if(((LinkSpan)span).getType().equals("p")) {
                        String url = span.getMachineReadable().toString();
                        PassageLinkSpan link = new PassageLinkSpan("", url);
                        scrollToFrame(link.getChapterId(), link.getFrameId());
                    }
                }

                @Override
                public void onLongClick(View view, Span span, int start, int end) {

                }
            });
            descriptionView.setText(renderer.render(word.getDefinition()));
            descriptionView.setMovementMethod(LocalLinkMovementMethod.getInstance());
            Typography.formatSub(getActivity(), descriptionView, rc.language.slug, rc.language.direction);

            seeAlsoView.removeAllViews();
            for(int i = 0; i < word.getSeeAlso().length; i ++) {
                final TranslationWord relatedWord = null;//getPreferredWord(rc, word.getSeeAlso()[i]);
                if(relatedWord != null) {
                    Button button = new Button(new ContextThemeWrapper(getActivity(), R.style.Widget_Button_Tag), null, R.style.Widget_Button_Tag);
                    button.setText(relatedWord.getTerm());
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onTranslationWordClick(relatedWord.getId(), mResourcesDrawer.getLayoutParams().width);
                        }
                    });
                    Typography.formatSub(getActivity(), button, rc.language.slug, rc.language.direction);
                    seeAlsoView.addView(button);
                }
            }
            if(word.getSeeAlso().length > 0) {
                seeAlsoTitle.setVisibility(View.VISIBLE);
            } else {
                seeAlsoTitle.setVisibility(View.GONE);
            }
            Typography.formatTitle(getActivity(), seeAlsoTitle, rc.language.slug, rc.language.direction);

            examplesView.removeAllViews();
            for(final TranslationWord.Example example:word.getExamples()) {
                LinearLayout exampleView = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.fragment_resources_example_item, null);
                TextView referenceView = (TextView)exampleView.findViewById(R.id.reference);
                HtmlTextView passageView = (HtmlTextView)exampleView.findViewById(R.id.passage);
//                Frame frame = library.getFrame(rc, example.getChapterId(), example.getFrameId());
//                referenceView.setText(rc.getProjectTitle() + " " + Integer.parseInt(example.getChapterId()) + ":" + frame.getTitle());
                passageView.setHtmlFromString(example.getPassage(), true);
                exampleView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        scrollToFrame(example.getChapterId(), example.getFrameId());
                    }
                });
                Typography.formatSub(getActivity(), referenceView, rc.language.slug, rc.language.direction);
                Typography.formatSub(getActivity(), passageView, rc.language.slug, rc.language.direction);
                examplesView.addView(exampleView);
            }
            if(word.getExamples().length > 0) {
                examplesTitle.setVisibility(View.VISIBLE);
            } else {
                examplesTitle.setVisibility(View.GONE);
            }
            Typography.formatTitle(getActivity(), examplesTitle, rc.language.slug, rc.language.direction);

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

        final Door43Client library = App.getLibrary();
        final ResourceContainer sourceTranslation = getSelectedResourceContainer();
        TranslationNote note = null;//getPreferredNote(sourceTranslation, chapterId, frameId, noteId);
        if(mResourcesDrawerContent != null) {
            mResourcesDrawerContent.setVisibility(View.GONE);
        }
        if(mScrollingResourcesDrawerContent != null && note != null) {
            mScrollingResourcesDrawerContent.setVisibility(View.VISIBLE);
            mScrollingResourcesDrawerContent.scrollTo(0, 0);
            LinearLayout view = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.fragment_resources_note, null);

//            mCloseResourcesDrawerButton.setText(note.getTitle());

            TextView title = (TextView)view.findViewById(R.id.title);
            TextView descriptionView = (TextView)view.findViewById(R.id.description);

            HtmlRenderer renderer = new HtmlRenderer(new HtmlRenderer.OnPreprocessLink() {
                @Override
                public boolean onPreprocess(Span span) {
                    if(span instanceof ArticleLinkSpan) {
                        ArticleLinkSpan link = ((ArticleLinkSpan)span);
                        TranslationArticle article = getPreferredTranslationArticle(sourceTranslation, link.getVolume(), link.getManual(), link.getId());
                        if(article != null) {
                            link.setTitle(article.getTitle());
                        } else {
                            return false;
                        }
                    } else if(span instanceof PassageLinkSpan) {
                        PassageLinkSpan link = (PassageLinkSpan)span;
                        String chapterID = link.getChapterId();
                        // TODO: 3/30/2016 rather than assuming passage links are always referring to the current source translation we need to support links to other source translations
                        Frame frame = null;//library.getFrame(sourceTranslation, chapterID, link.getFrameId());
                        if(frame != null) {
//                            String chapter = (chapterID != null) ? String.valueOf(Integer.parseInt(chapterID)) : ""; // handle null chapter ID
//                            String title = sourceTranslation.getProjectTitle() + " " + chapter + ":" + frame.getTitle();
//                            link.setTitle(title);
//                            return library.getFrame(sourceTranslation, chapterID, link.getFrameId()) != null;
                            return false;
                        } else {
                            return false;
                        }
                    }
                    return true;
                }
            }, new Span.OnClickListener() {
                @Override
                public void onClick(View view, Span span, int start, int end) {
                    if(((LinkSpan)span).getType().equals("ta")) {
                        String url = span.getMachineReadable().toString();
                        ArticleLinkSpan link = ArticleLinkSpan.parse(url);
                        if(link != null) {
                            onTranslationArticleClick(link.getVolume(), link.getManual(), link.getId(), mResourcesDrawer.getLayoutParams().width);
                        }
                    } else if(((LinkSpan)span).getType().equals("p")) {
                        String url = span.getMachineReadable().toString();
                        PassageLinkSpan link = new PassageLinkSpan("", url);
                        scrollToFrame(link.getChapterId(), link.getFrameId());
                    }
                }

                @Override
                public void onLongClick(View view, Span span, int start, int end) {

                }
            });

            title.setText(note.getTitle());
            SourceLanguage sourceLanguage = library.index().getSourceLanguage(sourceTranslation.language.slug);
            Typography.format(getActivity(), title, sourceLanguage.slug, sourceLanguage.direction);

            descriptionView.setText(renderer.render(note.getBody()));
            Typography.formatSub(getActivity(), descriptionView, sourceLanguage.slug, sourceLanguage.direction);
            descriptionView.setMovementMethod(LocalLinkMovementMethod.getInstance());

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

        final Door43Client library = App.getLibrary();
        ResourceContainer sourceTranslation = getSelectedResourceContainer();
        CheckingQuestion question = null;//getPreferredQuestion(sourceTranslation, chapterId, frameId, questionId);
        SourceLanguage sourceLanguage = library.index().getSourceLanguage(sourceTranslation.language.slug);
        if(mResourcesDrawerContent != null && question != null) {
            mResourcesDrawerContent.setVisibility(View.GONE);
        }
        if(mScrollingResourcesDrawerContent != null) {
            mScrollingResourcesDrawerContent.setVisibility(View.VISIBLE);
            mScrollingResourcesDrawerContent.scrollTo(0, 0);
            LinearLayout view = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.fragment_resources_question, null);

//            mCloseResourcesDrawerButton.setText(question.getQuestion());

            TextView questionTitle = (TextView)view.findViewById(R.id.question_title);
            Typography.formatTitle(getActivity(), questionTitle, sourceLanguage.slug, sourceLanguage.direction);
            TextView questionView = (TextView)view.findViewById(R.id.question);
            TextView answerTitle = (TextView)view.findViewById(R.id.answer_title);
            Typography.formatTitle(getActivity(), answerTitle, sourceLanguage.slug, sourceLanguage.direction);
            TextView answerView = (TextView)view.findViewById(R.id.answer);
            TextView referencesTitle = (TextView)view.findViewById(R.id.references_title);
            Typography.formatTitle(getActivity(), referencesTitle, sourceLanguage.slug, sourceLanguage.direction);
            LinearLayout referencesLayout = (LinearLayout)view.findViewById(R.id.references);

            referencesLayout.removeAllViews();
            for(final CheckingQuestion.Reference reference:question.getReferences()) {
                TextView referenceView = (TextView) getActivity().getLayoutInflater().inflate(R.layout.fragment_resources_reference_item, null);
//                Frame frame = library.getFrame(sourceTranslation, reference.getChapterId(), reference.getFrameId());
//                referenceView.setText(sourceTranslation.getProjectTitle() + " " + Integer.parseInt(reference.getChapterId()) + ":" + frame.getTitle());
                Typography.formatSub(getActivity(), referenceView, sourceLanguage.slug, sourceLanguage.direction);
                referenceView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        scrollToFrame(reference.getChapterId(), reference.getFrameId());
                    }
                });
                referencesLayout.addView(referenceView);
            }

            questionView.setText(question.getQuestion());
            Typography.formatSub(getActivity(), questionView, sourceLanguage.slug, sourceLanguage.direction);
            answerView.setText(question.getAnswer());
            Typography.formatSub(getActivity(), answerView, sourceLanguage.slug, sourceLanguage.direction);

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
        Door43Client library = App.getLibrary();
        TranslationNote note = null;//library.getTranslationNote(sourceTranslation, chapterId, frameId, noteId);
        if(note == null && !sourceTranslation.language.slug.equals("en")) {
//            SourceTranslation defaultSourceTranslation = library.getDefaultSourceTranslation(sourceTranslation.projectSlug, "en");
//            note = library.getTranslationNote(defaultSourceTranslation, chapterId, frameId, noteId);
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
        Door43Client library = App.getLibrary();
        TranslationWord word = null;//library.getTranslationWord(sourceTranslation, wordId);
        if(word == null && !sourceTranslation.language.slug.equals("en")) {
//            SourceTranslation defaultSourceTranslation = library.getDefaultSourceTranslation(sourceTranslation.projectSlug, "en");
//            word = library.getTranslationWord(defaultSourceTranslation, wordId);
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
        Door43Client library = App.getLibrary();
        CheckingQuestion question = null;//library.getCheckingQuestion(sourceTranslation, chapterId, frameId, questionId);
        if(question == null && !sourceTranslation.language.slug.equals("en")) {
//            SourceTranslation defaultSourceTranslation = library.getDefaultSourceTranslation(sourceTranslation.projectSlug, "en");
//            question = library.getCheckingQuestion(defaultSourceTranslation, chapterId, frameId, questionId);
        }
        return question;
    }

    /**
     * Returns the preferred translation academy
     * if none exist in the source language it will return the english version.
     * @param resourceContainer
     * @param volume
     *@param manual
     * @param articleId  @return
     */
    private static TranslationArticle getPreferredTranslationArticle(ResourceContainer resourceContainer, String volume, String manual, String articleId) {
//        Door43Client library = App.getLibrary();
//        TranslationArticle article = library.getTranslationArticle(resourceContainer, volume, manual, articleId);
//        if(article == null && !resourceContainer.sourceLanguageSlug.equals("en")) {
//            SourceTranslation defaultSourceTranslation = library.getDefaultSourceTranslation(resourceContainer.projectSlug, "en");
//            article = library.getTranslationArticle(defaultSourceTranslation, volume, manual, articleId);
//        }
        return null;//article;
    }
}
