package com.roklenarcic.util.strings;

import com.roklenarcic.util.strings.threshold.RangeNodeThreshold;
import com.roklenarcic.util.strings.threshold.Thresholder;

// A set that matches only whole word matches. Non-word characters are user defined (with a default).
// Any non-word characters around input strings get trimmed. Non-word characters are allowed in the keywords.
class WholeWordLongestMatchSet implements StringSet {

    private boolean caseSensitive = true;
    private TrieNode root;
    private Thresholder thresholdStrategy;
    private boolean[] wordChars;

    // Set where digits and letters, '-' and '_' are considered word characters.
    public WholeWordLongestMatchSet(final Iterable<String> keywords, boolean caseSensitive) {
        this(keywords, caseSensitive, new RangeNodeThreshold());
    }

    // Set where the characters in the given array are considered word characters
    public WholeWordLongestMatchSet(final Iterable<String> keywords, boolean caseSensitive, char[] wordCharacters) {
        this(keywords, caseSensitive, wordCharacters, new RangeNodeThreshold());
    }

    // Set where digits and letters and '-' and '_' are considered word characters but modified by the two
    // given arrays
    public WholeWordLongestMatchSet(final Iterable<String> keywords, boolean caseSensitive, char[] wordCharacters, boolean[] toggleFlags) {
        this(keywords, caseSensitive, wordCharacters, toggleFlags, new RangeNodeThreshold());
    }

    // Set where digits and letters and '-' and '_' are considered word characters but modified by the two
    // given arrays
    public WholeWordLongestMatchSet(final Iterable<String> keywords, boolean caseSensitive, char[] wordCharacters, boolean[] toggleFlags,
            Thresholder thresholdStrategy) {
        init(keywords, caseSensitive, WordCharacters.generateWordCharsFlags(wordCharacters, toggleFlags), thresholdStrategy);
    }

    // Set where the characters in the given array are considered word characters
    public WholeWordLongestMatchSet(final Iterable<String> keywords, boolean caseSensitive, char[] wordCharacters, Thresholder thresholdStrategy) {
        init(keywords, caseSensitive, WordCharacters.generateWordCharsFlags(wordCharacters), thresholdStrategy);
    }

    // Set where digits and letters, '-' and '_' are considered word characters.
    public WholeWordLongestMatchSet(final Iterable<String> keywords, boolean caseSensitive, Thresholder thresholdStrategy) {
        init(keywords, caseSensitive, WordCharacters.generateWordCharsFlags(), thresholdStrategy);
    }

    public void match(final String haystack, final SetMatchListener listener) {
        // Nodes contain fail matches, which is the last normal match up the tree before the current node
        // match.

        // Start with the root node.
        TrieNode currentNode = root;

        int idx = 0;
        // For each character.
        final int len = haystack.length();
        // Putting this if into the loop worsens the performance so we'll sadly
        // have to deal with duplicated code.
        if (caseSensitive) {
            while (idx < len) {
                char c = haystack.charAt(idx);
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
                            if (!listener.match(haystack, idx - currentNode.matchLength, idx)) {
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
                    currentNode = root;
                } else {
                    // If we have transition just take it.
                    ++idx;
                    currentNode = nextNode;
                }
            }
            // Output any matches on the last node, either a normal match or fail match.
            if (currentNode.matchLength != 0) {
                if (!listener.match(haystack, idx - currentNode.matchLength, idx)) {
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
                            if (!listener.match(haystack, idx - currentNode.matchLength, idx)) {
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
                    currentNode = root;
                } else {
                    // If we have transition just take it.
                    ++idx;
                    currentNode = nextNode;
                }
            }
            // Output any matches on the last node, either a normal match or fail match.
            if (currentNode.matchLength != 0) {
                if (!listener.match(haystack, idx - currentNode.matchLength, idx)) {
                    return;
                }
            } else if (currentNode.failMatchLength != 0) {
                int failMatchEnd = idx - currentNode.failMatchOffset;
                if (!listener.match(haystack, failMatchEnd - currentNode.failMatchLength, failMatchEnd)) {
                    return;
                }
            }
        }
    }

    boolean[] getWordChars() {
        return wordChars;
    }

    private void init(final Iterable<String> keywords, boolean caseSensitive, final boolean[] wordChars, Thresholder thresholdStrategy) {
        this.thresholdStrategy = thresholdStrategy;
        this.wordChars = wordChars;
        // Create the root node
        root = new HashmapNode();
        // Add all keywords
        for (String keyword : keywords) {
            // Skip any empty keywords
            if (keyword != null) {
                keyword = WordCharacters.trim(keyword, wordChars);
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
        // Go through nodes depth first, swap any hashmap nodes,
        // whose size is close to the size of range of keys with
        // flat array based nodes.
        root = optimizeNodes(root, 0);
        // Fill the fail match variables. We do that by carrying the last match up the tree
        // and increasing the offset.
        root.mapEntries(new EntryVisitor() {

            private int failMatchLength = 0;
            private int failMatchOffset = 0;

            public void visit(TrieNode parent, char key, TrieNode value) {
                // We save the state so we can restore it later. It's a poor man's stack.
                int length = failMatchLength;
                int offset = failMatchOffset;
                // If the 'parent' node has a match and the transition is a non-word character
                // we carry that match as a fail match to children after that transition.
                if (parent.matchLength != 0 && !wordChars[key]) {
                    failMatchLength = parent.matchLength;
                    failMatchOffset = 1;
                } else {
                    failMatchOffset++;
                }
                value.failMatchLength = failMatchLength;
                value.failMatchOffset = failMatchOffset;
                value.mapEntries(this);
                // Reset the state before exiting.
                failMatchLength = length;
                failMatchOffset = offset;

            }
        });
    }

    // A recursive function that replaces hashmap nodes with range nodes
    // when appropriate.
    private final TrieNode optimizeNodes(TrieNode n, int level) {
        if (n instanceof HashmapNode) {
            HashmapNode node = (HashmapNode) n;
            char minKey = '\uffff';
            char maxKey = 0;
            // Find you the min and max key on the node.
            int size = node.numEntries;
            for (int i = 0; i < node.children.length; i++) {
                if (node.children[i] != null) {
                    node.children[i] = optimizeNodes(node.children[i], level + 1);
                    if (node.keys[i] > maxKey) {
                        maxKey = node.keys[i];
                    }
                    if (node.keys[i] < minKey) {
                        minKey = node.keys[i];
                    }
                }
            }
            // If difference between min and max key are small
            // or only slightly larger than number of entries, use a range node
            int keyIntervalSize = maxKey - minKey + 1;
            if (thresholdStrategy.isOverThreshold(size, level, keyIntervalSize)) {
                return new RangeNode(node, minKey, maxKey);
            }
        }
        return n;
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

    }

}