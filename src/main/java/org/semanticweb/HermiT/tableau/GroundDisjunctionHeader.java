package org.semanticweb.HermiT.tableau;

import org.semanticweb.HermiT.Prefixes;
import org.semanticweb.HermiT.model.AtLeastConcept;
import org.semanticweb.HermiT.model.AtomicNegationConcept;
import org.semanticweb.HermiT.model.DLPredicate;

public final class GroundDisjunctionHeader {
    final DLPredicate[] m_dlPredicates;
    final int[] m_disjunctStart;
    final int m_hashCode;
    private final DisjunctIndexWithBacktrackings[] m_disjunctIndexesWithBacktrackings;
    private final int m_firstAtLeastPositiveIndex;
    private final int m_firstAtLeastNegativeIndex;
    GroundDisjunctionHeader m_nextEntry;

    GroundDisjunctionHeader(DLPredicate[] dlPredicates, int hashCode, GroundDisjunctionHeader nextEntry) {
        this.m_dlPredicates = dlPredicates;
        this.m_disjunctStart = new int[this.m_dlPredicates.length];
        int argumentsSize = 0;
        for (int disjunctIndex = 0; disjunctIndex < this.m_dlPredicates.length; ++disjunctIndex) {
            this.m_disjunctStart[disjunctIndex] = argumentsSize;
            argumentsSize += this.m_dlPredicates[disjunctIndex].getArity();
        }
        this.m_hashCode = hashCode;
        this.m_nextEntry = nextEntry;
        this.m_disjunctIndexesWithBacktrackings = new DisjunctIndexWithBacktrackings[dlPredicates.length];
        int numberOfAtLeastPositiveDisjuncts = 0;
        int numberOfAtLeastNegativeDisjuncts = 0;
        for (int index = 0; index < dlPredicates.length; ++index) {
            if (!(this.m_dlPredicates[index] instanceof AtLeastConcept)) continue;
            AtLeastConcept atLeast = (AtLeastConcept)this.m_dlPredicates[index];
            if (atLeast.getToConcept() instanceof AtomicNegationConcept) {
                ++numberOfAtLeastNegativeDisjuncts;
                continue;
            }
            ++numberOfAtLeastPositiveDisjuncts;
        }
        this.m_firstAtLeastNegativeIndex = this.m_disjunctIndexesWithBacktrackings.length - numberOfAtLeastPositiveDisjuncts - numberOfAtLeastNegativeDisjuncts;
        this.m_firstAtLeastPositiveIndex = this.m_disjunctIndexesWithBacktrackings.length - numberOfAtLeastPositiveDisjuncts;
        int nextAtomicDisjunct = 0;
        int nextAtLeastNegativeDisjunct = this.m_firstAtLeastNegativeIndex;
        int nextAtLeastPositiveDisjunct = this.m_firstAtLeastPositiveIndex;
        for (int index = 0; index < dlPredicates.length; ++index) {
            if (this.m_dlPredicates[index] instanceof AtLeastConcept) {
                AtLeastConcept atLeast = (AtLeastConcept)this.m_dlPredicates[index];
                if (atLeast.getToConcept() instanceof AtomicNegationConcept) {
                    this.m_disjunctIndexesWithBacktrackings[nextAtLeastNegativeDisjunct++] = new DisjunctIndexWithBacktrackings(index);
                    continue;
                }
                this.m_disjunctIndexesWithBacktrackings[nextAtLeastPositiveDisjunct++] = new DisjunctIndexWithBacktrackings(index);
                continue;
            }
            this.m_disjunctIndexesWithBacktrackings[nextAtomicDisjunct++] = new DisjunctIndexWithBacktrackings(index);
        }
    }

    boolean isEqual(DLPredicate[] dlPredicates) {
        if (this.m_dlPredicates.length != dlPredicates.length) {
            return false;
        }
        for (int index = this.m_dlPredicates.length - 1; index >= 0; --index) {
            if (this.m_dlPredicates[index].equals(dlPredicates[index])) continue;
            return false;
        }
        return true;
    }

    public int[] getSortedDisjunctIndexes() {
        int[] sortedDisjunctIndexes = new int[this.m_disjunctIndexesWithBacktrackings.length];
        for (int index = this.m_disjunctIndexesWithBacktrackings.length - 1; index >= 0; --index) {
            sortedDisjunctIndexes[index] = this.m_disjunctIndexesWithBacktrackings[index].m_disjunctIndex;
        }
        return sortedDisjunctIndexes;
    }

    public void increaseNumberOfBacktrackings(int disjunctIndex) {
        for (int index = 0; index < this.m_disjunctIndexesWithBacktrackings.length; ++index) {
            DisjunctIndexWithBacktrackings disjunctIndexWithBacktrackings = this.m_disjunctIndexesWithBacktrackings[index];
            if (disjunctIndexWithBacktrackings.m_disjunctIndex != disjunctIndex) continue;
            ++disjunctIndexWithBacktrackings.m_numberOfBacktrackings;
            int partitionEnd = index < this.m_firstAtLeastNegativeIndex ? this.m_firstAtLeastNegativeIndex : (index >= this.m_firstAtLeastNegativeIndex && index < this.m_firstAtLeastPositiveIndex ? this.m_firstAtLeastPositiveIndex : this.m_disjunctIndexesWithBacktrackings.length);
            int currentIndex = index;
            int nextIndex = currentIndex + 1;
            while (nextIndex < partitionEnd && disjunctIndexWithBacktrackings.m_numberOfBacktrackings > this.m_disjunctIndexesWithBacktrackings[nextIndex].m_numberOfBacktrackings) {
                this.m_disjunctIndexesWithBacktrackings[currentIndex] = this.m_disjunctIndexesWithBacktrackings[nextIndex];
                this.m_disjunctIndexesWithBacktrackings[nextIndex] = disjunctIndexWithBacktrackings;
                currentIndex = nextIndex++;
            }
            break;
        }
    }

    public String toString(Prefixes prefixes) {
        StringBuffer buffer = new StringBuffer();
        for (int disjunctIndex = 0; disjunctIndex < this.m_dlPredicates.length; ++disjunctIndex) {
            if (disjunctIndex > 0) {
                buffer.append(" \\/ ");
            }
            buffer.append(this.m_dlPredicates[disjunctIndex].toString(prefixes));
            buffer.append(" (");
            for (DisjunctIndexWithBacktrackings disjunctIndexWithBacktrackings : this.m_disjunctIndexesWithBacktrackings) {
                if (disjunctIndexWithBacktrackings.m_disjunctIndex != disjunctIndex) continue;
                buffer.append(disjunctIndexWithBacktrackings.m_numberOfBacktrackings);
                break;
            }
            buffer.append(")");
        }
        return buffer.toString();
    }

    public String toString() {
        return this.toString(Prefixes.STANDARD_PREFIXES);
    }

    protected static class DisjunctIndexWithBacktrackings {
        protected final int m_disjunctIndex;
        protected int m_numberOfBacktrackings;

        public DisjunctIndexWithBacktrackings(int index) {
            this.m_disjunctIndex = index;
        }
    }

}

