package com.roklenarcic.util.strings;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.Assert;

public class WholeWordLongestMatchTest extends SetTest {

    boolean normalizeWhitespace = false;

    public static void main(final String[] args) throws IOException {
//        System.in.read();
        System.out.println("without normalization whitespace!");
        new WholeWordLongestMatchTest(false, false, 1000000).testLiteral();
        new WholeWordLongestMatchTest(false, false, 1000000).testOverlap();
        new WholeWordLongestMatchTest(false, false, 1000000).testLongKeywords();
        new WholeWordLongestMatchTest(false, false, 1000000).testFullRandom();
        new WholeWordLongestMatchTest(false, false, 1000000).testFailureTransitions();
        new WholeWordLongestMatchTest(false, false, 1000000).testDictionary();
        new WholeWordLongestMatchTest(false, false, 1000000).testShortestMatch();

        System.out.println("normalized whitespace!");
        new WholeWordLongestMatchTest(true, false, 1000000).testLiteral();
        new WholeWordLongestMatchTest(true, false, 1000000).testOverlap();
        new WholeWordLongestMatchTest(true, false, 1000000).testLongKeywords();
        new WholeWordLongestMatchTest(true, false, 1000000).testFullRandom();
        new WholeWordLongestMatchTest(true, false, 1000000).testFailureTransitions();
        new WholeWordLongestMatchTest(true, false, 1000000).testDictionary();
        new WholeWordLongestMatchTest(true, false, 1000000).testShortestMatch();

        new WholeWordLongestMatchTest(true, false, 1000000).test("a     bobswims", "a bob");
        new WholeWordLongestMatchTest(true, false, 1000000).test("a     bob swims", "a bob");
        new WholeWordLongestMatchTest(true, false, 1000000).test("   a\t     bob    swims   ", "a bob");
        new WholeWordLongestMatchTest(true, false, 1000000).test(".a     bob swims", "a bob");
        new WholeWordLongestMatchTest(true, false, 1000000).test("a     bob! swims", "a bob");
        new WholeWordLongestMatchTest(true, false, 1000000).test("a bob      swims", "bob swims");
        new WholeWordLongestMatchTest(true, false, 1000000).test("a BoB      swims.", "bob swims");
        new WholeWordLongestMatchTest(true, false, 1000000).test("a BoB swims.", "bob swims");
    }

    public WholeWordLongestMatchTest() {
        super();
    }

    private WholeWordLongestMatchTest(boolean normalizeWhitespace, boolean printTimesOnly, int testLoopSize) {
        super(printTimesOnly, testLoopSize);
        this.normalizeWhitespace = normalizeWhitespace;
        this.isCaseSensitive = false;
    }

    @Override
    protected void assertCorrectMatch(int startPosition, int endPosition, List<String> keywords, String haystack, StringSet set) {
        WholeWordLongestMatchSet wwset = (WholeWordLongestMatchSet) set;
        String matched;
        if(normalizeWhitespace) {
            matched = haystack.substring(startPosition, endPosition).replaceAll("\\s+", " ");
        } else {
            matched = haystack.substring(startPosition, endPosition);
        }

        if(!isCaseSensitive) {
            matched = matched.toLowerCase();
        }

        Assert.assertTrue("Could not find needle " + matched + " surround(" + haystack.substring(startPosition - 1 >= 0 ? startPosition - 1 : startPosition , endPosition+1 <= haystack.length() ? endPosition+1 : endPosition)  + ") at position " + startPosition + "/" + endPosition + " in set(" + keywords + ')' + " in complete haystack:\n" + haystack ,
                keywords.contains(matched));
        Assert.assertTrue("Needle " + matched + " at end position " + endPosition
                + " doesn't end in whitespace or string end in \n" + haystack,
                haystack.length() == endPosition || !wwset.getWordChars()[haystack.charAt(endPosition)]);
        Assert.assertTrue("Needle " + matched + " at end position " + endPosition
                + " doesn't start in whitespace or string start in \n" + haystack,
                startPosition == 0 || !wwset.getWordChars()[haystack.charAt(startPosition - 1)]);
    }

    @Override
    protected int getCorrectCount(List<String> keywords, String haystack, StringSet set) {

        if(normalizeWhitespace)
            haystack = haystack.replaceAll("\\s+", " ");

        if(!isCaseSensitive) {
            haystack = haystack.toLowerCase();
        }

        System.out.println(haystack);
        int normalCount = 0;
        for (int i = 0; i < haystack.length(); i++) {
            for (final String needle : keywords) {
                if (needle.length() > 0 && i + needle.length() <= haystack.length() && haystack.substring(i, i + needle.length()).equals(needle)
                        && (i + needle.length() == haystack.length() || !Character.isLetterOrDigit(haystack.charAt(i + needle.length())))
                        && (i == 0 || !Character.isLetterOrDigit(haystack.charAt(i - 1)))) {
                    normalCount++;
                    i += needle.length() - 1;
                    while (++i < haystack.length() && !((WholeWordLongestMatchSet) set).getWordChars()[haystack.charAt(i)]) {
                    }
                    i--;
                    break;
                }
            }
        }
        return normalCount;
    }

    @Override
    protected StringSet instantiateSet(List<String> keywords, boolean caseSensitive) {
        WholeWordLongestMatchSet s = new WholeWordLongestMatchSet(keywords, isCaseSensitive, normalizeWhitespace);
        for (int i = 0; i < keywords.size(); i++) {
            String updatedKeyword = WordCharacters.trim(keywords.get(i), s.getWordChars());

            if(normalizeWhitespace) {
                updatedKeyword = updatedKeyword.replaceAll("\\s+", " ");
            }

            if(!isCaseSensitive) {
                updatedKeyword = updatedKeyword.toLowerCase();
            }

            keywords.set(i, updatedKeyword);
        }
        return s;
    }

    @Override
    protected List<String> prepareKeywords(String[] keywords) {
        Arrays.sort(keywords, new Comparator<String>() {

            public int compare(String o1, String o2) {
                return o2.length() - o1.length();
            }
        });
        return super.prepareKeywords(keywords);
    }

}
