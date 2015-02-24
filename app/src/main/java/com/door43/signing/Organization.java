package com.door43.signing;

import com.door43.logging.Logger;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by joel on 2/23/2015.
 */
public class Organization {

    public final Date createdAt;
    public final String email;
    public final Date expiresAt;
    public final Date modifiedAt;
    public final String name;
    public final String slug;
    public final String url;

    /**
     * Creates a new organization instance
     * @param createdAt
     * @param email
     * @param expiresAt
     * @param modifiedAt
     * @param name
     * @param slug
     * @param url
     */
    public Organization(Date createdAt, String email, Date expiresAt, Date modifiedAt, String name, String slug, String url) {
        this.createdAt = createdAt;
        this.email = email;
        this.expiresAt = expiresAt;
        this.modifiedAt = modifiedAt;
        this.name = name;
        this.slug = slug;
        this.url = url;
    }

    @Override
    public String toString() {
        return name+" <"+slug+">\n"+"email: "+email+"\nurl: "+url+"\ncreated: "+createdAt+"\nmodified: "+modifiedAt+"\nexpires: "+expiresAt;
    }

    /**
     * Generates a new organization instance from a json string
     * @param orgJsonString the organization information
     * @return
     */
    public static Organization generate(String orgJsonString) {
        try {
            JSONObject json = new JSONObject(orgJsonString);
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            return new Organization(format.parse(json.getString("created")), json.getString("email"), format.parse(json.getString("expires")), format.parse(json.getString("modified")), json.getString("org"), json.getString("slug"), json.getString("web"));
        } catch (Exception e) {
            Logger.e(Organization.class.getName(), "Failed to load the organization information", e);
        }
        return null;
    }
}
