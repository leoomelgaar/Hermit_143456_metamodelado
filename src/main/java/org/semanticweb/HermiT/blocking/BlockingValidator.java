package org.semanticweb.HermiT.blocking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.semanticweb.HermiT.model.AnnotatedEquality;
import org.semanticweb.HermiT.model.AtLeastConcept;
import org.semanticweb.HermiT.model.Atom;
import org.semanticweb.HermiT.model.AtomicConcept;
import org.semanticweb.HermiT.model.AtomicRole;
import org.semanticweb.HermiT.model.DLClause;
import org.semanticweb.HermiT.model.DLPredicate;
import org.semanticweb.HermiT.model.Equality;
import org.semanticweb.HermiT.model.InverseRole;
import org.semanticweb.HermiT.model.LiteralConcept;
import org.semanticweb.HermiT.model.Role;
import org.semanticweb.HermiT.model.Variable;
import org.semanticweb.HermiT.tableau.ExtensionManager;
import org.semanticweb.HermiT.tableau.ExtensionTable;
import org.semanticweb.HermiT.tableau.Node;
import org.semanticweb.HermiT.tableau.Tableau;

public class BlockingValidator {
    protected final ExtensionManager m_extensionManager;
    protected final ExtensionTable.Retrieval m_binaryRetrieval1Bound;
    protected final ExtensionTable.Retrieval m_ternaryRetrieval01Bound;
    protected final ExtensionTable.Retrieval m_ternaryRetrieval02Bound;
    protected final ExtensionTable.Retrieval m_ternaryRetrieval1Bound;
    protected final ExtensionTable.Retrieval m_ternaryRetrieval2Bound;
    protected final List<DLClauseInfo> m_dlClauseInfos;
    protected final Map<AtomicConcept, List<DLClauseInfo>> m_dlClauseInfosByXConcepts;
    protected final List<DLClauseInfo> m_dlClauseInfosWithoutXConcepts;
    protected final Map<AtLeastConcept, Node> inValidAtleastForBlockedParent = new HashMap<AtLeastConcept, Node>();
    protected final Map<DLClauseInfo, Node> inValidClausesForBlockedParent = new HashMap<DLClauseInfo, Node>();
    protected final Map<AtLeastConcept, Node> inValidAtleastForBlocker = new HashMap<AtLeastConcept, Node>();
    protected final Map<DLClauseInfo, Node> inValidClausesForBlocker = new HashMap<DLClauseInfo, Node>();
    protected final boolean debuggingMode = false;

    public BlockingValidator(Tableau tableau, Set<DLClause> dlClauses) {
        this.m_extensionManager = tableau.getExtensionManager();
        this.m_binaryRetrieval1Bound = this.m_extensionManager.getBinaryExtensionTable().createRetrieval(new boolean[]{false, true}, ExtensionTable.View.TOTAL);
        this.m_ternaryRetrieval01Bound = this.m_extensionManager.getTernaryExtensionTable().createRetrieval(new boolean[]{true, true, false}, ExtensionTable.View.TOTAL);
        this.m_ternaryRetrieval02Bound = this.m_extensionManager.getTernaryExtensionTable().createRetrieval(new boolean[]{true, false, true}, ExtensionTable.View.TOTAL);
        this.m_ternaryRetrieval1Bound = this.m_extensionManager.getTernaryExtensionTable().createRetrieval(new boolean[]{false, true, false}, ExtensionTable.View.TOTAL);
        this.m_ternaryRetrieval2Bound = this.m_extensionManager.getTernaryExtensionTable().createRetrieval(new boolean[]{false, false, true}, ExtensionTable.View.TOTAL);
        this.m_dlClauseInfos = new ArrayList<DLClauseInfo>();
        for (DLClause dlClause : dlClauses) {
            if (!dlClause.isGeneralConceptInclusion()) continue;
            //AtomicConcept[] clauseInfo = new AtomicConcept[](dlClause, this.m_extensionManager);
            DLClauseInfo clauseInfo=new DLClauseInfo(dlClause,m_extensionManager);
            if (clauseInfo.m_yNodes.length <= 0 && clauseInfo.m_zConcepts.length <= 0) continue;
            this.m_dlClauseInfos.add(clauseInfo);
        }
        this.m_dlClauseInfosByXConcepts = new HashMap<AtomicConcept, List<DLClauseInfo>>();
        this.m_dlClauseInfosWithoutXConcepts = new ArrayList<DLClauseInfo>();
        for (DLClauseInfo dlClauseInfo : this.m_dlClauseInfos) {
            if (dlClauseInfo.m_xConcepts.length == 0) {
                this.m_dlClauseInfosWithoutXConcepts.add(dlClauseInfo);
                continue;
            }
            for (AtomicConcept xConcept : dlClauseInfo.m_xConcepts) {
                List<DLClauseInfo> dlClauseInfosForXConcept = this.m_dlClauseInfosByXConcepts.get(xConcept);
                if (dlClauseInfosForXConcept == null) {
                    dlClauseInfosForXConcept = new ArrayList<DLClauseInfo>();
                    this.m_dlClauseInfosByXConcepts.put(xConcept, dlClauseInfosForXConcept);
                }
                dlClauseInfosForXConcept.add(dlClauseInfo);
            }
        }
    }

    public void clear() {
        this.m_binaryRetrieval1Bound.clear();
        this.m_ternaryRetrieval01Bound.clear();
        this.m_ternaryRetrieval02Bound.clear();
        this.m_ternaryRetrieval1Bound.clear();
        this.m_ternaryRetrieval2Bound.clear();
        for (int index = this.m_dlClauseInfos.size() - 1; index >= 0; --index) {
            this.m_dlClauseInfos.get(index).clear();
        }
    }

    public void blockerChanged(Node node) {
        Node parent = node.getParent();
        ((ValidatedSingleDirectBlockingChecker.ValidatedBlockingObject)parent.getBlockingObject()).setHasAlreadyBeenChecked(false);
    }

    public boolean isBlockValid(Node blocked) {
        Node blockedParent = blocked.getParent();
        if (!((ValidatedSingleDirectBlockingChecker.ValidatedBlockingObject)blockedParent.getBlockingObject()).hasAlreadyBeenChecked()) {
            this.resetChildFlags(blockedParent);
            this.checkConstraintsForNonblockedX(blockedParent);
            ((ValidatedSingleDirectBlockingChecker.ValidatedBlockingObject)blockedParent.getBlockingObject()).setHasAlreadyBeenChecked(true);
        }
        if (((ValidatedSingleDirectBlockingChecker.ValidatedBlockingObject)blocked.getBlockingObject()).blockViolatesParentConstraints()) {
            return false;
        }
        return this.satisfiesConstraintsForBlockedX(blocked);
    }

    protected void resetChildFlags(Node parent) {
        Node node;
        this.m_ternaryRetrieval1Bound.getBindingsBuffer()[1] = parent;
        this.m_ternaryRetrieval1Bound.open();
        Object[] tupleBuffer = this.m_ternaryRetrieval1Bound.getTupleBuffer();
        while (!this.m_ternaryRetrieval1Bound.afterLast()) {
            node = (Node)tupleBuffer[2];
            if (!node.isAncestorOf(parent)) {
                ((ValidatedSingleDirectBlockingChecker.ValidatedBlockingObject)node.getBlockingObject()).setBlockViolatesParentConstraints(false);
            }
            this.m_ternaryRetrieval1Bound.next();
        }
        this.m_ternaryRetrieval2Bound.getBindingsBuffer()[2] = parent;
        this.m_ternaryRetrieval2Bound.open();
        tupleBuffer = this.m_ternaryRetrieval2Bound.getTupleBuffer();
        while (!this.m_ternaryRetrieval2Bound.afterLast()) {
            node = (Node)tupleBuffer[1];
            if (!node.isAncestorOf(parent)) {
                ((ValidatedSingleDirectBlockingChecker.ValidatedBlockingObject)node.getBlockingObject()).setBlockViolatesParentConstraints(false);
            }
            this.m_ternaryRetrieval2Bound.next();
        }
    }

    protected boolean satisfiesConstraintsForBlockedX(Node blockedX) {
        Node blocker = blockedX.getBlocker();
        Node blockerParent = blocker.getParent();
        this.m_binaryRetrieval1Bound.getBindingsBuffer()[1] = blocker;
        this.m_binaryRetrieval1Bound.open();
        Object[] tupleBuffer = this.m_binaryRetrieval1Bound.getTupleBuffer();
        while (!this.m_binaryRetrieval1Bound.afterLast()) {
        	AtLeastConcept atleast;
            if (tupleBuffer[0] instanceof AtomicConcept) {
                AtomicConcept atomicConcept = (AtomicConcept)tupleBuffer[0];
                List<DLClauseInfo> dlClauseInfosForXConcept = this.m_dlClauseInfosByXConcepts.get(atomicConcept);
                if (dlClauseInfosForXConcept != null) {
                    for (DLClauseInfo dlClauseInfo : dlClauseInfosForXConcept) {
                        if (this.satisfiesDLClauseForBlockedX(dlClauseInfo, blockedX)) continue;
                        return false;
                    }
                }
            } else if (tupleBuffer[0] instanceof AtLeastConcept && this.m_extensionManager.containsRoleAssertion((atleast = (AtLeastConcept)tupleBuffer[0]).getOnRole(), blocker, blockerParent) && this.m_extensionManager.containsConceptAssertion(atleast.getToConcept(), blockerParent) && !this.isSatisfiedAtLeastForBlocked(atleast, blockedX, blocker, blockerParent)) {
                return false;
            }
            this.m_binaryRetrieval1Bound.next();
        }
        for (DLClauseInfo dlClauseInfo : this.m_dlClauseInfosWithoutXConcepts) {
            if (this.satisfiesDLClauseForBlockedX(dlClauseInfo, blockedX)) continue;
            return false;
        }
        return true;
    }

    protected boolean isSatisfiedAtLeastForBlocked(AtLeastConcept atleast, Node blockedX, Node blocker, Node blockerParent) {
        ExtensionTable.Retrieval retrieval;
        int position;
        Role r = atleast.getOnRole();
        LiteralConcept c = atleast.getToConcept();
        Node blockedXParent = blockedX.getParent();
        if (this.m_extensionManager.containsRoleAssertion(r, blockedX, blockedXParent) && this.m_extensionManager.containsConceptAssertion(c, blockedXParent)) {
            return true;
        }
        if (r instanceof AtomicRole) {
            retrieval = this.m_ternaryRetrieval01Bound;
            retrieval.getBindingsBuffer()[0] = r;
            retrieval.getBindingsBuffer()[1] = blocker;
            position = 2;
        } else {
            retrieval = this.m_ternaryRetrieval02Bound;
            retrieval.getBindingsBuffer()[0] = ((InverseRole)r).getInverseOf();
            retrieval.getBindingsBuffer()[2] = blocker;
            position = 1;
        }
        retrieval.open();
        Object[] tupleBuffer = retrieval.getTupleBuffer();
        int suitableSuccessors = 0;
        int requiredSuccessors = atleast.getNumber();
        while (!retrieval.afterLast() && suitableSuccessors < requiredSuccessors) {
            Node rSuccessor = (Node)tupleBuffer[position];
            if (rSuccessor != blockerParent && this.m_extensionManager.containsConceptAssertion(c, rSuccessor)) {
                ++suitableSuccessors;
            }
            retrieval.next();
        }
        return suitableSuccessors >= requiredSuccessors;
    }

    protected boolean satisfiesDLClauseForBlockedX(DLClauseInfo dlClauseInfo, Node blockedX) {
        assert (blockedX.isDirectlyBlocked());
        Node blockedXParent = blockedX.getParent();
        Node blocker = blockedX.getBlocker();
        for (AtomicConcept atomicConcept : dlClauseInfo.m_xConcepts) {
            if (this.m_extensionManager.containsAssertion(atomicConcept, blocker)) continue;
            return true;
        }
        for (DLPredicate atomicRole : dlClauseInfo.m_x2xRoles) {
            if (this.m_extensionManager.containsAssertion(atomicRole, blocker, blocker)) continue;
            return true;
        }
        int matchingYConstraintIndex = -1;
        for (int yIndex = 0; matchingYConstraintIndex == -1 && yIndex < dlClauseInfo.m_yConstraints.length; ++yIndex) {
            if (!dlClauseInfo.m_yConstraints[yIndex].isSatisfiedExplicitly(this.m_extensionManager, blockedX, blockedXParent)) continue;
            matchingYConstraintIndex = yIndex;
        }
        if (matchingYConstraintIndex == -1) {
            return true;
        }
        dlClauseInfo.m_xNode = blocker;
        dlClauseInfo.m_yNodes[matchingYConstraintIndex] = blockedXParent;
        boolean result = this.satisfiesDLClauseForBlockedXAndAnyZ(dlClauseInfo, blockedX, matchingYConstraintIndex, 0);
        dlClauseInfo.m_xNode = null;
        dlClauseInfo.m_yNodes[matchingYConstraintIndex] = null;
        return result;
    }

    protected boolean satisfiesDLClauseForBlockedXAndAnyZ(DLClauseInfo dlClauseInfo, Node blockedX, int parentOfBlockedXIndex, int toMatchIndex) {
        if (toMatchIndex == dlClauseInfo.m_zNodes.length) {
            return this.satisfiesDLClauseForBlockedXAnyZAndAnyY(dlClauseInfo, blockedX, parentOfBlockedXIndex, 0, 0);
        }
        AtomicConcept[] zConcepts = dlClauseInfo.m_zConcepts[toMatchIndex];
        ExtensionTable.Retrieval retrieval = dlClauseInfo.m_zRetrievals[toMatchIndex];
        retrieval.getBindingsBuffer()[0] = zConcepts[0];
        retrieval.open();
        Object[] tupleBuffer = retrieval.getTupleBuffer();
        while (!retrieval.afterLast()) {
            Node nodeZ = (Node)tupleBuffer[1];
            boolean allMatched = true;
            for (int index = 1; index < zConcepts.length; ++index) {
                if (this.m_extensionManager.containsAssertion(zConcepts[index], nodeZ)) continue;
                allMatched = false;
                break;
            }
            if (allMatched) {
                dlClauseInfo.m_zNodes[toMatchIndex] = nodeZ;
                boolean result = this.satisfiesDLClauseForBlockedXAndAnyZ(dlClauseInfo, blockedX, parentOfBlockedXIndex, toMatchIndex + 1);
                dlClauseInfo.m_zNodes[toMatchIndex] = null;
                if (!result) {
                    return false;
                }
            }
            retrieval.next();
        }
        return true;
    }

    protected boolean satisfiesDLClauseForBlockedXAnyZAndAnyY(DLClauseInfo dlClauseInfo, Node blockedX, int parentOfBlockedXIndex, int toMatchIndexXToY, int toMatchIndexYToX) {
        int yNodeIndex;
        ExtensionTable.Retrieval retrieval;
        if (toMatchIndexXToY + toMatchIndexYToX == parentOfBlockedXIndex) {
            if (dlClauseInfo.m_yConstraints[parentOfBlockedXIndex].m_x2yRoles.length != 0) {
                return this.satisfiesDLClauseForBlockedXAnyZAndAnyY(dlClauseInfo, blockedX, parentOfBlockedXIndex, toMatchIndexXToY + 1, toMatchIndexYToX);
            }
            return this.satisfiesDLClauseForBlockedXAnyZAndAnyY(dlClauseInfo, blockedX, parentOfBlockedXIndex, toMatchIndexXToY, toMatchIndexYToX + 1);
        }
        if (toMatchIndexXToY + toMatchIndexYToX == dlClauseInfo.m_yConstraints.length) {
            return this.satisfiesDLClauseForBlockedXAndMatchedNodes(dlClauseInfo, blockedX);
        }
        int xToYIncrement = 0;
        int yToXIncrement = 0;
        Node blocker = blockedX.getBlocker();
        Node blockerParent = blocker.getParent();
        YConstraint yConstraint = dlClauseInfo.m_yConstraints[toMatchIndexXToY + toMatchIndexYToX];
        assert (yConstraint.m_x2yRoles.length != 0 || yConstraint.m_y2xRoles.length != 0);
        if (yConstraint.m_x2yRoles.length != 0) {
            retrieval = dlClauseInfo.m_x2yRetrievals[toMatchIndexXToY];
            retrieval.getBindingsBuffer()[0] = dlClauseInfo.m_x2yRoles[toMatchIndexXToY];
            retrieval.getBindingsBuffer()[1] = blocker;
            yNodeIndex = 2;
            xToYIncrement = 1;
        } else {
            retrieval = dlClauseInfo.m_y2xRetrievals[toMatchIndexYToX];
            retrieval.getBindingsBuffer()[0] = dlClauseInfo.m_y2xRoles[toMatchIndexYToX];
            retrieval.getBindingsBuffer()[2] = blocker;
            yNodeIndex = 1;
            yToXIncrement = 1;
        }
        retrieval.open();
        Object[] tupleBuffer = retrieval.getTupleBuffer();
        while (!retrieval.afterLast()) {
            Node nodeY = (Node)tupleBuffer[yNodeIndex];
            if (nodeY != blockerParent && yConstraint.isSatisfiedExplicitly(this.m_extensionManager, blocker, nodeY)) {
                dlClauseInfo.m_yNodes[toMatchIndexXToY + toMatchIndexYToX] = nodeY;
                boolean result = this.satisfiesDLClauseForBlockedXAnyZAndAnyY(dlClauseInfo, blockedX, parentOfBlockedXIndex, toMatchIndexXToY + xToYIncrement, toMatchIndexYToX + yToXIncrement);
                dlClauseInfo.m_yNodes[toMatchIndexXToY + toMatchIndexYToX] = null;
                if (!result) {
                    return false;
                }
            }
            retrieval.next();
        }
        return true;
    }

    protected boolean satisfiesDLClauseForBlockedXAndMatchedNodes(DLClauseInfo dlClauseInfo, Node blockedX) {
        for (ConsequenceAtom consequenceAtom : dlClauseInfo.m_consequencesForBlockedX) {
            if (!consequenceAtom.isSatisfied(this.m_extensionManager, dlClauseInfo, blockedX)) continue;
            return true;
        }
        return false;
    }

    protected void checkConstraintsForNonblockedX(Node nonblockedX) {
        this.m_binaryRetrieval1Bound.getBindingsBuffer()[1] = nonblockedX;
        this.m_binaryRetrieval1Bound.open();
        Object[] tupleBuffer = this.m_binaryRetrieval1Bound.getTupleBuffer();
        while (!this.m_binaryRetrieval1Bound.afterLast()) {
            if (tupleBuffer[0] instanceof AtLeastConcept) {
                AtLeastConcept atleast = (AtLeastConcept)tupleBuffer[0];
                this.checkAtLeastForNonblocked(atleast, nonblockedX);
            }
            this.m_binaryRetrieval1Bound.next();
        }
        for (DLClauseInfo dlClauseInfo : this.m_dlClauseInfos) {
            this.checkDLClauseForNonblockedX(dlClauseInfo, nonblockedX);
        }
    }

    protected void checkAtLeastForNonblocked(AtLeastConcept atleast, Node nonblocked) {
        ExtensionTable.Retrieval retrieval;
        int position;
        int suitableSuccessors = 0;
        int requiredSuccessors = atleast.getNumber();
        Role r = atleast.getOnRole();
        LiteralConcept c = atleast.getToConcept();
        if (r instanceof AtomicRole) {
            retrieval = this.m_ternaryRetrieval01Bound;
            retrieval.getBindingsBuffer()[0] = r;
            retrieval.getBindingsBuffer()[1] = nonblocked;
            position = 2;
        } else {
            retrieval = this.m_ternaryRetrieval02Bound;
            retrieval.getBindingsBuffer()[0] = ((InverseRole)r).getInverseOf();
            retrieval.getBindingsBuffer()[2] = nonblocked;
            position = 1;
        }
        retrieval.open();
        Object[] tupleBuffer = retrieval.getTupleBuffer();
        ArrayList<Node> possiblyInvalidlyBlocked = new ArrayList<Node>();
        while (!retrieval.afterLast() && suitableSuccessors < requiredSuccessors) {
            Node rSuccessor = (Node)tupleBuffer[position];
            if (rSuccessor.isBlocked() && !((ValidatedSingleDirectBlockingChecker.ValidatedBlockingObject)rSuccessor.getBlockingObject()).blockViolatesParentConstraints()) {
                if (this.m_extensionManager.containsConceptAssertion(c, rSuccessor.getBlocker())) {
                    ++suitableSuccessors;
                } else {
                    possiblyInvalidlyBlocked.add(rSuccessor);
                }
            } else if (this.m_extensionManager.containsConceptAssertion(c, rSuccessor)) {
                ++suitableSuccessors;
            }
            retrieval.next();
        }
        for (int i = 0; i < possiblyInvalidlyBlocked.size() && suitableSuccessors < requiredSuccessors; ++i) {
            Node blocked = possiblyInvalidlyBlocked.get(i);
            if (!this.m_extensionManager.containsConceptAssertion(c, blocked)) continue;
            ((ValidatedSingleDirectBlockingChecker.ValidatedBlockingObject)blocked.getBlockingObject()).setBlockViolatesParentConstraints(true);
            ++suitableSuccessors;
        }
    }

    protected void checkDLClauseForNonblockedX(DLClauseInfo dlClauseInfo, Node nonblockedX) {
        for (AtomicConcept atomicConcept : dlClauseInfo.m_xConcepts) {
            if (this.m_extensionManager.containsAssertion(atomicConcept, nonblockedX)) continue;
            return;
        }
        for (DLPredicate atomicRole : dlClauseInfo.m_x2xRoles) {
            if (this.m_extensionManager.containsAssertion(atomicRole, nonblockedX, nonblockedX)) continue;
            return;
        }
        dlClauseInfo.m_xNode = nonblockedX;
        this.checkDLClauseForNonblockedXAndAnyZ(dlClauseInfo, nonblockedX, 0);
        dlClauseInfo.m_xNode = null;
    }

    protected void checkDLClauseForNonblockedXAndAnyZ(DLClauseInfo dlClauseInfo, Node nonblockedX, int toMatchIndex) {
        if (toMatchIndex != dlClauseInfo.m_zNodes.length) {
            AtomicConcept[] zConcepts = dlClauseInfo.m_zConcepts[toMatchIndex];
            ExtensionTable.Retrieval retrieval = dlClauseInfo.m_zRetrievals[toMatchIndex];
            retrieval.getBindingsBuffer()[0] = zConcepts[0];
            retrieval.open();
            Object[] tupleBuffer = retrieval.getTupleBuffer();
            while (!retrieval.afterLast()) {
                Node nodeZ = (Node)tupleBuffer[1];
                boolean allMatched = true;
                for (int index = 1; index < zConcepts.length; ++index) {
                    if (this.m_extensionManager.containsAssertion(zConcepts[index], nodeZ)) continue;
                    allMatched = false;
                    break;
                }
                if (allMatched) {
                    dlClauseInfo.m_zNodes[toMatchIndex] = nodeZ;
                    this.checkDLClauseForNonblockedXAndAnyZ(dlClauseInfo, nonblockedX, toMatchIndex + 1);
                    dlClauseInfo.m_zNodes[toMatchIndex] = null;
                    return;
                }
                retrieval.next();
            }
            return;
        }
        this.checkDLClauseForNonblockedXAnyZAndAnyY(dlClauseInfo, nonblockedX, 0, 0);
    }

    protected void checkDLClauseForNonblockedXAnyZAndAnyY(DLClauseInfo dlClauseInfo, Node nonblockedX, int toMatchIndexXtoY, int toMatchIndexYtoX) {
        if (toMatchIndexXtoY + toMatchIndexYtoX == dlClauseInfo.m_yConstraints.length) {
            this.checkDLClauseForNonblockedXAndMatchedNodes(dlClauseInfo, nonblockedX);
        } else {
            int yNodeIndex;
            ExtensionTable.Retrieval retrieval;
            YConstraint yConstraint = dlClauseInfo.m_yConstraints[toMatchIndexXtoY + toMatchIndexYtoX];
            assert (yConstraint.m_x2yRoles.length != 0 || yConstraint.m_y2xRoles.length != 0);
            int xToYIncrement = 0;
            int yToXIncrement = 0;
            if (yConstraint.m_x2yRoles.length != 0) {
                xToYIncrement = 1;
                retrieval = dlClauseInfo.m_x2yRetrievals[toMatchIndexXtoY];
                retrieval.getBindingsBuffer()[0] = dlClauseInfo.m_x2yRoles[toMatchIndexXtoY];
                retrieval.getBindingsBuffer()[1] = nonblockedX;
                yNodeIndex = 2;
            } else {
                yToXIncrement = 1;
                retrieval = dlClauseInfo.m_y2xRetrievals[toMatchIndexYtoX];
                retrieval.getBindingsBuffer()[0] = dlClauseInfo.m_y2xRoles[toMatchIndexYtoX];
                retrieval.getBindingsBuffer()[2] = nonblockedX;
                yNodeIndex = 1;
            }
            retrieval.open();
            Object[] tupleBuffer = retrieval.getTupleBuffer();
            while (!retrieval.afterLast()) {
                Node nodeY = (Node)tupleBuffer[yNodeIndex];
                if (yConstraint.isSatisfiedViaMirroringY(this.m_extensionManager, nonblockedX, nodeY)) {
                    dlClauseInfo.m_yNodes[toMatchIndexXtoY + toMatchIndexYtoX] = nodeY;
                    this.checkDLClauseForNonblockedXAnyZAndAnyY(dlClauseInfo, nonblockedX, toMatchIndexXtoY + xToYIncrement, toMatchIndexYtoX + yToXIncrement);
                    dlClauseInfo.m_yNodes[toMatchIndexXtoY + toMatchIndexYtoX] = null;
                }
                retrieval.next();
            }
        }
    }

    protected void checkDLClauseForNonblockedXAndMatchedNodes(DLClauseInfo dlClauseInfo, Node nonblockedX) {
        boolean containsAtLeastOneBlockedY = false;
        for (Node y : dlClauseInfo.m_yNodes) {
            if (!y.isBlocked() || ((ValidatedSingleDirectBlockingChecker.ValidatedBlockingObject)y.getBlockingObject()).blockViolatesParentConstraints()) continue;
            containsAtLeastOneBlockedY = true;
            break;
        }
        if (!containsAtLeastOneBlockedY) {
            return;
        }
        for (ConsequenceAtom consequenceAtom : dlClauseInfo.m_consequencesForNonblockedX) {
            if (!consequenceAtom.isSatisfied(this.m_extensionManager, dlClauseInfo, nonblockedX)) continue;
            return;
        }
        for (int i = dlClauseInfo.m_yConstraints.length - 1; i >= 0; --i) {
            YConstraint yConstraint = dlClauseInfo.m_yConstraints[i];
            Node yi = dlClauseInfo.m_yNodes[i];
            for (AtomicConcept c : yConstraint.m_yConcepts) {
                if (!yi.isBlocked() || ((ValidatedSingleDirectBlockingChecker.ValidatedBlockingObject)yi.getBlockingObject()).blockViolatesParentConstraints() || this.m_extensionManager.containsAssertion(c, yi) || !this.m_extensionManager.containsAssertion(c, yi.getBlocker())) continue;
                ((ValidatedSingleDirectBlockingChecker.ValidatedBlockingObject)yi.getBlockingObject()).setBlockViolatesParentConstraints(true);
                return;
            }
        }
        for (ConsequenceAtom consequenceAtom : dlClauseInfo.m_consequencesForNonblockedX) {
            MirroredYConsequenceAtom atom;
            if (!(consequenceAtom instanceof MirroredYConsequenceAtom) || !(atom = (MirroredYConsequenceAtom)consequenceAtom).isSatisfiedNonMirrored(this.m_extensionManager, dlClauseInfo)) continue;
            Node nodeY = dlClauseInfo.m_yNodes[atom.m_yArgumentIndex];
            ((ValidatedSingleDirectBlockingChecker.ValidatedBlockingObject)nodeY.getBlockingObject()).setBlockViolatesParentConstraints(true);
            return;
        }
        assert (false);
    }

    protected static class MirroredYConsequenceAtom
    implements ConsequenceAtom {
        protected final AtomicConcept m_atomicConcept;
        public final int m_yArgumentIndex;

        public MirroredYConsequenceAtom(AtomicConcept atomicConcept, int yArgumentIndex) {
            this.m_atomicConcept = atomicConcept;
            this.m_yArgumentIndex = yArgumentIndex;
        }

        @Override
        public boolean isSatisfied(ExtensionManager extensionManager, DLClauseInfo dlClauseInfo, Node nodeX) {
            Node nodeY = dlClauseInfo.m_yNodes[this.m_yArgumentIndex];
            Node nodeYMirror = nodeY.isBlocked() ? nodeY.getBlocker() : nodeY;
            return extensionManager.containsAssertion(this.m_atomicConcept, nodeYMirror);
        }

        public boolean isSatisfiedNonMirrored(ExtensionManager extensionManager, DLClauseInfo dlClauseInfo) {
            return extensionManager.containsAssertion(this.m_atomicConcept, dlClauseInfo.m_yNodes[this.m_yArgumentIndex]);
        }

        public String toString() {
            return this.m_atomicConcept + "(y_i)";
        }
    }

    protected static class X2YOrY2XConsequenceAtom
    implements ConsequenceAtom {
        protected final AtomicRole m_atomicRole;
        protected final int m_yArgumentIndex;
        protected final boolean m_isX2Y;

        public X2YOrY2XConsequenceAtom(AtomicRole atomicRole, int yArgumentIndex, boolean isX2Y) {
            this.m_atomicRole = atomicRole;
            this.m_yArgumentIndex = yArgumentIndex;
            this.m_isX2Y = isX2Y;
        }

        @Override
        public boolean isSatisfied(ExtensionManager extensionManager, DLClauseInfo dlClauseInfo, Node nodeX) {
            Node nodeY = dlClauseInfo.m_yNodes[this.m_yArgumentIndex];
            Node nodeXReal = nodeY == nodeX.getParent() ? nodeX : dlClauseInfo.m_xNode;
            if (this.m_isX2Y) {
                return extensionManager.containsAssertion(this.m_atomicRole, nodeXReal, nodeY);
            }
            return extensionManager.containsAssertion(this.m_atomicRole, nodeY, nodeXReal);
        }

        public String toString() {
            return this.m_atomicRole + "(" + (this.m_isX2Y ? "x,yi" : "y_i,x") + ")";
        }
    }

    protected static class SimpleConsequenceAtom
    implements ConsequenceAtom {
        protected final Object[] m_assertionBuffer;
        protected final ArgumentType[] m_argumentTypes;
        protected final int[] m_argumentIndexes;

        public SimpleConsequenceAtom(DLPredicate dlPredicate, ArgumentType[] argumentTypes, int[] argumentIndexes) {
            this.m_assertionBuffer = new Object[argumentIndexes.length + 1];
            this.m_assertionBuffer[0] = dlPredicate;
            this.m_argumentTypes = argumentTypes;
            this.m_argumentIndexes = argumentIndexes;
        }

        @Override
        public boolean isSatisfied(ExtensionManager extensionManager, DLClauseInfo dlClauseInfo, Node nodeX) {
            block5 : for (int argumentIndex = this.m_argumentIndexes.length - 1; argumentIndex >= 0; --argumentIndex) {
                switch (this.m_argumentTypes[argumentIndex]) {
                    case XVAR: {
                        this.m_assertionBuffer[argumentIndex + 1] = dlClauseInfo.m_xNode;
                        continue block5;
                    }
                    case YVAR: {
                        this.m_assertionBuffer[argumentIndex + 1] = dlClauseInfo.m_yNodes[this.m_argumentIndexes[argumentIndex]];
                        continue block5;
                    }
                    case ZVAR: {
                        this.m_assertionBuffer[argumentIndex + 1] = dlClauseInfo.m_zNodes[this.m_argumentIndexes[argumentIndex]];
                        break;
                    }
                }
            }
            if (this.m_assertionBuffer[0] instanceof AnnotatedEquality) {
                return this.m_assertionBuffer[1] == this.m_assertionBuffer[2];
            }
            return extensionManager.containsTuple(this.m_assertionBuffer);
        }

        public String toString() {
            String result = "";
            for (Object o : this.m_assertionBuffer) {
                result = result + " " + o.toString();
            }
            return result;
        }
    }

    protected interface ConsequenceAtom {
        boolean isSatisfied(ExtensionManager var1, DLClauseInfo var2, Node var3);
    }

    protected enum ArgumentType {
        XVAR,
        YVAR,
        ZVAR

    }

    protected static class YConstraint {
        protected final AtomicConcept[] m_yConcepts;
        protected final AtomicRole[] m_x2yRoles;
        protected final AtomicRole[] m_y2xRoles;

        public YConstraint(AtomicConcept[] yConcepts, AtomicRole[] x2yRoles, AtomicRole[] y2xRoles) {
            this.m_yConcepts = yConcepts;
            this.m_x2yRoles = x2yRoles;
            this.m_y2xRoles = y2xRoles;
        }

        public boolean isSatisfiedExplicitly(ExtensionManager extensionManager, Node nodeX, Node nodeY) {
            for (AtomicRole x2yRole : this.m_x2yRoles) {
                if (extensionManager.containsAssertion(x2yRole, nodeX, nodeY)) continue;
                return false;
            }
            for (AtomicRole y2xRole : this.m_y2xRoles) {
                if (extensionManager.containsAssertion(y2xRole, nodeY, nodeX)) continue;
                return false;
            }
            for (DLPredicate yConcept : this.m_yConcepts) {
                if (extensionManager.containsAssertion(yConcept, nodeY)) continue;
                return false;
            }
            return true;
        }

        public boolean isSatisfiedViaMirroringY(ExtensionManager extensionManager, Node nodeX, Node nodeY) {
            for (AtomicRole x2yRole : this.m_x2yRoles) {
                if (extensionManager.containsAssertion(x2yRole, nodeX, nodeY)) continue;
                return false;
            }
            for (AtomicRole y2xRole : this.m_y2xRoles) {
                if (extensionManager.containsAssertion(y2xRole, nodeY, nodeX)) continue;
                return false;
            }
            Node nodeYMirror = nodeY.isBlocked() && !((ValidatedSingleDirectBlockingChecker.ValidatedBlockingObject)nodeY.getBlockingObject()).blockViolatesParentConstraints() ? nodeY.getBlocker() : nodeY;
            for (AtomicConcept yConcept : this.m_yConcepts) {
                if (extensionManager.containsAssertion(yConcept, nodeYMirror)) continue;
                return false;
            }
            return true;
        }
    }

    protected static class DLClauseInfo {
        protected final AtomicConcept[] m_xConcepts;
        protected final AtomicRole[] m_x2xRoles;
        protected final YConstraint[] m_yConstraints;
        protected final AtomicConcept[][] m_zConcepts;
        protected final ExtensionTable.Retrieval[] m_x2yRetrievals;
        protected final AtomicRole[] m_x2yRoles;
        protected final ExtensionTable.Retrieval[] m_y2xRetrievals;
        protected final AtomicRole[] m_y2xRoles;
        protected final ExtensionTable.Retrieval[] m_zRetrievals;
        protected final ConsequenceAtom[] m_consequencesForBlockedX;
        protected final ConsequenceAtom[] m_consequencesForNonblockedX;
        protected final DLClause m_dlClause;
        protected Node m_xNode;
        protected final Node[] m_yNodes;
        protected final Variable[] m_yVariables;
        protected final Node[] m_zNodes;
        protected final Variable[] m_zVariables;

        public DLClauseInfo(DLClause dlClause, ExtensionManager extensionManager) {
            this.m_dlClause = dlClause;
            Variable X = Variable.create("X");
            HashSet<AtomicConcept> xConcepts = new HashSet<AtomicConcept>();
            HashSet<AtomicRole> x2xRoles = new HashSet<AtomicRole>();
            HashSet<Variable> ys = new HashSet<Variable>();
            HashMap y2concepts = new HashMap();
            HashMap<Variable,Set<AtomicConcept>> z2concepts = new HashMap<Variable,Set<AtomicConcept>>();
            HashMap x2yRoles = new HashMap();
            HashMap y2xRoles = new HashMap();
            for (int i = 0; i < dlClause.getBodyLength(); ++i) {
                HashSet<AtomicRole> roles;
                Atom atom = dlClause.getBodyAtom(i);
                DLPredicate predicate = atom.getDLPredicate();
                Variable var1 = atom.getArgumentVariable(0);
                if (predicate instanceof AtomicConcept) {
                    Set<AtomicConcept> concepts;
                    if (var1 == X) {
                        xConcepts.add((AtomicConcept)predicate);
                        continue;
                    }
                    if (var1.getName().startsWith("Y")) {
                        ys.add(var1);
                        if (y2concepts.containsKey(var1)) {
                            ((Set)y2concepts.get(var1)).add(predicate);
                            continue;
                        }
                        concepts = new HashSet<AtomicConcept>();
                        concepts.add((AtomicConcept)predicate);
                        y2concepts.put(var1, concepts);
                        continue;
                    }
                    if (var1.getName().startsWith("Z")) {
                        if (z2concepts.containsKey(var1)) {
                            concepts = z2concepts.get(var1);
                            concepts.add((AtomicConcept)predicate);
                            continue;
                        }
                        concepts = new HashSet();
                        concepts.add((AtomicConcept)predicate);
                        z2concepts.put(var1, concepts);
                        continue;
                    }
                    throw new IllegalStateException("Internal error: Clause premise contained variables other than X, Yi, and Zi in a concept atom. ");
                }
                if (!(predicate instanceof AtomicRole)) continue;
                Variable var2 = atom.getArgumentVariable(1);
                if (var1 == X) {
                    if (var2 == X) {
                        x2xRoles.add((AtomicRole)atom.getDLPredicate());
                        continue;
                    }
                    if (var2.getName().startsWith("Y")) {
                        ys.add(var2);
                        if (x2yRoles.containsKey(var2)) {
                            ((Set)x2yRoles.get(var2)).add(predicate);
                            continue;
                        }
                        roles = new HashSet<AtomicRole>();
                        roles.add((AtomicRole)predicate);
                        x2yRoles.put(var2, roles);
                        continue;
                    }
                    throw new IllegalStateException("Internal error: Clause premise contains a role atom with virales other than X and Yi. ");
                }
                if (var2 == X) {
                    if (var1.getName().startsWith("Y")) {
                        ys.add(var1);
                        if (y2xRoles.containsKey(var1)) {
                            ((Set)y2xRoles.get(var1)).add(predicate);
                            continue;
                        }
                        roles = new HashSet();
                        roles.add((AtomicRole)predicate);
                        y2xRoles.put(var1, roles);
                        continue;
                    }
                    throw new IllegalStateException("Internal error: Clause premise contains a role atom with virales other than X and Yi. ");
                }
                throw new IllegalStateException("Internal error: Clause premise contained variables other than X and Yi in a role atom. ");
            }
            AtomicConcept[] noConcepts = new AtomicConcept[]{};
            AtomicRole[] noRoles = new AtomicRole[]{};
            Variable[] noVariables = new Variable[]{};
            this.m_xNode = null;
            this.m_xConcepts = xConcepts.toArray(noConcepts);
            this.m_x2xRoles = x2xRoles.toArray(noRoles);
            this.m_yVariables = ys.toArray(noVariables);
            this.m_yNodes = new Node[this.m_yVariables.length];
            this.m_yConstraints = new YConstraint[this.m_yVariables.length];
            this.m_x2yRetrievals = new ExtensionTable.Retrieval[x2yRoles.size()];
            this.m_x2yRoles = new AtomicRole[x2yRoles.size()];
            this.m_y2xRetrievals = new ExtensionTable.Retrieval[y2xRoles.size()];
            this.m_y2xRoles = new AtomicRole[y2xRoles.size()];
            int i = 0;
            int num_xyRoles = 0;
            for (i = 0; i < this.m_yVariables.length; ++i) {
                Set<AtomicRole> yxRoles;
                Variable y = this.m_yVariables[i];
                Set<AtomicConcept> yConcepts = (Set)y2concepts.get(y);
                Set<AtomicRole> xyRoles = (Set)x2yRoles.get(y);
                if (xyRoles != null) {
                    assert (xyRoles.size() == 1);
                    assert (this.m_y2xRetrievals.length < this.m_x2yRetrievals.length);
                    this.m_x2yRetrievals[num_xyRoles] = extensionManager.getTernaryExtensionTable().createRetrieval(new boolean[]{true, true, false}, ExtensionTable.View.TOTAL);
                    this.m_x2yRoles[num_xyRoles] = xyRoles.iterator().next();
                    ++num_xyRoles;
                }
                if ((yxRoles = (Set)y2xRoles.get(y)) != null) {
                    assert (yxRoles.size() == 1);
                    assert (i - num_xyRoles >= 0);
                    assert (i - num_xyRoles < this.m_y2xRetrievals.length);
                    this.m_y2xRetrievals[i - num_xyRoles] = extensionManager.getTernaryExtensionTable().createRetrieval(new boolean[]{true, false, true}, ExtensionTable.View.TOTAL);
                    this.m_y2xRoles[i - num_xyRoles] = yxRoles.iterator().next();
                }
                this.m_yConstraints[i] = new YConstraint(yConcepts != null ? yConcepts.toArray(noConcepts) : noConcepts, xyRoles != null ? xyRoles.toArray(noRoles) : noRoles, yxRoles != null ? yxRoles.toArray(noRoles) : noRoles);
            }
            this.m_zVariables = z2concepts.keySet().toArray(noVariables);
            this.m_zNodes = new Node[this.m_zVariables.length];
            this.m_zConcepts = new AtomicConcept[this.m_zNodes.length][];
            for (int varIndex = 0; varIndex < this.m_zVariables.length; ++varIndex) {
                this.m_zConcepts[varIndex] = z2concepts.get(this.m_zVariables[varIndex]).toArray(noConcepts);
            }
            this.m_zRetrievals = new ExtensionTable.Retrieval[this.m_zNodes.length];
            for (i = 0; i < this.m_zRetrievals.length; ++i) {
                this.m_zRetrievals[i] = extensionManager.getBinaryExtensionTable().createRetrieval(new boolean[]{true, false}, ExtensionTable.View.TOTAL);
            }
            this.m_consequencesForBlockedX = new ConsequenceAtom[dlClause.getHeadLength()];
            this.m_consequencesForNonblockedX = new ConsequenceAtom[dlClause.getHeadLength()];
            for (i = 0; i < dlClause.getHeadLength(); ++i) {
                int var2Index;
                int argIndex;
                Atom atom = dlClause.getHeadAtom(i);
                DLPredicate predicate = atom.getDLPredicate();
                Variable var1 = atom.getArgumentVariable(0);
                Variable var2 = null;
                if (predicate.getArity() == 2) {
                    var2 = atom.getArgumentVariable(1);
                }
                if (predicate instanceof AtomicConcept) {
                    ArgumentType argType = ArgumentType.YVAR;
                    argIndex = this.getIndexFor(this.m_yVariables, var1);
                    if (argIndex == -1) {
                        assert (var1 == X);
                        argIndex = 0;
                        argType = ArgumentType.XVAR;
                    }
                    this.m_consequencesForBlockedX[i] = new SimpleConsequenceAtom(predicate, new ArgumentType[]{argType}, new int[]{argIndex});
                    if (argType == ArgumentType.XVAR) {
                        this.m_consequencesForNonblockedX[i] = this.m_consequencesForBlockedX[i];
                        continue;
                    }
                    this.m_consequencesForNonblockedX[i] = new MirroredYConsequenceAtom((AtomicConcept)predicate, argIndex);
                    continue;
                }
                if (predicate instanceof AtLeastConcept) {
                    assert (var1 == X);
                    this.m_consequencesForBlockedX[i] = new SimpleConsequenceAtom(predicate, new ArgumentType[]{ArgumentType.XVAR}, new int[]{0});
                    this.m_consequencesForNonblockedX[i] = this.m_consequencesForBlockedX[i];
                    continue;
                }
                if (predicate == Equality.INSTANCE) {
                    Variable tmp;
                    if (var1 == X || var2 == X) {
                        if (var2 == X) {
                            tmp = var1;
                            var1 = var2;
                            var2 = tmp;
                        }
                        assert (var2 != null && var2.getName().startsWith("Z"));
                        int var2Index2 = this.getIndexFor(this.m_zVariables, var2);
                        assert (var1 == X && var2Index2 != -1);
                        this.m_consequencesForBlockedX[i] = new SimpleConsequenceAtom(predicate, new ArgumentType[]{ArgumentType.XVAR, ArgumentType.ZVAR}, new int[]{0, var2Index2});
                        this.m_consequencesForNonblockedX[i] = this.m_consequencesForBlockedX[i];
                        continue;
                    }
                    if (var1.getName().startsWith("Z") || var2.getName().startsWith("Z")) {
                        if (var2.getName().startsWith("Y")) {
                            tmp = var1;
                            var1 = var2;
                            var2 = tmp;
                        }
                        assert (var2.getName().startsWith("Z"));
                        int var2Index3 = this.getIndexFor(this.m_zVariables, var2);
                        int var1Index = this.getIndexFor(this.m_yVariables, var1);
                        assert (var1Index > -1 && var2Index3 > -1);
                        this.m_consequencesForBlockedX[i] = new SimpleConsequenceAtom(predicate, new ArgumentType[]{ArgumentType.YVAR, ArgumentType.ZVAR}, new int[]{var1Index, var2Index3});
                        this.m_consequencesForNonblockedX[i] = this.m_consequencesForBlockedX[i];
                        continue;
                    }
                    if (var1.getName().startsWith("Y") && var2.getName().startsWith("Y")) {
                        int var1Index = this.getIndexFor(this.m_yVariables, var1);
                        var2Index = this.getIndexFor(this.m_yVariables, var2);
                        assert (var1Index > -1 && var2Index > -1);
                        this.m_consequencesForBlockedX[i] = new SimpleConsequenceAtom(predicate, new ArgumentType[]{ArgumentType.YVAR, ArgumentType.YVAR}, new int[]{var1Index, var2Index});
                        this.m_consequencesForNonblockedX[i] = this.m_consequencesForBlockedX[i];
                        continue;
                    }
                    throw new IllegalArgumentException("Internal error: The clause " + dlClause + " is not an HT clause. ");
                }
                if (predicate instanceof AnnotatedEquality) {
                    var1 = atom.getArgumentVariable(0);
                    var2 = atom.getArgumentVariable(1);
                    int var1Index = this.getIndexFor(this.m_yVariables, var1);
                    var2Index = this.getIndexFor(this.m_yVariables, var2);
                    assert (var1Index != -1 && var2Index != -1);
                    this.m_consequencesForBlockedX[i] = new SimpleConsequenceAtom(predicate, new ArgumentType[]{ArgumentType.YVAR, ArgumentType.YVAR, ArgumentType.XVAR}, new int[]{var1Index, var2Index, 0});
                    this.m_consequencesForNonblockedX[i] = this.m_consequencesForBlockedX[i];
                    continue;
                }
                if (!(predicate instanceof AtomicRole)) continue;
                assert (predicate instanceof AtomicRole);
                AtomicRole role = (AtomicRole)predicate;
                if (X == var1 && X == var2) {
                    this.m_consequencesForBlockedX[i] = new SimpleConsequenceAtom(predicate, new ArgumentType[]{ArgumentType.XVAR, ArgumentType.XVAR}, new int[]{0, 0});
                    this.m_consequencesForNonblockedX[i] = this.m_consequencesForBlockedX[i];
                    continue;
                }
                assert (var1 == X || var2 == X);
                argIndex = -1;
                if (var1 == X) {
                    argIndex = this.getIndexFor(this.m_yVariables, var2);
                    if (argIndex == -1) {
                        argIndex = this.getIndexFor(this.m_zVariables, var2);
                        assert (argIndex > -1);
                        this.m_consequencesForBlockedX[i] = new SimpleConsequenceAtom(predicate, new ArgumentType[]{ArgumentType.XVAR, ArgumentType.ZVAR}, new int[]{0, argIndex});
                        this.m_consequencesForNonblockedX[i] = this.m_consequencesForBlockedX[i];
                        continue;
                    }
                    this.m_consequencesForBlockedX[i] = new X2YOrY2XConsequenceAtom(role, argIndex, true);
                    this.m_consequencesForNonblockedX[i] = new SimpleConsequenceAtom(predicate, new ArgumentType[]{ArgumentType.XVAR, ArgumentType.YVAR}, new int[]{0, argIndex});
                    continue;
                }
                argIndex = this.getIndexFor(this.m_yVariables, var1);
                if (argIndex == -1) {
                    argIndex = this.getIndexFor(this.m_zVariables, var1);
                    assert (argIndex > -1);
                    this.m_consequencesForBlockedX[i] = new SimpleConsequenceAtom(predicate, new ArgumentType[]{ArgumentType.ZVAR, ArgumentType.XVAR}, new int[]{argIndex, 0});
                    this.m_consequencesForNonblockedX[i] = this.m_consequencesForBlockedX[i];
                    continue;
                }
                this.m_consequencesForBlockedX[i] = new X2YOrY2XConsequenceAtom(role, argIndex, false);
                this.m_consequencesForNonblockedX[i] = new SimpleConsequenceAtom(predicate, new ArgumentType[]{ArgumentType.YVAR, ArgumentType.XVAR}, new int[]{argIndex, 0});
            }
        }

        public void clear() {
            for (ExtensionTable.Retrieval retrieval : this.m_x2yRetrievals) {
                retrieval.clear();
            }
            for (ExtensionTable.Retrieval retrieval : this.m_y2xRetrievals) {
                retrieval.clear();
            }
            for (ExtensionTable.Retrieval retrieval : this.m_zRetrievals) {
                retrieval.clear();
            }
        }

        protected int getIndexFor(Variable[] variables, Variable variable) {
            for (int index = 0; index < variables.length; ++index) {
                if (variables[index] != variable) continue;
                return index;
            }
            return -1;
        }

        public String toString() {
            return this.m_dlClause.toString();
        }
    }

}

