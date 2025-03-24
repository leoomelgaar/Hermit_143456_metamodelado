package org.semanticweb.HermiT.tableau;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.semanticweb.HermiT.graph.Graph;
import org.semanticweb.HermiT.model.AtLeast;
import org.semanticweb.HermiT.model.AtLeastConcept;
import org.semanticweb.HermiT.model.AtLeastDataRange;
import org.semanticweb.HermiT.model.AtomicRole;
import org.semanticweb.HermiT.model.DLClause;
import org.semanticweb.HermiT.model.ExistentialConcept;
import org.semanticweb.HermiT.model.Inequality;
import org.semanticweb.HermiT.model.InverseRole;
import org.semanticweb.HermiT.model.Role;

public final class ExistentialExpansionManager
implements Serializable {
    private static final long serialVersionUID = 4794168582297181623L;
    private final Tableau m_tableau;
    private final ExtensionManager m_extensionManager;
    private final TupleTable m_expandedExistentials;
    private final Object[] m_auxiliaryTuple;
    private final List<Node> m_auxiliaryNodes;
    private final ExtensionTable.Retrieval m_ternaryExtensionTableSearch01Bound;
    private final ExtensionTable.Retrieval m_ternaryExtensionTableSearch02Bound;
    private final Map<Role, Role[]> m_functionalRoles;
    private final UnionDependencySet m_binaryUnionDependencySet;
    private int[] m_indicesByBranchingPoint;

    public ExistentialExpansionManager(Tableau tableau) {
        this.m_tableau = tableau;
        this.m_extensionManager = this.m_tableau.m_extensionManager;
        this.m_expandedExistentials = new TupleTable(2);
        this.m_auxiliaryTuple = new Object[2];
        this.m_auxiliaryNodes = new ArrayList<Node>();
        this.m_ternaryExtensionTableSearch01Bound = this.m_extensionManager.getTernaryExtensionTable().createRetrieval(new boolean[]{true, true, false}, ExtensionTable.View.TOTAL);
        this.m_ternaryExtensionTableSearch02Bound = this.m_extensionManager.getTernaryExtensionTable().createRetrieval(new boolean[]{true, false, true}, ExtensionTable.View.TOTAL);
        this.m_functionalRoles = new HashMap<Role, Role[]>();
        this.updateFunctionalRoles();
        this.m_binaryUnionDependencySet = new UnionDependencySet(2);
        this.m_indicesByBranchingPoint = new int[2];
    }

    private void updateFunctionalRoles() {
        Graph<Role> superRoleGraph = new Graph();
        HashSet<Role> functionalRoles = new HashSet<Role>();
        ExistentialExpansionManager.loadDLClausesIntoGraph(this.m_tableau.m_permanentDLOntology.getDLClauses(), superRoleGraph, functionalRoles);
        for (Role role : superRoleGraph.getElements()) {
            superRoleGraph.addEdge(role, role);
            superRoleGraph.addEdge(role.getInverse(), role.getInverse());
        }
        superRoleGraph.transitivelyClose();
        Graph<Role> subRoleGraph = superRoleGraph.getInverse();
        this.m_functionalRoles.clear();
        for (Role role : superRoleGraph.getElements()) {
            HashSet<Role> relevantRoles = new HashSet<Role>();
            Set<Role> allSuperroles = superRoleGraph.getSuccessors(role);
            for (Role superrole : allSuperroles) {
                if (!functionalRoles.contains(superrole)) continue;
                relevantRoles.addAll(subRoleGraph.getSuccessors(superrole));
            }
            if (relevantRoles.isEmpty()) continue;
            Role[] relevantRolesArray = new Role[relevantRoles.size()];
            relevantRoles.toArray(relevantRolesArray);
            this.m_functionalRoles.put(role, relevantRolesArray);
        }
    }

    private static void loadDLClausesIntoGraph(Set<DLClause> dlClauses, Graph<Role> superRoleGraph, Set<Role> functionalRoles) {
        for (DLClause dlClause : dlClauses) {
            AtomicRole atomicRole;
            AtomicRole subrole;
            AtomicRole superrole;
            if (dlClause.isAtomicRoleInclusion()) {
                subrole = (AtomicRole)dlClause.getBodyAtom(0).getDLPredicate();
                superrole = (AtomicRole)dlClause.getHeadAtom(0).getDLPredicate();
                superRoleGraph.addEdge(subrole, superrole);
                superRoleGraph.addEdge(subrole.getInverse(), superrole.getInverse());
                continue;
            }
            if (dlClause.isAtomicRoleInverseInclusion()) {
                subrole = (AtomicRole)dlClause.getBodyAtom(0).getDLPredicate();
                superrole = (AtomicRole)dlClause.getHeadAtom(0).getDLPredicate();
                superRoleGraph.addEdge(subrole, superrole.getInverse());
                superRoleGraph.addEdge(subrole.getInverse(), superrole);
                continue;
            }
            if (dlClause.isFunctionalityAxiom()) {
                atomicRole = (AtomicRole)dlClause.getBodyAtom(0).getDLPredicate();
                functionalRoles.add(atomicRole);
                continue;
            }
            if (!dlClause.isInverseFunctionalityAxiom()) continue;
            atomicRole = (AtomicRole)dlClause.getBodyAtom(0).getDLPredicate();
            functionalRoles.add(atomicRole.getInverse());
        }
    }

    public void markExistentialProcessed(ExistentialConcept existentialConcept, Node forNode) {
        this.m_auxiliaryTuple[0] = existentialConcept;
        this.m_auxiliaryTuple[1] = forNode;
        this.m_expandedExistentials.addTuple(this.m_auxiliaryTuple);
        forNode.removeFromUnprocessedExistentials(existentialConcept);
    }

    public void branchingPointPushed() {
        int start = this.m_tableau.getCurrentBranchingPoint().m_level;
        int requiredSize = start + 1;
        if (requiredSize > this.m_indicesByBranchingPoint.length) {
            int newSize = this.m_indicesByBranchingPoint.length * 3 / 2;
            while (requiredSize > newSize) {
                newSize = newSize * 3 / 2;
            }
            int[] newIndicesByBranchingPoint = new int[newSize];
            System.arraycopy(this.m_indicesByBranchingPoint, 0, newIndicesByBranchingPoint, 0, this.m_indicesByBranchingPoint.length);
            this.m_indicesByBranchingPoint = newIndicesByBranchingPoint;
        }
        this.m_indicesByBranchingPoint[start] = this.m_expandedExistentials.getFirstFreeTupleIndex();
    }

    public void backtrack() {
        int newFirstFreeTupleIndex = this.m_indicesByBranchingPoint[this.m_tableau.getCurrentBranchingPoint().m_level];
        for (int tupleIndex = this.m_expandedExistentials.getFirstFreeTupleIndex() - 1; tupleIndex >= newFirstFreeTupleIndex; --tupleIndex) {
            this.m_expandedExistentials.retrieveTuple(this.m_auxiliaryTuple, tupleIndex);
            ExistentialConcept existentialConcept = (ExistentialConcept)this.m_auxiliaryTuple[0];
            Node forNode = (Node)this.m_auxiliaryTuple[1];
            forNode.addToUnprocessedExistentials(existentialConcept);
        }
        this.m_expandedExistentials.truncate(newFirstFreeTupleIndex);
    }

    public void clear() {
        this.m_expandedExistentials.clear();
        this.m_auxiliaryTuple[0] = null;
        this.m_auxiliaryTuple[1] = null;
        this.m_ternaryExtensionTableSearch01Bound.clear();
        this.m_ternaryExtensionTableSearch02Bound.clear();
        this.m_binaryUnionDependencySet.m_dependencySets[0] = null;
        this.m_binaryUnionDependencySet.m_dependencySets[1] = null;
    }

    public boolean tryFunctionalExpansion(AtLeast atLeast, Node forNode) {
        if (atLeast.getNumber() == 1) {
            if (this.getFunctionalExpansionNode(atLeast.getOnRole(), forNode, this.m_auxiliaryTuple)) {
                if (this.m_tableau.m_tableauMonitor != null) {
                    this.m_tableau.m_tableauMonitor.existentialExpansionStarted(atLeast, forNode);
                }
                Node functionalityNode = (Node)this.m_auxiliaryTuple[0];
                this.m_binaryUnionDependencySet.m_dependencySets[0] = this.m_extensionManager.getConceptAssertionDependencySet(atLeast, forNode);
                this.m_binaryUnionDependencySet.m_dependencySets[1] = (DependencySet)this.m_auxiliaryTuple[1];
                this.m_extensionManager.addRoleAssertion(atLeast.getOnRole(), forNode, functionalityNode, this.m_binaryUnionDependencySet, true);
                if (atLeast instanceof AtLeastConcept) {
                    this.m_extensionManager.addConceptAssertion(((AtLeastConcept)atLeast).getToConcept(), functionalityNode, this.m_binaryUnionDependencySet, true);
                } else {
                    this.m_extensionManager.addDataRangeAssertion(((AtLeastDataRange)atLeast).getToDataRange(), functionalityNode, this.m_binaryUnionDependencySet, true);
                }
                if (this.m_tableau.m_tableauMonitor != null) {
                    this.m_tableau.m_tableauMonitor.existentialExpansionFinished(atLeast, forNode);
                }
                return true;
            }
        } else if (atLeast.getNumber() > 1 && this.m_functionalRoles.containsKey(atLeast.getOnRole())) {
            if (this.m_tableau.m_tableauMonitor != null) {
                this.m_tableau.m_tableauMonitor.existentialExpansionStarted(atLeast, forNode);
            }
            DependencySet existentialDependencySet = this.m_extensionManager.getConceptAssertionDependencySet(atLeast, forNode);
            this.m_extensionManager.setClash(existentialDependencySet);
            if (this.m_tableau.m_tableauMonitor != null) {
                this.m_tableau.m_tableauMonitor.existentialExpansionFinished(atLeast, forNode);
            }
            return true;
        }
        return false;
    }

    private boolean getFunctionalExpansionNode(Role role, Node forNode, Object[] result) {
        Role[] relevantRoles = this.m_functionalRoles.get(role);
        if (relevantRoles != null) {
            for (Role relevantRole : relevantRoles) {
                ExtensionTable.Retrieval retrieval;
                int toNodeIndex;
                if (relevantRole instanceof AtomicRole) {
                    retrieval = this.m_ternaryExtensionTableSearch01Bound;
                    retrieval.getBindingsBuffer()[0] = relevantRole;
                    retrieval.getBindingsBuffer()[1] = forNode;
                    toNodeIndex = 2;
                } else {
                    retrieval = this.m_ternaryExtensionTableSearch02Bound;
                    retrieval.getBindingsBuffer()[0] = ((InverseRole)relevantRole).getInverseOf();
                    retrieval.getBindingsBuffer()[2] = forNode;
                    toNodeIndex = 1;
                }
                retrieval.open();
                if (retrieval.afterLast()) continue;
                result[0] = retrieval.getTupleBuffer()[toNodeIndex];
                result[1] = retrieval.getDependencySet();
                return true;
            }
        }
        return false;
    }

    public void doNormalExpansion(AtLeastConcept atLeastConcept, Node forNode) {
        if (this.m_tableau.m_tableauMonitor != null) {
            this.m_tableau.m_tableauMonitor.existentialExpansionStarted(atLeastConcept, forNode);
        }
        DependencySet existentialDependencySet = this.m_extensionManager.getConceptAssertionDependencySet(atLeastConcept, forNode);
        int cardinality = atLeastConcept.getNumber();
        if (cardinality == 1) {
            Node newNode = this.m_tableau.createNewTreeNode(existentialDependencySet, forNode);
            this.m_extensionManager.addRoleAssertion(atLeastConcept.getOnRole(), forNode, newNode, existentialDependencySet, true);
            this.m_extensionManager.addConceptAssertion(atLeastConcept.getToConcept(), newNode, existentialDependencySet, true);
        } else {
            this.m_auxiliaryNodes.clear();
            for (int index = 0; index < cardinality; ++index) {
                Node newNode = this.m_tableau.createNewTreeNode(existentialDependencySet, forNode);
                this.m_extensionManager.addRoleAssertion(atLeastConcept.getOnRole(), forNode, newNode, existentialDependencySet, true);
                this.m_extensionManager.addConceptAssertion(atLeastConcept.getToConcept(), newNode, existentialDependencySet, true);
                this.m_auxiliaryNodes.add(newNode);
            }
            for (int outerIndex = 0; outerIndex < cardinality; ++outerIndex) {
                Node outerNode = this.m_auxiliaryNodes.get(outerIndex);
                for (int innerIndex = outerIndex + 1; innerIndex < cardinality; ++innerIndex) {
                    this.m_extensionManager.addAssertion(Inequality.INSTANCE, outerNode, this.m_auxiliaryNodes.get(innerIndex), existentialDependencySet, true);
                }
            }
            this.m_auxiliaryNodes.clear();
        }
        if (this.m_tableau.m_tableauMonitor != null) {
            this.m_tableau.m_tableauMonitor.existentialExpansionFinished(atLeastConcept, forNode);
        }
    }

    public void doNormalExpansion(AtLeastDataRange atLeastDataRange, Node forNode) {
        if (this.m_tableau.m_tableauMonitor != null) {
            this.m_tableau.m_tableauMonitor.existentialExpansionStarted(atLeastDataRange, forNode);
        }
        DependencySet existentialDependencySet = this.m_extensionManager.getConceptAssertionDependencySet(atLeastDataRange, forNode);
        int cardinality = atLeastDataRange.getNumber();
        if (cardinality == 1) {
            Node newNode = this.m_tableau.createNewConcreteNode(existentialDependencySet, forNode);
            this.m_extensionManager.addRoleAssertion(atLeastDataRange.getOnRole(), forNode, newNode, existentialDependencySet, true);
            this.m_extensionManager.addDataRangeAssertion(atLeastDataRange.getToDataRange(), newNode, existentialDependencySet, true);
        } else {
            this.m_auxiliaryNodes.clear();
            for (int index = 0; index < cardinality; ++index) {
                Node newNode = this.m_tableau.createNewConcreteNode(existentialDependencySet, forNode);
                this.m_extensionManager.addRoleAssertion(atLeastDataRange.getOnRole(), forNode, newNode, existentialDependencySet, true);
                this.m_extensionManager.addDataRangeAssertion(atLeastDataRange.getToDataRange(), newNode, existentialDependencySet, true);
                this.m_auxiliaryNodes.add(newNode);
            }
            for (int outerIndex = 0; outerIndex < cardinality; ++outerIndex) {
                Node outerNode = this.m_auxiliaryNodes.get(outerIndex);
                for (int innerIndex = outerIndex + 1; innerIndex < cardinality; ++innerIndex) {
                    this.m_extensionManager.addAssertion(Inequality.INSTANCE, outerNode, this.m_auxiliaryNodes.get(innerIndex), existentialDependencySet, true);
                }
            }
            this.m_auxiliaryNodes.clear();
        }
        if (this.m_tableau.m_tableauMonitor != null) {
            this.m_tableau.m_tableauMonitor.existentialExpansionFinished(atLeastDataRange, forNode);
        }
    }

    public void expand(AtLeast atLeast, Node forNode) {
        if (!this.tryFunctionalExpansion(atLeast, forNode)) {
            if (atLeast instanceof AtLeastConcept) {
                this.doNormalExpansion((AtLeastConcept)atLeast, forNode);
            } else {
                this.doNormalExpansion((AtLeastDataRange)atLeast, forNode);
            }
        }
    }
}

