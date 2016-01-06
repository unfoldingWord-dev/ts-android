package com.door43.translationstudio.core;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jshuma on 1/4/16.
 */
public class Profile implements Serializable {
    public final String name;
    public final String email;
    public final String phone;

    private static final String TAG_NAME = "name";
    private static final String TAG_PHONE = "phone";
    private static final String TAG_EMAIL = "email";

    public Profile(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    /**
     * Indicates whether this object is sufficiently complete to be used.
     *
     * <p>This is where business logic for profile validation goes. Currently it consists of
     * checking for the presence of a few fields, but other checks can be added here.</p>
     * @return true if valid, otherwise false
     */
    public boolean isValid() {
        return name != null && !name.isEmpty() && email != null && !email.isEmpty();
    }

    public JSONObject encodeJsonObject() throws JSONException {
        JSONObject o = new JSONObject();
        o.put(TAG_NAME, name);
        o.put(TAG_EMAIL, email);
        o.put(TAG_PHONE, phone);
        return o;
    }

    public static JSONArray encodeJsonArray(List<? extends Profile> persons) throws JSONException {
        JSONArray a = new JSONArray();
        for (Profile p : persons) {
            a.put(p.encodeJsonObject());
        }
        return a;
    }

    public static Profile decodeJsonObject(JSONObject o) {
        String name = (String) o.opt(TAG_NAME);
        String email = (String) o.opt(TAG_EMAIL);
        String phone = (String) o.opt(TAG_PHONE);
        return (name != null) ? new Profile(name, email, phone) : null;
    }

    public static List<Profile> decodeJsonArray(JSONArray a) throws JSONException {
        List<Profile> profiles = new ArrayList<>(a.length());
        for (int i = 0; i < a.length(); ++i) {
            profiles.add(Profile.decodeJsonObject((JSONObject) a.get(i)));
        }
        return profiles;
    }
}
