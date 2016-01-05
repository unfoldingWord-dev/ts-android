package com.door43.translationstudio.core;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by jshuma on 1/4/16.
 */
public class Person implements Serializable {
    public final String name;
    public final String email;
    public final String phone;

    private static final String TAG_NAME = "name";
    private static final String TAG_PHONE = "phone";
    private static final String TAG_EMAIL = "email";

    public Person(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    public JSONObject encodeJsonObject() throws JSONException {
        JSONObject o = new JSONObject();
        o.put(TAG_NAME, name);
        o.put(TAG_EMAIL, email);
        o.put(TAG_PHONE, phone);
        return o;
    }

    public static JSONArray encodeJsonArray(List<? extends Person> persons) throws JSONException {
        JSONArray a = new JSONArray();
        for (Person p : persons) {
            a.put(p.encodeJsonObject());
        }
        return a;
    }

    public static Person decodeJsonObject(JSONObject o) {
        String name = (String) o.opt(TAG_NAME);
        String email = (String) o.opt(TAG_EMAIL);
        String phone = (String) o.opt(TAG_PHONE);
        return (name != null) ? new Person(name, email, phone) : null;
    }

    public static List<Person> decodeJsonArray(JSONArray a) throws JSONException {
        List<Person> persons = new ArrayList<>(a.length());
        for (int i = 0; i < a.length(); ++i) {
            persons.add(Person.decodeJsonObject((JSONObject) a.get(i)));
        }
        return persons;
    }
}
