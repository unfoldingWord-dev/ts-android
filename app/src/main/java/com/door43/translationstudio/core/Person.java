package com.door43.translationstudio.core;

import java.io.Serializable;

/**
 * Created by jshuma on 1/4/16.
 */
public class Person implements Serializable {
    public final String name;
    public final String email;
    public final String phone;

    public Person(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
    }
}
