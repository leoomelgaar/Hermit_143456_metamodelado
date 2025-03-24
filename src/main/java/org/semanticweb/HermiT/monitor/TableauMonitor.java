package org.semanticweb.HermiT.monitor;

import org.semanticweb.HermiT.model.AnnotatedEquality;
import org.semanticweb.HermiT.model.DataRange;
import org.semanticweb.HermiT.model.ExistentialConcept;
import org.semanticweb.HermiT.tableau.BranchingPoint;
import org.semanticweb.HermiT.tableau.DLClauseEvaluator;
import org.semanticweb.HermiT.tableau.DatatypeManager;
import org.semanticweb.HermiT.tableau.GroundDisjunction;
import org.semanticweb.HermiT.tableau.Node;
import org.semanticweb.HermiT.tableau.ReasoningTaskDescription;
import org.semanticweb.HermiT.tableau.Tableau;

public interface TableauMonitor {
    void setTableau(Tableau var1);

    void isSatisfiableStarted(ReasoningTaskDescription var1);

    void isSatisfiableFinished(ReasoningTaskDescription var1, boolean var2);

    void tableauCleared();

    void saturateStarted();

    void saturateFinished(boolean var1);

    void iterationStarted();

    void iterationFinished();

    void dlClauseMatchedStarted(DLClauseEvaluator var1, int var2);

    void dlClauseMatchedFinished(DLClauseEvaluator var1, int var2);

    void addFactStarted(Object[] var1, boolean var2);

    void addFactFinished(Object[] var1, boolean var2, boolean var3);

    void mergeStarted(Node var1, Node var2);

    void nodePruned(Node var1);

    void mergeFactStarted(Node var1, Node var2, Object[] var3, Object[] var4);

    void mergeFactFinished(Node var1, Node var2, Object[] var3, Object[] var4);

    void mergeFinished(Node var1, Node var2);

    /* varargs */ void clashDetectionStarted(Object[]... var1);

    /* varargs */ void clashDetectionFinished(Object[]... var1);

    void clashDetected();

    void backtrackToStarted(BranchingPoint var1);

    void tupleRemoved(Object[] var1);

    void backtrackToFinished(BranchingPoint var1);

    void groundDisjunctionDerived(GroundDisjunction var1);

    void processGroundDisjunctionStarted(GroundDisjunction var1);

    void groundDisjunctionSatisfied(GroundDisjunction var1);

    void processGroundDisjunctionFinished(GroundDisjunction var1);

    void disjunctProcessingStarted(GroundDisjunction var1, int var2);

    void disjunctProcessingFinished(GroundDisjunction var1, int var2);

    void pushBranchingPointStarted(BranchingPoint var1);

    void pushBranchingPointFinished(BranchingPoint var1);

    void startNextBranchingPointStarted(BranchingPoint var1);

    void startNextBranchingPointFinished(BranchingPoint var1);

    void existentialExpansionStarted(ExistentialConcept var1, Node var2);

    void existentialExpansionFinished(ExistentialConcept var1, Node var2);

    void existentialSatisfied(ExistentialConcept var1, Node var2);

    void nominalIntorductionStarted(Node var1, Node var2, AnnotatedEquality var3, Node var4, Node var5);

    void nominalIntorductionFinished(Node var1, Node var2, AnnotatedEquality var3, Node var4, Node var5);

    void descriptionGraphCheckingStarted(int var1, int var2, int var3, int var4, int var5, int var6);

    void descriptionGraphCheckingFinished(int var1, int var2, int var3, int var4, int var5, int var6);

    void nodeCreated(Node var1);

    void nodeDestroyed(Node var1);

    void unknownDatatypeRestrictionDetectionStarted(DataRange var1, Node var2, DataRange var3, Node var4);

    void unknownDatatypeRestrictionDetectionFinished(DataRange var1, Node var2, DataRange var3, Node var4);

    void datatypeCheckingStarted();

    void datatypeCheckingFinished(boolean var1);

    void datatypeConjunctionCheckingStarted(DatatypeManager.DConjunction var1);

    void datatypeConjunctionCheckingFinished(DatatypeManager.DConjunction var1, boolean var2);

    void blockingValidationStarted();

    void blockingValidationFinished(int var1);

    void possibleInstanceIsInstance();

    void possibleInstanceIsNotInstance();
}

