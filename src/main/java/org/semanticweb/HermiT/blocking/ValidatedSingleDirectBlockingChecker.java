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

public class ValidatedSingleDirectBlockingChecker
implements DirectBlockingChecker,
Serializable {
    private static final long serialVersionUID = 9093753046859877016L;
    protected final SetFactory<AtomicConcept> m_atomicConceptsSetFactory = new SetFactory();
    protected final SetFactory<AtomicRole> m_atomicRolesSetFactory = new SetFactory();
    protected final Set<AtomicConcept> m_atomicConceptsBuffer = new LinkedHashSet<AtomicConcept>();
    protected final Set<AtomicRole> m_atomicRolesBuffer = new LinkedHashSet<AtomicRole>();
    protected final boolean m_hasInverses;
    protected ExtensionTable.Retrieval m_binaryTableSearch1Bound;
    protected ExtensionTable.Retrieval m_ternaryTableSearch12Bound;

    public ValidatedSingleDirectBlockingChecker(boolean hasInverses) {
        this.m_hasInverses = hasInverses;
    }

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
        boolean isBlockedBy = !blocker.isBlocked() && blocker.getNodeType() == NodeType.TREE_NODE && blocked.getNodeType() == NodeType.TREE_NODE && ((ValidatedSingleBlockingObject)blocker.getBlockingObject()).getAtomicConceptsLabel() == ((ValidatedSingleBlockingObject)blocked.getBlockingObject()).getAtomicConceptsLabel();
        return isBlockedBy;
    }

    @Override
    public int blockingHashCode(Node node) {
        return ((ValidatedSingleBlockingObject)node.getBlockingObject()).m_blockingRelevantHashCode;
    }

    @Override
    public boolean canBeBlocker(Node node) {
        Node parent = node.getParent();
        return node.getNodeType() == NodeType.TREE_NODE && (!this.m_hasInverses || node.getParent().getNodeType() == NodeType.TREE_NODE || parent.getNodeType() == NodeType.GRAPH_NODE);
    }

    @Override
    public boolean canBeBlocked(Node node) {
        Node parent = node.getParent();
        return node.getNodeType() == NodeType.TREE_NODE && (!this.m_hasInverses || node.getParent().getNodeType() == NodeType.TREE_NODE || parent.getNodeType() == NodeType.GRAPH_NODE);
    }

    @Override
    public boolean hasBlockingInfoChanged(Node node) {
        return ((ValidatedSingleBlockingObject)node.getBlockingObject()).m_hasChangedForBlocking;
    }

    @Override
    public void clearBlockingInfoChanged(Node node) {
        ((ValidatedSingleBlockingObject)node.getBlockingObject()).m_hasChangedForBlocking = false;
    }

    @Override
    public boolean hasChangedSinceValidation(Node node) {
        return ((ValidatedSingleBlockingObject)node.getBlockingObject()).m_hasChangedForValidation;
    }

    @Override
    public void setHasChangedSinceValidation(Node node, boolean hasChanged) {
        ((ValidatedSingleBlockingObject)node.getBlockingObject()).m_hasChangedForValidation = hasChanged;
    }

    @Override
    public void nodeInitialized(Node node) {
        if (node.getBlockingObject() == null) {
            node.setBlockingObject(new ValidatedSingleBlockingObject(node));
        }
        ((ValidatedSingleBlockingObject)node.getBlockingObject()).initialize();
    }

    @Override
    public void nodeDestroyed(Node node) {
        ((ValidatedSingleBlockingObject)node.getBlockingObject()).destroy();
    }

    @Override
    public Node assertionAdded(Concept concept, Node node, boolean isCore) {
        ((ValidatedSingleBlockingObject)node.getBlockingObject()).addConcept(concept, isCore);
        return concept instanceof AtomicConcept && isCore ? node : null;
    }

    @Override
    public Node assertionRemoved(Concept concept, Node node, boolean isCore) {
        ((ValidatedSingleBlockingObject)node.getBlockingObject()).removeConcept(concept, isCore);
        return concept instanceof AtomicConcept && isCore ? node : null;
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

    protected Set<AtomicConcept> fetchAtomicConceptsLabel(Node node, boolean onlyCore) {
        this.m_atomicConceptsBuffer.clear();
        this.m_binaryTableSearch1Bound.getBindingsBuffer()[1] = node;
        this.m_binaryTableSearch1Bound.open();
        Object[] tupleBuffer = this.m_binaryTableSearch1Bound.getTupleBuffer();
        while (!this.m_binaryTableSearch1Bound.afterLast()) {
            Object concept = tupleBuffer[0];
            if (concept instanceof AtomicConcept && (!onlyCore || this.m_binaryTableSearch1Bound.isCore())) {
                this.m_atomicConceptsBuffer.add((AtomicConcept)concept);
            }
            this.m_binaryTableSearch1Bound.next();
        }
        Set<AtomicConcept> result = this.m_atomicConceptsSetFactory.getSet(this.m_atomicConceptsBuffer);
        this.m_atomicConceptsBuffer.clear();
        return result;
    }

    protected Set<AtomicRole> fetchAtomicRolesLabel(Node nodeFrom, Node nodeTo, boolean onlyCore) {
        this.m_atomicRolesBuffer.clear();
        this.m_ternaryTableSearch12Bound.getBindingsBuffer()[1] = nodeFrom;
        this.m_ternaryTableSearch12Bound.getBindingsBuffer()[2] = nodeTo;
        this.m_ternaryTableSearch12Bound.open();
        Object[] tupleBuffer = this.m_ternaryTableSearch12Bound.getTupleBuffer();
        while (!this.m_ternaryTableSearch12Bound.afterLast()) {
            Object atomicRole = tupleBuffer[0];
            if (atomicRole instanceof AtomicRole && (!onlyCore || this.m_binaryTableSearch1Bound.isCore())) {
                this.m_atomicRolesBuffer.add((AtomicRole)atomicRole);
            }
            this.m_ternaryTableSearch12Bound.next();
        }
        Set<AtomicRole> result = this.m_atomicRolesSetFactory.getSet(this.m_atomicRolesBuffer);
        this.m_atomicRolesBuffer.clear();
        return result;
    }

    @Override
    public BlockingSignature getBlockingSignatureFor(Node node) {
        return new ValidatedBlockingSignature(this, node);
    }

    public interface ValidatedBlockingObject {
        void initialize();

        void destroy();

        Set<AtomicConcept> getAtomicConceptsLabel();

        void addConcept(Concept var1, boolean var2);

        void removeConcept(Concept var1, boolean var2);

        Set<AtomicConcept> getFullAtomicConceptsLabel();

        Set<AtomicRole> getFullFromParentLabel();

        Set<AtomicRole> getFullToParentLabel();

        void setBlockViolatesParentConstraints(boolean var1);

        void setHasAlreadyBeenChecked(boolean var1);

        boolean hasAlreadyBeenChecked();

        boolean blockViolatesParentConstraints();
    }

    protected static class ValidatedBlockingSignature
    extends BlockingSignature {
        protected final Set<AtomicConcept> m_blockingRelevantConceptsLabel;
        protected final Set<AtomicConcept> m_fullAtomicConceptsLabel;
        protected final Set<AtomicConcept> m_parentFullAtomicConceptsLabel;
        protected final Set<AtomicRole> m_fromParentLabel;
        protected final Set<AtomicRole> m_toParentLabel;
        protected final int m_hashCode;

        public ValidatedBlockingSignature(ValidatedSingleDirectBlockingChecker checker, Node node) {
            ValidatedSingleBlockingObject nodeBlockingObject = (ValidatedSingleBlockingObject)node.getBlockingObject();
            this.m_blockingRelevantConceptsLabel = nodeBlockingObject.getAtomicConceptsLabel();
            this.m_fullAtomicConceptsLabel = nodeBlockingObject.getFullAtomicConceptsLabel();
            this.m_parentFullAtomicConceptsLabel = ((ValidatedSingleBlockingObject)node.getParent().getBlockingObject()).getFullAtomicConceptsLabel();
            this.m_fromParentLabel = nodeBlockingObject.getFullFromParentLabel();
            this.m_toParentLabel = nodeBlockingObject.getFullToParentLabel();
            this.m_hashCode = this.m_blockingRelevantConceptsLabel.hashCode();
            checker.m_atomicConceptsSetFactory.makePermanent(this.m_fullAtomicConceptsLabel);
            checker.m_atomicConceptsSetFactory.makePermanent(this.m_parentFullAtomicConceptsLabel);
            checker.m_atomicRolesSetFactory.makePermanent(this.m_fromParentLabel);
            checker.m_atomicRolesSetFactory.makePermanent(this.m_toParentLabel);
        }

        @Override
        public boolean blocksNode(Node node) {
            ValidatedSingleBlockingObject nodeBlockingObject = (ValidatedSingleBlockingObject)node.getBlockingObject();
            return nodeBlockingObject.getAtomicConceptsLabel() == this.m_blockingRelevantConceptsLabel;
        }

        public int hashCode() {
            return this.m_hashCode;
        }

        public boolean equals(Object that) {
            if (this == that) {
                return true;
            }
            if (!(that instanceof ValidatedBlockingSignature)) {
                return false;
            }
            ValidatedBlockingSignature thatSignature = (ValidatedBlockingSignature)that;
            return this.m_blockingRelevantConceptsLabel == thatSignature.m_blockingRelevantConceptsLabel && this.m_fullAtomicConceptsLabel == thatSignature.m_fullAtomicConceptsLabel && this.m_parentFullAtomicConceptsLabel == thatSignature.m_parentFullAtomicConceptsLabel && this.m_fromParentLabel == thatSignature.m_fromParentLabel && this.m_toParentLabel == thatSignature.m_toParentLabel;
        }
    }

    public class ValidatedSingleBlockingObject
    implements ValidatedBlockingObject {
        protected final Node m_node;
        protected boolean m_hasChangedForBlocking;
        protected boolean m_hasChangedForValidation;
        protected Set<AtomicConcept> m_blockingRelevantLabel;
        protected Set<AtomicConcept> m_fullAtomicConceptsLabel;
        protected Set<AtomicRole> m_fullFromParentLabel;
        protected Set<AtomicRole> m_fullToParentLabel;
        protected int m_blockingRelevantHashCode;
        public boolean m_blockViolatesParentConstraints = false;
        public boolean m_hasAlreadyBeenChecked = false;

        public ValidatedSingleBlockingObject(Node node) {
            this.m_node = node;
        }

        @Override
        public void initialize() {
            this.m_blockingRelevantLabel = null;
            this.m_blockingRelevantHashCode = 0;
            this.m_fullAtomicConceptsLabel = null;
            this.m_fullFromParentLabel = null;
            this.m_fullToParentLabel = null;
            this.m_hasChangedForBlocking = true;
            this.m_hasChangedForValidation = true;
        }

        @Override
        public void destroy() {
            if (this.m_blockingRelevantLabel != null) {
                ValidatedSingleDirectBlockingChecker.this.m_atomicConceptsSetFactory.removeReference(this.m_blockingRelevantLabel);
                this.m_blockingRelevantLabel = null;
            }
            if (this.m_fullAtomicConceptsLabel != null) {
                ValidatedSingleDirectBlockingChecker.this.m_atomicConceptsSetFactory.removeReference(this.m_fullAtomicConceptsLabel);
                this.m_fullAtomicConceptsLabel = null;
            }
            if (this.m_fullFromParentLabel != null) {
                ValidatedSingleDirectBlockingChecker.this.m_atomicRolesSetFactory.removeReference(this.m_fullFromParentLabel);
                this.m_fullFromParentLabel = null;
            }
            if (this.m_fullToParentLabel != null) {
                ValidatedSingleDirectBlockingChecker.this.m_atomicRolesSetFactory.removeReference(this.m_fullToParentLabel);
                this.m_fullToParentLabel = null;
            }
        }

        @Override
        public Set<AtomicConcept> getAtomicConceptsLabel() {
            if (this.m_blockingRelevantLabel == null) {
                this.m_blockingRelevantLabel = ValidatedSingleDirectBlockingChecker.this.fetchAtomicConceptsLabel(this.m_node, true);
                ValidatedSingleDirectBlockingChecker.this.m_atomicConceptsSetFactory.addReference(this.m_blockingRelevantLabel);
            }
            return this.m_blockingRelevantLabel;
        }

        @Override
        public void addConcept(Concept concept, boolean isCore) {
            this.m_hasChangedForValidation = true;
            if (concept instanceof AtomicConcept) {
                if (this.m_fullAtomicConceptsLabel != null) {
                    ValidatedSingleDirectBlockingChecker.this.m_atomicConceptsSetFactory.removeReference(this.m_fullAtomicConceptsLabel);
                    this.m_fullAtomicConceptsLabel = null;
                }
                if (isCore) {
                    if (this.m_blockingRelevantLabel != null) {
                        ValidatedSingleDirectBlockingChecker.this.m_atomicConceptsSetFactory.removeReference(this.m_blockingRelevantLabel);
                        this.m_blockingRelevantLabel = null;
                    }
                    this.m_blockingRelevantHashCode += concept.hashCode();
                    this.m_hasChangedForBlocking = true;
                }
            }
        }

        @Override
        public void removeConcept(Concept concept, boolean isCore) {
            this.m_hasChangedForValidation = true;
            if (concept instanceof AtomicConcept) {
                if (this.m_fullAtomicConceptsLabel != null) {
                    ValidatedSingleDirectBlockingChecker.this.m_atomicConceptsSetFactory.removeReference(this.m_fullAtomicConceptsLabel);
                    this.m_fullAtomicConceptsLabel = null;
                }
                if (isCore) {
                    if (this.m_blockingRelevantLabel != null) {
                        ValidatedSingleDirectBlockingChecker.this.m_atomicConceptsSetFactory.removeReference(this.m_blockingRelevantLabel);
                        this.m_blockingRelevantLabel = null;
                    }
                    this.m_blockingRelevantHashCode -= concept.hashCode();
                    this.m_hasChangedForBlocking = true;
                }
            }
        }

        @Override
        public Set<AtomicConcept> getFullAtomicConceptsLabel() {
            if (this.m_fullAtomicConceptsLabel == null) {
                this.m_fullAtomicConceptsLabel = ValidatedSingleDirectBlockingChecker.this.fetchAtomicConceptsLabel(this.m_node, false);
                ValidatedSingleDirectBlockingChecker.this.m_atomicConceptsSetFactory.addReference(this.m_fullAtomicConceptsLabel);
            }
            return this.m_fullAtomicConceptsLabel;
        }

        @Override
        public Set<AtomicRole> getFullFromParentLabel() {
            if (this.m_hasChangedForValidation || this.m_fullFromParentLabel == null) {
                this.m_fullFromParentLabel = ValidatedSingleDirectBlockingChecker.this.fetchAtomicRolesLabel(this.m_node.getParent(), this.m_node, false);
                ValidatedSingleDirectBlockingChecker.this.m_atomicRolesSetFactory.addReference(this.m_fullFromParentLabel);
            }
            return this.m_fullFromParentLabel;
        }

        @Override
        public Set<AtomicRole> getFullToParentLabel() {
            if (this.m_hasChangedForValidation || this.m_fullToParentLabel == null) {
                this.m_fullToParentLabel = ValidatedSingleDirectBlockingChecker.this.fetchAtomicRolesLabel(this.m_node, this.m_node.getParent(), false);
                ValidatedSingleDirectBlockingChecker.this.m_atomicRolesSetFactory.addReference(this.m_fullToParentLabel);
            }
            return this.m_fullToParentLabel;
        }

        @Override
        public void setBlockViolatesParentConstraints(boolean violates) {
            this.m_blockViolatesParentConstraints = violates;
        }

        @Override
        public void setHasAlreadyBeenChecked(boolean hasBeenChecked) {
            this.m_hasAlreadyBeenChecked = hasBeenChecked;
        }

        @Override
        public boolean hasAlreadyBeenChecked() {
            return this.m_hasAlreadyBeenChecked;
        }

        @Override
        public boolean blockViolatesParentConstraints() {
            return this.m_blockViolatesParentConstraints;
        }
    }

}

