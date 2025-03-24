package org.semanticweb.HermiT.tableau;

public interface DependencySet {
    boolean containsBranchingPoint(int var1);

    boolean isEmpty();

    int getMaximumBranchingPoint();
}

