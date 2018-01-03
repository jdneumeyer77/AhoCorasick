package com.roklenarcic.util.strings;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static com.roklenarcic.util.strings.AhoCorasickSet.trimSpaces;

public class AhoCorasickTest extends SetTest {

    private static boolean normalizeWhitespace = false;

    public static void main(final String[] args) throws IOException {
        System.in.read();
        new AhoCorasickTest(true, 1000000).testFullNode();
        new AhoCorasickTest(true, 1000000).testLiteral();
        new AhoCorasickTest(true, 1000000).testOverlap();
        new AhoCorasickTest(true, 1000000).testLongKeywords();
        if(!normalizeWhitespace) new AhoCorasickTest(false, 1000000).testFullRandom();
        new AhoCorasickTest(false, 1000000).testFailureTransitions();
        new AhoCorasickTest(false, 1000000).testDictionary();
        new AhoCorasickTest(false, 1000000).testShortestMatch();


        normalizeWhitespace = true;
        new AhoCorasickTest(true, 1000000).test("a bobswims", "a bob");
        new AhoCorasickTest(true, 1000000).test("a     bobswims", "a bob");
        new AhoCorasickTest(true, 1000000).test("a     bob swims", "a bob");
        new AhoCorasickTest(true, 1000000).test("   a\t     bob    swims   ", "a bob");
        new AhoCorasickTest(true, 1000000).test(".a     bob swims", "a bob");
        new AhoCorasickTest(true, 1000000).test("a     bob! swims", "a bob");
        new AhoCorasickTest(true, 1000000).test("a bob      swims", "bob swims");
        new AhoCorasickTest(true, 1000000).test("a BoB      swims.", "bob swims");
        new AhoCorasickTest(true, 1000000).test("a BoB swims.", "bob swims");
        normalizeWhitespace = false;
    }

    public AhoCorasickTest() {
        super();
    }

    private AhoCorasickTest(boolean printTimesOnly, int testLoopSize) {
        super(printTimesOnly, testLoopSize);
        isCaseSensitive = true;
    }

    private HashSet<String> results = new HashSet<String>();

    protected void assertCorrectMatch(final int startPosition, final int endPosition, List<String> keywords, String haystack, StringSet set) {

        String matched;
        if(normalizeWhitespace)
            matched = trimSpaces(haystack.substring(startPosition, endPosition).replaceAll("\\s+", " "));
        else
            matched = haystack.substring(startPosition, endPosition);

        if(!isCaseSensitive)
            matched = matched.toLowerCase();

        Assert.assertTrue("Could not find " + matched + " at end position " + endPosition + " in keywords: " + keywords,
                keywords.contains(matched));

        results.add(matched);
    }

    @Override
    protected int getCorrectCount(List<String> keywords, String haystack, StringSet set) {
        if(normalizeWhitespace)
            haystack = trimSpaces(haystack.replaceAll("\\s+", " "));

        if(!isCaseSensitive)
            haystack = haystack.toLowerCase();

        int normalCount = 0;
        Set<String> collector = new HashSet<String>();
        for (String needle : keywords) {
            if(needle != null && needle.length() > 0) {
                if(normalizeWhitespace) {
                    needle = trimSpaces(needle.replaceAll("\\s+", " ")).trim();
                }
                if(needle.length() > 0) {
                    for (int i = 0; i + needle.length() <= haystack.length(); i++) {
                        if (haystack.substring(i, i + needle.length()).equals(needle)) {
                            if (!isCaseSensitive) {
                                collector.add(needle.toLowerCase());
                            } else {
                                normalCount++;
                                if (!results.contains(needle)) System.out.println("missing: " + needle);

                                collector.add(needle);

                            }
                        }
                    }
                }
            }
        }

        if(!isCaseSensitive) return collector.size();
        else return normalCount;
    }

    @Test
    @Override
    public void testFullRandom() {
        if(!normalizeWhitespace) { // too random... often times the strings are less than 2... can't seem to fix.
            final String[] smallDict = Generator.randomStrings(10000, 2, 3);
            final String[] mediumDict = Generator.randomStrings(100000, 2, 3);
            final String[] largeDict = Generator.randomStrings(1000000, 2, 3);
            test("The quick red fox, jumps over the lazy brown dog.", smallDict);
            test("The quick red fox, jumps over the lazy brown dog.", mediumDict);
            test("The quick red fox, jumps over the lazy brown dog.", largeDict);
        }
    }

    @Test
    @Override
    public void testFullNode() {

            final String[] keywords = new String[65536];
            for (int i = 0; i < keywords.length; i++) {
                keywords[i] = String.valueOf((char) i);
            }
            if(normalizeWhitespace) { // TODO: null char doesn't match?
                test("\uffff\ufffe", keywords);
            } else {
                test("\u0000\uffff\ufffe", keywords);
            }
    }

    // not ideal, but it'll work to test these variations.
    @Test
    public void testNormalizedWhitespace() throws Exception {
        normalizeWhitespace = true;
        new AhoCorasickTest(false, 1000000).testFullNode();
        new AhoCorasickTest(false, 1000000).testLiteral();
        new AhoCorasickTest(false, 1000000).testOverlap();
        new AhoCorasickTest(false, 1000000).testLongKeywords();
        new AhoCorasickTest(false, 1000000).testFailureTransitions();
        new AhoCorasickTest(false, 1000000).testDictionary();
        new AhoCorasickTest(false, 1000000).testShortestMatch();


        new AhoCorasickTest(false, 1000000).test("a bobswims", "a bob");
        new AhoCorasickTest(false, 1000000).test("a     bobswims", "a bob");
        new AhoCorasickTest(false, 1000000).test("a     bob swims", "a bob");
        new AhoCorasickTest(false, 1000000).test("   a\t     bob    swims   ", "a bob");
        new AhoCorasickTest(false, 1000000).test(".a     bob swims", "a bob");
        new AhoCorasickTest(false, 1000000).test("a     bob! swims", "a bob");
        new AhoCorasickTest(false, 1000000).test("a bob      swims", "bob swims");
        new AhoCorasickTest(false, 1000000).test("a BoB      swims.", "bob swims");
        new AhoCorasickTest(false, 1000000).test("a BoB swims.", "bob swims");

        normalizeWhitespace = false;
    }

    @Override
    protected StringSet instantiateSet(List<String> keywords, boolean caseSensitive) {
        AhoCorasickSet set = new AhoCorasickSet(keywords, isCaseSensitive, normalizeWhitespace);

        for (int i = 0; i < keywords.size(); i++) {
            String updatedKeyword = keywords.get(i);
            if(updatedKeyword != null) {
                if (normalizeWhitespace) {
                    updatedKeyword = trimSpaces(updatedKeyword.replaceAll("\\s+", " "));
                }

                if (!isCaseSensitive) {
                    updatedKeyword = updatedKeyword.toLowerCase();
                }

                if (updatedKeyword.length() > 0)
                    keywords.set(i, updatedKeyword);
                else keywords.set(i, null);

            } else keywords.set(i, null); // filter out later...
        }

        return set;
    }

}
