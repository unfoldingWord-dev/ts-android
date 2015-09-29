package com.door43.translationstudio.newui.translate;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.ContextThemeWrapper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Frame;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TranslationNote;
import com.door43.translationstudio.core.TranslationWord;
import com.door43.translationstudio.projects.Translation;
import com.door43.translationstudio.rendering.LinkRenderer;
import com.door43.translationstudio.spannables.PassageLinkSpan;
import com.door43.translationstudio.spannables.Span;
import com.door43.translationstudio.spannables.TermSpan;
import com.door43.translationstudio.util.AppContext;
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
    private GestureDetector mGesture;
    private boolean mResourcesOpen = false;
    private boolean mResourcesDrawerOpen = false;
    private CardView mResourcesDrawer;
    private ScrollView mResourcesDrawerContent;
    private Button mCloseResourcesDrawerButton;
    private SourceTranslation mSourceTranslation;
    private String mTranslationWordId;
    private String mTranslationNoteId;
    private String mFrameId;
    private String mChapterId;

    @Override
    ViewModeAdapter generateAdapter(Activity activity, String targetTranslationId, String sourceTranslationId, String chapterId, String frameId) {
        mSourceTranslation = AppContext.getLibrary().getSourceTranslation(sourceTranslationId);
        return new ReviewModeAdapter(activity, targetTranslationId, sourceTranslationId, chapterId, frameId, mResourcesOpen);
    }

    @Override
    protected void onPrepareView(final View rootView) {
        mResourcesDrawer = (CardView)rootView.findViewById(R.id.resources_drawer_card);
        mResourcesDrawerContent = (ScrollView)rootView.findViewById(R.id.resources_drawer_content);
        mCloseResourcesDrawerButton = (Button)rootView.findViewById(R.id.close_resources_drawer_btn);
        mCloseResourcesDrawerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeResourcesDrawer();
            }
        });

        mGesture = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
            public MotionEvent mLastOnDownEvent;
            private final float SWIPE_THRESHOLD_VELOCITY = 20f;
            private final float SWIPE_MIN_DISTANCE = 50f;
            private final float SWIPE_MAX_ANGLE_DEG = 30;
            @Override
            public boolean onDown(MotionEvent e) {
                mLastOnDownEvent = e;
                return super.onDown(e);
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if(e1 == null) {
                    e1 = mLastOnDownEvent;
                }
                try {
                    float distanceX = e2.getX() - e1.getX();
                    float distanceY = e2.getY() - e1.getY();
                    // don't handle vertical swipes (division error)
                    if (distanceX == 0) return false;

                    double flingAngle = Math.toDegrees(Math.asin(Math.abs(distanceY / distanceX)));
                    if (flingAngle <= SWIPE_MAX_ANGLE_DEG && Math.abs(distanceX) >= SWIPE_MIN_DISTANCE && Math.abs(velocityX) >= SWIPE_THRESHOLD_VELOCITY) {
                        if (distanceX > 0) {
                            onRightSwipe();
                        } else {
                            onLeftSwipe();

                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
        });

        // open the drawer on rotate
        if(mResourcesDrawerOpen && mResourcesOpen) {
            ViewTreeObserver viewTreeObserver = rootView.getViewTreeObserver();
            if(viewTreeObserver.isAlive()) {
                viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
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
                            }
                        }
                    }
                });
            }
        }
        closeResourcesDrawer();
    }

    private void onRightSwipe() {
        if(mResourcesDrawerOpen) {
            closeResourcesDrawer();
        } else {
            if (getAdapter() != null) {
                ((ReviewModeAdapter) getAdapter()).closeResources();
            }
        }
    }

    private void onLeftSwipe() {
        if(getAdapter() != null) {
            ((ReviewModeAdapter)getAdapter()).openResources();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(mGesture != null) {
            return mGesture.onTouchEvent(event);
        } else {
            Logger.w(this.getClass().getName(), "The gesture dectector was not initialized so the touch was not handled");
            return false;
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

    /**
     * Prepares the resoruces drawer with the translation word
     * @param translationWordId
     */
    private void renderTranslationWord(String translationWordId) {
        mTranslationWordId = translationWordId;
        mTranslationNoteId = null;

        Library library = AppContext.getLibrary();
        TranslationWord word = library.getTranslationWord(mSourceTranslation, translationWordId);
        if(mResourcesDrawerContent != null) {
            mResourcesDrawerContent.scrollTo(0, 0);
            LinearLayout view = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.fragment_resources_word, null);

            mCloseResourcesDrawerButton.setText(word.getTitle());

            TextView descriptionTitle = (TextView)view.findViewById(R.id.description_title);
            HtmlTextView descriptionView = (HtmlTextView)view.findViewById(R.id.description);
            TextView seeAlsoTitle = (TextView)view.findViewById(R.id.see_also_title);
            FlowLayout seeAlsoView = (FlowLayout)view.findViewById(R.id.see_also);
            LinearLayout examplesView = (LinearLayout)view.findViewById(R.id.examples);
            TextView examplesTitle = (TextView)view.findViewById(R.id.examples_title);

            descriptionTitle.setText(word.getDefinitionTitle());
            descriptionView.setHtmlFromString(word.getDefinition(), true);

            seeAlsoView.removeAllViews();
            for(int i = 0; i < word.getSeeAlso().length; i ++) {
                final TranslationWord relatedWord = library.getTranslationWord(mSourceTranslation, word.getSeeAlso()[i]);
                if(relatedWord != null) {
                    Button button = new Button(new ContextThemeWrapper(getActivity(), R.style.Widget_Button_Tag), null, R.style.Widget_Button_Tag);
                    button.setText(relatedWord.getTitle());
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                             onTranslationWordClick(relatedWord.getId(), mResourcesDrawer.getLayoutParams().width);
                        }
                    });
                    seeAlsoView.addView(button);
                }
            }
            if(word.getSeeAlso().length > 0) {
                seeAlsoTitle.setVisibility(View.VISIBLE);
            } else {
                seeAlsoTitle.setVisibility(View.GONE);
            }

            examplesView.removeAllViews();
            for(final TranslationWord.Example example:word.getExamples()) {
                LinearLayout exampleView = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.fragment_pane_right_resources_example_item, null);
                TextView referenceView = (TextView)exampleView.findViewById(R.id.reference);
                HtmlTextView passageView = (HtmlTextView)exampleView.findViewById(R.id.passage);
                Frame frame = library.getFrame(mSourceTranslation, example.getChapterId(), example.getFrameId());
                referenceView.setText(mSourceTranslation.getProjectTitle() + " " + Integer.parseInt(example.getChapterId()) + ":" + frame.getTitle());
                passageView.setHtmlFromString(example.getPassage(), true);
                exampleView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        scrollToFrame(example.getChapterId(), example.getFrameId());
                    }
                });
                examplesView.addView(exampleView);
            }
            if(word.getExamples().length > 0) {
                examplesTitle.setVisibility(View.VISIBLE);
            } else {
                examplesTitle.setVisibility(View.GONE);
            }

            mResourcesDrawerContent.removeAllViews();
            mResourcesDrawerContent.addView(view);
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
        TranslationNote note = library.getTranslationNote(mSourceTranslation, chapterId, frameId, noteId);
        if(mResourcesDrawerContent != null) {
            mResourcesDrawerContent.scrollTo(0, 0);
            LinearLayout view = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.fragment_resources_note, null);

            mCloseResourcesDrawerButton.setText(note.getTitle());

            TextView title = (TextView)view.findViewById(R.id.title);
            TextView description = (TextView)view.findViewById(R.id.description);

            LinkRenderer renderer = new LinkRenderer(new LinkRenderer.OnPreprocessLink() {
                @Override
                public boolean onPreprocess(PassageLinkSpan span) {
                    Frame frame = library.getFrame(mSourceTranslation, span.getChapterId(), span.getFrameId());
                    String title = mSourceTranslation.getProjectTitle() + " " + Integer.parseInt(span.getChapterId()) + ":" + frame.getTitle();
                    span.setTitle(title);
                    return library.getFrame(mSourceTranslation, span.getChapterId(), span.getFrameId()) != null;
                }
            }, new Span.OnClickListener() {
                @Override
                public void onClick(View view, Span span, int start, int end) {
                    PassageLinkSpan link = (PassageLinkSpan)span;
                    scrollToFrame(link.getChapterId(), link.getFrameId());
                }
            });

            title.setText(note.getTitle());
            description.setText(renderer.render(Html.fromHtml(note.getBody())));
            ViewUtil.makeLinksClickable(description);

            mResourcesDrawerContent.removeAllViews();
            mResourcesDrawerContent.addView(view);
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
            out.remove(STATE_CHAPTER_ID);
            out.remove(STATE_FRAME_ID);
        }
        super.onSaveInstanceState(out);
    }
}
