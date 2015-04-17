package com.door43.translationstudio.panes.left.tabs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.door43.translationstudio.MainActivity;
import com.door43.translationstudio.R;
import com.door43.translationstudio.dialogs.ModelItemAdapter;
import com.door43.translationstudio.projects.Model;
import com.door43.translationstudio.util.AppContext;
import com.door43.util.Logger;
import com.door43.translationstudio.util.TabsFragmentAdapterNotification;
import com.door43.translationstudio.util.TranslatorBaseFragment;

/**
 * Created by joel on 8/29/2014.
 */
public class ChaptersTabFragment extends TranslatorBaseFragment implements TabsFragmentAdapterNotification{
    private ChaptersTabFragment me = this;
    private ModelItemAdapter mModelItemAdapter;
    private ListView mListView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pane_left_chapters, container, false);
        mListView = (ListView)view.findViewById(R.id.chapters_list_view);

        // create adapter
        if(mModelItemAdapter == null) {
            if(AppContext.projectManager().getSelectedProject() == null) {
                mModelItemAdapter = new ModelItemAdapter(app(), new Model[]{});
            } else {
                mModelItemAdapter = new ModelItemAdapter(app(), AppContext.projectManager().getSelectedProject().getChapters());
            }
        }
        // connectAsync adapter
        mListView.setAdapter(mModelItemAdapter);
        mListView.deferNotifyDataSetChanged();
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(getActivity() != null) {
                    // save changes to the current frame first
                    ((MainActivity) getActivity()).save();
                    // select the chapter
                    AppContext.projectManager().getSelectedProject().setSelectedChapter(i);
                    // reload the center pane so we don't accidently overwrite a frame
                    ((MainActivity) getActivity()).reload();
                    // open up the frames tab
                    ((MainActivity) getActivity()).openFramesTab();
                    // let the adapter redraw itself so the selected chapter is corectly highlighted
                    NotifyAdapterDataSetChanged();
                } else{
                    Logger.e(this.getClass().getName(), "onItemClickListener the activity is null");
                }
            }
        });

        return view;
    }

    @Override
    public void NotifyAdapterDataSetChanged() {
        if(mModelItemAdapter != null && AppContext.projectManager().getSelectedProject() != null) {
            mModelItemAdapter.changeDataSet(AppContext.projectManager().getSelectedProject().getChapters());
        } else if(mModelItemAdapter != null)  {
            mModelItemAdapter.changeDataSet(new Model[]{});
        }
        if(mListView != null) {
            mListView.setSelectionAfterHeaderView();
        }
    }
}
