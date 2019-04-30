package com.door43.util.usfm;

/**
 * Holds a chunk of text.
 */
public class Chunk {
    public final String chapter;
    public final String verse;
    public final String content;

    public Chunk(String chapter, String verse, String content) {
        this.chapter =  chapter;
        this.verse = verse;
        this.content = content;
    }
}
