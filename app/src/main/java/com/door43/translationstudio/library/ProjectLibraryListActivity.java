package com.door43.translationstudio.library;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.door43.translationstudio.R;
import com.door43.translationstudio.library.temp.LibraryTempData;
import com.door43.translationstudio.util.TranslatorBaseActivity;

/**
 * An activity representing a list of Projects. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link ProjectLibraryDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p/>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link ProjectLibraryListFragment} and the item details
 * (if present) is a {@link ProjectLibraryDetailFragment}.
 * <p/>
 * This activity also implements the required
 * {@link ProjectLibraryListFragment.Callbacks} interface
 * to listen for item selections.
 */
public class ProjectLibraryListActivity extends TranslatorBaseActivity implements ProjectLibraryListFragment.Callbacks, TranslationDraftsTabFragment.Callbacks {

    public static final String ARG_ONLY_SHOW_UPDATES = "only_show_updates";
    public static final String ARG_ONLY_SHOW_NEW = "only_show_new";
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_library_list);
        // Show the Up button in the action bar.
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (findViewById(R.id.project_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            ((ProjectLibraryListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.project_list))
                    .setActivateOnItemClick(true);

        }

        // TODO: If exposing deep links into your app, handle intents here.
    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        int id = item.getItemId();
//        if (id == android.R.id.home) {
//            // This ID represents the Home or Up button. In the case of this
//            // activity, the Up button is shown. Use NavUtils to allow users
//            // to navigate up one level in the application structure. For
//            // more details, see the Navigation pattern on Android Design:
//            //
//            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
//            //
//            NavUtils.navigateUpFromSameTask(this);
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }

    /**
     * Callback method from {@link ProjectLibraryListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(int index) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
//            Bundle arguments = new Bundle();
            Bundle arguments = getIntent().getExtras();
            arguments.putInt(ProjectLibraryDetailFragment.ARG_ITEM_INDEX, index);
            ProjectLibraryDetailFragment fragment = new ProjectLibraryDetailFragment();
            if(arguments != null) {
                fragment.setArguments(arguments);
            }
            getFragmentManager().beginTransaction().replace(R.id.project_detail_container, fragment).commit();

        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, ProjectLibraryDetailActivity.class);
            Bundle arguments = getIntent().getExtras();
            if(arguments != null) {
                detailIntent.putExtras(arguments);
            }
            detailIntent.putExtra(ProjectLibraryDetailFragment.ARG_ITEM_INDEX, index + "");
            startActivity(detailIntent);
        }
    }

    @Override
    public void onEmptyDraftsList() {
        ProjectLibraryDetailFragment fragment = (ProjectLibraryDetailFragment)getFragmentManager().findFragmentById(R.id.project_detail_container);
        fragment.hideDraftsTab();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_project_library, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(LibraryTempData.getShowNewProjects()) {
            menu.findItem(R.id.action_show_new).setVisible(false);
            menu.findItem(R.id.action_download_all).setVisible(true);
        } else{
            menu.findItem(R.id.action_show_new).setVisible(true);
            menu.findItem(R.id.action_download_all).setVisible(false);
        }
        if(LibraryTempData.getShowProjectUpdates()) {
            menu.findItem(R.id.action_show_updates).setVisible(false);
            menu.findItem(R.id.action_update_all).setVisible(true);
        } else {
            menu.findItem(R.id.action_show_updates).setVisible(true);
            menu.findItem(R.id.action_update_all).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_show_new:
                LibraryTempData.setShowProjectUpdates(false);
                LibraryTempData.setShowNewProjects(true);
                reload();
                return true;
            case R.id.action_show_updates:
                LibraryTempData.setShowProjectUpdates(true);
                LibraryTempData.setShowNewProjects(false);
                reload();
                return true;
            case R.id.action_update_all:
                // TODO: display confirmation
                return true;
            case R.id.action_download_all:
                // TODO: display confirmation
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void reload() {
        invalidateOptionsMenu();
        if(mTwoPane) {
            // remove details
            if(getFragmentManager().findFragmentById(R.id.project_detail_container) != null) {
                getFragmentManager().beginTransaction().remove(getFragmentManager().findFragmentById(R.id.project_detail_container)).commit();
            }
        }
        // reload list
        ProjectLibraryListFragment listFragment = ((ProjectLibraryListFragment) getSupportFragmentManager().findFragmentById(R.id.project_list));
        listFragment.reload();
    }
}
