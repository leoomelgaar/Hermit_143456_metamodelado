package org.semanticweb.HermiT.blocking;

import java.io.Serializable;
import java.util.List;
import org.semanticweb.HermiT.model.AtomicRole;
import org.semanticweb.HermiT.model.Concept;
import org.semanticweb.HermiT.model.DLClause;
import org.semanticweb.HermiT.model.DLOntology;
import org.semanticweb.HermiT.model.DataRange;
import org.semanticweb.HermiT.model.Variable;
import org.semanticweb.HermiT.monitor.TableauMonitor;
import org.semanticweb.HermiT.tableau.DLClauseEvaluator;
import org.semanticweb.HermiT.tableau.Node;
import org.semanticweb.HermiT.tableau.NodeType;
import org.semanticweb.HermiT.tableau.Tableau;

public class AnywhereValidatedBlocking
implements BlockingStrategy {
    protected final DirectBlockingChecker m_directBlockingChecker;
    protected final ValidatedBlockersCache m_currentBlockersCache;
    protected BlockingValidator m_permanentBlockingValidator;
    protected BlockingValidator m_additionalBlockingValidator;
    protected Tableau m_tableau;
    protected Node m_firstChangedNode;
    protected Node m_lastValidatedUnchangedNode;
    protected final boolean m_useSimpleCore;

    public AnywhereValidatedBlocking(DirectBlockingChecker directBlockingChecker, boolean useSimpleCore) {
        this.m_directBlockingChecker = directBlockingChecker;
        this.m_currentBlockersCache = new ValidatedBlockersCache(this.m_directBlockingChecker);
        this.m_useSimpleCore = useSimpleCore;
    }

    @Override
    public void initialize(Tableau tableau) {
        this.m_tableau = tableau;
        this.m_directBlockingChecker.initialize(tableau);
        this.m_permanentBlockingValidator = new BlockingValidator(this.m_tableau, this.m_tableau.getPermanentDLOntology().getDLClauses());
        this.updateAdditionalBlockingValidator();
    }

    @Override
    public void additionalDLOntologySet(DLOntology additionalDLOntology) {
        this.updateAdditionalBlockingValidator();
    }

    @Override
    public void additionalDLOntologyCleared() {
        this.updateAdditionalBlockingValidator();
    }

    protected void updateAdditionalBlockingValidator() {
        this.m_additionalBlockingValidator = this.m_tableau.getAdditionalHyperresolutionManager() == null ? null : new BlockingValidator(this.m_tableau, this.m_tableau.getAdditionalDLOntology().getDLClauses());
    }

    @Override
    public void clear() {
        this.m_currentBlockersCache.clear();
        this.m_firstChangedNode = null;
        this.m_directBlockingChecker.clear();
        this.m_lastValidatedUnchangedNode = null;
        this.m_permanentBlockingValidator.clear();
        if (this.m_additionalBlockingValidator != null) {
            this.m_additionalBlockingValidator.clear();
        }
    }

    @Override
    public void computeBlocking(boolean finalChance) {
        if (finalChance) {
            this.validateBlocks();
        } else {
            this.computePreBlocking();
        }
    }

    public void computePreBlocking() {
        if (this.m_firstChangedNode != null) {
            Node node;
            for (node = this.m_firstChangedNode; node != null; node = node.getNextTableauNode()) {
                this.m_currentBlockersCache.removeNode(node);
            }
            for (node = this.m_firstChangedNode; node != null; node = node.getNextTableauNode()) {
                if (node.isActive() && (this.m_directBlockingChecker.canBeBlocked(node) || this.m_directBlockingChecker.canBeBlocker(node))) {
                    if (this.m_directBlockingChecker.hasBlockingInfoChanged(node) || !node.isDirectlyBlocked() || node.getBlocker().getNodeID() >= this.m_firstChangedNode.getNodeID()) {
                        Node parent = node.getParent();
                        if (parent == null) {
                            node.setBlocked(null, false);
                        } else if (parent.isBlocked()) {
                            node.setBlocked(parent, false);
                        } else {
                            Node blocker = null;
                            if (this.m_lastValidatedUnchangedNode == null) {
                                blocker = this.m_currentBlockersCache.getBlocker(node);
                            } else {
                                Node previousBlocker = node.getBlocker();
                                boolean nodeModified = this.m_directBlockingChecker.hasChangedSinceValidation(node);
                                for (Node possibleBlocker : this.m_currentBlockersCache.getPossibleBlockers(node)) {
                                    if (!nodeModified && !this.m_directBlockingChecker.hasChangedSinceValidation(possibleBlocker) && previousBlocker != possibleBlocker) continue;
                                    blocker = possibleBlocker;
                                    break;
                                }
                            }
                            node.setBlocked(blocker, blocker != null);
                        }
                    }
                    if (!node.isBlocked() && this.m_directBlockingChecker.canBeBlocker(node)) {
                        this.m_currentBlockersCache.addNode(node);
                    }
                }
                this.m_directBlockingChecker.clearBlockingInfoChanged(node);
            }
            this.m_firstChangedNode = null;
        }
    }

    public void validateBlocks() {
        Node node;
        boolean debuggingMode = false;
        int checkedBlocks = 0;
        int invalidBlocks = 0;
        TableauMonitor monitor = this.m_tableau.getTableauMonitor();
        if (monitor != null) {
            monitor.blockingValidationStarted();
        }
        Node firstValidatedNode = node = this.m_lastValidatedUnchangedNode == null ? this.m_tableau.getFirstTableauNode() : this.m_lastValidatedUnchangedNode;
        while (node != null) {
            this.m_currentBlockersCache.removeNode(node);
            node = node.getNextTableauNode();
        }
        node = firstValidatedNode;
        if (debuggingMode) {
            System.out.print("Model size: " + (this.m_tableau.getNumberOfNodesInTableau() - this.m_tableau.getNumberOfMergedOrPrunedNodes()) + " Current ID:");
        }
        Node firstInvalidlyBlockedNode = null;
        while (node != null) {
            if (node.isActive()) {
                if (node.isBlocked()) {
                    ++checkedBlocks;
                    if (node.isDirectlyBlocked() && (this.m_directBlockingChecker.hasChangedSinceValidation(node) || this.m_directBlockingChecker.hasChangedSinceValidation(node.getParent()) || this.m_directBlockingChecker.hasChangedSinceValidation(node.getBlocker())) || !node.getParent().isBlocked()) {
                        Node validBlocker = null;
                        Node currentBlocker = node.getBlocker();
                        if (node.isDirectlyBlocked() && currentBlocker != null && this.isBlockValid(node)) {
                            validBlocker = currentBlocker;
                        }
                        if (validBlocker == null) {
                            for (Node possibleBlocker : this.m_currentBlockersCache.getPossibleBlockers(node)) {
                                if (possibleBlocker == currentBlocker) continue;
                                node.setBlocked(possibleBlocker, true);
                                this.m_permanentBlockingValidator.blockerChanged(node);
                                if (this.m_additionalBlockingValidator != null) {
                                    this.m_additionalBlockingValidator.blockerChanged(node);
                                }
                                if (!this.isBlockValid(node)) continue;
                                validBlocker = possibleBlocker;
                                break;
                            }
                        }
                        if (validBlocker == null && node.hasUnprocessedExistentials()) {
                            ++invalidBlocks;
                            if (firstInvalidlyBlockedNode == null) {
                                firstInvalidlyBlockedNode = node;
                            }
                        }
                        node.setBlocked(validBlocker, validBlocker != null);
                    }
                }
                this.m_lastValidatedUnchangedNode = node;
                if (!node.isBlocked() && this.m_directBlockingChecker.canBeBlocker(node)) {
                    this.m_currentBlockersCache.addNode(node);
                }
            }
            node = node.getNextTableauNode();
        }
        for (node = firstValidatedNode; node != null; node = node.getNextTableauNode()) {
            if (!node.isActive()) continue;
            this.m_directBlockingChecker.setHasChangedSinceValidation(node, false);
            ValidatedSingleDirectBlockingChecker.ValidatedBlockingObject blockingObject = (ValidatedSingleDirectBlockingChecker.ValidatedBlockingObject)node.getBlockingObject();
            blockingObject.setBlockViolatesParentConstraints(false);
            blockingObject.setHasAlreadyBeenChecked(false);
        }
        this.m_firstChangedNode = firstInvalidlyBlockedNode;
        if (monitor != null) {
            monitor.blockingValidationFinished(invalidBlocks);
        }
        if (debuggingMode) {
            System.out.println();
            System.out.println("Checked " + checkedBlocks + " blocked nodes of which " + invalidBlocks + " were invalid.");
        }
    }

    protected boolean isBlockValid(Node node) {
        if (this.m_permanentBlockingValidator.isBlockValid(node)) {
            if (this.m_additionalBlockingValidator != null) {
                return this.m_additionalBlockingValidator.isBlockValid(node);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isPermanentAssertion(Concept concept, Node node) {
        return true;
    }

    @Override
    public boolean isPermanentAssertion(DataRange range, Node node) {
        return true;
    }

    protected void validationInfoChanged(Node node) {
        if (node != null) {
            if (this.m_lastValidatedUnchangedNode != null && node.getNodeID() < this.m_lastValidatedUnchangedNode.getNodeID()) {
                this.m_lastValidatedUnchangedNode = node;
            }
            this.m_directBlockingChecker.setHasChangedSinceValidation(node, true);
        }
    }

    @Override
    public void assertionAdded(Concept concept, Node node, boolean isCore) {
        this.updateNodeChange(this.m_directBlockingChecker.assertionAdded(concept, node, isCore));
        this.validationInfoChanged(node);
        this.validationInfoChanged(node.getParent());
    }

    @Override
    public void assertionCoreSet(Concept concept, Node node) {
        this.updateNodeChange(this.m_directBlockingChecker.assertionAdded(concept, node, true));
        this.validationInfoChanged(node);
        this.validationInfoChanged(node.getParent());
    }

    @Override
    public void assertionRemoved(Concept concept, Node node, boolean isCore) {
        this.updateNodeChange(this.m_directBlockingChecker.assertionRemoved(concept, node, isCore));
        this.validationInfoChanged(node);
        this.validationInfoChanged(node.getParent());
    }

    @Override
    public void assertionAdded(DataRange range, Node node, boolean isCore) {
        this.updateNodeChange(this.m_directBlockingChecker.assertionAdded(range, node, isCore));
        this.validationInfoChanged(node);
        this.validationInfoChanged(node.getParent());
    }

    @Override
    public void assertionCoreSet(DataRange range, Node node) {
        this.updateNodeChange(this.m_directBlockingChecker.assertionAdded(range, node, true));
        this.validationInfoChanged(node);
        this.validationInfoChanged(node.getParent());
    }

    @Override
    public void assertionRemoved(DataRange range, Node node, boolean isCore) {
        this.updateNodeChange(this.m_directBlockingChecker.assertionRemoved(range, node, isCore));
        this.validationInfoChanged(node);
        this.validationInfoChanged(node.getParent());
    }

    @Override
    public void assertionAdded(AtomicRole atomicRole, Node nodeFrom, Node nodeTo, boolean isCore) {
        if (isCore) {
            this.updateNodeChange(nodeFrom);
        }
        if (isCore) {
            this.updateNodeChange(nodeTo);
        }
        this.validationInfoChanged(nodeFrom);
        this.validationInfoChanged(nodeTo);
    }

    @Override
    public void assertionCoreSet(AtomicRole atomicRole, Node nodeFrom, Node nodeTo) {
        this.updateNodeChange(this.m_directBlockingChecker.assertionAdded(atomicRole, nodeFrom, nodeTo, true));
        this.validationInfoChanged(nodeFrom);
        this.validationInfoChanged(nodeTo);
    }

    @Override
    public void assertionRemoved(AtomicRole atomicRole, Node nodeFrom, Node nodeTo, boolean isCore) {
        this.updateNodeChange(this.m_directBlockingChecker.assertionRemoved(atomicRole, nodeFrom, nodeTo, true));
        this.validationInfoChanged(nodeFrom);
        this.validationInfoChanged(nodeTo);
    }

    @Override
    public void nodesMerged(Node mergeFrom, Node mergeInto) {
        Node parent = mergeFrom.getParent();
        if (parent != null && (this.m_directBlockingChecker.canBeBlocker(parent) || this.m_directBlockingChecker.canBeBlocked(parent))) {
            this.validationInfoChanged(parent);
        }
    }

    @Override
    public void nodesUnmerged(Node mergeFrom, Node mergeInto) {
        Node parent = mergeFrom.getParent();
        if (parent != null && (this.m_directBlockingChecker.canBeBlocker(parent) || this.m_directBlockingChecker.canBeBlocked(parent))) {
            this.validationInfoChanged(parent);
        }
    }

    @Override
    public void nodeStatusChanged(Node node) {
        this.updateNodeChange(node);
        this.validationInfoChanged(node);
        this.validationInfoChanged(node.getParent());
    }

    protected final void updateNodeChange(Node node) {
        if (node != null && (this.m_firstChangedNode == null || node.getNodeID() < this.m_firstChangedNode.getNodeID())) {
            this.m_firstChangedNode = node;
        }
    }

    @Override
    public void nodeInitialized(Node node) {
        this.m_directBlockingChecker.nodeInitialized(node);
    }

    @Override
    public void nodeDestroyed(Node node) {
        this.m_currentBlockersCache.removeNode(node);
        this.m_directBlockingChecker.nodeDestroyed(node);
        if (this.m_firstChangedNode != null && this.m_firstChangedNode.getNodeID() >= node.getNodeID()) {
            this.m_firstChangedNode = null;
        }
        if (this.m_lastValidatedUnchangedNode != null && node.getNodeID() < this.m_lastValidatedUnchangedNode.getNodeID()) {
            this.m_lastValidatedUnchangedNode = node;
        }
    }

    @Override
    public void modelFound() {
    }

    @Override
    public boolean isExact() {
        return false;
    }

    @Override
    public void dlClauseBodyCompiled(List<DLClauseEvaluator.Worker> workers, DLClause dlClause, List<Variable> variables, Object[] valuesBuffer, boolean[] coreVariables) {
        if (this.m_useSimpleCore) {
            for (int i = 0; i < coreVariables.length; ++i) {
                coreVariables[i] = false;
            }
        } else {
            if (dlClause.getHeadLength() == 0) {
                return;
            }
            if (dlClause.getHeadLength() > 1) {
                for (int i = 0; i < coreVariables.length; ++i) {
                    coreVariables[i] = true;
                }
            } else {
                for (int i = 0; i < coreVariables.length; ++i) {
                    coreVariables[i] = false;
                }
                if (dlClause.isAtomicConceptInclusion() && variables.size() > 1) {
                    workers.add(new ComputeCoreVariables(valuesBuffer, coreVariables));
                }
            }
        }
    }

    protected static final class ComputeCoreVariables
    implements DLClauseEvaluator.Worker,
    Serializable {
        private static final long serialVersionUID = 899293772370136783L;
        private final Object[] m_valuesBuffer;
        private final boolean[] m_coreVariables;

        public ComputeCoreVariables(Object[] valuesBuffer, boolean[] coreVariables) {
            this.m_valuesBuffer = valuesBuffer;
            this.m_coreVariables = coreVariables;
        }

        public void clear() {
        }

        @Override
        public int execute(int programCounter) {
            int variableIndex;
            Node node;
            Node potentialNonCore = null;
            for (variableIndex = this.m_coreVariables.length - 1; variableIndex >= 0; --variableIndex) {
                node = (Node)this.m_valuesBuffer[variableIndex];
                if (node.getNodeType() != NodeType.TREE_NODE || potentialNonCore != null && node.getTreeDepth() >= potentialNonCore.getTreeDepth()) continue;
                potentialNonCore = node;
            }
            if (potentialNonCore != null) {
                for (variableIndex = this.m_coreVariables.length - 1; variableIndex >= 0; --variableIndex) {
                    node = (Node)this.m_valuesBuffer[variableIndex];
                    if (node.isRootNode() || potentialNonCore == node || potentialNonCore.getTreeDepth() >= node.getTreeDepth()) continue;
                    this.m_coreVariables[variableIndex] = true;
                }
            }
            return programCounter + 1;
        }

        public String toString() {
            return "Compute core variables";
        }
    }

}

