package com.door43.translationstudio.core;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;


public class SlugSorterTest {

    @Test
    public void sort() throws Exception {
        SlugSorter sorter = new SlugSorter();
        List<String> slugs = new ArrayList<>();

        slugs.add("01");
        slugs.add("06");
        slugs.add("02");
        slugs.add("11");
        slugs.add("back");
        slugs.add("07");
        slugs.add("front");
        slugs.add("03");
        slugs.add("reference");
        slugs.add("title");

        sorter.sort(slugs);

        assertEquals("front", slugs.get(0));
        assertEquals("title", slugs.get(1));
        assertEquals("reference", slugs.get(2));
        assertEquals("01", slugs.get(3));
        assertEquals("02", slugs.get(4));
        assertEquals("03", slugs.get(5));
        assertEquals("06", slugs.get(6));
        assertEquals("07", slugs.get(7));
        assertEquals("11", slugs.get(8));
        assertEquals("back", slugs.get(9));
    }
}
