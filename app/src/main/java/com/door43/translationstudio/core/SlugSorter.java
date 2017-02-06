package com.door43.translationstudio.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A utility for sorting slugs in resource container.
 * TRICKY: we sort slightly differently that defined in the spec
 * to ease the translation process.
 *
 */
public class SlugSorter {

    /**
     * Sorts the slugs
     * @param slugs the sorted slugs
     * @return
     */
    public List<String> sort(List<String> slugs) {
        Collections.sort(slugs, new SlugComparator());
        return slugs;
    }

    /**
     * Sorts the slugs
     * @param slugs the sorted slugs
     * @return
     */
    public List<String> sort(String[] slugs) {
        return sort(Arrays.asList(slugs));
    }
}

/**
 * Do all the sorting
 */
class SlugComparator implements Comparator<String>
{
    private static final int NUMERIC_WEIGHT = 6;
    private static final int NON_NUMERIC_WEIGHT = 7;

    public int compare(String left, String right) {
        int leftWeight = getWeight(left);
        int rightWeight = getWeight(right);

        if(leftWeight > rightWeight) return 1;
        if(leftWeight < rightWeight) return -1;

        // sort numeric
        if(leftWeight == NUMERIC_WEIGHT) {
            return compare(Integer.parseInt(left), Integer.parseInt(right));
        }

        // sort non-numeric
        if(leftWeight == NON_NUMERIC_WEIGHT){
            return left.compareTo(right);
        }

        // default top
        return 0;
    }

    /**
     * Compares the sort order of two ints
     * @param left
     * @param right
     * @return
     */
    int compare(int left, int right) {
        if(left > right) return 1;
        if(left < right) return -1;
        return 0;
    }

    /**
     * Returns the relative weight of the slug sort.
     * Smaller values float to the top.
     *
     * @param slug the slug
     * @return the weight
     */
    int getWeight(String slug) {
        switch(slug) {
            case "front":
                return 1;
            case "title":
                return 2;
            case "sub-title":
                return 3;
            case "intro":
                return 4;
            case "reference":
                return 5;
            case "summary":
                // numeric: 6
                // non-numeric: 7
                return 8;
            case "back":
                return 9;
        }

        try {
            // numeric
            Integer.valueOf(slug);
            return NUMERIC_WEIGHT;
        } catch (NumberFormatException e) {
            // non-numeric
            return NON_NUMERIC_WEIGHT;
        }
    }
}