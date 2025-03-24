package org.semanticweb.HermiT.existentials;

import java.util.List;
import org.semanticweb.HermiT.model.AtomicRole;
import org.semanticweb.HermiT.model.Concept;
import org.semanticweb.HermiT.model.DLClause;
import org.semanticweb.HermiT.model.DLOntology;
import org.semanticweb.HermiT.model.DataRange;
import org.semanticweb.HermiT.model.Variable;
import org.semanticweb.HermiT.tableau.DLClauseEvaluator;
import org.semanticweb.HermiT.tableau.Node;
import org.semanticweb.HermiT.tableau.Tableau;

public interface ExistentialExpansionStrategy {
    void initialize(Tableau var1);

    void additionalDLOntologySet(DLOntology var1);

    void additionalDLOntologyCleared();

    void clear();

    boolean expandExistentials(boolean var1);

    void assertionAdded(Concept var1, Node var2, boolean var3);

    void assertionAdded(DataRange var1, Node var2, boolean var3);

    void assertionCoreSet(Concept var1, Node var2);

    void assertionCoreSet(DataRange var1, Node var2);

    void assertionRemoved(Concept var1, Node var2, boolean var3);

    void assertionRemoved(DataRange var1, Node var2, boolean var3);

    void assertionAdded(AtomicRole var1, Node var2, Node var3, boolean var4);

    void assertionCoreSet(AtomicRole var1, Node var2, Node var3);

    void assertionRemoved(AtomicRole var1, Node var2, Node var3, boolean var4);

    void nodesMerged(Node var1, Node var2);

    void nodesUnmerged(Node var1, Node var2);

    void nodeStatusChanged(Node var1);

    void nodeInitialized(Node var1);

    void nodeDestroyed(Node var1);

    void branchingPointPushed();

    void backtrack();

    void modelFound();

    boolean isDeterministic();

    boolean isExact();

    void dlClauseBodyCompiled(List<DLClauseEvaluator.Worker> var1, DLClause var2, List<Variable> var3, Object[] var4, boolean[] var5);
}

