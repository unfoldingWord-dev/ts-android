package com.door43.translationstudio.newui.draft;

import android.app.Activity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.door43.translationstudio.AppContext;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Chapter;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.SourceLanguage;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TranslationFormat;
import com.door43.translationstudio.core.Typography;
import com.door43.translationstudio.dialogs.CustomAlertDialog;
import com.door43.translationstudio.rendering.DefaultRenderer;
import com.door43.translationstudio.rendering.RenderingGroup;
import com.door43.translationstudio.rendering.USXRenderer;
import com.door43.translationstudio.spannables.NoteSpan;
import com.door43.translationstudio.spannables.Span;
import com.door43.widget.ViewUtil;


/**
 * Created by blm on 1/10/2016.
 */
public class DraftAdapter extends RecyclerView.Adapter<DraftAdapter.ViewHolder> {

    private SourceLanguage mSourceLanguage;
    private CharSequence[] mRenderedDraftBody;
    private final Activity mContext;

    private SourceTranslation mDraftTranslation;
    private final Library mLibrary;
    private Chapter[] mChapters;
    private int mLayoutBuildNumber = 0;

    public DraftAdapter(Activity context, SourceTranslation draftTranslation) {
        mLibrary = AppContext.getLibrary();
        mContext = context;
        mDraftTranslation = draftTranslation;
        mSourceLanguage = mLibrary.getSourceLanguage(mDraftTranslation.projectSlug, mDraftTranslation.sourceLanguageSlug);
        mChapters = mLibrary.getChapters(mDraftTranslation);
        mRenderedDraftBody = new CharSequence[mChapters.length];
    }

    /**
     * Updates the draft translation displayed
     * @param draftTranslation
     */
    public void setDraftTranslation(SourceTranslation draftTranslation) {
        mDraftTranslation = draftTranslation;
        mSourceLanguage = mLibrary.getSourceLanguage(mDraftTranslation.projectSlug, mDraftTranslation.sourceLanguageSlug);
        mChapters = mLibrary.getChapters(mDraftTranslation);
        mRenderedDraftBody = new CharSequence[mChapters.length];
        notifyDataSetChanged();
    }

    public String getFocusedFrameId(int position) {
        return null;
    }

    public String getFocusedChapterId(int position) {
        if(position >= 0 && position < mChapters.length) {
            return mChapters[position].getId();
        } else {
            return null;
        }
    }

    public int getItemPosition(String chapterId, String frameId) {
        for(int i = 0; i < mChapters.length; i ++) {
            Chapter chapter = mChapters[i];
            if(chapter.getId().equals(chapterId)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_draft_list_item, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        int cardMargin = mContext.getResources().getDimensionPixelSize(R.dimen.card_margin);
        int stackedCardMargin = mContext.getResources().getDimensionPixelSize(R.dimen.stacked_card_margin);

        CardView.LayoutParams sourceParams = (CardView.LayoutParams)holder.mSourceCard.getLayoutParams();
        sourceParams.setMargins(cardMargin, cardMargin, stackedCardMargin, stackedCardMargin);
        holder.mSourceCard.setLayoutParams(sourceParams);
        ((View) holder.mSourceCard.getParent()).requestLayout();
        ((View) holder.mSourceCard.getParent()).invalidate();

        final Chapter chapter = mChapters[position];

        // render the draft chapter body
        if(mRenderedDraftBody[position] == null) {
            String chapterBody = mLibrary.getChapterBody(mDraftTranslation, chapter.getId());
            TranslationFormat bodyFormat = mLibrary.getChapterBodyFormat(mDraftTranslation, chapter.getId());
            RenderingGroup sourceRendering = new RenderingGroup();
            if (bodyFormat == TranslationFormat.USX) {
                // TODO: add click listeners
                USXRenderer renderer = new USXRenderer(null, new Span.OnClickListener() {
                    @Override
                    public void onClick(View view, Span span, int start, int end) {
                        if(span instanceof NoteSpan) {
                            CustomAlertDialog.Create(mContext)
                                    .setTitle(R.string.title_note)
                                    .setMessage(((NoteSpan)span).getNotes())
                                    .setPositiveButton(R.string.dismiss, null)
                                    .show("note");
                        }
                    }

                    @Override
                    public void onLongClick(View view, Span span, int start, int end) {

                    }
                });
                sourceRendering.addEngine(renderer);

                // In read mode (and only in read mode), pull leading major section headings out for
                // display above chapter headings.
                renderer.setSuppressLeadingMajorSectionHeadings(true);
                CharSequence heading = renderer.getLeadingMajorSectionHeading(chapterBody);
                holder.mSourceHeading.setText(heading);
                holder.mSourceHeading.setVisibility(
                        heading.length() > 0 ? View.VISIBLE : View.GONE);
            } else {
                sourceRendering.addEngine(new DefaultRenderer());
            }
            sourceRendering.init(chapterBody);
            mRenderedDraftBody[position] = sourceRendering.start();
        }

        holder.mSourceBody.setText(mRenderedDraftBody[position]);
        ViewUtil.makeLinksClickable(holder.mSourceBody);
        String chapterTitle = chapter.title;
        if(chapter.title.isEmpty()) {
            chapterTitle = mDraftTranslation.getProjectTitle() + " " + Integer.parseInt(chapter.getId());
        }
        holder.mSourceTitle.setText(chapterTitle);

        // set up fonts
        if(holder.mLayoutBuildNumber != mLayoutBuildNumber) {
            holder.mLayoutBuildNumber = mLayoutBuildNumber;
            Typography.formatTitle(mContext, holder.mSourceHeading, mSourceLanguage.getId(), mSourceLanguage.getDirection());
            Typography.formatTitle(mContext, holder.mSourceTitle, mSourceLanguage.getId(), mSourceLanguage.getDirection());
            Typography.format(mContext, holder.mSourceBody, mSourceLanguage.getId(), mSourceLanguage.getDirection());
        }
    }

    @Override
    public int getItemCount() {
        return mChapters.length;
    }

    public void rebuild() {
        mLayoutBuildNumber ++;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final CardView mSourceCard;
        public TextView mSourceHeading;
        public TextView mSourceTitle;
        public TextView mSourceBody;
        public int mLayoutBuildNumber = -1;

        public ViewHolder(View v) {
            super(v);
            mSourceCard = (CardView)v.findViewById(R.id.source_translation_card);
            mSourceHeading = (TextView)v.findViewById(R.id.source_translation_heading);
            mSourceTitle = (TextView)v.findViewById(R.id.source_translation_title);
            mSourceBody = (TextView)v.findViewById(R.id.source_translation_body);
        }
    }
}
