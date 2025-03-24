package org.semanticweb.HermiT.tableau;

import java.io.Serializable;
import java.util.Arrays;

final public class UnionDependencySet
implements DependencySet,
Serializable {
    private static final long serialVersionUID = 8296150535316233960L;
    int m_numberOfConstituents;
    DependencySet[] m_dependencySets;

    public UnionDependencySet(int numberOfConstituents) {
        this.m_dependencySets = new DependencySet[numberOfConstituents];
        this.m_numberOfConstituents = numberOfConstituents;
    }

    @Override
    public boolean containsBranchingPoint(int branchingPoint) {
        for (int index = this.m_numberOfConstituents - 1; index >= 0; --index) {
            if (!this.m_dependencySets[index].containsBranchingPoint(branchingPoint)) continue;
            return true;
        }
        return false;
    }

    @Override
    public int getMaximumBranchingPoint() {
        int maximumSoFar = this.m_dependencySets[0].getMaximumBranchingPoint();
        for (int index = this.m_numberOfConstituents - 1; index >= 1; --index) {
            maximumSoFar = Math.max(maximumSoFar, this.m_dependencySets[index].getMaximumBranchingPoint());
        }
        return maximumSoFar;
    }

    @Override
    public boolean isEmpty() {
        for (int index = this.m_numberOfConstituents - 1; index >= 0; --index) {
            if (this.m_dependencySets[index].isEmpty()) continue;
            return false;
        }
        return true;
    }

    public void clearConstituents() {
        Arrays.fill(this.m_dependencySets, null);
        this.m_numberOfConstituents = 0;
    }

    public void addConstituent(DependencySet constituent) {
        if (this.m_numberOfConstituents == this.m_dependencySets.length) {
            DependencySet[] newDependencySets = new DependencySet[this.m_numberOfConstituents * 3 / 2];
            System.arraycopy(this.m_dependencySets, 0, newDependencySets, 0, this.m_dependencySets.length);
            this.m_dependencySets = newDependencySets;
        }
        this.m_dependencySets[this.m_numberOfConstituents++] = constituent;
    }
}

