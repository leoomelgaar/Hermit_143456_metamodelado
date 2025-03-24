package org.semanticweb.HermiT.blocking;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.semanticweb.HermiT.tableau.Node;

class ValidatedBlockersCache {
    protected final DirectBlockingChecker m_directBlockingChecker;
    protected CacheEntry[] m_buckets;
    protected int m_numberOfElements;
    protected int m_threshold;
    protected CacheEntry m_emptyEntries;

    public ValidatedBlockersCache(DirectBlockingChecker directBlockingChecker) {
        this.m_directBlockingChecker = directBlockingChecker;
        this.clear();
    }

    public boolean isEmpty() {
        return this.m_numberOfElements == 0;
    }

    public void clear() {
        this.m_buckets = new CacheEntry[1024];
        this.m_threshold = (int)((double)this.m_buckets.length * 0.75);
        this.m_numberOfElements = 0;
        this.m_emptyEntries = null;
    }

    public boolean removeNode(Node node) {
        CacheEntry removeEntry = (CacheEntry)node.getBlockingCargo();
        if (removeEntry != null) {
            int bucketIndex = ValidatedBlockersCache.getIndexFor(removeEntry.m_hashCode, this.m_buckets.length);
            CacheEntry lastEntry = null;
            CacheEntry entry = this.m_buckets[bucketIndex];
            while (entry != null) {
                if (entry == removeEntry) {
                    if (node == entry.m_nodes.get(0)) {
                        for (Node n : entry.m_nodes) {
                            n.setBlockingCargo(null);
                        }
                        if (lastEntry == null) {
                            this.m_buckets[bucketIndex] = entry.m_nextEntry;
                        } else {
                            lastEntry.m_nextEntry = entry.m_nextEntry;
                        }
                        entry.m_nextEntry = this.m_emptyEntries;
                        entry.m_nodes = new ArrayList<Node>();
                        entry.m_hashCode = 0;
                        this.m_emptyEntries = entry;
                        --this.m_numberOfElements;
                    } else if (entry.m_nodes.contains(node)) {
                        for (int i = entry.m_nodes.size() - 1; i >= entry.m_nodes.indexOf(node); --i) {
                            entry.m_nodes.get(i).setBlockingCargo(null);
                        }
                        entry.m_nodes.subList(entry.m_nodes.indexOf(node), entry.m_nodes.size()).clear();
                    } else {
                        throw new IllegalStateException("Internal error: entry not in cache!");
                    }
                    return true;
                }
                lastEntry = entry;
                entry = entry.m_nextEntry;
            }
            throw new IllegalStateException("Internal error: entry not in cache!");
        }
        return false;
    }

    public void addNode(Node node) {
        int hashCode = this.m_directBlockingChecker.blockingHashCode(node);
        int bucketIndex = ValidatedBlockersCache.getIndexFor(hashCode, this.m_buckets.length);
        CacheEntry entry = this.m_buckets[bucketIndex];
        while (entry != null) {
            if (hashCode == entry.m_hashCode && this.m_directBlockingChecker.isBlockedBy(entry.m_nodes.get(0), node)) {
                if (!entry.m_nodes.contains(node)) {
                    entry.add(node);
                    node.setBlockingCargo(entry);
                    return;
                }
                throw new IllegalStateException("Internal error: node already in the cache!");
            }
            entry = entry.m_nextEntry;
        }
        if (this.m_emptyEntries == null) {
            entry = new CacheEntry();
        } else {
            entry = this.m_emptyEntries;
            this.m_emptyEntries = this.m_emptyEntries.m_nextEntry;
        }
        entry.initialize(node, hashCode, this.m_buckets[bucketIndex]);
        this.m_buckets[bucketIndex] = entry;
        node.setBlockingCargo(entry);
        ++this.m_numberOfElements;
        if (this.m_numberOfElements >= this.m_threshold) {
            this.resize(this.m_buckets.length * 2);
        }
    }

    protected void resize(int newCapacity) {
        CacheEntry[] newBuckets = new CacheEntry[newCapacity];
        for (int i = 0; i < this.m_buckets.length; ++i) {
            CacheEntry entry = this.m_buckets[i];
            while (entry != null) {
                CacheEntry nextEntry = entry.m_nextEntry;
                int newIndex = ValidatedBlockersCache.getIndexFor(entry.m_hashCode, newCapacity);
                entry.m_nextEntry = newBuckets[newIndex];
                newBuckets[newIndex] = entry;
                entry = nextEntry;
            }
        }
        this.m_buckets = newBuckets;
        this.m_threshold = (int)((double)newCapacity * 0.75);
    }

    public Node getBlocker(Node node) {
        if (this.m_directBlockingChecker.canBeBlocked(node)) {
            int hashCode = this.m_directBlockingChecker.blockingHashCode(node);
            int bucketIndex = ValidatedBlockersCache.getIndexFor(hashCode, this.m_buckets.length);
            CacheEntry entry = this.m_buckets[bucketIndex];
            while (entry != null) {
                if (hashCode == entry.m_hashCode && this.m_directBlockingChecker.isBlockedBy(entry.m_nodes.get(0), node)) {
                    if (node.getBlocker() != null && entry.m_nodes.contains(node.getBlocker())) {
                        return node.getBlocker();
                    }
                    return entry.m_nodes.get(0);
                }
                entry = entry.m_nextEntry;
            }
        }
        return null;
    }

    public List<Node> getPossibleBlockers(Node node) {
        if (this.m_directBlockingChecker.canBeBlocked(node)) {
            int hashCode = this.m_directBlockingChecker.blockingHashCode(node);
            int bucketIndex = ValidatedBlockersCache.getIndexFor(hashCode, this.m_buckets.length);
            CacheEntry entry = this.m_buckets[bucketIndex];
            while (entry != null) {
                if (hashCode == entry.m_hashCode && this.m_directBlockingChecker.isBlockedBy(entry.m_nodes.get(0), node)) {
                    assert (!entry.m_nodes.contains(node));
                    return entry.m_nodes;
                }
                entry = entry.m_nextEntry;
            }
        }
        return new ArrayList<Node>();
    }

    protected static int getIndexFor(int _hashCode, int tableLength) {
        int hashCode = _hashCode;
        hashCode += ~ (hashCode << 9);
        hashCode ^= hashCode >>> 14;
        hashCode += hashCode << 4;
        hashCode ^= hashCode >>> 10;
        return hashCode & tableLength - 1;
    }

    public String toString() {
        String buckets = "";
        for (int i = 0; i < this.m_buckets.length; ++i) {
            CacheEntry entry = this.m_buckets[i];
            if (entry == null) continue;
            buckets = buckets + "Bucket " + i + ": [" + entry + "] ";
        }
        return buckets;
    }

    public static class CacheEntry
    implements Serializable {
        private static final long serialVersionUID = -7047487963170250200L;
        protected List<Node> m_nodes;
        protected int m_hashCode;
        protected CacheEntry m_nextEntry;

        public void initialize(Node node, int hashCode, CacheEntry nextEntry) {
            this.m_nodes = new ArrayList<Node>();
            this.add(node);
            this.m_hashCode = hashCode;
            this.m_nextEntry = nextEntry;
        }

        public boolean add(Node node) {
            for (Node n : this.m_nodes) {
                assert (n.getNodeID() <= node.getNodeID());
            }
            return this.m_nodes.add(node);
        }

        public String toString() {
            String nodes = "HashCode: " + this.m_hashCode + " Nodes: ";
            for (Node n : this.m_nodes) {
                nodes = nodes + n.getNodeID() + " ";
            }
            return nodes;
        }
    }

}

