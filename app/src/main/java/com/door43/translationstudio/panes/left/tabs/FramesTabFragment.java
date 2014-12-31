package com.door43.translationstudio.panes.left.tabs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.door43.translationstudio.MainActivity;
import com.door43.translationstudio.R;
import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.util.TabsFragmentAdapterNotification;
import com.door43.translationstudio.util.TranslatorBaseFragment;

/**
 * Created by joel on 8/29/2014.
 */
public class FramesTabFragment extends TranslatorBaseFragment implements TabsFragmentAdapterNotification {
    private FramesTabFragment me = this;
    private ModelItemAdapter mModelItemAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pane_left_frames, container, false);
        ListView listView = (ListView)view.findViewById(R.id.frames_list_view);

        // create adapter
        if(mModelItemAdapter == null) {
            if(app().getSharedProjectManager().getSelectedProject() == null) {
                mModelItemAdapter = new ModelItemAdapter(app(), new Model[]{});
            } else {
                mModelItemAdapter = new ModelItemAdapter(app(), app().getSharedProjectManager().getSelectedProject().getSelectedChapter().getFrames());
            }
        }

        listView.setAdapter(mModelItemAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // save changes to the current frame first
                ((MainActivity)me.getActivity()).save();
                // select the new frame
                app().getSharedProjectManager().getSelectedProject().getSelectedChapter().setSelectedFrame(i);
                ((MainActivity)me.getActivity()).reloadCenterPane();
                // we're ready to begin translating. close the left pane
                ((MainActivity)me.getActivity()).closeDrawers();
                // let the adapter redraw itself so the selected frame is corectly highlighted
                NotifyAdapterDataSetChanged();

                // Display chapter translation dialog if translating a new chapter
                if(!app().getSharedProjectManager().getSelectedProject().getSelectedChapter().translationInProgress()) {
                    // reload the content so we don't overwrite stuff
                    ((MainActivity)me.getActivity()).showChapterSettingsMenu();
                }
            }
        });

        return view;
    }

    @Override
    public void NotifyAdapterDataSetChanged() {
        if(mModelItemAdapter != null && app().getSharedProjectManager().getSelectedProject() != null) {
            mModelItemAdapter.changeDataSet(app().getSharedProjectManager().getSelectedProject().getSelectedChapter().getFrames());
        }
    }
}
