package com.door43.translationstudio.services;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Represents a single request to a peer on the network
 */
public class Request {
    public final UUID uuid;
    public final Type type;
    public final JSONObject context;
    private final String payload;

    /**
     * Creates a new request object
     * @param type
     * @param context
     */
    public Request(Type type, JSONObject context) throws JSONException {
        this.type = type;
        this.uuid = UUID.randomUUID();
        this.context = context;
        payload = generatePayload();
    }

    /**
     * Creates a request object from existing values
     * @param type
     * @param uuid
     * @param context
     */
    private Request(Type type, UUID uuid, JSONObject context) throws JSONException {
        this.type = type;
        this.uuid = uuid;
        this.context = context;
        payload = generatePayload();
    }

    /**
     * Generates the payload string
     * @return
     * @throws JSONException
     */
    private String generatePayload() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("request", this.type.toString());
        json.put("uuid", this.uuid.toString());
        if(context != null) {
            json.put("context", context);
        }
        return json.toString();
    }

    /**
     * Outputs the json object as a string that can be sent over the wire
     * @return
     */
    @Override
    public String toString() {
        return payload.toString();
    }

    /**
     * Parses a request from a string
     * @param message
     * @return
     */
    public static Request parse(String message) throws JSONException {
        JSONObject json = new JSONObject(message);
        Type type = Type.get(json.getString("request"));
        UUID uuid = UUID.fromString(json.getString("uuid"));
        JSONObject context = null;
        if(json.has("context")) {
            context = json.getJSONObject("context");
        }
        if(type != null && uuid != null) {
            return new Request(type, uuid, context);
        } else {
            return null;
        }
    }

    /**
     * Creates a reply to this request
     * @param context
     * @return
     */
    public Request makeReply(JSONObject context) throws JSONException {
        return new Request(this.type, this.uuid, context);
    }

    /**
     * Request types
     */
    public enum Type {
        AlertTargetTranslation("alert-target-translation"),
        TargetTranslation("target-translation"),
        TargetTranslationList("target-translation-list");

        private final String slug;

        Type(String slug) {
            this.slug = slug;
        }

        @Override
        public String toString() {
            return this.slug;
        }

        /**
         * Return the type by it's slug
         * @param slug
         * @return
         */
        public static Type get(String slug) {
            if(slug != null) {
                for(Type c:Type.values()) {
                    if(c.toString().equals(slug.toLowerCase())) {
                        return c;
                    }
                }
            }
            return null;
        }
    }
}
