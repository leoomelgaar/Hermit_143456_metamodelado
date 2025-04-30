package org.semanticweb.HermiT.tableau;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class DependencySetFactory
implements Serializable {
    private static final long serialVersionUID = 8632867055646817311L;
    private final IntegerArray m_mergeArray = new IntegerArray();
    private final List<PermanentDependencySet> m_mergeSets = new ArrayList<PermanentDependencySet>();
    private final List<UnionDependencySet> m_unprocessedSets = new ArrayList<UnionDependencySet>();
    PermanentDependencySet m_emptySet;
    private PermanentDependencySet m_firstUnusedSet;
    private PermanentDependencySet m_firstDestroyedSet;
    private PermanentDependencySet[] m_entries;
    private int m_size;
    private int m_resizeThreshold;
    int lastEntryAddedIndex;

    public DependencySetFactory() {
        this.clear();
    }

    public int sizeInMemory() {
        return this.m_entries.length * 4 + this.m_size * 20;
    }

    public void clear() {
        this.m_mergeArray.clear();
        this.m_mergeSets.clear();
        this.m_unprocessedSets.clear();
        this.m_emptySet = new PermanentDependencySet();
        this.m_emptySet.m_branchingPoint = -1;
        this.m_emptySet.m_usageCounter = 1;
        this.m_emptySet.m_rest = null;
        this.m_emptySet.m_previousUnusedSet = null;
        this.m_emptySet.m_nextUnusedSet = null;
        this.m_firstUnusedSet = null;
        this.m_firstDestroyedSet = null;
        this.m_entries = new PermanentDependencySet[16];
        this.m_resizeThreshold = (int)((double)this.m_entries.length * 0.75);
        this.m_size = 0;
        this.lastEntryAddedIndex = -1;
    }

    public PermanentDependencySet emptySet() {
        return this.m_emptySet;
    }

    public void removeUnusedSets() {
        while (this.m_firstUnusedSet != null) {
            this.destroyDependencySet(this.m_firstUnusedSet);
        }
    }

    public void addUsage(PermanentDependencySet dependencySet) {
        assert (dependencySet.m_branchingPoint >= 0 || dependencySet == this.m_emptySet);
        if (dependencySet.m_usageCounter == 0) {
            this.removeFromUnusedList(dependencySet);
        }
        ++dependencySet.m_usageCounter;
    }

    public void removeUsage(PermanentDependencySet dependencySet) {
        assert (dependencySet.m_branchingPoint >= 0 || dependencySet == this.m_emptySet);
        assert (dependencySet.m_usageCounter > 0);
        assert (dependencySet.m_previousUnusedSet == null);
        assert (dependencySet.m_nextUnusedSet == null);
        --dependencySet.m_usageCounter;
        if (dependencySet.m_usageCounter == 0) {
            this.addToUnusedList(dependencySet);
        }
    }

    public PermanentDependencySet addBranchingPoint(DependencySet dependencySet, int branchingPoint) {
        PermanentDependencySet permanentDependencySet = this.getPermanent(dependencySet);
        if (branchingPoint > permanentDependencySet.m_branchingPoint) {
            return this.getDepdendencySet(permanentDependencySet, branchingPoint);
        }
        if (branchingPoint == permanentDependencySet.m_branchingPoint) {
            return permanentDependencySet;
        }
        this.m_mergeArray.clear();
        PermanentDependencySet rest = permanentDependencySet;
        while (branchingPoint < rest.m_branchingPoint) {
            this.m_mergeArray.add(rest.m_branchingPoint);
            rest = rest.m_rest;
        }
        if (branchingPoint == rest.m_branchingPoint) {
            return permanentDependencySet;
        }
        rest = this.getDepdendencySet(rest, branchingPoint);
        for (int index = this.m_mergeArray.size() - 1; index >= 0; --index) {
            rest = this.getDepdendencySet(rest, this.m_mergeArray.get(index));
        }
        return rest;
    }

    private PermanentDependencySet getDepdendencySet(PermanentDependencySet rest, int branchingPoint) {
        int index = rest.hashCode() + branchingPoint & this.m_entries.length - 1;
        PermanentDependencySet dependencySet = this.m_entries[index];
        while (dependencySet != null) {
            if (dependencySet.m_rest == rest && dependencySet.m_branchingPoint == branchingPoint) {
                return dependencySet;
            }
            dependencySet = dependencySet.m_nextEntry;
        }
        dependencySet = this.createDependencySet(rest, branchingPoint);
        dependencySet.m_nextEntry = this.m_entries[index];
        this.m_entries[index] = dependencySet;
        this.lastEntryAddedIndex = index;
        if (this.m_size >= this.m_resizeThreshold) {
            this.resizeEntries();
        }
        return dependencySet;
    }

    private PermanentDependencySet createDependencySet(PermanentDependencySet rest, int branchingPoint) {
        PermanentDependencySet newSet;
        if (this.m_firstDestroyedSet == null) {
            newSet = new PermanentDependencySet();
        } else {
            newSet = this.m_firstDestroyedSet;
            this.m_firstDestroyedSet = this.m_firstDestroyedSet.m_nextEntry;
        }
        newSet.m_rest = rest;
        newSet.m_branchingPoint = branchingPoint;
        newSet.m_usageCounter = 0;
        this.addUsage(newSet.m_rest);
        this.addToUnusedList(newSet);
        ++this.m_size;
        return newSet;
    }

    private void destroyDependencySet(PermanentDependencySet dependencySet) {
        assert (dependencySet.m_branchingPoint >= 0);
        assert (dependencySet.m_usageCounter == 0);
        assert (dependencySet.m_rest.m_usageCounter > 0);
        this.removeFromUnusedList(dependencySet);
        this.removeUsage(dependencySet.m_rest);
        this.removeFromEntries(dependencySet);
        dependencySet.m_rest = null;
        dependencySet.m_branchingPoint = -2;
        dependencySet.m_nextEntry = this.m_firstDestroyedSet;
        this.m_firstDestroyedSet = dependencySet;
        --this.m_size;
    }

    private void removeFromEntries(PermanentDependencySet dependencySet) {
        int index = dependencySet.m_rest.hashCode() + dependencySet.m_branchingPoint & this.m_entries.length - 1;
        PermanentDependencySet lastEntry = null;
        PermanentDependencySet entry = this.m_entries[index];
        while (entry != null) {
            if (entry == dependencySet) {
                if (lastEntry == null) {
                    this.m_entries[index] = dependencySet.m_nextEntry;
                } else {
                    lastEntry.m_nextEntry = dependencySet.m_nextEntry;
                }
                return;
            }
            lastEntry = entry;
            this.lastEntryAddedIndex = index;
            entry = entry.m_nextEntry;
        }
        throw new IllegalStateException("Internal error: dependency set not in the entries table. Please inform HermiT authors about this.");
    }

    private void removeFromUnusedList(PermanentDependencySet dependencySet) {
        if (dependencySet.m_previousUnusedSet != null) {
            dependencySet.m_previousUnusedSet.m_nextUnusedSet = dependencySet.m_nextUnusedSet;
        } else {
            this.m_firstUnusedSet = dependencySet.m_nextUnusedSet;
        }
        if (dependencySet.m_nextUnusedSet != null) {
            dependencySet.m_nextUnusedSet.m_previousUnusedSet = dependencySet.m_previousUnusedSet;
        }
        dependencySet.m_previousUnusedSet = null;
        dependencySet.m_nextUnusedSet = null;
    }

    private void addToUnusedList(PermanentDependencySet dependencySet) {
        dependencySet.m_previousUnusedSet = null;
        dependencySet.m_nextUnusedSet = this.m_firstUnusedSet;
        if (this.m_firstUnusedSet != null) {
            this.m_firstUnusedSet.m_previousUnusedSet = dependencySet;
        }
        this.m_firstUnusedSet = dependencySet;
    }

    private void resizeEntries() {
        int newLength = this.m_entries.length * 2;
        int newLengthMinusOne = newLength - 1;
        PermanentDependencySet[] newEntries = new PermanentDependencySet[newLength];
        for (int oldIndex = 0; oldIndex < this.m_entries.length; ++oldIndex) {
            PermanentDependencySet entry = this.m_entries[oldIndex];
            while (entry != null) {
                PermanentDependencySet nextEntry = entry.m_nextEntry;
                int newIndex = entry.m_rest.hashCode() + entry.m_branchingPoint & newLengthMinusOne;
                entry.m_nextEntry = newEntries[newIndex];
                newEntries[newIndex] = entry;
                entry = nextEntry;
                this.lastEntryAddedIndex = newIndex;
            }
        }
        this.m_entries = newEntries;
        this.m_resizeThreshold = (int)((double)this.m_entries.length * 0.75);
    }

    public PermanentDependencySet removeBranchingPoint(DependencySet dependencySet, int branchingPoint) {
        PermanentDependencySet permanentDependencySet = this.getPermanent(dependencySet);
        if (branchingPoint == permanentDependencySet.m_branchingPoint) {
            return permanentDependencySet.m_rest;
        }
        if (branchingPoint > permanentDependencySet.m_branchingPoint) {
            return permanentDependencySet;
        }
        this.m_mergeArray.clear();
        PermanentDependencySet rest = permanentDependencySet;
        while (branchingPoint < rest.m_branchingPoint) {
            this.m_mergeArray.add(rest.m_branchingPoint);
            rest = rest.m_rest;
        }
        if (branchingPoint != rest.m_branchingPoint) {
            return permanentDependencySet;
        }
        rest = rest.m_rest;
        for (int index = this.m_mergeArray.size() - 1; index >= 0; --index) {
            rest = this.getDepdendencySet(rest, this.m_mergeArray.get(index));
        }
        return rest;
    }

    public PermanentDependencySet unionWith(DependencySet set1, DependencySet set2) {
        PermanentDependencySet permanentSet2;
        PermanentDependencySet permanentSet1 = this.getPermanent(set1);
        if (permanentSet1 == (permanentSet2 = this.getPermanent(set2))) {
            return permanentSet1;
        }
        this.m_mergeArray.clear();
        while (permanentSet1 != permanentSet2) {
            if (permanentSet1.m_branchingPoint > permanentSet2.m_branchingPoint) {
                this.m_mergeArray.add(permanentSet1.m_branchingPoint);
                permanentSet1 = permanentSet1.m_rest;
                continue;
            }
            if (permanentSet1.m_branchingPoint < permanentSet2.m_branchingPoint) {
                this.m_mergeArray.add(permanentSet2.m_branchingPoint);
                permanentSet2 = permanentSet2.m_rest;
                continue;
            }
            this.m_mergeArray.add(permanentSet1.m_branchingPoint);
            permanentSet1 = permanentSet1.m_rest;
            permanentSet2 = permanentSet2.m_rest;
        }
        PermanentDependencySet result = permanentSet1;
        for (int index = this.m_mergeArray.size() - 1; index >= 0; --index) {
            result = this.getDepdendencySet(result, this.m_mergeArray.get(index));
        }
        return result;
    }

    public PermanentDependencySet getPermanent(DependencySet dependencySet) {
        if (dependencySet instanceof PermanentDependencySet) {
            return (PermanentDependencySet)dependencySet;
        }
        this.m_unprocessedSets.clear();
        this.m_mergeSets.clear();
        this.m_unprocessedSets.add((UnionDependencySet)dependencySet);
        while (!this.m_unprocessedSets.isEmpty()) {
            UnionDependencySet unionDependencySet = this.m_unprocessedSets.remove(this.m_unprocessedSets.size() - 1);
            int numberOfConstituents = 0;
            if (unionDependencySet != null) numberOfConstituents = unionDependencySet.m_numberOfConstituents;
            for (int index = 0; index < numberOfConstituents; ++index) {
                DependencySet constituent = unionDependencySet.m_dependencySets[index];
                if (constituent instanceof UnionDependencySet) {
                    this.m_unprocessedSets.add((UnionDependencySet)constituent);
                    continue;
                }
                this.m_mergeSets.add((PermanentDependencySet)constituent);
            }
        }
        int numberOfSets = this.m_mergeSets.size();
        this.m_mergeArray.clear();
        if (numberOfSets > 0) {
            block2:
            do {
                PermanentDependencySet permanentDependencySet;
                int index;
                PermanentDependencySet firstSet = this.m_mergeSets.get(0);
                int maximal = firstSet.m_branchingPoint;
                int maximalIndex = 0;
                boolean hasEquals = false;
                boolean allAreEqual = true;
                for (index = 1; index < numberOfSets; ++index) {
                    permanentDependencySet = this.m_mergeSets.get(index);
                    int branchingPoint = permanentDependencySet.m_branchingPoint;
                    if (branchingPoint > maximal) {
                        maximal = branchingPoint;
                        hasEquals = false;
                        maximalIndex = index;
                    } else if (branchingPoint == maximal) {
                        hasEquals = true;
                    }
                    if (permanentDependencySet == firstSet) continue;
                    allAreEqual = false;
                }
                if (allAreEqual) break;
                this.m_mergeArray.add(maximal);
                if (hasEquals) {
                    index = 0;
                    do {
                        if (index >= numberOfSets) continue block2;
                        permanentDependencySet = this.m_mergeSets.get(index);
                        if (permanentDependencySet.m_branchingPoint == maximal) {
                            this.m_mergeSets.set(index, permanentDependencySet.m_rest);
                        }
                        ++index;
                    } while (true);
                }
                PermanentDependencySet permanentDependencySet2 = this.m_mergeSets.get(maximalIndex);
                this.m_mergeSets.set(maximalIndex, permanentDependencySet2.m_rest);
            } while (true);
            PermanentDependencySet result = this.m_mergeSets.get(0);
            for (int index = this.m_mergeArray.size() - 1; index >= 0; --index) {
                result = this.getDepdendencySet(result, this.m_mergeArray.get(index));
            }
            this.m_mergeSets.clear();
            return result;
        }
        return
    }

    protected static final class IntegerArray
    implements Serializable {
        private static final long serialVersionUID = 7070190530381846058L;
        private int[] m_elements = new int[64];
        private int m_size = 0;

        public void clear() {
            this.m_size = 0;
        }

        public int size() {
            return this.m_size;
        }

        public int get(int index) {
            return this.m_elements[index];
        }

        public void add(int element) {
            if (this.m_size >= this.m_elements.length) {
                int[] newElements = new int[this.m_elements.length * 3 / 2];
                System.arraycopy(this.m_elements, 0, newElements, 0, this.m_elements.length);
                this.m_elements = newElements;
            }
            this.m_elements[this.m_size++] = element;
        }
    }
    
    DependencySet getActualDependencySet() {
		return lastEntryAddedIndex == -1 || m_entries[lastEntryAddedIndex] == null ? emptySet() : m_entries[lastEntryAddedIndex];
	}

}

