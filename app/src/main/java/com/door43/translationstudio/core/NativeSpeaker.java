package com.door43.translationstudio.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joel on 11/12/2015.
 */
public class NativeSpeaker {
    public final String name;
    public final String email;
    public final String phone;

    public NativeSpeaker(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    public NativeSpeaker(Person p) {
        this.name = p.name;
        this.email = p.email;
        this.phone = p.phone;
    }

    public static List<NativeSpeaker> nativeSpeakersFromProfiles(List<Person> persons) {
        ArrayList<NativeSpeaker> a = new ArrayList<>(persons.size());
        for (Person p : persons) {
            a.add(new NativeSpeaker(p));
        }
        return a;
    }
}
