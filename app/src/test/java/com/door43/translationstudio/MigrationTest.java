package com.door43.translationstudio;

import com.door43.translationstudio.core.Migration;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class MigrationTest {

    @Test
    public void migrateSourceTranslationSlugTest() throws Exception {
        String simple = Migration.migrateSourceTranslationSlug("mat-en-udb");
        assertEquals("en_mat_udb", simple);

        String complex = Migration.migrateSourceTranslationSlug("mat-pt-br-udb");
        assertEquals("pt-br_mat_udb", complex);

        // unable to parse
        String skip = Migration.migrateSourceTranslationSlug("mat_pt-br-udb");
        assertEquals("mat_pt-br-udb", skip);

        // invalid slug
        String invalid = Migration.migrateSourceTranslationSlug("mat-udb");
        assertEquals("mat-udb", invalid);
    }
}
