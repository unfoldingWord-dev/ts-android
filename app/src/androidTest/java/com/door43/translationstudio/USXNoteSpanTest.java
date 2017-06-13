package com.door43.translationstudio;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import com.door43.translationstudio.ui.spannables.USXNoteSpan;

import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class USXNoteSpanTest {

    @Test
    public void testParseNote() {
        String usx = "<note caller=\"+\" style=\"f\">\n" +
                "  <char style=\"ft\">Leading text </char>\n" +
                "  <char style=\"fqa\">Quoted Text </char>trailing text \n" +
                "  <char style=\"fqa\">More quoted text </char>more trailing text\n" +
                "</note>";
        String text = "Leading text \"Quoted Text\" trailing text \"More quoted text\" more trailing text";
        USXNoteSpan span = USXNoteSpan.parseNote(usx);
        assertNotNull(span);
        assertEquals(text, span.getNotes());
    }
}
