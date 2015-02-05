package com.door43.translationstudio.user;

/**
 * Represents a user profile
 */
public class Profile {
    private String mName;
    private String mEmail;
    private String mPhone;

    /**
     * Creates a new user profile
     * @param name the name of the user
     * @param email the email of the user
     */
    public Profile(String name, String email) {
        mName = name;
        mEmail = email;
    }

    /**
     * Sets or updates the user's phone number
     * @param phone
     */
    public void setPhone(String phone) {
        mPhone = phone;
    }

    @Override
    public String toString() {
        return mName;
    }

    public String getName() {
        return mName;
    }

    public String getEmail() {
        return mEmail;
    }

    public String getPhone() {
        return mPhone;
    }
}
