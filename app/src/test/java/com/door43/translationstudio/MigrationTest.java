package com.door43.translationstudio;

import com.door43.translationstudio.core.Migration;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class MigrationTest {

    @Test
    public void migrateSourceTranslationSlugTest() throws Exception {
        String simple = Migration.migrateSourceTranslationSlug("mat-en-udb");
        assertEquals("en_udb_mat", simple);

        String complex = Migration.migrateSourceTranslationSlug("mat-pt-br-udb");
        assertEquals("pt-br_udb_mat", complex);

        // unable to parse
        String skip = Migration.migrateSourceTranslationSlug("mat_pt-br-udb");
        assertEquals("mat_pt-br-udb", skip);

        // invalid slug
        String invalid = Migration.migrateSourceTranslationSlug("mat-udb");
        assertEquals("mat-udb", invalid);
    }
}
