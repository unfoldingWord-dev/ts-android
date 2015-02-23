package com.door43.signing;

import com.door43.logging.Logger;

import org.json.JSONObject;

/**
 * Created by joel on 2/23/2015.
 */
public class Organization {

    public final String createdAt;
    public final String email;
    public final String expiresAt;
    public final String modifiedAt;
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
    public Organization(String createdAt, String email, String expiresAt, String modifiedAt, String name, String slug, String url) {
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
            return new Organization(json.getString("created"), json.getString("email"), json.getString("expires"), json.getString("modified"), json.getString("org"), json.getString("slug"), json.getString("web"));
        } catch (Exception e) {
            Logger.e(Organization.class.getName(), "Failed to load the organization information", e);
        }
        return null;
    }
}
