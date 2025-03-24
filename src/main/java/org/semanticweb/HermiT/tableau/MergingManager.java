package org.semanticweb.HermiT.tableau;

import java.io.Serializable;

import org.semanticweb.HermiT.model.DescriptionGraph;
import org.semanticweb.HermiT.monitor.TableauMonitor;

public final class MergingManager
implements Serializable {
    private static final long serialVersionUID = -8404748898127176927L;
    private final Tableau m_tableau;
    private final TableauMonitor m_tableauMonitor;
    private final ExtensionManager m_extensionManager;
    private final ExtensionTable.Retrieval m_binaryExtensionTableSearch1Bound;
    private final ExtensionTable.Retrieval m_ternaryExtensionTableSearch1Bound;
    private final ExtensionTable.Retrieval m_ternaryExtensionTableSearch2Bound;
    private final Object[] m_binaryAuxiliaryTuple;
    private final Object[] m_ternaryAuxiliaryTuple;
    private final UnionDependencySet m_binaryUnionDependencySet;

    public MergingManager(Tableau tableau) {
        this.m_tableau = tableau;
        this.m_tableauMonitor = this.m_tableau.m_tableauMonitor;
        this.m_extensionManager = this.m_tableau.m_extensionManager;
        this.m_binaryExtensionTableSearch1Bound = this.m_extensionManager.m_binaryExtensionTable.createRetrieval(new boolean[]{false, true}, ExtensionTable.View.TOTAL);
        this.m_ternaryExtensionTableSearch1Bound = this.m_extensionManager.m_ternaryExtensionTable.createRetrieval(new boolean[]{false, true, false}, ExtensionTable.View.TOTAL);
        this.m_ternaryExtensionTableSearch2Bound = this.m_extensionManager.m_ternaryExtensionTable.createRetrieval(new boolean[]{false, false, true}, ExtensionTable.View.TOTAL);
        this.m_binaryAuxiliaryTuple = new Object[2];
        this.m_ternaryAuxiliaryTuple = new Object[3];
        this.m_binaryUnionDependencySet = new UnionDependencySet(2);
    }

    public void clear() {
        this.m_binaryExtensionTableSearch1Bound.clear();
        this.m_ternaryExtensionTableSearch1Bound.clear();
        this.m_ternaryExtensionTableSearch2Bound.clear();
        this.m_binaryAuxiliaryTuple[0] = null;
        this.m_binaryAuxiliaryTuple[1] = null;
        this.m_ternaryAuxiliaryTuple[0] = null;
        this.m_ternaryAuxiliaryTuple[1] = null;
        this.m_ternaryAuxiliaryTuple[2] = null;
    }

    public boolean mergeNodes(Node node0, Node node1, DependencySet dependencySet) {
        Node mergeInto;
        Node mergeFrom;
        int node1Precedence;
        assert (node0.getNodeType().isAbstract() == node1.getNodeType().isAbstract());
        if (!node0.isActive() || !node1.isActive() || node0 == node1) {
            return false;
        }
        int node0Precedence = node0.getNodeType().getMergePrecedence();
        if (node0Precedence < (node1Precedence = node1.getNodeType().getMergePrecedence())) {
            mergeFrom = node1;
            mergeInto = node0;
        } else if (node0Precedence > node1Precedence) {
            mergeFrom = node0;
            mergeInto = node1;
        } else {
            boolean canMerge1Into0;
            Node node0ClusterAnchor = node0.getClusterAnchor();
            Node node1ClusterAnchor = node1.getClusterAnchor();
            boolean canMerge0Into1 = node0.m_parent == node1.m_parent || MergingManager.isDescendantOfAtMostThreeLevels(node0, node1ClusterAnchor);
            boolean bl = canMerge1Into0 = node0.m_parent == node1.m_parent || MergingManager.isDescendantOfAtMostThreeLevels(node1, node0ClusterAnchor);
            if (canMerge0Into1 && canMerge1Into0) {
                if (node0.m_numberOfPositiveAtomicConcepts > node1.m_numberOfPositiveAtomicConcepts) {
                    mergeFrom = node1;
                    mergeInto = node0;
                } else {
                    mergeFrom = node0;
                    mergeInto = node1;
                }
            } else if (canMerge0Into1) {
                mergeFrom = node0;
                mergeInto = node1;
            } else if (canMerge1Into0) {
                mergeFrom = node1;
                mergeInto = node0;
            } else {
                throw new IllegalStateException("Internal error: unsupported merge type.");
            }
        }
        if (this.m_tableauMonitor != null) {
            this.m_tableauMonitor.mergeStarted(mergeFrom, mergeInto);
        }
        for (Node node = mergeFrom; node != null; node = node.getNextTableauNode()) {
            if (!node.isActive() || node.m_parent == null || node.m_parent.isActive() && node.m_parent != mergeFrom) continue;
            if (this.m_tableauMonitor != null) {
                this.m_tableauMonitor.nodePruned(node);
            }
            this.m_tableau.pruneNode(node);
        }
        this.m_binaryUnionDependencySet.m_dependencySets[1] = dependencySet;
        this.m_binaryAuxiliaryTuple[1] = mergeInto;
        this.m_binaryExtensionTableSearch1Bound.getBindingsBuffer()[1] = mergeFrom;
        this.m_binaryExtensionTableSearch1Bound.open();
        Object[] tupleBuffer = this.m_binaryExtensionTableSearch1Bound.getTupleBuffer();
        while (!this.m_binaryExtensionTableSearch1Bound.afterLast()) {
            Object predicate = tupleBuffer[0];
            if (!(predicate instanceof DescriptionGraph)) {
                this.m_binaryAuxiliaryTuple[0] = predicate;
                if (this.m_tableauMonitor != null) {
                    this.m_tableauMonitor.mergeFactStarted(mergeFrom, mergeInto, tupleBuffer, this.m_binaryAuxiliaryTuple);
                }
                this.m_binaryUnionDependencySet.m_dependencySets[0] = this.m_binaryExtensionTableSearch1Bound.getDependencySet();
                this.m_extensionManager.addTuple(this.m_binaryAuxiliaryTuple, this.m_binaryUnionDependencySet, this.m_binaryExtensionTableSearch1Bound.isCore());
                if (this.m_tableauMonitor != null) {
                    this.m_tableauMonitor.mergeFactFinished(mergeFrom, mergeInto, tupleBuffer, this.m_binaryAuxiliaryTuple);
                }
            }
            this.m_binaryExtensionTableSearch1Bound.next();
        }
        this.m_ternaryAuxiliaryTuple[1] = mergeInto;
        this.m_ternaryExtensionTableSearch1Bound.getBindingsBuffer()[1] = mergeFrom;
        this.m_ternaryExtensionTableSearch1Bound.open();
        tupleBuffer = this.m_ternaryExtensionTableSearch1Bound.getTupleBuffer();
        while (!this.m_ternaryExtensionTableSearch1Bound.afterLast()) {
            Object predicate = tupleBuffer[0];
            if (!(predicate instanceof DescriptionGraph)) {
                this.m_ternaryAuxiliaryTuple[0] = predicate;
                Object object = this.m_ternaryAuxiliaryTuple[2] = tupleBuffer[2] == mergeFrom ? mergeInto : tupleBuffer[2];
                if (this.m_tableauMonitor != null) {
                    this.m_tableauMonitor.mergeFactStarted(mergeFrom, mergeInto, tupleBuffer, this.m_ternaryAuxiliaryTuple);
                }
                this.m_binaryUnionDependencySet.m_dependencySets[0] = this.m_ternaryExtensionTableSearch1Bound.getDependencySet();
                this.m_extensionManager.addTuple(this.m_ternaryAuxiliaryTuple, this.m_binaryUnionDependencySet, this.m_ternaryExtensionTableSearch1Bound.isCore());
                if (this.m_tableauMonitor != null) {
                    this.m_tableauMonitor.mergeFactFinished(mergeFrom, mergeInto, tupleBuffer, this.m_ternaryAuxiliaryTuple);
                }
            }
            this.m_ternaryExtensionTableSearch1Bound.next();
        }
        this.m_ternaryAuxiliaryTuple[2] = mergeInto;
        this.m_ternaryExtensionTableSearch2Bound.getBindingsBuffer()[2] = mergeFrom;
        this.m_ternaryExtensionTableSearch2Bound.open();
        tupleBuffer = this.m_ternaryExtensionTableSearch2Bound.getTupleBuffer();
        while (!this.m_ternaryExtensionTableSearch2Bound.afterLast()) {
            Object predicate = tupleBuffer[0];
            if (!(predicate instanceof DescriptionGraph)) {
                this.m_ternaryAuxiliaryTuple[0] = predicate;
                Object object = this.m_ternaryAuxiliaryTuple[1] = tupleBuffer[1] == mergeFrom ? mergeInto : tupleBuffer[1];
                if (this.m_tableauMonitor != null) {
                    this.m_tableauMonitor.mergeFactStarted(mergeFrom, mergeInto, tupleBuffer, this.m_ternaryAuxiliaryTuple);
                }
                this.m_binaryUnionDependencySet.m_dependencySets[0] = this.m_ternaryExtensionTableSearch2Bound.getDependencySet();
                this.m_extensionManager.addTuple(this.m_ternaryAuxiliaryTuple, this.m_binaryUnionDependencySet, this.m_ternaryExtensionTableSearch2Bound.isCore());
                if (this.m_tableauMonitor != null) {
                    this.m_tableauMonitor.mergeFactFinished(mergeFrom, mergeInto, tupleBuffer, this.m_ternaryAuxiliaryTuple);
                }
            }
            this.m_ternaryExtensionTableSearch2Bound.next();
        }
        this.m_tableau.m_descriptionGraphManager.mergeGraphs(mergeFrom, mergeInto);
        this.m_tableau.mergeNode(mergeFrom, mergeInto, dependencySet);
        if (this.m_tableauMonitor != null) {
            this.m_tableauMonitor.mergeFinished(mergeFrom, mergeInto);
        }
    	this.m_tableau.metamodellingFlag = true;
        return true;
    }

    private static boolean isDescendantOfAtMostThreeLevels(Node descendant, Node ancestor) {
        if (descendant != null) {
            Node descendantParent = descendant.m_parent;
            if (descendantParent == ancestor) {
                return true;
            }
            if (descendantParent != null) {
                Node descendantParentParentParent;
                Node descendantParentParent = descendantParent.m_parent;
                if (descendantParentParent == ancestor) {
                    return true;
                }
                return descendantParentParent != null && (descendantParentParentParent = descendantParentParent.m_parent) == ancestor;
            }
        }
        return false;
    }
}

