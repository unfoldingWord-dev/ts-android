package com.door43.translationstudio.core;

/**
 * Represents a single chunk marker
 */
@Deprecated
public class ChunkMarker {

    public final String chapterSlug;
    public final String firstVerseSlug;

    public ChunkMarker(String chapterSlug, String firstVerseSlug) {
        this.chapterSlug = chapterSlug;
        this.firstVerseSlug = firstVerseSlug;
    }
}
