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

public class PairWiseDirectBlockingChecker
implements DirectBlockingChecker,
Serializable {
    private static final long serialVersionUID = -8296420442452625109L;
    protected final SetFactory<AtomicConcept> m_atomicConceptsSetFactory = new SetFactory();
    protected final SetFactory<AtomicRole> m_atomicRolesSetFactory = new SetFactory();
    protected final Set<AtomicConcept> m_atomicConceptsBuffer = new LinkedHashSet<AtomicConcept>();
    protected final Set<AtomicRole> m_atomicRolesBuffer = new LinkedHashSet<AtomicRole>();
    protected ExtensionTable.Retrieval m_binaryTableSearch1Bound;
    protected ExtensionTable.Retrieval m_ternaryTableSearch12Bound;

    @Override
    public void initialize(Tableau tableau) {
        this.m_binaryTableSearch1Bound = tableau.getExtensionManager().getBinaryExtensionTable().createRetrieval(new boolean[]{false, true}, ExtensionTable.View.TOTAL);
        this.m_ternaryTableSearch12Bound = tableau.getExtensionManager().getTernaryExtensionTable().createRetrieval(new boolean[]{false, true, true}, ExtensionTable.View.TOTAL);
    }

    @Override
    public void clear() {
        this.m_atomicConceptsSetFactory.clearNonpermanent();
        this.m_atomicRolesSetFactory.clearNonpermanent();
        this.m_binaryTableSearch1Bound.clear();
        this.m_ternaryTableSearch12Bound.clear();
    }

    @Override
    public boolean isBlockedBy(Node blocker, Node blocked) {
        PairWiseBlockingObject blockerObject = (PairWiseBlockingObject)blocker.getBlockingObject();
        PairWiseBlockingObject blockedObject = (PairWiseBlockingObject)blocked.getBlockingObject();
        return !blocker.isBlocked() && blocker.getNodeType() == NodeType.TREE_NODE && blocked.getNodeType() == NodeType.TREE_NODE && blockerObject.getAtomicConceptsLabel() == blockedObject.getAtomicConceptsLabel() && ((PairWiseBlockingObject)blocker.getParent().getBlockingObject()).getAtomicConceptsLabel() == ((PairWiseBlockingObject)blocked.getParent().getBlockingObject()).getAtomicConceptsLabel() && blockerObject.getFromParentLabel() == blockedObject.getFromParentLabel() && blockerObject.getToParentLabel() == blockedObject.getToParentLabel();
    }

    @Override
    public int blockingHashCode(Node node) {
        PairWiseBlockingObject nodeObject = (PairWiseBlockingObject)node.getBlockingObject();
        return nodeObject.m_atomicConceptsLabelHashCode + ((PairWiseBlockingObject)node.getParent().getBlockingObject()).m_atomicConceptsLabelHashCode + nodeObject.m_fromParentLabelHashCode + nodeObject.m_toParentLabelHashCode;
    }

    @Override
    public boolean canBeBlocker(Node node) {
        Node parent = node.getParent();
        return node.getNodeType() == NodeType.TREE_NODE && (parent.getNodeType() == NodeType.TREE_NODE || parent.getNodeType() == NodeType.GRAPH_NODE);
    }

    @Override
    public boolean canBeBlocked(Node node) {
        Node parent = node.getParent();
        return node.getNodeType() == NodeType.TREE_NODE && (parent.getNodeType() == NodeType.TREE_NODE || parent.getNodeType() == NodeType.GRAPH_NODE);
    }

    @Override
    public boolean hasBlockingInfoChanged(Node node) {
        return ((PairWiseBlockingObject)node.getBlockingObject()).m_hasChanged;
    }

    @Override
    public void clearBlockingInfoChanged(Node node) {
        ((PairWiseBlockingObject)node.getBlockingObject()).m_hasChanged = false;
    }

    @Override
    public void nodeInitialized(Node node) {
        if (node.getBlockingObject() == null) {
            node.setBlockingObject(new PairWiseBlockingObject(node));
        }
        ((PairWiseBlockingObject)node.getBlockingObject()).initialize();
    }

    @Override
    public void nodeDestroyed(Node node) {
        ((PairWiseBlockingObject)node.getBlockingObject()).destroy();
    }

    @Override
    public Node assertionAdded(Concept concept, Node node, boolean isCore) {
        if (concept instanceof AtomicConcept) {
            ((PairWiseBlockingObject)node.getBlockingObject()).addAtomicConcept((AtomicConcept)concept);
            return node;
        }
        return null;
    }

    @Override
    public Node assertionRemoved(Concept concept, Node node, boolean isCore) {
        if (concept instanceof AtomicConcept) {
            ((PairWiseBlockingObject)node.getBlockingObject()).removeAtomicConcept((AtomicConcept)concept);
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
        if (nodeFrom.isParentOf(nodeTo)) {
            ((PairWiseBlockingObject)nodeTo.getBlockingObject()).addToFromParentLabel(atomicRole);
            return nodeTo;
        }
        if (nodeTo.isParentOf(nodeFrom)) {
            ((PairWiseBlockingObject)nodeFrom.getBlockingObject()).addToToParentLabel(atomicRole);
            return nodeFrom;
        }
        return null;
    }

    @Override
    public Node assertionRemoved(AtomicRole atomicRole, Node nodeFrom, Node nodeTo, boolean isCore) {
        if (nodeFrom.isParentOf(nodeTo)) {
            ((PairWiseBlockingObject)nodeTo.getBlockingObject()).removeFromFromParentLabel(atomicRole);
            return nodeTo;
        }
        if (nodeTo.isParentOf(nodeFrom)) {
            ((PairWiseBlockingObject)nodeFrom.getBlockingObject()).removeFromToParentLabel(atomicRole);
            return nodeFrom;
        }
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
        return new PairWiseBlockingSignature(this, node);
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

    public Set<AtomicRole> fetchEdgeLabel(Node nodeFrom, Node nodeTo) {
        this.m_atomicRolesBuffer.clear();
        this.m_ternaryTableSearch12Bound.getBindingsBuffer()[1] = nodeFrom;
        this.m_ternaryTableSearch12Bound.getBindingsBuffer()[2] = nodeTo;
        this.m_ternaryTableSearch12Bound.open();
        Object[] tupleBuffer = this.m_ternaryTableSearch12Bound.getTupleBuffer();
        while (!this.m_ternaryTableSearch12Bound.afterLast()) {
            Object atomicRole = tupleBuffer[0];
            if (atomicRole instanceof AtomicRole) {
                this.m_atomicRolesBuffer.add((AtomicRole)atomicRole);
            }
            this.m_ternaryTableSearch12Bound.next();
        }
        Set<AtomicRole> result = this.m_atomicRolesSetFactory.getSet(this.m_atomicRolesBuffer);
        this.m_atomicRolesBuffer.clear();
        return result;
    }

    @Override
    public boolean hasChangedSinceValidation(Node node) {
        return false;
    }

    @Override
    public void setHasChangedSinceValidation(Node node, boolean hasChanged) {
    }

    protected static class PairWiseBlockingSignature
    extends BlockingSignature
    implements Serializable {
        private static final long serialVersionUID = 4697990424058632618L;
        protected final Set<AtomicConcept> m_atomicConceptLabel;
        protected final Set<AtomicConcept> m_parentAtomicConceptLabel;
        protected final Set<AtomicRole> m_fromParentLabel;
        protected final Set<AtomicRole> m_toParentLabel;
        protected final int m_hashCode;

        public PairWiseBlockingSignature(PairWiseDirectBlockingChecker checker, Node node) {
            PairWiseBlockingObject nodeBlockingObject = (PairWiseBlockingObject)node.getBlockingObject();
            this.m_atomicConceptLabel = nodeBlockingObject.getAtomicConceptsLabel();
            this.m_parentAtomicConceptLabel = ((PairWiseBlockingObject)node.getParent().getBlockingObject()).getAtomicConceptsLabel();
            this.m_fromParentLabel = nodeBlockingObject.getFromParentLabel();
            this.m_toParentLabel = nodeBlockingObject.getToParentLabel();
            this.m_hashCode = this.m_atomicConceptLabel.hashCode() + this.m_parentAtomicConceptLabel.hashCode() + this.m_fromParentLabel.hashCode() + this.m_toParentLabel.hashCode();
            checker.m_atomicConceptsSetFactory.makePermanent(this.m_atomicConceptLabel);
            checker.m_atomicConceptsSetFactory.makePermanent(this.m_parentAtomicConceptLabel);
            checker.m_atomicRolesSetFactory.makePermanent(this.m_fromParentLabel);
            checker.m_atomicRolesSetFactory.makePermanent(this.m_toParentLabel);
        }

        @Override
        public boolean blocksNode(Node node) {
            PairWiseBlockingObject nodeBlockingObject = (PairWiseBlockingObject)node.getBlockingObject();
            return nodeBlockingObject.getAtomicConceptsLabel() == this.m_atomicConceptLabel && ((PairWiseBlockingObject)node.getParent().getBlockingObject()).getAtomicConceptsLabel() == this.m_parentAtomicConceptLabel && nodeBlockingObject.getFromParentLabel() == this.m_fromParentLabel && nodeBlockingObject.getToParentLabel() == this.m_toParentLabel;
        }

        public int hashCode() {
            return this.m_hashCode;
        }

        public boolean equals(Object that) {
            if (this == that) {
                return true;
            }
            if (!(that instanceof PairWiseBlockingSignature)) {
                return false;
            }
            PairWiseBlockingSignature thatSignature = (PairWiseBlockingSignature)that;
            return this.m_atomicConceptLabel == thatSignature.m_atomicConceptLabel && this.m_parentAtomicConceptLabel == thatSignature.m_parentAtomicConceptLabel && this.m_fromParentLabel == thatSignature.m_fromParentLabel && this.m_toParentLabel == thatSignature.m_toParentLabel;
        }
    }

    protected final class PairWiseBlockingObject
    implements Serializable {
        private static final long serialVersionUID = -5439737072100509531L;
        private final Node m_node;
        private boolean m_hasChanged;
        private Set<AtomicConcept> m_atomicConceptsLabel;
        private int m_atomicConceptsLabelHashCode;
        private Set<AtomicRole> m_fromParentLabel;
        private int m_fromParentLabelHashCode;
        private Set<AtomicRole> m_toParentLabel;
        private int m_toParentLabelHashCode;

        public PairWiseBlockingObject(Node node) {
            this.m_node = node;
        }

        public void initialize() {
            this.m_atomicConceptsLabel = null;
            this.m_atomicConceptsLabelHashCode = 0;
            this.m_fromParentLabel = null;
            this.m_fromParentLabelHashCode = 0;
            this.m_toParentLabel = null;
            this.m_toParentLabelHashCode = 0;
            this.m_hasChanged = true;
        }

        public void destroy() {
            if (this.m_atomicConceptsLabel != null) {
                PairWiseDirectBlockingChecker.this.m_atomicConceptsSetFactory.removeReference(this.m_atomicConceptsLabel);
                this.m_atomicConceptsLabel = null;
            }
            if (this.m_fromParentLabel != null) {
                PairWiseDirectBlockingChecker.this.m_atomicRolesSetFactory.removeReference(this.m_fromParentLabel);
                this.m_fromParentLabel = null;
            }
            if (this.m_toParentLabel != null) {
                PairWiseDirectBlockingChecker.this.m_atomicRolesSetFactory.removeReference(this.m_toParentLabel);
                this.m_toParentLabel = null;
            }
        }

        public Set<AtomicConcept> getAtomicConceptsLabel() {
            if (this.m_atomicConceptsLabel == null) {
                this.m_atomicConceptsLabel = PairWiseDirectBlockingChecker.this.fetchAtomicConceptsLabel(this.m_node);
                PairWiseDirectBlockingChecker.this.m_atomicConceptsSetFactory.addReference(this.m_atomicConceptsLabel);
            }
            return this.m_atomicConceptsLabel;
        }

        public void addAtomicConcept(AtomicConcept atomicConcept) {
            if (this.m_atomicConceptsLabel != null) {
                PairWiseDirectBlockingChecker.this.m_atomicConceptsSetFactory.removeReference(this.m_atomicConceptsLabel);
                this.m_atomicConceptsLabel = null;
            }
            this.m_atomicConceptsLabelHashCode += atomicConcept.hashCode();
            this.m_hasChanged = true;
        }

        public void removeAtomicConcept(AtomicConcept atomicConcept) {
            if (this.m_atomicConceptsLabel != null) {
                PairWiseDirectBlockingChecker.this.m_atomicConceptsSetFactory.removeReference(this.m_atomicConceptsLabel);
                this.m_atomicConceptsLabel = null;
            }
            this.m_atomicConceptsLabelHashCode -= atomicConcept.hashCode();
            this.m_hasChanged = true;
        }

        public Set<AtomicRole> getFromParentLabel() {
            if (this.m_fromParentLabel == null) {
                this.m_fromParentLabel = PairWiseDirectBlockingChecker.this.fetchEdgeLabel(this.m_node.getParent(), this.m_node);
                PairWiseDirectBlockingChecker.this.m_atomicRolesSetFactory.addReference(this.m_fromParentLabel);
            }
            return this.m_fromParentLabel;
        }

        private void addToFromParentLabel(AtomicRole atomicRole) {
            if (this.m_fromParentLabel != null) {
                PairWiseDirectBlockingChecker.this.m_atomicRolesSetFactory.removeReference(this.m_fromParentLabel);
                this.m_fromParentLabel = null;
            }
            this.m_fromParentLabelHashCode += atomicRole.hashCode();
            this.m_hasChanged = true;
        }

        private void removeFromFromParentLabel(AtomicRole atomicRole) {
            if (this.m_fromParentLabel != null) {
                PairWiseDirectBlockingChecker.this.m_atomicRolesSetFactory.removeReference(this.m_fromParentLabel);
                this.m_fromParentLabel = null;
            }
            this.m_fromParentLabelHashCode -= atomicRole.hashCode();
            this.m_hasChanged = true;
        }

        public Set<AtomicRole> getToParentLabel() {
            if (this.m_toParentLabel == null) {
                this.m_toParentLabel = PairWiseDirectBlockingChecker.this.fetchEdgeLabel(this.m_node, this.m_node.getParent());
                PairWiseDirectBlockingChecker.this.m_atomicRolesSetFactory.addReference(this.m_toParentLabel);
            }
            return this.m_toParentLabel;
        }

        private void addToToParentLabel(AtomicRole atomicRole) {
            if (this.m_toParentLabel != null) {
                PairWiseDirectBlockingChecker.this.m_atomicRolesSetFactory.removeReference(this.m_toParentLabel);
                this.m_toParentLabel = null;
            }
            this.m_toParentLabelHashCode += atomicRole.hashCode();
            this.m_hasChanged = true;
        }

        private void removeFromToParentLabel(AtomicRole atomicRole) {
            if (this.m_toParentLabel != null) {
                PairWiseDirectBlockingChecker.this.m_atomicRolesSetFactory.removeReference(this.m_toParentLabel);
                this.m_toParentLabel = null;
            }
            this.m_toParentLabelHashCode -= atomicRole.hashCode();
            this.m_hasChanged = true;
        }
    }

}

