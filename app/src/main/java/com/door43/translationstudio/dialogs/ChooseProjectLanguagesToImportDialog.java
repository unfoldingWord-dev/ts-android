package com.door43.translationstudio.dialogs;

import android.app.DialogFragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.events.ChoseProjectLanguagesEvent;
import com.door43.translationstudio.events.ChoseProjectLanguagesToImportEvent;
import com.door43.translationstudio.network.Peer;
import com.door43.translationstudio.projects.Language;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.util.LanguageAdapter;
import com.door43.translationstudio.util.MainContext;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.util.Arrays;

/**
 * This dialog handles the final import step for projects downloaded from the server or from another device.
 *
 */
public class ChooseProjectLanguagesToImportDialog extends DialogFragment {
    private Project mProject = null;
    private Peer mPeer;

    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.import_project);
        View v = inflater.inflate(R.layout.dialog_import_project, container, false);

        if(mProject != null) {
            final LanguageAdapter adapter = new LanguageAdapter(Arrays.asList(mProject.getTargetLanguages()), this.getActivity(), true);

            // TODO: populate the form and list.
            String imageUri = "assets://"+ mProject.getImagePath();
            final float imageWidth = getResources().getDimension(R.dimen.model_list_item_image_width);

            final ImageView icon = (ImageView)v.findViewById(R.id.modelImage);
            final LinearLayout bodyLayout = (LinearLayout)v.findViewById(R.id.bodyLayout);
            TextView title = (TextView)v.findViewById(R.id.modelTitle);
            TextView description = (TextView)v.findViewById(R.id.modelDescription);
            Button cancelButton = (Button)v.findViewById(R.id.buttonCancel);
            Button okButton = (Button)v.findViewById(R.id.buttonOk);
            ListView list = (ListView)v.findViewById(R.id.languageListView);

            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dismiss();
                }
            });

            okButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // TODO: import project if languages have been selected otherwise display notice.
                    MainContext.getContext().showToastMessage("Let's import this!");
                    Language[] selectedItems = adapter.getSelectedItems();
                    MainContext.getEventBus().post(new ChoseProjectLanguagesToImportEvent(mPeer, mProject, selectedItems));
                    dismiss();
                }
            });

            // project
            title.setText(mProject.getTitle());
            description.setText(mProject.getDescription());

            // load image
            MainContext.getContext().getImageLoader().loadImage(imageUri, new SimpleImageLoadingListener() {
                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    icon.setImageBitmap(loadedImage);
                }
            });

            // TODO: populate language list.
            // TODO: if we do not already have this project source provide an option to import the source in addition to the translation.
            // TRICKY: source languages do not have translation indicators on them so we set this to true.

            list.setAdapter(adapter);
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    adapter.toggleSelected(i);
                }
            });

        } else {
            dismiss();
        }

        return v;
    }

    /**
     * Specifies the project that will be imported.
     * This must be called before showing the dialog.
     * @param p The project that will be imported. This is not a fully loaded project, it just contains basic project information and available languages.
     */
    public void setImportDetails(Peer peer, Project p) {
        mPeer = peer;
        mProject = p;
    }
}
