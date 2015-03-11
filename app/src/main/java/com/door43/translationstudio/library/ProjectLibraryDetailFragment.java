package com.door43.translationstudio.library;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.door43.translationstudio.R;

import com.door43.translationstudio.library.temp.LibraryTempData;
import com.door43.translationstudio.projects.Project;

/**
 * A fragment representing a single Project detail screen.
 * This fragment is either contained in a {@link ProjectLibraryListActivity}
 * in two-pane mode (on tablets) or a {@link ProjectLibraryDetailActivity}
 * on handsets.
 */
public class ProjectLibraryDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_INDEX = "item_id";

    /**
     * The dummy content this fragment is presenting.
     */
    private Project mItem;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ProjectLibraryDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_INDEX)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            mItem = LibraryTempData.getProject(getArguments().getInt(ARG_ITEM_INDEX));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_project_library_detail, container, false);

        // Show the dummy content as text in a TextView.
        if (mItem != null) {
            ((TextView) rootView.findViewById(R.id.project_detail)).setText(mItem.getTitle());
        }

        return rootView;
    }
}
