package org.semanticweb.HermiT.existentials;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.semanticweb.HermiT.blocking.BlockingStrategy;
import org.semanticweb.HermiT.model.AtLeast;
import org.semanticweb.HermiT.model.AtLeastConcept;
import org.semanticweb.HermiT.model.AtLeastDataRange;
import org.semanticweb.HermiT.model.AtomicRole;
import org.semanticweb.HermiT.model.Concept;
import org.semanticweb.HermiT.model.DLClause;
import org.semanticweb.HermiT.model.DLOntology;
import org.semanticweb.HermiT.model.DataRange;
import org.semanticweb.HermiT.model.ExistentialConcept;
import org.semanticweb.HermiT.model.ExistsDescriptionGraph;
import org.semanticweb.HermiT.model.Inequality;
import org.semanticweb.HermiT.model.InverseRole;
import org.semanticweb.HermiT.model.LiteralConcept;
import org.semanticweb.HermiT.model.LiteralDataRange;
import org.semanticweb.HermiT.model.Role;
import org.semanticweb.HermiT.model.Variable;
import org.semanticweb.HermiT.monitor.TableauMonitor;
import org.semanticweb.HermiT.tableau.DLClauseEvaluator;
import org.semanticweb.HermiT.tableau.DescriptionGraphManager;
import org.semanticweb.HermiT.tableau.ExistentialExpansionManager;
import org.semanticweb.HermiT.tableau.ExtensionManager;
import org.semanticweb.HermiT.tableau.ExtensionTable;
import org.semanticweb.HermiT.tableau.InterruptFlag;
import org.semanticweb.HermiT.tableau.Node;
import org.semanticweb.HermiT.tableau.Tableau;

public abstract class AbstractExpansionStrategy
implements ExistentialExpansionStrategy,
Serializable {
    private static final long serialVersionUID = 2831957929321676444L;
    protected final BlockingStrategy m_blockingStrategy;
    protected final boolean m_expandNodeAtATime;
    protected final List<ExistentialConcept> m_processedExistentials;
    protected final List<Node> m_auxiliaryNodes1;
    protected final List<Node> m_auxiliaryNodes2;
    protected Tableau m_tableau;
    protected InterruptFlag m_interruptFlag;
    protected ExtensionManager m_extensionManager;
    protected ExtensionTable.Retrieval m_ternaryExtensionTableSearch01Bound;
    protected ExtensionTable.Retrieval m_ternaryExtensionTableSearch02Bound;
    protected ExistentialExpansionManager m_existentialExpansionManager;
    protected DescriptionGraphManager m_descriptionGraphManager;

    public AbstractExpansionStrategy(BlockingStrategy blockingStrategy, boolean expandNodeAtATime) {
        this.m_blockingStrategy = blockingStrategy;
        this.m_expandNodeAtATime = expandNodeAtATime;
        this.m_processedExistentials = new ArrayList<ExistentialConcept>();
        this.m_auxiliaryNodes1 = new ArrayList<Node>();
        this.m_auxiliaryNodes2 = new ArrayList<Node>();
    }

    @Override
    public void initialize(Tableau tableau) {
        this.m_tableau = tableau;
        this.m_interruptFlag = this.m_tableau.getInterruptFlag();
        this.m_extensionManager = this.m_tableau.getExtensionManager();
        this.m_ternaryExtensionTableSearch01Bound = this.m_extensionManager.getTernaryExtensionTable().createRetrieval(new boolean[]{true, true, false}, ExtensionTable.View.TOTAL);
        this.m_ternaryExtensionTableSearch02Bound = this.m_extensionManager.getTernaryExtensionTable().createRetrieval(new boolean[]{true, false, true}, ExtensionTable.View.TOTAL);
        this.m_existentialExpansionManager = this.m_tableau.getExistentialExpansionManager();
        this.m_descriptionGraphManager = this.m_tableau.getDescriptionGraphManager();
        this.m_blockingStrategy.initialize(this.m_tableau);
    }

    @Override
    public void additionalDLOntologySet(DLOntology additionalDLOntology) {
        this.m_blockingStrategy.additionalDLOntologySet(additionalDLOntology);
    }

    @Override
    public void additionalDLOntologyCleared() {
        this.m_blockingStrategy.additionalDLOntologyCleared();
    }

    @Override
    public void clear() {
        this.m_blockingStrategy.clear();
        this.m_processedExistentials.clear();
        this.m_ternaryExtensionTableSearch01Bound.clear();
        this.m_ternaryExtensionTableSearch02Bound.clear();
    }

    @Override
    public boolean expandExistentials(boolean finalChance) {
        TableauMonitor monitor = this.m_tableau.getTableauMonitor();
        this.m_blockingStrategy.computeBlocking(finalChance);
        boolean extensionsChanged = false;
        for (Node node = this.m_tableau.getFirstTableauNode(); !(node == null || extensionsChanged && this.m_expandNodeAtATime); node = node.getNextTableauNode()) {
            if (node.isActive() && !node.isBlocked() && node.hasUnprocessedExistentials()) {
                this.m_processedExistentials.clear();
                this.m_processedExistentials.addAll(node.getUnprocessedExistentials());
                for (int index = this.m_processedExistentials.size() - 1; index >= 0; --index) {
                    ExistentialConcept existentialConcept = this.m_processedExistentials.get(index);
                    if (existentialConcept instanceof AtLeast) {
                        AtLeast atLeast = (AtLeast)existentialConcept;
                        switch (this.isSatisfied(atLeast, node)) {
                            case NOT_SATISFIED: {
                                this.expandExistential(atLeast, node);
                                extensionsChanged = true;
                                break;
                            }
                            case PERMANENTLY_SATISFIED: {
                                this.m_existentialExpansionManager.markExistentialProcessed(existentialConcept, node);
                                if (monitor == null) break;
                                monitor.existentialSatisfied(existentialConcept, node);
                                break;
                            }
                            case CURRENTLY_SATISFIED: {
                                if (monitor == null) break;
                                monitor.existentialSatisfied(existentialConcept, node);
                                break;
                            }
                        }
                    } else if (existentialConcept instanceof ExistsDescriptionGraph) {
                        ExistsDescriptionGraph existsDescriptionGraph = (ExistsDescriptionGraph)existentialConcept;
                        if (!this.m_descriptionGraphManager.isSatisfied(existsDescriptionGraph, node)) {
                            this.m_descriptionGraphManager.expand(existsDescriptionGraph, node);
                            extensionsChanged = true;
                        } else if (monitor != null) {
                            monitor.existentialSatisfied(existsDescriptionGraph, node);
                        }
                        this.m_existentialExpansionManager.markExistentialProcessed(existentialConcept, node);
                    } else {
                        throw new IllegalStateException("Unsupported type of existential.");
                    }
                    this.m_interruptFlag.checkInterrupt();
                }
            }
            this.m_interruptFlag.checkInterrupt();
        }
        return extensionsChanged;
    }

    @Override
    public void assertionAdded(Concept concept, Node node, boolean isCore) {
        this.m_blockingStrategy.assertionAdded(concept, node, isCore);
    }

    @Override
    public void assertionCoreSet(Concept concept, Node node) {
        this.m_blockingStrategy.assertionCoreSet(concept, node);
    }

    @Override
    public void assertionRemoved(Concept concept, Node node, boolean isCore) {
        this.m_blockingStrategy.assertionRemoved(concept, node, isCore);
    }

    @Override
    public void assertionAdded(DataRange range, Node node, boolean isCore) {
        this.m_blockingStrategy.assertionAdded(range, node, isCore);
    }

    @Override
    public void assertionCoreSet(DataRange range, Node node) {
        this.m_blockingStrategy.assertionCoreSet(range, node);
    }

    @Override
    public void assertionRemoved(DataRange range, Node node, boolean isCore) {
        this.m_blockingStrategy.assertionRemoved(range, node, isCore);
    }

    @Override
    public void assertionAdded(AtomicRole atomicRole, Node nodeFrom, Node nodeTo, boolean isCore) {
        this.m_blockingStrategy.assertionAdded(atomicRole, nodeFrom, nodeTo, isCore);
    }

    @Override
    public void assertionCoreSet(AtomicRole atomicRole, Node nodeFrom, Node nodeTo) {
        this.m_blockingStrategy.assertionCoreSet(atomicRole, nodeFrom, nodeTo);
    }

    @Override
    public void assertionRemoved(AtomicRole atomicRole, Node nodeFrom, Node nodeTo, boolean isCore) {
        this.m_blockingStrategy.assertionRemoved(atomicRole, nodeFrom, nodeTo, isCore);
    }

    @Override
    public void nodesMerged(Node mergeFrom, Node mergeInto) {
        this.m_blockingStrategy.nodesMerged(mergeFrom, mergeInto);
    }

    @Override
    public void nodesUnmerged(Node mergeFrom, Node mergeInto) {
        this.m_blockingStrategy.nodesUnmerged(mergeFrom, mergeInto);
    }

    @Override
    public void nodeStatusChanged(Node node) {
        this.m_blockingStrategy.nodeStatusChanged(node);
    }

    @Override
    public void nodeInitialized(Node node) {
        this.m_blockingStrategy.nodeInitialized(node);
    }

    @Override
    public void nodeDestroyed(Node node) {
        this.m_blockingStrategy.nodeDestroyed(node);
    }

    @Override
    public void branchingPointPushed() {
    }

    @Override
    public void backtrack() {
    }

    @Override
    public void modelFound() {
        this.m_blockingStrategy.modelFound();
    }

    @Override
    public boolean isExact() {
        return this.m_blockingStrategy.isExact();
    }

    @Override
    public void dlClauseBodyCompiled(List<DLClauseEvaluator.Worker> workers, DLClause dlClause, List<Variable> variables, Object[] valuesBuffer, boolean[] coreVariables) {
        this.m_blockingStrategy.dlClauseBodyCompiled(workers, dlClause, variables, valuesBuffer, coreVariables);
    }

    protected SatType isSatisfied(AtLeast atLeast, Node forNode) {
        ExtensionTable.Retrieval retrieval;
        int toNodeIndex;
        int cardinality = atLeast.getNumber();
        if (cardinality <= 0) {
            return SatType.PERMANENTLY_SATISFIED;
        }
        Role onRole = atLeast.getOnRole();
        if (onRole instanceof AtomicRole) {
            retrieval = this.m_ternaryExtensionTableSearch01Bound;
            retrieval.getBindingsBuffer()[0] = onRole;
            retrieval.getBindingsBuffer()[1] = forNode;
            toNodeIndex = 2;
        } else {
            retrieval = this.m_ternaryExtensionTableSearch02Bound;
            retrieval.getBindingsBuffer()[0] = ((InverseRole)onRole).getInverseOf();
            retrieval.getBindingsBuffer()[2] = forNode;
            toNodeIndex = 1;
        }
        if (cardinality == 1) {
            retrieval.open();
            Object[] tupleBuffer = retrieval.getTupleBuffer();
            while (!retrieval.afterLast()) {
                Node toNode = (Node)tupleBuffer[toNodeIndex];
                if (atLeast instanceof AtLeastDataRange) {
                    LiteralDataRange toDataRange = ((AtLeastDataRange)atLeast).getToDataRange();
                    if (this.m_extensionManager.containsDataRangeAssertion(toDataRange, toNode)) {
                        if (this.isPermanentSatisfier(forNode, toNode) && this.m_blockingStrategy.isPermanentAssertion(toDataRange, toNode)) {
                            return SatType.PERMANENTLY_SATISFIED;
                        }
                        return SatType.CURRENTLY_SATISFIED;
                    }
                } else {
                    LiteralConcept toConcept = ((AtLeastConcept)atLeast).getToConcept();
                    if ((!toNode.isBlocked() || forNode.isParentOf(toNode)) && this.m_extensionManager.containsConceptAssertion(toConcept, toNode)) {
                        if (this.isPermanentSatisfier(forNode, toNode) && this.m_blockingStrategy.isPermanentAssertion(toConcept, toNode)) {
                            return SatType.PERMANENTLY_SATISFIED;
                        }
                        return SatType.CURRENTLY_SATISFIED;
                    }
                }
                retrieval.next();
            }
            return SatType.NOT_SATISFIED;
        }
        this.m_auxiliaryNodes1.clear();
        retrieval.open();
        Object[] tupleBuffer = retrieval.getTupleBuffer();
        boolean allSatisfiersArePermanent = true;
        while (!retrieval.afterLast()) {
            Node toNode = (Node)tupleBuffer[toNodeIndex];
            if (atLeast instanceof AtLeastDataRange) {
                LiteralDataRange toDataRange = ((AtLeastDataRange)atLeast).getToDataRange();
                if (this.m_extensionManager.containsDataRangeAssertion(toDataRange, toNode)) {
                    if (!this.isPermanentSatisfier(forNode, toNode) || !this.m_blockingStrategy.isPermanentAssertion(toDataRange, toNode)) {
                        allSatisfiersArePermanent = false;
                    }
                    this.m_auxiliaryNodes1.add(toNode);
                }
            } else {
                LiteralConcept toConcept = ((AtLeastConcept)atLeast).getToConcept();
                if ((!toNode.isBlocked() || forNode.isParentOf(toNode)) && this.m_extensionManager.containsConceptAssertion(toConcept, toNode)) {
                    if (!this.isPermanentSatisfier(forNode, toNode) || !this.m_blockingStrategy.isPermanentAssertion(toConcept, toNode)) {
                        allSatisfiersArePermanent = false;
                    }
                    this.m_auxiliaryNodes1.add(toNode);
                }
            }
            retrieval.next();
        }
        if (this.m_auxiliaryNodes1.size() >= cardinality) {
            this.m_auxiliaryNodes2.clear();
            if (this.containsSubsetOfNUnequalNodes(forNode, this.m_auxiliaryNodes1, 0, this.m_auxiliaryNodes2, cardinality)) {
                return allSatisfiersArePermanent ? SatType.PERMANENTLY_SATISFIED : SatType.CURRENTLY_SATISFIED;
            }
        }
        return SatType.NOT_SATISFIED;
    }

    protected boolean isPermanentSatisfier(Node forNode, Node toNode) {
        return forNode == toNode || forNode.getParent() == toNode || toNode.getParent() == forNode || toNode.isRootNode();
    }

    protected boolean containsSubsetOfNUnequalNodes(Node forNode, List<Node> nodes, int startAt, List<Node> selectedNodes, int cardinality) {
        if (selectedNodes.size() == cardinality) {
            return true;
        }
        block0 : for (int index = startAt; index < nodes.size(); ++index) {
            Node node = nodes.get(index);
            for (int selectedNodeIndex = 0; selectedNodeIndex < selectedNodes.size(); ++selectedNodeIndex) {
                Node selectedNode = selectedNodes.get(selectedNodeIndex);
                if (!this.m_extensionManager.containsAssertion(Inequality.INSTANCE, node, selectedNode) && !this.m_extensionManager.containsAssertion(Inequality.INSTANCE, selectedNode, node)) continue block0;
            }
            selectedNodes.add(node);
            if (this.containsSubsetOfNUnequalNodes(forNode, nodes, index + 1, selectedNodes, cardinality)) {
                return true;
            }
            selectedNodes.remove(selectedNodes.size() - 1);
        }
        return false;
    }

    protected abstract void expandExistential(AtLeast var1, Node var2);

    protected enum SatType {
        NOT_SATISFIED,
        PERMANENTLY_SATISFIED,
        CURRENTLY_SATISFIED

    }

}

