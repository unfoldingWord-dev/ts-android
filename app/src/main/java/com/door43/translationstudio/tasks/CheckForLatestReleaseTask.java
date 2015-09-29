package com.door43.translationstudio.tasks;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.door43.tools.reporting.Github;
import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.AppContext;
import com.door43.util.tasks.ManagedTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * This task will look for the latest release available that is greater than the version currently installed on the device.
 * Valid releases are identified by comparing the build numbers.
 * In order to work correctly the release must be built from a tag formatted like x.x.x-x where the last x is the build number.
 */
public class CheckForLatestReleaseTask extends ManagedTask {
    private Release mLatestRelease = null;
    public static final String TASK_ID = "check_for_latest_apk_release_task";

    @Override
    public void start() {
        String githubApiUrl = AppContext.context().getResources().getString(R.string.github_repo_api);
        Github github = new Github(githubApiUrl);
        String latestRelease = github.getLatestRelease();
        if(latestRelease != null) {
            try {
                JSONObject latestReleaseJson = new JSONObject(latestRelease);
                if(latestReleaseJson.has("tag_name")) {
                    String tag = latestReleaseJson.getString("tag_name");
                    String[] tagParts = tag.split("-");
                    if(tagParts.length == 2) {
                        int build = Integer.parseInt(tagParts[1]);
                        try {
                            PackageInfo pInfo = AppContext.context().getPackageManager().getPackageInfo(AppContext.context().getPackageName(), 0);
                            if(build > pInfo.versionCode) {
                                String downloadUrl = null;
                                int downloadSize = 0;
                                if(latestReleaseJson.has("assets")) {
                                    JSONArray assetsJson = latestReleaseJson.getJSONArray("assets");
                                    JSONObject assetJson = assetsJson.getJSONObject(0);
                                    if(assetJson.has("browser_download_url")) {
                                        downloadUrl = assetJson.getString("browser_download_url");
                                    }
                                    if(assetJson.has("size")) {
                                        downloadSize = assetJson.getInt("size");
                                    }
                                }
                                mLatestRelease = new Release(latestReleaseJson.getString("name"), downloadUrl, downloadSize, build);
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            Logger.e(this.getClass().getName(), "Failed to fetch the package info", e);
                        }
                    }
                }
            } catch (JSONException e) {
                Logger.e(this.getClass().getName(), "Failed to parse the latest release", e);
            }
        }
    }

    /**
     * Returns the latest release available from github
     * @return null if no release is available
     */
    public Release getLatestRelease() {
        return mLatestRelease;
    }

    public class Release implements Serializable {
        public final String name;
        public final String downloadUrl;
        public final int downloadSize;
        public final int build;
        private static final long serialVersionUID = 1000000;

        public Release(String name, String downloadUrl, int downloadSize, int build) {
            this.name = name;
            this.downloadUrl = downloadUrl;
            this.downloadSize = downloadSize;
            this.build = build;
        }
    }
}
