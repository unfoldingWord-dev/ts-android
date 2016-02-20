package com.door43.translationstudio.newui.publish;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.NativeSpeaker;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 2/19/2016.
 */
public class NativeSpeakerAdapter extends RecyclerView.Adapter<NativeSpeakerAdapter.GenericViewHolder> {

    private static final int TYPE_SECURITY_NOTICE = 0;
    private static final int TYPE_SPEAKER = 1;
    private static final int TYPE_CONTROLS = 2;
    private List<NativeSpeaker> mData = new ArrayList<>();
    private OnClickListener mListener;

    /**
     * Loads a new set of native speakers
     * @param speakers
     */
    public void setTranslators(List<NativeSpeaker> speakers) {
        mData = speakers;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if(position == 0) {
            return TYPE_SECURITY_NOTICE;
        } else if(position == getItemCount() - 1) {
            return TYPE_CONTROLS;
        } else {
            return TYPE_SPEAKER;
        }
    }

    @Override
    public GenericViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_SECURITY_NOTICE:
                View noticeView = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_publish_native_speaker_security_notice, parent, false);
                return new ViewHolderNotice(noticeView);
            case TYPE_CONTROLS:
                View controlsView = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_publish_native_speaker_controls, parent, false);
                return new ViewHolderControls(controlsView);
            case TYPE_SPEAKER:
            default:
                View speakerView = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_publish_native_speaker_item, parent, false);
                return new ViewHolderSpeaker(speakerView);
        }
    }

    @Override
    public void onBindViewHolder(GenericViewHolder holder, int position) {
        // TRICKY: only the Native Speaker holder uses the position so we must account for the privacy notice
        holder.loadView(mData, position - 1, mListener);
    }

    @Override
    public int getItemCount() {
        if(mData.size() > 0) {
            return mData.size() + 2; // add space for security info and controls
        } else {
            return 1 + 2; // display notice card explaining they must add a translator
        }
    }

    public void setOnClickListener(OnClickListener listener) {
        mListener = listener;
    }

    public interface OnClickListener {
        void onEditNativeSpeaker(NativeSpeaker speaker);
        void onClickAddNativeSpeaker();
        void onClickNext();
        void onClickPrivacyNotice();
    }

    public static abstract class GenericViewHolder extends RecyclerView.ViewHolder {

        public GenericViewHolder(View v) {
            super(v);
        }

        public abstract void loadView(List<NativeSpeaker> speakers, int position, OnClickListener listener);
    }

    public static class ViewHolderNotice extends GenericViewHolder {
        private final View mView;
        public ViewHolderNotice(View v) {
            super(v);
            mView = v;
        }

        @Override
        public void loadView(List<NativeSpeaker> speakers, int position, final OnClickListener listener) {
            mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClickPrivacyNotice();
                }
            });
        }
    }

    public static class ViewHolderSpeaker extends GenericViewHolder {
        private final TextView mNameView;
        private final ImageButton mEditButton;

        public ViewHolderSpeaker(View v) {
            super(v);

            mNameView = (TextView)v.findViewById(R.id.name);
            mEditButton = (ImageButton)v.findViewById(R.id.edit_button);
        }

        @Override
        public void loadView(final List<NativeSpeaker> speakers, final int position, final OnClickListener listener) {
            if(speakers.size() > 0) {
                mNameView.setText(speakers.get(position).toString());
                mEditButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onEditNativeSpeaker(speakers.get(position));
                    }
                });
            } else {
                // display notice a translator must be added
                mNameView.setText(R.string.who_translated_notice);
                mEditButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onClickAddNativeSpeaker();
                    }
                });
            }
        }
    }

    public static class ViewHolderControls extends GenericViewHolder {

        private final Button mNextButton;
        private final Button mAddButton;

        public ViewHolderControls(View v) {
            super(v);

            mNextButton = (Button)v.findViewById(R.id.next_button);
            mAddButton = (Button)v.findViewById(R.id.add_button);
        }

        @Override
        public void loadView(List<NativeSpeaker> speakers, int position, final OnClickListener listener) {
            mNextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClickNext();
                }
            });
            mAddButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClickAddNativeSpeaker();
                }
            });
        }
    }
}
