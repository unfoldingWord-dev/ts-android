package com.door43.translationstudio.services;

/**
 * Created by joel on 11/20/2015.
 */
@Deprecated
public enum PeerCommand {
    OK("ok"),
    @Deprecated
    ProjectArchive("pa"),
    @Deprecated
    ProjectList("pl"),
    AuthError("ae"),
    InvalidRequest("ir"),
    PublicKey("pk"),
    Exception("ex"),
    TargetTranslation("tt");

    private final String slug;

    PeerCommand(String slug) {
        this.slug = slug;
    }

    @Override
    public String toString() {
        return this.slug;
    }

    /**
     * Return the command by it's slug
     * @param slug
     * @return
     */
    public static PeerCommand get(String slug) {
        if(slug != null) {
            for(PeerCommand c:PeerCommand.values()) {
                if(c.toString().equals(slug.toLowerCase())) {
                    return c;
                }
            }
        }
        return null;
    }
}
