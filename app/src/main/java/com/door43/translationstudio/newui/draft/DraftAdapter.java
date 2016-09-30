package com.door43.translationstudio.newui.draft;

import android.app.Activity;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.translationstudio.core.TranslationFormat;
import com.door43.translationstudio.core.Typography;
import com.door43.translationstudio.rendering.ClickableRenderingEngine;
import com.door43.translationstudio.rendering.Clickables;
import com.door43.translationstudio.rendering.DefaultRenderer;
import com.door43.translationstudio.rendering.RenderingGroup;
import com.door43.translationstudio.spannables.NoteSpan;
import com.door43.translationstudio.spannables.Span;
import com.door43.widget.ViewUtil;

import org.json.JSONException;
import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.models.SourceLanguage;
import org.unfoldingword.resourcecontainer.ResourceContainer;

import java.io.IOException;


/**
 * Created by blm on 1/10/2016.
 */
public class DraftAdapter extends RecyclerView.Adapter<DraftAdapter.ViewHolder> {

    private SourceLanguage mSourceLanguage;
    private CharSequence[] mRenderedDraftBody;
    private final Activity mContext;

    private ResourceContainer mDraftTranslation;
    private final Door43Client mLibrary;
    private String[] mChapters;
    private int mLayoutBuildNumber = 0;

    public DraftAdapter(Activity context, ResourceContainer draftTranslation) {
        mLibrary = App.getLibrary();
        mContext = context;
        mDraftTranslation = draftTranslation;
        mSourceLanguage = null;
        try {
            mSourceLanguage = mLibrary.index().getSourceLanguage(mDraftTranslation.info.getJSONObject("language").getString("slug"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mChapters = draftTranslation.chapters();
        mRenderedDraftBody = new CharSequence[mChapters.length];
    }

    /**
     * Updates the draft translation displayed
     * @param draftTranslation
     */
    public void setDraftTranslation(ResourceContainer draftTranslation) {
        mDraftTranslation = draftTranslation;
        mSourceLanguage = null;
        try {
            mSourceLanguage = mLibrary.index().getSourceLanguage(mDraftTranslation.info.getJSONObject("language").getString("slug"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mChapters = draftTranslation.chapters();
        mRenderedDraftBody = new CharSequence[mChapters.length];
        notifyDataSetChanged();
    }

    public String getFocusedFrameId(int position) {
        return null;
    }

    public String getFocusedChapterId(int position) {
        if(position >= 0 && position < mChapters.length) {
            return mChapters[position];
        } else {
            return null;
        }
    }

    public int getItemPosition(String chapterId, String frameId) {
        for(int i = 0; i < mChapters.length; i ++) {
            if(mChapters[i].equals(chapterId)) {
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

        final String chapterSlug = mChapters[position];

        // render the draft chapter body
        if(mRenderedDraftBody[position] == null) {
            String chapterBody = "";
            for (String chunk:mDraftTranslation.chunks(chapterSlug)) {
                try {
                    chapterBody += mDraftTranslation.readChunk(chapterSlug, chunk);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
//            String chapterBody = getChapterBody(mDraftTranslation, chapterSlug.getId());/
            TranslationFormat bodyFormat = null;// mLibrary.getChapterBodyFormat(mDraftTranslation, chapterSlug.getId());
            try {
                bodyFormat = TranslationFormat.parse(mDraftTranslation.info.getString("content_mime_type"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            RenderingGroup sourceRendering = new RenderingGroup();
            if (Clickables.isClickableFormat(bodyFormat)) {
                // TODO: add click listeners
                Span.OnClickListener noteClickListener = new Span.OnClickListener() {
                    @Override
                    public void onClick(View view, Span span, int start, int end) {
                        if(span instanceof NoteSpan) {
                            new AlertDialog.Builder(mContext, R.style.AppTheme_Dialog)
                                    .setTitle(R.string.title_note)
                                    .setMessage(((NoteSpan)span).getNotes())
                                    .setPositiveButton(R.string.dismiss, null)
                                    .show();
                        }
                    }

                    @Override
                    public void onLongClick(View view, Span span, int start, int end) {

                    }
                };
                ClickableRenderingEngine renderer = Clickables.setupRenderingGroup(bodyFormat, sourceRendering, null, noteClickListener, true);

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
        String chapterTitle = null;//chapterSlug.title;
        try {
            chapterTitle = mDraftTranslation.readChunk(chapterSlug, "title");
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(chapterTitle == null) {
            try {
                chapterTitle = mDraftTranslation.readChunk("front", "title") + " " + Integer.parseInt(chapterSlug);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        holder.mSourceTitle.setText(chapterTitle);

        // set up fonts
        if(holder.mLayoutBuildNumber != mLayoutBuildNumber) {
            holder.mLayoutBuildNumber = mLayoutBuildNumber;
            Typography.formatTitle(mContext, holder.mSourceHeading, mSourceLanguage.slug, mSourceLanguage.direction);
            Typography.formatTitle(mContext, holder.mSourceTitle, mSourceLanguage.slug, mSourceLanguage.direction);
            Typography.format(mContext, holder.mSourceBody, mSourceLanguage.slug, mSourceLanguage.direction);
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
