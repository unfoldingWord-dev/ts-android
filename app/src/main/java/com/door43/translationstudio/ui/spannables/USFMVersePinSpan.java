package com.door43.translationstudio.ui.spannables;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.widget.ViewUtil;

/**
 * Created by joel on 10/1/2015.
 */
public class USFMVersePinSpan extends USFMVerseSpan {

    private SpannableStringBuilder mSpannable;

    public USFMVersePinSpan(String verse) {
        super(verse);
    }

    public USFMVersePinSpan(int verse) {
        super(verse);
    }

    public SpannableStringBuilder render() {
        if(mSpannable == null) {
            mSpannable = super.render();
            // apply custom styles
            mSpannable.setSpan(new RelativeSizeSpan(0.8f), 0, mSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            mSpannable.setSpan(new ForegroundColorSpan(App.context().getResources().getColor(R.color.white)), 0, mSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            LayoutInflater inflater = (LayoutInflater) App.context().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            FrameLayout verseLayout = (FrameLayout)inflater.inflate(R.layout.fragment_verse_marker, null);
            TextView verseTitle = (TextView)verseLayout.findViewById(R.id.verse);

            if(getEndVerseNumber() > 0) {
                verseTitle.setText(getStartVerseNumber() + "-" + getEndVerseNumber());
            } else {
                verseTitle.setText("" + getStartVerseNumber());
            }
            Bitmap image = ViewUtil.convertToBitmap(verseLayout);
            BitmapDrawable background = new BitmapDrawable(App.context().getResources(), image);
            background.setBounds(0, 0, background.getMinimumWidth(), background.getMinimumHeight());
            mSpannable.setSpan(new ImageSpan(background), 0, mSpannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        }
        return mSpannable;
    }
}

