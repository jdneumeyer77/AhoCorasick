package com.roklenarcic.util.strings;

import com.roklenarcic.util.strings.threshold.RangeNodeThreshold;
import com.roklenarcic.util.strings.threshold.Thresholder;

import java.util.regex.Pattern;

// A set that matches only whole word matches. Non-word characters are user defined (with a default).
// Any non-word characters around input strings get trimmed. Non-word characters are allowed in the keywords.
public class WholeWordLongestMatchSet implements StringSet {

    private boolean caseSensitive = true;
    private boolean normalizeWhitespace = false;
    private TrieNode root;
    private boolean[] wordChars;


    static WhitespaceReader NoOpWhiteSpaceReaderInstance = new NoOpWhitespaceReader();
    static WhitespaceReader SkipWhiteSpaceReaderInstance = new SkipWhitespaceReader();
    private WhitespaceReader whitespaceReader;

    public static Pattern keywordWhitespaceNormalizer = Pattern.compile("\\s+");

    // Set where digits and letters, '-' and '_' are considered word characters.
    public WholeWordLongestMatchSet(final Iterable<String> keywords, boolean caseSensitive, boolean normalizeWhitespace) {
        this(keywords, caseSensitive, normalizeWhitespace, new RangeNodeThreshold());
    }

    // Set where the characters in the given array are considered word characters
    public WholeWordLongestMatchSet(final Iterable<String> keywords, boolean caseSensitive, boolean normalizeWhitespace, char[] wordCharacters) {
        this(keywords, caseSensitive, normalizeWhitespace, wordCharacters, new RangeNodeThreshold());
    }

    // Set where digits and letters and '-' and '_' are considered word characters but modified by the two
    // given arrays
    public WholeWordLongestMatchSet(final Iterable<String> keywords, boolean caseSensitive, boolean normalizeWhitespace, char[] wordCharacters, boolean[] toggleFlags) {
        this(keywords, caseSensitive, normalizeWhitespace, wordCharacters, toggleFlags, new RangeNodeThreshold());
    }

    // Set where digits and letters and '-' and '_' are considered word characters but modified by the two
    // given arrays
    public WholeWordLongestMatchSet(final Iterable<String> keywords, boolean caseSensitive, boolean normalizeWhitespace, char[] wordCharacters, boolean[] toggleFlags,
            Thresholder thresholdStrategy) {
        init(keywords, caseSensitive, normalizeWhitespace, WordCharacters.generateWordCharsFlags(wordCharacters, toggleFlags), thresholdStrategy);
    }

    // Set where the characters in the given array are considered word characters
    public WholeWordLongestMatchSet(final Iterable<String> keywords, boolean caseSensitive, boolean normalizeWhitespace, char[] wordCharacters, Thresholder thresholdStrategy) {
        init(keywords, caseSensitive, normalizeWhitespace, WordCharacters.generateWordCharsFlags(wordCharacters), thresholdStrategy);
    }

    // Set where digits and letters, '-' and '_' are considered word characters.
    public WholeWordLongestMatchSet(final Iterable<String> keywords, boolean caseSensitive, boolean normalizeWhitespace, Thresholder thresholdStrategy) {
        init(keywords, caseSensitive, normalizeWhitespace, WordCharacters.generateWordCharsFlags(), thresholdStrategy);
    }

    public void match(final String haystack, final SetMatchListener listener) {
        match(haystack, 0, haystack.length(), listener);
    }

    public void match(final String haystack, final int offset, final int length, final SetMatchListener listener) {
        // Nodes contain fail matches, which is the last normal match up the tree before the current node
        // match.

        // Start with the root node.
        TrieNode currentNode = root;
        boolean skipWhitespace = whitespaceReader.enabled();

        int idx = offset;
        int skipped = 0;
        // For each character.
        final int len = length;
        // Putting this if into the loop worsens the performance so we'll sadly
        // have to deal with duplicated code.
        if (caseSensitive) {
            while (idx < len) {
                char c = haystack.charAt(idx);
                boolean skippedWhitespace = skipWhitespace && whitespaceReader.isWhitespace(c);
                if(skippedWhitespace) {
                    c = ' ';
                }

                TrieNode nextNode = currentNode.getTransition(c);
                // Regardless of the type of the character, we keep moving till we run into
                // a situation where there's no transition available.
                if (nextNode == null) {
                    // Awkward if structure saves us a branch in the else statement.
                    if (!wordChars[c]) {
                        // If we ran into no-transition scenario on non-word character we can
                        // output the match on the current node if there is one, else we output
                        // a fail match if there is one.
                        // Later we will run through non-word characters to the start of the next word.
                        if (currentNode.matchLength != 0) {
                            if (!listener.match(haystack, idx - skipped - currentNode.matchLength, idx)) {
                                return;
                            }
                        } else if (currentNode.failMatchLength != 0) {
                            int failMatchEnd = idx - currentNode.failMatchOffset;
                            if (!listener.match(haystack, failMatchEnd - currentNode.failMatchLength, failMatchEnd)) {
                                return;
                            }
                        }
                    } else {
                        // If we ran into no-transition situation on a word character, we output any
                        // fail match on the node and scroll through word characters to a non-word character.
                        if (currentNode.failMatchLength != 0) {
                            int failMatchEnd = idx - currentNode.failMatchOffset;
                            if (!listener.match(haystack, failMatchEnd - currentNode.failMatchLength, failMatchEnd)) {
                                return;
                            }
                        }
                        // Scroll to the first non-word character
                        while (++idx < len && wordChars[haystack.charAt(idx)]) {
                            ;
                        }
                    }

                    // Scroll to the first word character
                    while (++idx < len && !wordChars[haystack.charAt(idx)]) {
                        ;
                    }

                    skipped = 0;

                    currentNode = root;
                } else {
                    if(skippedWhitespace) {
                        while (++idx < len && whitespaceReader.isWhitespace(haystack.charAt(idx)))  {
                            ++skipped;
                        }
                        --idx;
                    }
                    // If we have transition just take it.
                    ++idx;
                    currentNode = nextNode;
                }
            }
            // Output any matches on the last node, either a normal match or fail match.
            if (currentNode.matchLength != 0) {
                if (!listener.match(haystack, idx - skipped - currentNode.matchLength, idx)) {
                    return;
                }
            } else if (currentNode.failMatchLength != 0) {
                int failMatchEnd = idx - currentNode.failMatchOffset;
                if (!listener.match(haystack, failMatchEnd - currentNode.failMatchLength, failMatchEnd)) {
                    return;
                }
            }
        } else {
            while (idx < len) {
                char c = Character.toLowerCase(haystack.charAt(idx));
                boolean skippedWhitespace = skipWhitespace && whitespaceReader.isWhitespace(c);
                if(skippedWhitespace) {
                    c = ' ';
                }
                TrieNode nextNode = currentNode.getTransition(c);
                // Regardless of the type of the character, we keep moving till we run into
                // a situation where there's no transition available.
                if (nextNode == null) {
                    // Awkward if structure saves us a branch in the else statement.
                    if (!wordChars[c]) {
                        // If we ran into no-transition scenario on non-word character we can
                        // output the match on the current node if there is one, else we output
                        // a fail match if there is one.
                        // Later we will run through non-word characters to the start of the next word.
                        if (currentNode.matchLength != 0) {
                            if (!listener.match(haystack, idx - skipped - currentNode.matchLength, idx)) {
                                return;
                            }
                        } else if (currentNode.failMatchLength != 0) {
                            int failMatchEnd = idx - currentNode.failMatchOffset;
                            if (!listener.match(haystack, failMatchEnd - skipped - currentNode.failMatchLength, failMatchEnd)) {
                                return;
                            }
                        }
                    } else {
                        // If we ran into no-transition situation on a word character, we output any
                        // fail match on the node and scroll through word characters to a non-word character.
                        if (currentNode.failMatchLength != 0) {
                            int failMatchEnd = idx - currentNode.failMatchOffset;
                            if (!listener.match(haystack, failMatchEnd - skipped - currentNode.failMatchLength, failMatchEnd)) {
                                return;
                            }
                        }
                        // Scroll to the first non-word character
                        while (++idx < len && wordChars[haystack.charAt(idx)]) {
                            ;
                        }
                    }

                    // Scroll to the first word character
                    while (++idx < len && !wordChars[haystack.charAt(idx)]) {
                        ;
                    }
                    skipped = 0;
                    currentNode = root;
                } else {
                    if(skippedWhitespace) { // in the middle of a transition/partial match. If it was a whitespace skip it until we encounter non-whitespace.
                        while (++idx < len && whitespaceReader.isWhitespace(haystack.charAt(idx))) {
                            ++skipped;
                        }
                        --idx;
                    }
                    // If we have transition just take it.
                    ++idx;
                    currentNode = nextNode;
                }
            }
            // Output any matches on the last node, either a normal match or fail match.
            if (currentNode.matchLength != 0) {
                if (!listener.match(haystack, idx - skipped - currentNode.matchLength, idx)) {
                    return;
                }
            } else if (currentNode.failMatchLength != 0) {
                int failMatchEnd = idx - currentNode.failMatchOffset;
                if (!listener.match(haystack, failMatchEnd - skipped - currentNode.failMatchLength, failMatchEnd)) {
                    return;
                }
            }
        }
    }

    boolean[] getWordChars() {
        return wordChars;
    }

    private void init(final Iterable<String> keywords, boolean caseSensitive, boolean normalizeWhitespace, final boolean[] wordChars, final Thresholder thresholdStrategy) {
        this.caseSensitive = caseSensitive;
        this.normalizeWhitespace = normalizeWhitespace;
        this.wordChars = wordChars;

        if(normalizeWhitespace) {
            whitespaceReader = SkipWhiteSpaceReaderInstance;
        } else {
            whitespaceReader = NoOpWhiteSpaceReaderInstance;
        }

        // Create the root node
        root = new HashmapNode();
        // Add all keywords
        for (String keyword : keywords) {
            // Skip any empty keywords
            if (keyword != null) {
                keyword = WordCharacters.trim(keyword, wordChars);
                if(normalizeWhitespace) {
                    keyword = keywordWhitespaceNormalizer.matcher(keyword).replaceAll(" ");
                }

                if (keyword.length() > 0) {
                    // Start with the current node and traverse the tree
                    // character by character. Add nodes as needed to
                    // fill out the tree.
                    HashmapNode currentNode = (HashmapNode) root;
                    for (int idx = 0; idx < keyword.length(); idx++) {
                        currentNode = currentNode.getOrAddChild(caseSensitive ? keyword.charAt(idx) : Character.toLowerCase(keyword.charAt(idx)));
                    }
                    // Last node will contains the keyword as a match.
                    // Suffix matches will be added later.
                    currentNode.matchLength = keyword.length();
                }
            }
        }
        // Go through nodes breadth first, swap any hashmap nodes,
        // whose size is close to the size of range of keys with
        // flat array based nodes.

        // Calculate fail transitions and add suffix matches to nodes.
        // A lot of these properties are defined in a recursive fashion i.e.
        // calculating for a 3 letter word requires having done the calculation
        // for all 2 letter words.
        //
        final Queue<TrieNode> queue = new Queue<TrieNode>();
        root = root.optimizeNode(0, thresholdStrategy);
        queue.push(root);
        queue.push(null);
        // Need to use array to get mutateable state for anonymous class
        final int[] level = new int[] { 1 };

        EntryVisitor optimizeNodesAndFailTransitions = new EntryVisitor() {

            public void visit(TrieNode parent, char key, TrieNode value) {
                // First optimize node
                value = value.optimizeNode(level[0], thresholdStrategy);
                parent.updateTransition(key, value);
                // Fill the fail match variables. We do that by carrying the last match up the tree
                // and increasing the offset.
                // If the 'parent' node has a match and the transition is a non-word character
                // we carry that match as a fail match to children after that transition.
                if (parent.matchLength != 0 && !wordChars[key]) {
                    value.failMatchLength = parent.matchLength;
                    value.failMatchOffset = 1;
                } else {
                    value.failMatchLength = parent.failMatchLength;
                    value.failMatchOffset = parent.failMatchOffset + 1;
                }
                // Queue the non-leaf node.
                if (!value.isEmpty()) {
                    queue.push(value);
                }

            }
        };
        while (!queue.isEmpty()) {
            TrieNode n = queue.take();
            if (n == null) {
                if (!queue.isEmpty()) {
                    queue.push(null);
                    level[0]++;
                }
            } else {
                n.mapEntries(optimizeNodesAndFailTransitions);
            }
        }

    }

    private interface EntryVisitor {
        void visit(TrieNode parent, char key, TrieNode value);
    }

    // An open addressing hashmap implementation with linear probing
    // and capacity of 2^n
    private final static class HashmapNode extends TrieNode {

        // Start with capacity of 1 and resize as needed.
        private TrieNode[] children = new TrieNode[1];

        private char[] keys = new char[1];
        // Since capacity is a power of 2, we calculate mod by just
        // bitwise AND with the right mask.
        private int modulusMask = keys.length - 1;
        private int numEntries = 0;

        @Override
        public void clear() {
            children = new TrieNode[1];
            keys = new char[1];
            modulusMask = keys.length - 1;
            numEntries = 0;
        }

        @Override
        public TrieNode getTransition(final char key) {
            int defaultSlot = hash(key) & modulusMask;
            int currentSlot = defaultSlot;
            // Linear probing to find the entry for key.
            do {
                if (keys[currentSlot] == key) {
                    return children[currentSlot];
                } else if (children[currentSlot] == null) {
                    return null;
                } else {
                    currentSlot = ++currentSlot & modulusMask;
                }
            } while (currentSlot != defaultSlot);
            return null;
        }

        @Override
        public boolean isEmpty() {
            return numEntries == 0;
        }

        @Override
        public void mapEntries(EntryVisitor visitor) {
            for (int i = 0; i < keys.length; i++) {
                if (children[i] != null) {
                    visitor.visit(this, keys[i], children[i]);
                }
            }
        }

        @Override
        public void updateTransition(char c, TrieNode node) {
            int defaultSlot = hash(c) & modulusMask;
            int currentSlot = defaultSlot;
            do {
                if (children[currentSlot] == null) {
                    throw new IllegalArgumentException("Transition for " + c + " doesn't exist.");
                } else if (keys[currentSlot] == c) {
                    children[currentSlot] = node;
                    return;
                } else {
                    currentSlot = ++currentSlot & modulusMask;
                }
            } while (currentSlot != defaultSlot);
            throw new IllegalArgumentException("Transition for " + c + " doesn't exist.");
        }

        @Override
        protected TrieNode optimizeNode(int level, Thresholder thresholdStrategy) {
            char minKey = '\uffff';
            char maxKey = 0;
            // Find you the min and max key on the node.
            int size = numEntries;
            for (int i = 0; i < children.length; i++) {
                if (children[i] != null) {
                    if (keys[i] > maxKey) {
                        maxKey = keys[i];
                    }
                    if (keys[i] < minKey) {
                        minKey = keys[i];
                    }
                }
            }
            // If difference between min and max key are small
            // or only slightly larger than number of entries, use a range node
            int keyIntervalSize = maxKey - minKey + 1;
            if (thresholdStrategy.isOverThreshold(size, level, keyIntervalSize)) {
                return new RangeNode(this, minKey, maxKey);
            } else {
                return this;
            }
        }

        // Double the capacity of the node, calculate the new mask,
        // rehash and reinsert the entries
        private void enlarge() {
            char[] biggerKeys = new char[keys.length * 2];
            TrieNode[] biggerChildren = new TrieNode[children.length * 2];
            int biggerMask = biggerKeys.length - 1;
            for (int i = 0; i < children.length; i++) {
                char key = keys[i];
                TrieNode node = children[i];
                if (node != null) {
                    int defaultSlot = hash(key) & biggerMask;
                    int currentSlot = defaultSlot;
                    do {
                        if (biggerChildren[currentSlot] == null) {
                            biggerKeys[currentSlot] = key;
                            biggerChildren[currentSlot] = node;
                            break;
                        } else if (biggerKeys[currentSlot] == key) {
                            throw new IllegalStateException();
                        } else {
                            currentSlot = ++currentSlot & biggerMask;
                        }
                    } while (currentSlot != defaultSlot);
                }
            }
            this.keys = biggerKeys;
            this.children = biggerChildren;
            this.modulusMask = biggerMask;
        }

        // Return the node for a key or create a new hashmap node for that key
        // and return that.
        private HashmapNode getOrAddChild(char key) {
            // Check if we need to resize. Capacity of 2^16 doesn't need to resize.
            // If capacity is <16 and arrays are full or capacity is >16 and
            // arrays are 90% full, resize
            if (keys.length < 0x10000 && ((numEntries >= keys.length) || (numEntries > 16 && (numEntries >= keys.length * 0.90f)))) {
                enlarge();
            }
            int defaultSlot = hash(key) & modulusMask;
            int currentSlot = defaultSlot;
            do {
                if (children[currentSlot] == null) {
                    keys[currentSlot] = key;
                    HashmapNode newChild = new HashmapNode();
                    children[currentSlot] = newChild;
                    ++numEntries;
                    return newChild;
                } else if (keys[currentSlot] == key) {
                    return (HashmapNode) children[currentSlot];
                } else {
                    currentSlot = ++currentSlot & modulusMask;
                }
            } while (currentSlot != defaultSlot);
            throw new IllegalStateException();
        }

        // FNV-1a hash
        private int hash(char c) {
            // HASH_BASIS = 0x811c9dc5;
            final int HASH_PRIME = 16777619;
            return (((0x811c9dc5 ^ (c >> 8)) * HASH_PRIME) ^ (c & 0xff)) * HASH_PRIME;
        }

    }

    // This node is good at representing dense ranges of keys.
    // It has a single array of nodes and a base key value.
    // Child at array index 3 has key of baseChar + 3.
    private static final class RangeNode extends TrieNode {

        private char baseChar = 0;
        private TrieNode[] children;
        private int size = 0;

        private RangeNode(HashmapNode oldNode, char from, char to) {
            // Value of the first character
            this.baseChar = from;
            this.size = to - from + 1;
            this.matchLength = oldNode.matchLength;
            // Avoid even allocating a children array if size is 0.
            if (size <= 0) {
                size = 0;
            } else {
                this.children = new TrieNode[size];
                // Grab the children of the old node.
                for (int i = 0; i < oldNode.children.length; i++) {
                    if (oldNode.children[i] != null) {
                        children[oldNode.keys[i] - from] = oldNode.children[i];
                    }
                }
            }
        }

        @Override
        public void clear() {
            children = null;
            size = 0;
        }

        @Override
        public TrieNode getTransition(char c) {
            // First check if the key is between max and min value.
            // Here we use the fact that char type is unsigned to figure it out
            // with a single condition.
            int idx = (char) (c - baseChar);
            if (idx < size) {
                return children[idx];
            }
            return null;
        }

        @Override
        public boolean isEmpty() {
            return size == 0;
        }

        @Override
        public void mapEntries(EntryVisitor visitor) {
            if (children != null) {
                for (int i = 0; i < children.length; i++) {
                    if (children[i] != null && children[i] != this) {
                        visitor.visit(this, (char) (baseChar + i), children[i]);
                    }
                }
            }
        }

        @Override
        public void updateTransition(char c, TrieNode node) {
            // First check if the key is between max and min value.
            // Here we use the fact that char type is unsigned to figure it out
            // with a single condition.
            int idx = (char) (c - baseChar);
            if (idx < size) {
                if (children[idx] != null) {
                    children[idx] = node;
                    return;
                } else {
                    throw new IllegalArgumentException("Transition for " + c + " doesn't exist.");
                }
            }
            throw new IllegalArgumentException("Transition for " + c + " doesn't exist.");
        }

    }

    // Basic node for both
    private static abstract class TrieNode {

        protected int failMatchLength = 0;
        protected int failMatchOffset = 0;
        protected int matchLength = 0;

        public abstract void clear();

        // Get transition (root node returns something non-null for all characters - itself)
        public abstract TrieNode getTransition(char c);

        public abstract boolean isEmpty();

        public abstract void mapEntries(final EntryVisitor visitor);

        public abstract void updateTransition(char c, TrieNode node);

        protected TrieNode optimizeNode(int level, Thresholder thresholdStrategy) {
            return this;
        }

    }

    protected interface WhitespaceReader {
        boolean isWhitespace(char c);
        boolean enabled();
    }

    protected final static class NoOpWhitespaceReader implements WhitespaceReader {
        public boolean enabled() {
            return false;
        }

        public boolean isWhitespace(char c) {
            return false;
        }
    }

    protected final static class SkipWhitespaceReader implements WhitespaceReader {
        public boolean enabled() {
            return true;
        }

        public boolean isWhitespace(char c) {
            return c == ' ' || c == '\t' || c == '\n' || c == '\f' || c == '\r' || c == 11; // equivalent to regex '\s'
        }
    }

}
