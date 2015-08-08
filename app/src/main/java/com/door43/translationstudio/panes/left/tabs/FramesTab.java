package com.door43.translationstudio.panes.left.tabs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.MainActivity;
import com.door43.translationstudio.R;
import com.door43.translationstudio.dialogs.ModelItemAdapter;
import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.projects.Project;
import com.door43.translationstudio.projects.TranslationManager;
import com.door43.translationstudio.util.AppContext;
import com.door43.translationstudio.util.TabsAdapterNotification;
import com.door43.translationstudio.util.TranslatorBaseFragment;

/**
 * Created by joel on 8/29/2014.
 */
public class FramesTab extends TranslatorBaseFragment implements TabsAdapterNotification {
    private FramesTab me = this;
    private ModelItemAdapter mModelItemAdapter;
    private ListView mListView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pane_left_frames, container, false);
        mListView = (ListView)view.findViewById(R.id.frames_list_view);

        // create adapter
        if(mModelItemAdapter == null) {
            Project p = AppContext.projectManager().getSelectedProject();
            if(p == null || p.getSelectedChapter() == null ) {
                mModelItemAdapter = new ModelItemAdapter(app(), new Model[]{}, 0, 0, "");
            } else {
                mModelItemAdapter = new ModelItemAdapter(app(), AppContext.projectManager().getSelectedProject().getSelectedChapter().getFrames(), 0, 0, "");
            }
        }

        mListView.setAdapter(mModelItemAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                TranslationManager.save();
                if(getActivity() != null) {
                    // select the new frame
                    AppContext.projectManager().getSelectedProject().getSelectedChapter().setSelectedFrame(i);
                    ((MainActivity) getActivity()).closeDrawers();
                    NotifyAdapterDataSetChanged();

                    // Display chapter translation dialog if translating a new chapter
                    if (!AppContext.projectManager().getSelectedProject().getSelectedChapter().translationInProgress()) {
                        // only display the chapter settings if the title and reference are not null
                        if (AppContext.projectManager().getSelectedProject().getSelectedChapter().hasChapterSettings()) {
                            ((MainActivity) getActivity()).showChapterSettingsMenu();
                        } else {
                            ((MainActivity) getActivity()).reload();
                        }
                    } else {
                        ((MainActivity) getActivity()).reload();
                    }
                } else {
                    Logger.e(this.getClass().getName(), "onItemClickListener the activity is null");
                }
            }
        });

        return view;
    }

    public void onResume() {
        super.onResume();
        if(mModelItemAdapter != null) {
            mModelItemAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void NotifyAdapterDataSetChanged() {
        Project p = AppContext.projectManager().getSelectedProject();
        if(mModelItemAdapter != null && p != null && p.getSelectedChapter() != null) {
            mModelItemAdapter.changeDataSet(p.getSelectedChapter().getFrames());
        } else if(mModelItemAdapter != null) {
            mModelItemAdapter.changeDataSet(new Model[]{});
        }
        if(mListView != null) {
            mListView.setSelectionAfterHeaderView();
        }
    }
}
