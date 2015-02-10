package com.door43.translationstudio.user;

import com.door43.translationstudio.R;
import com.door43.translationstudio.util.FileUtilities;
import com.door43.translationstudio.util.Logger;
import com.door43.translationstudio.util.MainContext;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

/**
 * Created by joel on 2/9/2015.
 */
public class ProfileManager {
    private static Profile mProfile = null;

    /**
     * Returns the profile
     * @return
     */
    public static Profile getProfile() {
        if(mProfile == null) {
            File profileDir = new File(getRepositoryPath());
            if (profileDir.exists() && profileDir.isDirectory()) {
                File contactFile = new File(profileDir, "contact.json");
                try {
                    JSONObject contactJson = new JSONObject(FileUtils.readFileToString(contactFile));
                   mProfile = new Profile(contactJson.getString("name"), contactJson.getString("email"));
                    if (contactJson.has("phone")) {
                        mProfile.setPhone(contactJson.getString("phone"));
                    }
                } catch (Exception e) {
                    Logger.e(ProfileManager.class.getName(), "failed to load the profile details", e);
                }
            }
        }
        return mProfile;
    }

    /**
     * Specifies the user profile details
     * @param profile
     */
    public static void setProfile(Profile profile) {
        // TODO: the profile needs to be a git repo that gets pushed to the server
        File profileDir = new File(getRepositoryPath());
        if(profileDir.exists()) {
            FileUtilities.deleteRecursive(profileDir);
        }
        profileDir.mkdirs();

        File contactFile = new File(profileDir, "contact.json");
        File avatarFile = new File(profileDir, "avatar.png");

        // write contact details
        JSONObject contactJson = new JSONObject();
        try {
            contactJson.put("name", profile.getName());
            contactJson.put("email", profile.getEmail());
            contactJson.put("phone", profile.getPhone());

            FileUtils.write(contactFile, contactJson.toString());
        } catch (Exception e) {
            Logger.e(ProfileManager.class.getName(), "failed to write the profile contact details", e);
            return;
        }

        // TODO: write avatar

        // TODO: write signature, certificate

        mProfile = profile;
    }

    private static String getRepositoryPath() {
        return MainContext.getContext().getFilesDir() + "/" + MainContext.getContext().getResources().getString(R.string.git_repository_dir) + "/profile/";
    }
}
