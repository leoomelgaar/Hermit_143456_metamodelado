package org.semanticweb.HermiT.blocking;

import org.semanticweb.HermiT.model.AtomicRole;
import org.semanticweb.HermiT.model.Concept;
import org.semanticweb.HermiT.model.DataRange;
import org.semanticweb.HermiT.tableau.Node;
import org.semanticweb.HermiT.tableau.Tableau;

public interface DirectBlockingChecker {
    void initialize(Tableau var1);

    void clear();

    boolean isBlockedBy(Node var1, Node var2);

    int blockingHashCode(Node var1);

    boolean canBeBlocker(Node var1);

    boolean canBeBlocked(Node var1);

    boolean hasBlockingInfoChanged(Node var1);

    void clearBlockingInfoChanged(Node var1);

    boolean hasChangedSinceValidation(Node var1);

    void setHasChangedSinceValidation(Node var1, boolean var2);

    void nodeInitialized(Node var1);

    void nodeDestroyed(Node var1);

    Node assertionAdded(Concept var1, Node var2, boolean var3);

    Node assertionRemoved(Concept var1, Node var2, boolean var3);

    Node assertionAdded(DataRange var1, Node var2, boolean var3);

    Node assertionRemoved(DataRange var1, Node var2, boolean var3);

    Node assertionAdded(AtomicRole var1, Node var2, Node var3, boolean var4);

    Node assertionRemoved(AtomicRole var1, Node var2, Node var3, boolean var4);

    Node nodesMerged(Node var1, Node var2);

    Node nodesUnmerged(Node var1, Node var2);

    BlockingSignature getBlockingSignatureFor(Node var1);
}

