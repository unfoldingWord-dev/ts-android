package com.door43.util;

import com.door43.util.usfm.Chunk;
import com.door43.util.usfm.Usfm;

import org.junit.Test;
import org.unfoldingword.door43client.models.ChunkMarker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class UsfmTest {

    @Test
    public void parseBookUsfm() {
        String usfm = "\\id GEN unfoldingWord Literal Text\n" +
                "\\ide UTF-8\n" +
                "\\h Genesis\n" +
                "\\toc1 The Book of Genesis\n" +
                "\\toc2 Genesis\n" +
                "\\toc3 Gen\n" +
                "\\mt Genesis \n" +
                "\n" +
                "\\s5\n" +
                "\\c 1\n" +
                "\\p\n" +
                "\\v 1 In the beginning, God created the heavens and the earth. \n" +
                "\\v 2 The earth was without form and empty. Darkness was upon the surface of the deep. The Spirit of God was moving above the surface of the waters.\n" +
                "\\s5\n" +
                "\\c 2\n" +
                "\\p\n" +
                "\\v 1 Then the heavens and the earth were finished, and all the living things that filled them.\n" +
                "\\v 2 On the seventh day God came to the end of his work which he had done, and so he rested on the seventh day from all his work.\n" +
                "\\v 3 God blessed the seventh day and sanctified it, because in it he rested from all his work which he had done in his creation.\n";
        Map<Integer, Map> results = Usfm.parseBook(usfm);
        assertEquals(3, results.size());
        assertEquals("In the beginning, God created the heavens and the earth.", results.get(1).get(1));
        assertEquals("Then the heavens and the earth were finished, and all the living things that filled them.", results.get(2).get(1));
        assertEquals("Genesis", results.get(0).get("title"));
    }

    @Test
    public void chunkUsfm() throws Exception {
        String usfm = "\\id GEN unfoldingWord Literal Text\n" +
                "\\ide UTF-8\n" +
                "\\h Genesis\n" +
                "\\toc1 The Book of Genesis\n" +
                "\\toc2 Genesis\n" +
                "\\toc3 Gen\n" +
                "\\mt Genesis \n" +
                "\n" +
                "\\s5\n" +
                "\\c 1\n" +
                "\\p\n" +
                "\\v 1 In the beginning, God created the heavens and the earth. \n" +
                "\\v 2 The earth was without form and empty. Darkness was upon the surface of the deep. The Spirit of God was moving above the surface of the waters.\n" +
                "\\s5\n" +
                "\\c 2\n" +
                "\\p\n" +
                "\\v 1 Then the heavens and the earth were finished, and all the living things that filled them.\n" +
                "\\v 2 On the seventh day God came to the end of his work which he had done, and so he rested on the seventh day from all his work.\n" +
                "\\v 3 God blessed the seventh day and sanctified it, because in it he rested from all his work which he had done in his creation.\n";
        List<ChunkMarker> chunks = new ArrayList<>();
        chunks.add(new ChunkMarker("01", "01"));
        chunks.add(new ChunkMarker("02", "01"));
        chunks.add(new ChunkMarker("02", "02"));
        List<Chunk> results = Usfm.chunkBook(usfm, chunks);
        assertEquals(4, results.size());

        assertEquals("Genesis", results.get(0).content);

        assertEquals("01", results.get(1).chapter);
        assertEquals("01", results.get(1).verse);

        assertEquals("02", results.get(2).chapter);
        assertEquals("01", results.get(2).verse);

        assertEquals("02", results.get(3).chapter);
        assertEquals("02", results.get(3).verse);
    }
}
