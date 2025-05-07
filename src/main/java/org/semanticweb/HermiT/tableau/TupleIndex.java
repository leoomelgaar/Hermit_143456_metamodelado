package org.semanticweb.HermiT.tableau;

import java.io.Serializable;

final class TupleIndex
implements Serializable {
    private static final long serialVersionUID = -4284072092430590904L;
    private static final float LOAD_FACTOR = 0.7f;
    private static final int BUCKET_OFFSET = 1;
    final int[] m_indexingSequence;
    private final TrieNodeManager m_trieNodeManager;
    private int m_root;
    private int[] m_buckets;
    private int m_bucketsLengthMinusOne;
    private int m_resizeThreshold;
    private int m_numberOfNodes;
    private static final int TRIE_NODE_PARENT = 0;
    private static final int TRIE_NODE_FIRST_CHILD = 1;
    private static final int TRIE_NODE_TUPLE_INDEX = 1;
    private static final int TRIE_NODE_PREVIOUS_SIBLING = 2;
    private static final int TRIE_NODE_NEXT_SIBLING = 3;
    private static final int TRIE_NODE_NEXT_ENTRY = 4;
    private static final int TRIE_NODE_SIZE = 5;
    private static final int TRIE_NODE_PAGE_SIZE = 1024;

    public TupleIndex(int[] indexingSequence) {
        this.m_indexingSequence = indexingSequence;
        this.m_trieNodeManager = new TrieNodeManager();
        this.clear();
    }

    public int sizeInMemoy() {
        return this.m_buckets.length * 4 + this.m_trieNodeManager.size();
    }

    public int[] getIndexingSequence() {
        return this.m_indexingSequence;
    }

    public void clear() {
        this.m_trieNodeManager.clear();
        this.m_root = this.m_trieNodeManager.newTrieNode();
        this.m_trieNodeManager.initializeTrieNode(this.m_root, -1, -1, -1, -1, -1, null);
        this.m_buckets = new int[16];
        this.m_bucketsLengthMinusOne = this.m_buckets.length - 1;
        this.m_resizeThreshold = (int)((float)this.m_buckets.length * 0.7f);
        this.m_numberOfNodes = 0;
    }

    public int addTuple(Object[] tuple, int potentialTupleIndex) {
        int trieNode = this.m_root;
        for (int i : this.m_indexingSequence) {
            Object object = tuple[i];
            trieNode = this.getChildNodeAddIfNecessary(trieNode, object);
        }
        if (this.m_trieNodeManager.getTrieNodeComponent(trieNode, 1) == -1) {
            this.m_trieNodeManager.setTrieNodeComponent(trieNode, 1, potentialTupleIndex);
            return potentialTupleIndex;
        }
        return this.m_trieNodeManager.getTrieNodeComponent(trieNode, 1);
    }

    public int getTupleIndex(Object[] tuple) {
        int trieNode = this.m_root;
        for (int position = 0; position < this.m_indexingSequence.length; ++position) {
            Object object = tuple[this.m_indexingSequence[position]];
            if ((trieNode = this.getChildNode(trieNode, object)) != -1) continue;
            return -1;
        }
        return this.m_trieNodeManager.getTrieNodeComponent(trieNode, 1);
    }

    public int removeTuple(Object[] tuple) {
        int leafTrieNode = this.m_root;
        for (int position = 0; position < this.m_indexingSequence.length; ++position) {
            Object object = tuple[this.m_indexingSequence[position]];
            if ((leafTrieNode = this.getChildNode(leafTrieNode, object)) != -1) continue;
            return -1;
        }
        int tupleIndex = this.m_trieNodeManager.getTrieNodeComponent(leafTrieNode, 1);
        int trieNode = this.m_trieNodeManager.getTrieNodeComponent(leafTrieNode, 0);
        this.removeTrieNode(leafTrieNode);
        while (trieNode != this.m_root && this.m_trieNodeManager.getTrieNodeComponent(trieNode, 1) == -1) {
            int parentTrieNode = this.m_trieNodeManager.getTrieNodeComponent(trieNode, 0);
            this.removeTrieNode(trieNode);
            trieNode = parentTrieNode;
        }
        return tupleIndex;
    }

    private void removeTrieNode(int trieNode) {
        Object object = this.m_trieNodeManager.getTrieNodeObject(trieNode);
        int parent = this.m_trieNodeManager.getTrieNodeComponent(trieNode, 0);
        int bucketIndex = TupleIndex.getIndexFor(object.hashCode() + parent, this.m_bucketsLengthMinusOne);
        int child = this.m_buckets[bucketIndex] - 1;
        int previousChild = -1;
        while (child != -1) {
            int nextChild = this.m_trieNodeManager.getTrieNodeComponent(child, 4);
            if (child == trieNode) {
                --this.m_numberOfNodes;
                int previousSibling = this.m_trieNodeManager.getTrieNodeComponent(trieNode, 2);
                int nextSibling = this.m_trieNodeManager.getTrieNodeComponent(trieNode, 3);
                if (previousSibling == -1) {
                    this.m_trieNodeManager.setTrieNodeComponent(parent, 1, nextSibling);
                } else {
                    this.m_trieNodeManager.setTrieNodeComponent(previousSibling, 3, nextSibling);
                }
                if (nextSibling != -1) {
                    this.m_trieNodeManager.setTrieNodeComponent(nextSibling, 2, previousSibling);
                }
                if (previousChild == -1) {
                    this.m_buckets[bucketIndex] = nextChild + 1;
                } else {
                    this.m_trieNodeManager.setTrieNodeComponent(previousChild, 4, nextChild);
                }
                this.m_trieNodeManager.deleteTrieNode(trieNode);
                return;
            }
            previousChild = child;
            child = nextChild;
        }
        throw new IllegalStateException("Internal error: should be able to remove the child node.");
    }

    private int getChildNode(int parent, Object object) {
        int bucketIndex = TupleIndex.getIndexFor(object.hashCode() + parent, this.m_bucketsLengthMinusOne);
        int child = this.m_buckets[bucketIndex] - 1;
        while (child != -1) {
            if (parent == this.m_trieNodeManager.getTrieNodeComponent(child, 0) && object.equals(this.m_trieNodeManager.getTrieNodeObject(child))) {
                return child;
            }
            child = this.m_trieNodeManager.getTrieNodeComponent(child, 4);
        }
        return -1;
    }

    private int getChildNodeAddIfNecessary(int parent, Object object) {
        int hashCode = object.hashCode() + parent;
        int bucketIndex = TupleIndex.getIndexFor(hashCode, this.m_bucketsLengthMinusOne);
        int child = this.m_buckets[bucketIndex] - 1;
        while (child != -1) {
            if (parent == this.m_trieNodeManager.getTrieNodeComponent(child, 0) && object.equals(this.m_trieNodeManager.getTrieNodeObject(child))) {
                return child;
            }
            child = this.m_trieNodeManager.getTrieNodeComponent(child, 4);
        }
        if (this.m_numberOfNodes >= this.m_resizeThreshold) {
            this.resizeBuckets();
            bucketIndex = TupleIndex.getIndexFor(hashCode, this.m_bucketsLengthMinusOne);
        }
        child = this.m_trieNodeManager.newTrieNode();
        int nextSibling = this.m_trieNodeManager.getTrieNodeComponent(parent, 1);
        if (nextSibling != -1) {
            this.m_trieNodeManager.setTrieNodeComponent(nextSibling, 2, child);
        }
        this.m_trieNodeManager.setTrieNodeComponent(parent, 1, child);
        this.m_trieNodeManager.initializeTrieNode(child, parent, -1, -1, nextSibling, this.m_buckets[bucketIndex] - 1, object);
        this.m_buckets[bucketIndex] = child + 1;
        ++this.m_numberOfNodes;
        return child;
    }

    private void resizeBuckets() {
        if (this.m_buckets.length == 1073741824) {
            this.m_resizeThreshold = Integer.MAX_VALUE;
        } else {
            int[] newBuckets = new int[this.m_buckets.length * 2];
            int newBucketsLengthMinusOne = newBuckets.length - 1;
            for (int bucketIndex = this.m_bucketsLengthMinusOne; bucketIndex >= 0; --bucketIndex) {
                int trieNode = this.m_buckets[bucketIndex] - 1;
                while (trieNode != -1) {
                    int nextTrieNode = this.m_trieNodeManager.getTrieNodeComponent(trieNode, 4);
                    int hashCode = this.m_trieNodeManager.getTrieNodeObject(trieNode).hashCode() + this.m_trieNodeManager.getTrieNodeComponent(trieNode, 0);
                    int newBucketIndex = TupleIndex.getIndexFor(hashCode, newBucketsLengthMinusOne);
                    this.m_trieNodeManager.setTrieNodeComponent(trieNode, 4, newBuckets[newBucketIndex] - 1);
                    newBuckets[newBucketIndex] = trieNode + 1;
                    trieNode = nextTrieNode;
                }
            }
            this.m_buckets = newBuckets;
            this.m_bucketsLengthMinusOne = newBucketsLengthMinusOne;
            this.m_resizeThreshold = (int)((float)this.m_buckets.length * 0.7f);
        }
    }

    private static int getIndexFor(int hashCode, int tableLengthMinusOne) {
        hashCode += ~ (hashCode << 9);
        hashCode ^= hashCode >>> 14;
        hashCode += hashCode << 4;
        hashCode ^= hashCode >>> 10;
        return hashCode & tableLengthMinusOne;
    }

    public static class TupleIndexRetrieval
    implements Serializable {
        private static final long serialVersionUID = 3052986474027614595L;
        protected final TupleIndex m_tupleIndex;
        protected final Object[] m_bindingsBuffer;
        protected final int[] m_selectionIndices;
        protected final int m_selectionIndicesLength;
        protected final int m_indexingSequenceLength;
        protected int m_currentTrieNode;

        public TupleIndexRetrieval(TupleIndex tupleIndex, Object[] bindingsBuffer, int[] selectionIndices) {
            this.m_tupleIndex = tupleIndex;
            this.m_bindingsBuffer = bindingsBuffer;
            this.m_selectionIndices = selectionIndices;
            this.m_selectionIndicesLength = this.m_selectionIndices.length;
            this.m_indexingSequenceLength = tupleIndex.m_indexingSequence.length;
        }

        public void open() {
            this.m_currentTrieNode = this.m_tupleIndex.m_root;
            for (int position = 0; position < this.m_selectionIndicesLength; ++position) {
                Object object = this.m_bindingsBuffer[this.m_selectionIndices[position]];
                this.m_currentTrieNode = this.m_tupleIndex.getChildNode(this.m_currentTrieNode, object);
                if (this.m_currentTrieNode != -1) continue;
                return;
            }
            if (this.m_selectionIndicesLength == 0 && this.m_tupleIndex.m_trieNodeManager.getTrieNodeComponent(this.m_tupleIndex.m_root, 1) == -1) {
                this.m_currentTrieNode = -1;
            } else {
                for (int index = this.m_selectionIndicesLength; index < this.m_indexingSequenceLength; ++index) {
                    this.m_currentTrieNode = this.m_tupleIndex.m_trieNodeManager.getTrieNodeComponent(this.m_currentTrieNode, 1);
                }
            }
        }

        public boolean afterLast() {
            return this.m_currentTrieNode == -1;
        }

        public int getCurrentTupleIndex() {
            return this.m_tupleIndex.m_trieNodeManager.getTrieNodeComponent(this.m_currentTrieNode, 1);
        }

        public void next() {
            int trieNodeDepth;
            for (trieNodeDepth = this.m_indexingSequenceLength; trieNodeDepth != this.m_selectionIndicesLength && this.m_tupleIndex.m_trieNodeManager.getTrieNodeComponent(this.m_currentTrieNode, 3) == -1; --trieNodeDepth) {
                this.m_currentTrieNode = this.m_tupleIndex.m_trieNodeManager.getTrieNodeComponent(this.m_currentTrieNode, 0);
            }
            if (trieNodeDepth == this.m_selectionIndicesLength) {
                this.m_currentTrieNode = -1;
            } else {
                this.m_currentTrieNode = this.m_tupleIndex.m_trieNodeManager.getTrieNodeComponent(this.m_currentTrieNode, 3);
                for (int index = trieNodeDepth; index < this.m_indexingSequenceLength; ++index) {
                    this.m_currentTrieNode = this.m_tupleIndex.m_trieNodeManager.getTrieNodeComponent(this.m_currentTrieNode, 1);
                }
            }
        }
    }

    protected static final class TrieNodeManager
    implements Serializable {
        private static final long serialVersionUID = -1978070096232682717L;
        private int[][] m_indexPages;
        private Object[][] m_objectPages;
        private int m_firstFreeTrieNode;
        private int m_numberOfPages;

        public TrieNodeManager() {
            this.clear();
        }

        public int size() {
            int i;
            int size = this.m_indexPages.length * 4 + this.m_objectPages.length * 4;
            for (i = this.m_indexPages.length - 1; i >= 0; --i) {
                if (this.m_indexPages[i] == null) continue;
                size += this.m_indexPages[i].length * 4;
            }
            for (i = this.m_objectPages.length - 1; i >= 0; --i) {
                if (this.m_objectPages[i] == null) continue;
                size += this.m_objectPages[i].length * 4;
            }
            return size;
        }

        public void clear() {
            this.m_indexPages = new int[10][];
            this.m_indexPages[0] = new int[5120];
            this.m_objectPages = new Object[10][];
            this.m_objectPages[0] = new Object[1024];
            this.m_numberOfPages = 1;
            this.m_firstFreeTrieNode = 0;
            this.setTrieNodeComponent(this.m_firstFreeTrieNode, 3, -1);
        }

        public int getTrieNodeComponent(int trieNode, int component) {
            return this.m_indexPages[trieNode / 1024][trieNode % 1024 * 5 + component];
        }

        public void setTrieNodeComponent(int trieNode, int component, int value) {
            this.m_indexPages[trieNode / 1024][trieNode % 1024 * 5 + component] = value;
        }

        public Object getTrieNodeObject(int trieNode) {
            return this.m_objectPages[trieNode / 1024][trieNode % 1024];
        }

        public void setTrieNodeObject(int trieNode, Object object) {
            this.m_objectPages[trieNode / 1024][trieNode % 1024] = object;
        }

        public void initializeTrieNode(int trieNode, int parent, int firstChild, int previousSibling, int nextSibling, int nextEntry, Object object) {
            int pageIndex = trieNode / 1024;
            int indexInPage = trieNode % 1024;
            int[] indexPage = this.m_indexPages[pageIndex];
            int start = indexInPage * 5;
            indexPage[start] = parent;
            indexPage[start + 1] = firstChild;
            indexPage[start + 2] = previousSibling;
            indexPage[start + 3] = nextSibling;
            indexPage[start + 4] = nextEntry;
            this.m_objectPages[pageIndex][indexInPage] = object;
        }

        public int newTrieNode() {
            int newTrieNode = this.m_firstFreeTrieNode;
            int nextFreeTrieNode = this.getTrieNodeComponent(this.m_firstFreeTrieNode, 3);
            if (nextFreeTrieNode != -1) {
                this.m_firstFreeTrieNode = nextFreeTrieNode;
            } else {
                ++this.m_firstFreeTrieNode;
                if (this.m_firstFreeTrieNode < 0) {
                    throw new OutOfMemoryError("The space of nodes in TupleIndex was exhausted: the ontology is just too large.");
                }
                int pageIndex = this.m_firstFreeTrieNode / 1024;
                if (pageIndex >= this.m_numberOfPages) {
                    if (pageIndex >= this.m_indexPages.length) {
                        int[][] newIndexPages = new int[this.m_indexPages.length * 3 / 2][];
                        System.arraycopy(this.m_indexPages, 0, newIndexPages, 0, this.m_indexPages.length);
                        this.m_indexPages = newIndexPages;
                        Object[][] newObjectPages = new Object[this.m_objectPages.length * 3 / 2][];
                        System.arraycopy(this.m_objectPages, 0, newObjectPages, 0, this.m_objectPages.length);
                        this.m_objectPages = newObjectPages;
                    }
                    this.m_indexPages[pageIndex] = new int[5120];
                    this.m_objectPages[pageIndex] = new Object[1024];
                    ++this.m_numberOfPages;
                }
                this.setTrieNodeComponent(this.m_firstFreeTrieNode, 3, -1);
            }
            return newTrieNode;
        }

        public void deleteTrieNode(int trieNode) {
            this.setTrieNodeComponent(trieNode, 3, this.m_firstFreeTrieNode);
            this.setTrieNodeObject(trieNode, null);
            this.m_firstFreeTrieNode = trieNode;
        }
    }

}

