package com.door43.translationstudio.dialogs;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Frame;
import com.door43.translationstudio.rendering.DefaultRenderer;
import com.door43.translationstudio.rendering.USXRenderer;

/**
 * Created by joel on 5/18/2015.
 */
public class RenderedTextDialog extends DialogFragment {
    public static final String ARG_TITLE = "title_text";
    public static final String ARG_BODY = "body_text";
    public static final String ARG_BODY_FORMAT = "body_format";
    public static final String ARG_TITLE_FORMAT = "title_format";


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        Bundle args = getArguments();
        CharSequence titleText = args.getCharSequence(ARG_TITLE);
        CharSequence bodyText = args.getCharSequence(ARG_BODY);

        Frame.Format bodyFormat;
        if(args.get(ARG_BODY_FORMAT) instanceof  Frame.Format) {
            bodyFormat = (Frame.Format) args.get(ARG_BODY_FORMAT);
        } else {
            bodyFormat = Frame.Format.DEFAULT;
        }

        Frame.Format titleFormat;
        if(args.get(ARG_TITLE_FORMAT) instanceof  Frame.Format) {
            titleFormat = (Frame.Format) args.get(ARG_TITLE_FORMAT);
        } else {
            titleFormat = Frame.Format.DEFAULT;
        }

        View v = inflater.inflate(R.layout.dialog_frame_preview, container, false);
        TextView body = (TextView)v.findViewById(R.id.bodyTextView);
        TextView title = (TextView)v.findViewById(R.id.titleTextView);
        Button okButton = (Button)v.findViewById(R.id.okButton);

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        CharSequence renderedBodyText = "";
        if(bodyFormat == Frame.Format.USX) {
            USXRenderer engine = new USXRenderer();
            renderedBodyText = engine.render(bodyText);
        } else {
            DefaultRenderer engine = new DefaultRenderer();
            renderedBodyText = engine.render(bodyText);
        }
        body.setText(renderedBodyText);

        CharSequence renderedTitleText = "";
        if(titleFormat == Frame.Format.USX) {
            USXRenderer engine = new USXRenderer();
            renderedTitleText = engine.render(titleText);
        } else {
            DefaultRenderer engine = new DefaultRenderer();
            renderedTitleText = engine.render(titleText);
        }
        title.setText(renderedTitleText);

        return v;
    }
}
