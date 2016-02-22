package com.door43.translationstudio.core;

import org.eclipse.jgit.lib.ObjectLoader;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a single native speaker.
 * A native speaker understands at least one gateway language in addition to their native language
 *
 * note: this is parse for now, but keeping it in a class for potential future addition of properties
 */
public class NativeSpeaker {
    public final String name;

    public NativeSpeaker(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if(o != null && o instanceof NativeSpeaker) {
            return ((NativeSpeaker)o).name.equals(this.name);
        }
        return false;
    }
}
