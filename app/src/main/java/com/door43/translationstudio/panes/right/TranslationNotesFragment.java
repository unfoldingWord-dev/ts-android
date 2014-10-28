package com.door43.translationstudio.panes.right;



import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.door43.translationstudio.R;
import com.door43.translationstudio.util.TranslatorBaseFragment;

/**
 * A simple {@link Fragment} subclass.
 *
 */
public class TranslationNotesFragment extends TranslatorBaseFragment {

    private View mainView;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pane_right_resources_notes, container, false);

        mainView = view.findViewById(R.id.resourcesNotesView);

        return view;
    }


    public void showNotes() {

    }

    public void show() {
        if(mainView != null) {
            mainView.setVisibility(View.VISIBLE);
        }
    }

    public void hide() {
        // TODO: fade out
        if(mainView != null) {
            mainView.setVisibility(View.GONE);
        }
    }
}