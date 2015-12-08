package com.door43.translationstudio.user;

import com.door43.tools.reporting.Logger;
import com.door43.translationstudio.R;
import com.door43.translationstudio.SettingsActivity;
import com.door43.translationstudio.git.Repo;
import com.door43.translationstudio.git.tasks.GitSyncAsyncTask;
import com.door43.translationstudio.git.tasks.repo.CommitTask;
import com.door43.translationstudio.git.tasks.repo.PushTask;
import com.door43.translationstudio.AppContext;
import com.door43.util.FileUtilities;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by joel on 2/9/2015.
 * // TODO: 9/29/2015 this should be changed the a User class in the core
 */
public class ProfileManager {
    private static HashMap<String, Profile> mProfiles = null;
    public static final String NAME = "name";
    public static final String PHONE = "phone";
    public static final String EMAIL = "email";
    public static final String PROFILES = "profiles";

    /**
     * Returns the profile
     * @return
     */
    public static HashMap<String, Profile> getProfiles() {
        if(mProfiles == null) {
            File profileDir = new File(getRepositoryPath());
            if (profileDir.exists() && profileDir.isDirectory()) {
                File contactFile = new File(profileDir, "contact.json");

                mProfiles = new HashMap<String, Profile>();

                if(contactFile.exists()) {
                    try {
                        JSONObject contactJson = new JSONObject(FileUtils.readFileToString(contactFile));
                        if(contactJson.has(NAME)) {
                            Profile profile = new Profile( contactJson.getString(NAME), contactJson.getString(EMAIL));
                            if (contactJson.has(PHONE)) {
                                profile.setPhone(contactJson.getString(PHONE));
                            }
                            mProfiles.put(profile.getName(), profile);

                        } else { // try looking for a map

                            if(contactJson.has(PROFILES)) {
                                JSONArray profiles = contactJson.getJSONArray(PROFILES);

                                for(int i = 0; i < profiles.length(); i++) {
                                    JSONObject contact = profiles.getJSONObject(i);
                                    if(contact.has(NAME)) {
                                        Profile profile = new Profile( contact.getString(NAME), contact.getString(EMAIL));
                                        if (contact.has(PHONE)) {
                                            profile.setPhone(contact.getString(PHONE));
                                        }
                                        mProfiles.put(profile.getName(), profile);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        Logger.e(ProfileManager.class.getName(), "failed to load the profile details", e);
                    }
                }
            }
        }
        return mProfiles;
    }

    /**
     * updates the user profile details
     * @param profile
     */
    public static boolean applyProfile(Profile profile) {

        HashMap<String, Profile> profiles = getProfiles();

        if(null != profiles) {

            mProfiles.put(profile.getName(), profile);
            return saveProfiles();
        }

        return false;
    }

    /**
     * Saves the profiles
     */
    public static boolean saveProfiles() {
        // TODO: the profile needs to be a git repo that gets pushed to the server
        File profileDir = new File(getRepositoryPath());
        if(profileDir.exists()) {
            FileUtilities.deleteRecursive(profileDir);
        }
        profileDir.mkdirs();

        File contactFile = new File(profileDir, "contact.json");
        File avatarFile = new File(profileDir, "avatar.png");

        // write contact details
        JSONArray contactArrayJson = new JSONArray();
        try {
            Set<Map.Entry<String, Profile>> setMap = mProfiles.entrySet();
            Iterator<Map.Entry<String,  Profile>> iteratorMap = setMap.iterator();
            while(iteratorMap.hasNext()) {
                Map.Entry<String, Profile> entry =
                        (Map.Entry<String, Profile>) iteratorMap.next();

                Profile profile = entry.getValue();

                JSONObject contactJson = new JSONObject();
                contactJson.put("name", profile.getName());
                contactJson.put("email", profile.getEmail());
                contactJson.put("phone", profile.getPhone());

                contactArrayJson.put(contactJson);
            }

            JSONObject contactsJson = new JSONObject();
            contactsJson.put(PROFILES, contactArrayJson);

            FileUtils.write(contactFile, contactsJson.toString());
        } catch (Exception e) {
            Logger.e(ProfileManager.class.getName(), "failed to write the profile contact details", e);
            return false;
        }

        // TODO: write avatar

        // TODO: write signature, certificate

        return true;
    }

    /**
     * gets profile entry at pos.  If not found returns null
     * @param pos
     */
    public static Profile getEntry(final int pos) {
        HashMap<String, Profile> profiles = getProfiles();
        if(profiles != null) {
            try {
                Set<Map.Entry<String, Profile>> setMap = profiles.entrySet();
                Iterator<Map.Entry<String, Profile>> iteratorMap = setMap.iterator();
                int i = 0;
                while (iteratorMap.hasNext()) {
                    Map.Entry<String, Profile> entry =
                            (Map.Entry<String, Profile>) iteratorMap.next();

                    if (pos == i) {
                        Profile profile = entry.getValue();
                        return profile;
                    }
                }

            } catch (Exception e) {
                Logger.e(ProfileManager.class.getName(), "failed to get entry", e);
                return null;
            }
        }

        return null;
    }

    /**
     * returns a concatenated list of names or null if error
     */
    public static String getConcatenatedNames(String between) {

        ArrayList<String> nameList = getNames();

        if(null != nameList) {
            String listString = "";

            for (String s : nameList) {
                if(!listString.isEmpty()) {
                    listString += between;
                }
                listString += s;
            }

            return listString;
        }
        return null;
    }

    /**
     * returns a list of names or null if error
    */
    public static ArrayList<String> getNames() {
        HashMap<String, Profile> profiles = getProfiles();
        if(profiles != null) {
            try {
                ArrayList<String> names = new ArrayList<String>();

                Set<Map.Entry<String, Profile>> setMap = profiles.entrySet();
                Iterator<Map.Entry<String, Profile>> iteratorMap = setMap.iterator();

                while (iteratorMap.hasNext()) {
                    Map.Entry<String, Profile> entry =
                            (Map.Entry<String, Profile>) iteratorMap.next();

                    Profile profile = entry.getValue();
                    names.add(profile.getName());
                }

                return names;

            } catch (Exception e) {
                Logger.e(ProfileManager.class.getName(), "failed to get entry", e);
            }
        }

        return null;
    }

    /**
     * Returns the local path to the repository
     * @return
     */
    public static String getRepositoryPath() {
        return AppContext.context().getFilesDir() + "/" + AppContext.PROFILES_DIR + "/profile/";
    }

    /**
     * Returns the remote repository url
     * @return
     */
    public static String getRemotePath() {
        String server = AppContext.context().getUserPreferences().getString(SettingsActivity.KEY_PREF_GIT_SERVER, AppContext.context().getResources().getString(R.string.pref_default_git_server));
        return server + ":tS/" + AppContext.udid() + "/profile";
    }

    /**
     * Pushes the profile to the server
     */
    @Deprecated
    public static void pushAsync() {
        if(getProfiles() != null) {

            final String remotePath = getRemotePath();
            final Repo repo = new Repo(getRepositoryPath());

            // TODO: manually perform the commit
            CommitTask add = new CommitTask(repo, ".", new CommitTask.OnAddComplete() {
                @Override
                public void success() {
                    // TODO: manually perform the push
                    PushTask push = new PushTask(repo, remotePath, true, true, new GitSyncAsyncTask.AsyncTaskCallback() {
                        public boolean doInBackground(Void... params) {
                            return false;
                        }

                        public void onPreExecute() {
                        }

                        public void onProgressUpdate(String... progress) {
                        }

                        @Override
                        public void onPostExecute(Boolean isSuccess) {
                            if (!isSuccess) {
                                Logger.e(ProfileManager.class.getName(), "failed to push the profile to the server");
                            }
                        }
                    });
                    push.executeTask();
                }

                @Override
                public void error(Throwable e) {
                    Logger.e(ProfileManager.class.getName(), "failed to commit chnages to the profile repo", e);
                }
            });
            add.executeTask();
        }
    }

}
