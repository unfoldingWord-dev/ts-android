package com.door43.translationstudio.panes.left.tabs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.door43.translationstudio.MainActivity;
import com.door43.translationstudio.R;
import com.door43.translationstudio.util.TabsFragmentAdapterNotification;
import com.door43.translationstudio.util.TranslatorBaseFragment;

/**
 * Created by joel on 8/29/2014.
 */
public class ChaptersTabFragment extends TranslatorBaseFragment implements TabsFragmentAdapterNotification{
    private ChaptersTabFragment me = this;
    private ChaptersItemAdapter mChapterItemAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pane_left_chapters, container, false);
        ListView listView = (ListView)view.findViewById(R.id.chapters_list_view);

        // create adapter
        if(mChapterItemAdapter == null) mChapterItemAdapter = new ChaptersItemAdapter(app());

        // connect adapter
        listView.setAdapter(mChapterItemAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // select the chapter
                Integer key = (Integer)app().getSharedProjectManager().getSelectedProject().getChaptersKeySet().get(i);
                app().getSharedProjectManager().getSelectedProject().setSelectedChapter(key);
                // open up the frames tab
                ((MainActivity)me.getActivity()).getLeftPane().selectTab(2);
            }
        });

        return view;
    }

    @Override
    public void NotifyAdapterDataSetChanged() {
        mChapterItemAdapter.notifyDataSetChanged();
    }
}
