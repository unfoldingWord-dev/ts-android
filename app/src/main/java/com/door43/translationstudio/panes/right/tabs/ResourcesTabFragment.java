package com.door43.translationstudio.panes.right.tabs;

import android.content.res.Resources;
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
 * Created by joel on 9/29/2014.
 * @deprecated
 */
public class ResourcesTabFragment extends TranslatorBaseFragment implements TabsFragmentAdapterNotification {
    private ResourcesTabFragment me = this;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pane_right_resources, container, false);

        return view;
    }

    @Override
    public void NotifyAdapterDataSetChanged() {

    }
}
