package org.semanticweb.HermiT.blocking;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;
import org.semanticweb.HermiT.model.AtomicConcept;
import org.semanticweb.HermiT.model.AtomicRole;
import org.semanticweb.HermiT.model.Concept;
import org.semanticweb.HermiT.model.DataRange;
import org.semanticweb.HermiT.tableau.ExtensionTable;
import org.semanticweb.HermiT.tableau.Node;
import org.semanticweb.HermiT.tableau.NodeType;
import org.semanticweb.HermiT.tableau.Tableau;

public class SingleDirectBlockingChecker
implements DirectBlockingChecker,
Serializable {
    private static final long serialVersionUID = 9093753046859877016L;
    protected final SetFactory<AtomicConcept> m_atomicConceptsSetFactory = new SetFactory();
    protected final Set<AtomicConcept> m_atomicConceptsBuffer = new LinkedHashSet<AtomicConcept>();
    protected ExtensionTable.Retrieval m_binaryTableSearch1Bound;

    @Override
    public void initialize(Tableau tableau) {
        this.m_binaryTableSearch1Bound = tableau.getExtensionManager().getBinaryExtensionTable().createRetrieval(new boolean[]{false, true}, ExtensionTable.View.TOTAL);
    }

    @Override
    public void clear() {
        this.m_atomicConceptsSetFactory.clearNonpermanent();
        this.m_binaryTableSearch1Bound.clear();
    }

    @Override
    public boolean isBlockedBy(Node blocker, Node blocked) {
        return !blocker.isBlocked() && blocker.getNodeType() == NodeType.TREE_NODE && blocked.getNodeType() == NodeType.TREE_NODE && ((SingleBlockingObject)blocker.getBlockingObject()).getAtomicConceptsLabel() == ((SingleBlockingObject)blocked.getBlockingObject()).getAtomicConceptsLabel();
    }

    @Override
    public int blockingHashCode(Node node) {
        return ((SingleBlockingObject)node.getBlockingObject()).m_atomicConceptsLabelHashCode;
    }

    @Override
    public boolean canBeBlocker(Node node) {
        return node.getNodeType() == NodeType.TREE_NODE;
    }

    @Override
    public boolean canBeBlocked(Node node) {
        return node.getNodeType() == NodeType.TREE_NODE;
    }

    @Override
    public boolean hasBlockingInfoChanged(Node node) {
        return ((SingleBlockingObject)node.getBlockingObject()).m_hasChanged;
    }

    @Override
    public void clearBlockingInfoChanged(Node node) {
        ((SingleBlockingObject)node.getBlockingObject()).m_hasChanged = false;
    }

    @Override
    public void nodeInitialized(Node node) {
        if (node.getBlockingObject() == null) {
            node.setBlockingObject(new SingleBlockingObject(node));
        }
        ((SingleBlockingObject)node.getBlockingObject()).initialize();
    }

    @Override
    public void nodeDestroyed(Node node) {
        ((SingleBlockingObject)node.getBlockingObject()).destroy();
    }

    @Override
    public Node assertionAdded(Concept concept, Node node, boolean isCore) {
        if (concept instanceof AtomicConcept) {
            ((SingleBlockingObject)node.getBlockingObject()).addAtomicConcept((AtomicConcept)concept);
            return node;
        }
        return null;
    }

    @Override
    public Node assertionRemoved(Concept concept, Node node, boolean isCore) {
        if (concept instanceof AtomicConcept) {
            ((SingleBlockingObject)node.getBlockingObject()).removeAtomicConcept((AtomicConcept)concept);
            return node;
        }
        return null;
    }

    @Override
    public Node assertionAdded(DataRange range, Node node, boolean isCore) {
        return null;
    }

    @Override
    public Node assertionRemoved(DataRange range, Node node, boolean isCore) {
        return null;
    }

    @Override
    public Node assertionAdded(AtomicRole atomicRole, Node nodeFrom, Node nodeTo, boolean isCore) {
        return null;
    }

    @Override
    public Node assertionRemoved(AtomicRole atomicRole, Node nodeFrom, Node nodeTo, boolean isCore) {
        return null;
    }

    @Override
    public Node nodesMerged(Node mergeFrom, Node mergeInto) {
        return null;
    }

    @Override
    public Node nodesUnmerged(Node mergeFrom, Node mergeInto) {
        return null;
    }

    @Override
    public BlockingSignature getBlockingSignatureFor(Node node) {
        return new SingleBlockingSignature(this, node);
    }

    protected Set<AtomicConcept> fetchAtomicConceptsLabel(Node node) {
        this.m_atomicConceptsBuffer.clear();
        this.m_binaryTableSearch1Bound.getBindingsBuffer()[1] = node;
        this.m_binaryTableSearch1Bound.open();
        Object[] tupleBuffer = this.m_binaryTableSearch1Bound.getTupleBuffer();
        while (!this.m_binaryTableSearch1Bound.afterLast()) {
            Object concept = tupleBuffer[0];
            if (concept instanceof AtomicConcept) {
                this.m_atomicConceptsBuffer.add((AtomicConcept)concept);
            }
            this.m_binaryTableSearch1Bound.next();
        }
        Set<AtomicConcept> result = this.m_atomicConceptsSetFactory.getSet(this.m_atomicConceptsBuffer);
        this.m_atomicConceptsBuffer.clear();
        return result;
    }

    @Override
    public boolean hasChangedSinceValidation(Node node) {
        return false;
    }

    @Override
    public void setHasChangedSinceValidation(Node node, boolean hasChanged) {
    }

    protected static class SingleBlockingSignature
    extends BlockingSignature
    implements Serializable {
        private static final long serialVersionUID = -7349489846772132258L;
        protected final Set<AtomicConcept> m_atomicConceptsLabel;

        public SingleBlockingSignature(SingleDirectBlockingChecker checker, Node node) {
            this.m_atomicConceptsLabel = ((SingleBlockingObject)node.getBlockingObject()).getAtomicConceptsLabel();
            checker.m_atomicConceptsSetFactory.makePermanent(this.m_atomicConceptsLabel);
        }

        @Override
        public boolean blocksNode(Node node) {
            return ((SingleBlockingObject)node.getBlockingObject()).getAtomicConceptsLabel() == this.m_atomicConceptsLabel;
        }

        public int hashCode() {
            return this.m_atomicConceptsLabel.hashCode();
        }

        public boolean equals(Object that) {
            if (this == that) {
                return true;
            }
            if (!(that instanceof SingleBlockingSignature)) {
                return false;
            }
            return this.m_atomicConceptsLabel == ((SingleBlockingSignature)that).m_atomicConceptsLabel;
        }
    }

    protected final class SingleBlockingObject
    implements Serializable {
        private static final long serialVersionUID = -5439737072100509531L;
        private final Node m_node;
        private boolean m_hasChanged;
        private Set<AtomicConcept> m_atomicConceptsLabel;
        private int m_atomicConceptsLabelHashCode;

        public SingleBlockingObject(Node node) {
            this.m_node = node;
        }

        public void initialize() {
            this.m_atomicConceptsLabel = null;
            this.m_atomicConceptsLabelHashCode = 0;
            this.m_hasChanged = true;
        }

        public void destroy() {
            if (this.m_atomicConceptsLabel != null) {
                SingleDirectBlockingChecker.this.m_atomicConceptsSetFactory.removeReference(this.m_atomicConceptsLabel);
                this.m_atomicConceptsLabel = null;
            }
        }

        public Set<AtomicConcept> getAtomicConceptsLabel() {
            if (this.m_atomicConceptsLabel == null) {
                this.m_atomicConceptsLabel = SingleDirectBlockingChecker.this.fetchAtomicConceptsLabel(this.m_node);
                SingleDirectBlockingChecker.this.m_atomicConceptsSetFactory.addReference(this.m_atomicConceptsLabel);
            }
            return this.m_atomicConceptsLabel;
        }

        public void addAtomicConcept(AtomicConcept atomicConcept) {
            if (this.m_atomicConceptsLabel != null) {
                SingleDirectBlockingChecker.this.m_atomicConceptsSetFactory.removeReference(this.m_atomicConceptsLabel);
                this.m_atomicConceptsLabel = null;
            }
            this.m_atomicConceptsLabelHashCode += atomicConcept.hashCode();
            this.m_hasChanged = true;
        }

        public void removeAtomicConcept(AtomicConcept atomicConcept) {
            if (this.m_atomicConceptsLabel != null) {
                SingleDirectBlockingChecker.this.m_atomicConceptsSetFactory.removeReference(this.m_atomicConceptsLabel);
                this.m_atomicConceptsLabel = null;
            }
            this.m_atomicConceptsLabelHashCode -= atomicConcept.hashCode();
            this.m_hasChanged = true;
        }
    }

}

