package com.door43.translationstudio.ui.translate;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
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
import com.door43.translationstudio.core.ContainerCache;
import com.door43.translationstudio.core.Frame;
import com.door43.translationstudio.core.TranslationArticle;
import com.door43.translationstudio.core.TranslationFormat;
import com.door43.translationstudio.core.TranslationType;
import com.door43.translationstudio.core.Typography;
import com.door43.translationstudio.rendering.HtmlRenderer;
import com.door43.translationstudio.rendering.LinkToHtmlRenderer;
import com.door43.translationstudio.ui.spannables.ArticleLinkSpan;
import com.door43.translationstudio.ui.spannables.LinkSpan;
import com.door43.translationstudio.ui.spannables.PassageLinkSpan;
import com.door43.translationstudio.ui.spannables.ShortReferenceSpan;
import com.door43.translationstudio.ui.spannables.Span;
import com.door43.translationstudio.ui.spannables.TranslationWordLinkSpan;
import com.door43.translationstudio.ui.translate.review.ReviewHolder;
import com.door43.util.StringUtilities;

import org.apmem.tools.layouts.FlowLayout;
import org.markdownj.MarkdownProcessor;
import org.sufficientlysecure.htmltextview.HtmlTextView;
import org.sufficientlysecure.htmltextview.LocalLinkMovementMethod;

import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.models.SourceLanguage;
import org.unfoldingword.door43client.models.Translation;
import org.unfoldingword.resourcecontainer.ResourceContainer;
import org.unfoldingword.tools.taskmanager.ManagedTask;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final String STATE_RESOURCE_CONTAINER_SLUG = "container-slug";
    private static final String STATE_HELP_TITLE = "state_help_title";
    private static final String STATE_HELP_BODY = "state_help_body";
    private static final String STATE_HELP_TYPE = "state_help_type";
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

    private String mResourceContainerSlug;
    private ResourceContainer mSourceContainer = null;
    private TranslationHelp mTranslationQuestion = null;
    private TranslationHelp mTranslationNote = null;

    @Override
    ViewModeAdapter generateAdapter(Activity activity, String targetTranslationId, String chapterId, String frameId, Bundle extras) {
        return new ReviewModeAdapter(activity, targetTranslationId, chapterId, frameId, mResourcesOpen);
    }

    @Override
    public void onTaskFinished(final ManagedTask task) {
        super.onTaskFinished(task);
    }

    @Override
    protected void onSourceContainerLoaded(final ResourceContainer container) {
        mSourceContainer = container;
    }

    @Override
    public void onResume() {
        super.onResume();
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
                        ReviewHolder sample = (ReviewHolder) getViewHolderSample();
                        if (sample != null) {
                            if (mTranslationNote != null) {
                                onTranslationNoteClick(mTranslationNote, sample.getResourceCardWidth());
                            } else if (mTranslationWordId != null) {
                                onTranslationWordClick(mResourceContainerSlug, mTranslationWordId, sample.getResourceCardWidth());
                            } else if (mTranslationQuestion != null) {
                                onTranslationQuestionClick(mTranslationQuestion, sample.getResourceCardWidth());
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
    public void onTranslationWordClick(String resourceContainerSlug, String chapterSlug, int width) {
        renderTranslationWord(resourceContainerSlug, chapterSlug);
        openResourcesDrawer(width);
    }

    @Override
    public void onTranslationArticleClick(String volume, String manual, String slug, int width) {
        renderTranslationArticle(volume, manual, slug);
        openResourcesDrawer(width);
    }

    @Override
    public void onTranslationNoteClick(TranslationHelp note, int width) {
        renderTranslationNote(note);
        openResourcesDrawer(width);
    }

    @Override
    public void onTranslationQuestionClick(TranslationHelp question, int width) {
        renderTranslationQuestion(question);
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
            ListView list = (ListView) getActivity().getLayoutInflater().inflate(R.layout.fragment_words_index_list, null);
            mResourcesDrawerContent.removeAllViews();
            mResourcesDrawerContent.addView(list);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), R.layout.list_clickable_text);
            final ResourceContainer rc = ContainerCache.get(mResourceContainerSlug);
            if(rc != null) {
                String[] chapters = rc.chapters();
                final List<String> words = Arrays.asList(chapters);
                Collections.sort(words);
                Pattern titlePattern = Pattern.compile("#(.*)");
                for(String slug:words) {
                    // get title and add to adapter
                    Matcher match = titlePattern.matcher(rc.readChunk(slug, "01"));
                    if(match.find()) {
                        adapter.add(match.group(1));
                    } else {
                        adapter.add(slug);
                    }
                }
                list.setAdapter(adapter);
                list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        String slug = words.get(position);
                        renderTranslationWord(rc.slug, slug);
                    }
                });
            }
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
//                        scrollToChunk(link.getChapterId(), link.getFrameId());
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
                        scrollToChunk(link.getChapterId(), link.getFrameId());
                    }
                }

                @Override
                public void onLinkClick(WebView view, final String url) {
                    // opens web url
                    new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                            .setTitle(R.string.view_online)
                            .setMessage(R.string.use_internet_confirmation)
                            .setNegativeButton(R.string.title_cancel, null)
                            .setPositiveButton(R.string.label_continue, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                    startActivity(intent);
                                }
                            })
                            .show();
                }
            });
            view.loadData(Typography.getStyle(getActivity(), TranslationType.SOURCE )
                    + renderer.render(article.getBody()).toString(), "text/html", "utf-8");

            mScrollingResourcesDrawerContent.removeAllViews();
            mScrollingResourcesDrawerContent.addView(view);
        }

    }

    /**
     * Prepares the resources drawer with the translation word
     * @param resourceContainerSlug
     * @param chapterSlug
     */
    private void renderTranslationWord(String resourceContainerSlug, String chapterSlug) {
        mTranslationWordId = chapterSlug;
        mResourceContainerSlug = resourceContainerSlug;
        mTranslationNoteId = null;

        final ResourceContainer rc = ContainerCache.get(resourceContainerSlug);
        if(mResourcesDrawerContent != null) {
            mResourcesDrawerContent.setVisibility(View.GONE);
        }
        if(mScrollingResourcesDrawerContent != null && rc != null) {
            mScrollingResourcesDrawerContent.setVisibility(View.VISIBLE);
            mScrollingResourcesDrawerContent.scrollTo(0, 0);
            LinearLayout view = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.fragment_resources_word, null);


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
            String word = rc.readChunk(chapterSlug, "01");
            Pattern pattern = Pattern.compile("#+([^\\n]+)\\n+([\\s\\S]*)");
            Matcher match = pattern.matcher(word);
            String description = "";
            if(match.find()) {
                wordTitle.setText(match.group(1));
                description = match.group(2);
                // TODO: 10/12/16 load the description title. This should be read from the config maybe?
                descriptionTitle.setText("Description");
            }
            Typography.formatTitle(getActivity(), TranslationType.SOURCE, descriptionTitle, rc.language.slug, rc.language.direction);
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
                        PassageLinkSpan link = (PassageLinkSpan)span;
                        String chunk = rc.readChunk(link.getChapterId(), link.getFrameId());
                        String versetitle = Frame.parseVerseTitle(chunk, TranslationFormat.parse(rc.contentMimeType));
                        String title = rc.readChunk("front", "title") + " " + Integer.parseInt(link.getChapterId()) + ":" + versetitle;
                        link.setTitle(title);
                        return !chunk.isEmpty();
                    } else if(span instanceof TranslationWordLinkSpan) {
                        ResourceContainer currentRC = getSelectedResourceContainer();
                        Pattern titlePattern = Pattern.compile("#(.*)");
                        ResourceContainer rc = ContainerCache.cacheClosest(App.getLibrary(), currentRC.language.slug, "bible", "tw");

                        String word = rc.readChunk(span.getMachineReadable().toString(), "01");
                        if(!word.isEmpty()) {
                            Matcher linkMatch = titlePattern.matcher(word.trim());
                            String title = span.getMachineReadable().toString();
                            if (linkMatch.find()) {
                                title = linkMatch.group(1);
                            }
                            ((TranslationWordLinkSpan) span).setTitle(title);
                        } else {
                            return false;
                        }
                    }
                    return true;
                }
            }, new Span.OnClickListener() {
                @Override
                public void onClick(View view, Span span, int start, int end) {
                    String type = ((LinkSpan)span).getType();
                    if(type.equals("ta")) {
                        String url = span.getMachineReadable().toString();
                        ArticleLinkSpan link = ArticleLinkSpan.parse(url);
                        if(link != null) {
                            onTranslationArticleClick(link.getVolume(), link.getManual(), link.getId(), mResourcesDrawer.getLayoutParams().width);
                        }
                    } else if(type.equals("p")) {
                        String url = span.getMachineReadable().toString();
                        PassageLinkSpan link = new PassageLinkSpan("", url);
                        scrollToChunk(link.getChapterId(), link.getFrameId());
                    } else if(type.equals("m")) {
                        // markdown link
                        final String url = span.getMachineReadable().toString();
                        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                                .setTitle(R.string.view_online)
                                .setMessage(R.string.use_internet_confirmation)
                                .setNegativeButton(R.string.title_cancel, null)
                                .setPositiveButton(R.string.label_continue, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                        startActivity(intent);
                                    }
                                })
                                .show();
                    } else if(type.equals("tw")) {
                        // translation word
                        ResourceContainer currentRC = getSelectedResourceContainer();
                        ResourceContainer rc = ContainerCache.cacheClosest(App.getLibrary(), currentRC.language.slug, "bible", "tw");
                        if(rc != null) {
                            onTranslationWordClick(rc.slug, span.getMachineReadable().toString(), mResourcesDrawer.getLayoutParams().width);
                        }
                    }
                }

                @Override
                public void onLongClick(View view, Span span, int start, int end) {

                }
            });
            descriptionView.setText(renderer.render(description));
            descriptionView.setMovementMethod(LocalLinkMovementMethod.getInstance());
            Typography.formatSub(getActivity(), TranslationType.SOURCE, descriptionView, rc.language.slug, rc.language.direction);

            seeAlsoView.removeAllViews();
            seeAlsoTitle.setVisibility(View.GONE);
            examplesView.removeAllViews();
            examplesTitle.setVisibility(View.GONE);
            if(rc.config != null && rc.config.containsKey(chapterSlug)) {
                Map chapterConfig = (Map<String, List<String>> )rc.config.get(chapterSlug);
                if(chapterConfig.containsKey("see_also")) {
                    Pattern titlePattern = Pattern.compile("#(.*)");
                    List<String> relatedSlugs = (List<String>)chapterConfig.get("see_also");
                    for(final String relatedSlug:relatedSlugs) {
                        // TODO: 10/12/16 the words need to have their title placed into a "title" file instead of being inline in the chunk
                        String relatedWord = rc.readChunk(relatedSlug, "01");
                        Matcher linkMatch = titlePattern.matcher(relatedWord.trim());
                        String relatedTitle = relatedSlug;
                        if(linkMatch.find()) {
                            relatedTitle = linkMatch.group(1);
                        }
                        Button button = new Button(new ContextThemeWrapper(getActivity(), R.style.Widget_Button_Tag), null, R.style.Widget_Button_Tag);
                        button.setText(relatedTitle);
                        button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                onTranslationWordClick(rc.slug, relatedSlug, mResourcesDrawer.getLayoutParams().width);
                            }
                        });
                        Typography.formatSub(getActivity(), TranslationType.SOURCE, button, rc.language.slug, rc.language.direction);
                        seeAlsoView.addView(button);
                    }
                    if(relatedSlugs.size() > 0) seeAlsoTitle.setVisibility(View.VISIBLE);
                }
                if(chapterConfig.containsKey("examples")) {
                    List<String> exampleSlugs = (List<String>)chapterConfig.get("examples");
                    for(String exampleSlug:exampleSlugs) {
                        final String[] slugs = exampleSlug.split("-");
                        if(slugs.length != 2) continue;
                        if(mSourceContainer == null) continue;
                        String projectTitle = mSourceContainer.readChunk("front", "title");

                        // get verse title
                        String verseTitle = StringUtilities.formatNumber(slugs[1]);
                        if(mSourceContainer.contentMimeType.equals("text/usfm")) {
                            verseTitle = Frame.parseVerseTitle(mSourceContainer.readChunk(slugs[0], slugs[1]), TranslationFormat.parse(mSourceContainer.contentMimeType));
                        }

                        LinearLayout exampleView = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.fragment_resources_example_item, null);
                        TextView referenceView = (TextView)exampleView.findViewById(R.id.reference);
                        HtmlTextView passageView = (HtmlTextView)exampleView.findViewById(R.id.passage);
                        referenceView.setText(projectTitle.trim() + " " + StringUtilities.formatNumber(slugs[0]) + ":" + verseTitle);
                        passageView.setHtmlFromString(mSourceContainer.readChunk(slugs[0], slugs[1]), true);
                        exampleView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                scrollToChunk(slugs[0], slugs[1]);
                            }
                        });
                        Typography.formatSub(getActivity(), TranslationType.SOURCE, referenceView, rc.language.slug, rc.language.direction);
                        Typography.formatSub(getActivity(), TranslationType.SOURCE, passageView, rc.language.slug, rc.language.direction);
                        examplesView.addView(exampleView);
                    }
                    if(exampleSlugs.size() > 0) examplesTitle.setVisibility(View.VISIBLE);
                }
            }
            Typography.formatTitle(getActivity(), TranslationType.SOURCE, seeAlsoTitle, rc.language.slug, rc.language.direction);
            Typography.formatTitle(getActivity(), TranslationType.SOURCE, examplesTitle, rc.language.slug, rc.language.direction);

            mScrollingResourcesDrawerContent.removeAllViews();
            mScrollingResourcesDrawerContent.addView(view);
        }
    }

    /**
     * Prepares the resources drawer with the translation note
     * @param note
     */
    private void renderTranslationNote(TranslationHelp note) {
        mTranslationNote = note;
        mTranslationWordId = null;
        mTranslationNoteId = null;
        mFrameId = null;
        mChapterId = null;

        final Door43Client library = App.getLibrary();
        final ResourceContainer sourceTranslation = getSelectedResourceContainer();
//        TranslationNote note = null;//getPreferredNote(sourceTranslation, chapterId, frameId, noteId);
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
                    } else if(span instanceof TranslationWordLinkSpan) {
                        ResourceContainer currentRC = getSelectedResourceContainer();
                        Pattern titlePattern = Pattern.compile("#(.*)");
                        ResourceContainer rc = ContainerCache.cacheClosest(library, currentRC.language.slug, "bible", "tw");

                        String word = rc.readChunk(span.getMachineReadable().toString(), "01");
                        if(!word.isEmpty()) {
                            Matcher linkMatch = titlePattern.matcher(word.trim());
                            String title = span.getMachineReadable().toString();
                            if (linkMatch.find()) {
                                title = linkMatch.group(1);
                            }
                            ((TranslationWordLinkSpan) span).setTitle(title);
                        } else {
                            return false;
                        }
                    } else if(span instanceof ShortReferenceSpan) {
                        ShortReferenceSpan link = (ShortReferenceSpan)span;
                        String chapterId = link.getChapter();
                        String chunkId = link.getVerse();
                        // TODO: 3/7/17 validate
                    }
                    return true;
                }
            }, new Span.OnClickListener() {
                @Override
                public void onClick(View view, Span span, int start, int end) {
                    String type = ((LinkSpan)span).getType();
                    if(type.equals("ta")) {
                        String url = span.getMachineReadable().toString();
                        ArticleLinkSpan link = ArticleLinkSpan.parse(url);
                        if(link != null) {
                            onTranslationArticleClick(link.getVolume(), link.getManual(), link.getId(), mResourcesDrawer.getLayoutParams().width);
                        }
                    } else if(type.equals("p")) {
                        String url = span.getMachineReadable().toString();
                        PassageLinkSpan link = new PassageLinkSpan("", url);
                        scrollToChunk(link.getChapterId(), link.getFrameId());
                    } else if(type.equals("m")) {
                        // markdown link
                        final String url = span.getMachineReadable().toString();
                        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
                                .setTitle(R.string.view_online)
                                .setMessage(R.string.use_internet_confirmation)
                                .setNegativeButton(R.string.title_cancel, null)
                                .setPositiveButton(R.string.label_continue, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                        startActivity(intent);
                                    }
                                })
                                .show();
                    } else if(type.equals("tw")) {
                        // translation word
                        ResourceContainer currentRC = getSelectedResourceContainer();
                        ResourceContainer rc = ContainerCache.cacheClosest(library, currentRC.language.slug, "bible", "tw");
                        if(rc != null) {
                            onTranslationWordClick(rc.slug, span.getMachineReadable().toString(), mResourcesDrawer.getLayoutParams().width);
                        }
                    } else if(type.equals("sr")) {
                        // reference
                        ShortReferenceSpan link = new ShortReferenceSpan(span.getMachineReadable().toString());
                        scrollToVerse(link.getChapter(), link.getVerse());
                    }
                }

                @Override
                public void onLongClick(View view, Span span, int start, int end) {

                }
            });

            title.setText(note.title);
            SourceLanguage sourceLanguage = library.index.getSourceLanguage(sourceTranslation.language.slug);
            Typography.format(getActivity(), TranslationType.SOURCE, title, sourceLanguage.slug, sourceLanguage.direction);

            // TRICKY: some helps are in markdown
            String markdownBody = note.body.replaceAll("<[bB][rR]\\s*\\\\?>", "\n");
            String htmlBody = new MarkdownProcessor().markdown(markdownBody);

            // render body
            descriptionView.setText(renderer.render(htmlBody));
            Typography.formatSub(getActivity(), TranslationType.SOURCE, descriptionView, sourceLanguage.slug, sourceLanguage.direction);
            descriptionView.setMovementMethod(LocalLinkMovementMethod.getInstance());

            mScrollingResourcesDrawerContent.removeAllViews();
            mScrollingResourcesDrawerContent.addView(view);
        }
    }

    /**
     * Prepares the resources drawer with the translation question
     * @param question the question to be displayed
     */
    private void renderTranslationQuestion(TranslationHelp question) {
        mTranslationWordId = null;
        mTranslationNoteId = null;
        mTranslationQuestion = question;

        // these are deprecated
        mFrameId = null;
        mChapterId = null;

        final Door43Client library = App.getLibrary();
        ResourceContainer sourceTranslation = getSelectedResourceContainer();
        SourceLanguage sourceLanguage = library.index.getSourceLanguage(sourceTranslation.language.slug);
        if(mResourcesDrawerContent != null && question != null) {
            mResourcesDrawerContent.setVisibility(View.GONE);
        }
        if(mScrollingResourcesDrawerContent != null) {
            mScrollingResourcesDrawerContent.setVisibility(View.VISIBLE);
            mScrollingResourcesDrawerContent.scrollTo(0, 0);
            LinearLayout view = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.fragment_resources_question, null);

//            mCloseResourcesDrawerButton.setText(question.getQuestion());

            TextView questionTitle = (TextView)view.findViewById(R.id.question_title);
            Typography.formatTitle(getActivity(), TranslationType.SOURCE, questionTitle, sourceLanguage.slug, sourceLanguage.direction);
            TextView questionView = (TextView)view.findViewById(R.id.question);
            TextView answerTitle = (TextView)view.findViewById(R.id.answer_title);
            Typography.formatTitle(getActivity(), TranslationType.SOURCE, answerTitle, sourceLanguage.slug, sourceLanguage.direction);
            TextView answerView = (TextView)view.findViewById(R.id.answer);

            questionView.setText(question.title);
            Typography.formatSub(getActivity(), TranslationType.SOURCE, questionView, sourceLanguage.slug, sourceLanguage.direction);
            answerView.setText(question.body);
            Typography.formatSub(getActivity(), TranslationType.SOURCE, answerView, sourceLanguage.slug, sourceLanguage.direction);

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
            if(savedInstanceState.containsKey(STATE_WORD_ID)) {
                mTranslationWordId = savedInstanceState.getString(STATE_WORD_ID);
            } else if(savedInstanceState.containsKey(STATE_HELP_TYPE)) {
                String type = savedInstanceState.getString(STATE_HELP_TYPE);
                TranslationHelp help = new TranslationHelp(savedInstanceState.getString(STATE_HELP_TITLE), savedInstanceState.getString(STATE_HELP_TITLE));
                if(type != null && type.equals("tn")) {
                    mTranslationNote = help;
                } else if(type != null && type.equals("tq")) {
                    mTranslationQuestion = help;
                }
            }
            if(savedInstanceState.containsKey(STATE_RESOURCE_CONTAINER_SLUG)) {
                mResourceContainerSlug = savedInstanceState.getString(STATE_RESOURCE_CONTAINER_SLUG);
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
        if(mTranslationNote != null) {
            out.putString(STATE_HELP_TITLE, mTranslationNote.title);
            out.putString(STATE_HELP_BODY, mTranslationNote.body);
            out.putString(STATE_HELP_TYPE, "tn");
        } else if(mTranslationQuestion != null) {
            out.putString(STATE_HELP_TITLE, mTranslationQuestion.title);
            out.putString(STATE_HELP_BODY, mTranslationQuestion.body);
            out.putString(STATE_HELP_TYPE, "tq");
        } else {
            out.remove(STATE_HELP_TITLE);
            out.remove(STATE_HELP_BODY);
            out.remove(STATE_HELP_TYPE);
        }
        out.putString(STATE_RESOURCE_CONTAINER_SLUG, mResourceContainerSlug);
        super.onSaveInstanceState(out);
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
        Door43Client library = App.getLibrary();
        // TRICKY: only english articles are supported
        List<Translation> translations = library.index.findTranslations("en", "ta-" + manual, volume, "man", null, 0, -1);
        if(translations.size() == 0) return null;
        ResourceContainer container = ContainerCache.cache(library, translations.get(0).resourceContainerSlug);
        if(container == null) return null;
        String title = container.readChunk(articleId, "title");
        String body = container.readChunk(articleId, "01");
        MarkdownProcessor md = new MarkdownProcessor();
        String html = md.markdown(body);

        return new TranslationArticle(volume, "ta-" + manual,  articleId, title, html, "test");
    }
}
