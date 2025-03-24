package org.semanticweb.HermiT.tableau;

import org.semanticweb.HermiT.model.AnnotatedEquality;
import org.semanticweb.HermiT.model.AtomicConcept;
import org.semanticweb.HermiT.model.DLPredicate;
import org.semanticweb.HermiT.model.Equality;
import org.semanticweb.HermiT.model.Inequality;
import org.semanticweb.owlapi.reasoner.InconsistentOntologyException;

public final class DisjunctionBranchingPoint
extends BranchingPoint {
    private static final long serialVersionUID = -8855083430836162354L;
    private final GroundDisjunction m_groundDisjunction;
    private final int[] m_sortedDisjunctIndexes;
    private int m_currentIndex;

    public DisjunctionBranchingPoint(Tableau tableau, GroundDisjunction groundDisjunction, int[] sortedDisjunctIndexes) {
        super(tableau);
        this.m_groundDisjunction = groundDisjunction;
        this.m_sortedDisjunctIndexes = sortedDisjunctIndexes;
    }
    
    @Override
    public boolean canStartNextChoice() {
        return this.m_sortedDisjunctIndexes.length > this.m_currentIndex + 1;
    }

    @Override
    public void startNextChoice(Tableau tableau, DependencySet clashDependencySet) {
        if (tableau.m_useDisjunctionLearning) {
            this.m_groundDisjunction.getGroundDisjunctionHeader().increaseNumberOfBacktrackings(this.m_sortedDisjunctIndexes[this.m_currentIndex]);
        }
        ++this.m_currentIndex;
        //assert (this.m_currentIndex < this.m_groundDisjunction.getNumberOfDisjuncts());
        if (this.m_currentIndex >= this.m_sortedDisjunctIndexes.length) throw new InconsistentOntologyException();
        int currentDisjunctIndex = this.m_sortedDisjunctIndexes[this.m_currentIndex];
        if (tableau.m_tableauMonitor != null) {
            tableau.m_tableauMonitor.disjunctProcessingStarted(this.m_groundDisjunction, currentDisjunctIndex);
        }
        PermanentDependencySet dependencySet = tableau.getDependencySetFactory().getPermanent(clashDependencySet);
        if (this.m_currentIndex + 1 == this.m_groundDisjunction.getNumberOfDisjuncts()) {
            dependencySet = tableau.getDependencySetFactory().removeBranchingPoint(dependencySet, this.m_level);
        }
        for (int previousIndex = 0; previousIndex < this.m_currentIndex; ++previousIndex) {
            int previousDisjunctIndex = this.m_sortedDisjunctIndexes[previousIndex];
            DLPredicate dlPredicate = this.m_groundDisjunction.getDLPredicate(previousDisjunctIndex);
            if (Equality.INSTANCE.equals(dlPredicate) || dlPredicate instanceof AnnotatedEquality) {
                tableau.m_extensionManager.addAssertion(Inequality.INSTANCE, this.m_groundDisjunction.getArgument(previousDisjunctIndex, 0), this.m_groundDisjunction.getArgument(previousDisjunctIndex, 1), dependencySet, false);
                continue;
            }
            if (!(dlPredicate instanceof AtomicConcept)) continue;
            tableau.m_extensionManager.addConceptAssertion(((AtomicConcept)dlPredicate).getNegation(), this.m_groundDisjunction.getArgument(previousDisjunctIndex, 0), dependencySet, false);
        }
        this.m_groundDisjunction.addDisjunctToTableau(tableau, currentDisjunctIndex, dependencySet);
        if (tableau.m_tableauMonitor != null) {
            tableau.m_tableauMonitor.disjunctProcessingFinished(this.m_groundDisjunction, currentDisjunctIndex);
        }
    }
}

