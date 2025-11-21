package org.semanticweb.HermiT.tableau;

import org.semanticweb.HermiT.model.*;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;

import java.util.*;

public final class MetamodellingManager {
    private final Tableau m_tableau;
    public Map<OWLClassExpression, Map<OWLClassExpression, Atom>> inequalityMetamodellingPairs;
    List<String> defAssertions;
    Map<Integer, Individual> nodeToMetaIndividual;
    List<Node> metamodellingNodes;
    Map<Integer, Individual> mapNodeIndividual;
    Map<Integer, Node> mapNodeIdtoNodes;
    Map<Integer, List<Integer>> createdDisjunction;
    Map<String, List<Map.Entry<Node, Node>>> closeMetaRuleDisjunctionsMap;
    public Map<Integer,List<Integer>> differentIndividualsMap;
    Map<Integer,Map<Integer, List<String>>> nodeProperties;
    List<DLClause> dlClausesAddedByMetamodelling;

    private MetamodellingRuleEngine ruleEngine;

    public MetamodellingManager(Tableau tableau) {
        this.m_tableau = tableau;
        inequalityMetamodellingPairs = new HashMap<OWLClassExpression, Map<OWLClassExpression, Atom>>();
        defAssertions = new ArrayList<String>();
        nodeToMetaIndividual = new HashMap<>();
        metamodellingNodes = new ArrayList<>();
        mapNodeIndividual = new HashMap<>();
        mapNodeIdtoNodes = new HashMap<>();
        createdDisjunction = new HashMap<>();
        closeMetaRuleDisjunctionsMap = new HashMap<>();
        differentIndividualsMap = new HashMap<>();
        nodeProperties = new HashMap<>();
        dlClausesAddedByMetamodelling = new ArrayList<DLClause>();

        initializeRuleEngine();
    }

    public MetamodellingManager(MetamodellingManager other) {
        this.m_tableau = other.m_tableau;
        this.inequalityMetamodellingPairs = new HashMap<>(other.inequalityMetamodellingPairs);
        this.defAssertions = new ArrayList<>(other.defAssertions);
        this.nodeToMetaIndividual = new HashMap<>(other.nodeToMetaIndividual);
        this.metamodellingNodes = new ArrayList<>(other.metamodellingNodes);
        this.mapNodeIndividual = new HashMap<>(other.mapNodeIndividual);
        this.mapNodeIdtoNodes = new HashMap<>(other.mapNodeIdtoNodes);
        this.createdDisjunction = new HashMap<>(other.createdDisjunction);
        this.closeMetaRuleDisjunctionsMap = new HashMap<>(other.closeMetaRuleDisjunctionsMap);
        this.differentIndividualsMap = new HashMap<>(other.differentIndividualsMap);
        this.nodeProperties = new HashMap<>(other.nodeProperties);
        this.ruleEngine = other.ruleEngine;
        this.dlClausesAddedByMetamodelling = new ArrayList<DLClause>(other.dlClausesAddedByMetamodelling);
    }

    private void initializeRuleEngine() {
        this.ruleEngine = new MetamodellingRuleEngine();
        this.ruleEngine.addRule(new org.semanticweb.HermiT.tableau.rules.EqualityMetamodellingRule());
        this.ruleEngine.addRule(new org.semanticweb.HermiT.tableau.rules.InequalityMetamodellingRule());
        this.ruleEngine.addRule(new org.semanticweb.HermiT.tableau.rules.CloseMetamodellingRule());
    }

    public boolean checkAllMetamodellingRules() {
        return ruleEngine.applyAllRules(m_tableau);
    }

    public boolean checkEqualityAndInequalityMetamodellingRules() {
        boolean anyApplied = false;

        if (ruleEngine.applyRule("Equality Metamodelling Rule", m_tableau)) {
            anyApplied = true;
        }
        if (ruleEngine.applyRule("Inequality Metamodelling Rule", m_tableau)) {
            anyApplied = true;
        }
        return anyApplied;
    }

    public boolean checkCloseMetamodellingRule() {
        return ruleEngine.applyRule("Close Metamodelling Rule", m_tableau);
    }

    boolean checkPropertyNegation() {
        boolean findClash = false;
        for (Node node0 : this.metamodellingNodes) {
            for (Node node1 : this.metamodellingNodes) {
                List<String> propertiesRForEqNodes = getObjectProperties(node0, node1);
                for (String propertyR : propertiesRForEqNodes) {
                    for (String propertyIter : propertiesRForEqNodes) {
                        if (propertyIter.equals("<~" + propertyR.substring(1))) {
                            DependencySet clashDependencySet = this.m_tableau.m_dependencySetFactory.getActualDependencySet();
                            this.m_tableau.m_extensionManager.setClash(clashDependencySet);
                            findClash = true;
                            break;
                        }
                    }
                }
            }
        }
        return findClash;
    }

    private List<String> getObjectProperties(Node node0, Node node1) {
        Set<String> objectProperties = new HashSet<String>();
        if (this.m_tableau.m_metamodellingManager.nodeProperties.containsKey(node0.getCanonicalNode().m_nodeID)) {
            if (this.m_tableau.m_metamodellingManager.nodeProperties.get(node0.getCanonicalNode().m_nodeID).containsKey(node1.getCanonicalNode().m_nodeID)) {
                objectProperties.addAll(this.m_tableau.m_metamodellingManager.nodeProperties.get(node0.getCanonicalNode().m_nodeID).get(node1.getCanonicalNode().m_nodeID));
            }
        }
        if (this.m_tableau.m_metamodellingManager.nodeProperties.containsKey(node0.getCanonicalNode().m_nodeID)) {
            if (this.m_tableau.m_metamodellingManager.nodeProperties.get(node0.getCanonicalNode().m_nodeID).containsKey(node1.m_nodeID)) {
                objectProperties.addAll(this.m_tableau.m_metamodellingManager.nodeProperties.get(node0.getCanonicalNode().m_nodeID).get(node1.m_nodeID));
            }
        }
        if (this.m_tableau.m_metamodellingManager.nodeProperties.containsKey(node0.m_nodeID)) {
            if (this.m_tableau.m_metamodellingManager.nodeProperties.get(node0.m_nodeID).containsKey(node1.getCanonicalNode().m_nodeID)) {
                objectProperties.addAll(this.m_tableau.m_metamodellingManager.nodeProperties.get(node0.m_nodeID).get(node1.getCanonicalNode().m_nodeID));
            }
        }
        if (this.m_tableau.m_metamodellingManager.nodeProperties.containsKey(node0.m_nodeID)) {
            if (this.m_tableau.m_metamodellingManager.nodeProperties.get(node0.m_nodeID).containsKey(node1.m_nodeID)) {
                objectProperties.addAll(this.m_tableau.m_metamodellingManager.nodeProperties.get(node0.m_nodeID).get(node1.m_nodeID));
            }
        }
        return new ArrayList<String>(objectProperties);
    }

    public Node getMetamodellingNodeFromIndividual(OWLIndividual individual) {
        int nodeId = -1;
        for (int metamodellingNodeId : this.nodeToMetaIndividual.keySet()) {
            if (this.nodeToMetaIndividual.get(metamodellingNodeId).toString().equals(individual.toString())) {
                nodeId = metamodellingNodeId;
            }
        }
        for (Node metamodellingNode : this.metamodellingNodes) {
            if (nodeId == metamodellingNode.m_nodeID) {
                return metamodellingNode;
            }
        }
        return null;
    }

    public boolean areDifferentIndividual(Node node1, Node node2) {
        if (this.differentIndividualsMap.containsKey(node1.m_nodeID)) {
            if (this.differentIndividualsMap.get(node1.m_nodeID).contains(node2.m_nodeID) || this.differentIndividualsMap.get(node1.m_nodeID).contains(node2.getCanonicalNode().m_nodeID)) {
                return true;
            }
        }
        if (this.differentIndividualsMap.containsKey(node2.m_nodeID)) {
            if (this.differentIndividualsMap.get(node2.m_nodeID).contains(node1.m_nodeID) || this.differentIndividualsMap.get(node2.m_nodeID).contains(node1.getCanonicalNode().m_nodeID)) {
                return true;
            }
        }
        if (this.differentIndividualsMap.containsKey(node1.getCanonicalNode().m_nodeID)) {
            if (this.differentIndividualsMap.get(node1.getCanonicalNode().m_nodeID).contains(node2.m_nodeID) || this.differentIndividualsMap.get(node1.getCanonicalNode().m_nodeID).contains(node2.getCanonicalNode().m_nodeID)) {
                return true;
            }
        }
        if (this.differentIndividualsMap.containsKey(node2.getCanonicalNode().m_nodeID)) {
            return this.differentIndividualsMap.get(node2.getCanonicalNode().m_nodeID).contains(node1.m_nodeID) || this.differentIndividualsMap.get(node2.getCanonicalNode().m_nodeID).contains(node1.getCanonicalNode().m_nodeID);
        }
        return false;
    }

    public boolean areSameIndividual(Node node1, Node node2) {
        if ((node1.m_nodeID == node2.m_nodeID) || (node1.getCanonicalNode() == node2.getCanonicalNode())) return true;
        return (node1.isMerged() && node1.m_mergedInto == node2) || (node2.isMerged() && node2.m_mergedInto == node1);
    }

    public void addDifferentIndividual(Node node1, Node node2) {
        this.differentIndividualsMap.putIfAbsent(node1.m_nodeID, new ArrayList<Integer>());
        this.differentIndividualsMap.get(node1.m_nodeID).add(node2.m_nodeID);
    }

    public void removeDifferentIndividual(Node node1, Node node2) {
        if (this.differentIndividualsMap.containsKey(node1.m_nodeID)) {
            this.differentIndividualsMap.get(node1.m_nodeID).remove(Integer.valueOf(node2.m_nodeID));
        }
    }

    public Set<Integer> getDifferentIndividualsForNode(Node node) {
        if (this.differentIndividualsMap.containsKey(node.m_nodeID)) {
            return new HashSet<>(this.differentIndividualsMap.get(node.m_nodeID));
        }
        return new HashSet<>();
    }

    public Set<Integer> getAllDifferentIndividualsKeys() {
        return this.differentIndividualsMap.keySet();
    }

    public boolean findAndRemoveDifferentIndividual(Node node0, Node node1) {
        if (this.differentIndividualsMap.containsKey(node0.m_nodeID)) {
            return this.differentIndividualsMap.get(node0.m_nodeID).remove(Integer.valueOf(node1.m_nodeID));
        }
        return false;
    }

    public Set<Integer> getDifferentIndividualsForNodeId(int nodeId) {
        if (this.differentIndividualsMap.containsKey(nodeId)) {
            return new HashSet<>(this.differentIndividualsMap.get(nodeId));
        }
        return new HashSet<>();
    }

    public void recordAddedClause(DLClause dlClause) {
        this.dlClausesAddedByMetamodelling.add(dlClause);
    }

    public void revertMetamodellingEffects() {
        if (this.dlClausesAddedByMetamodelling.isEmpty()) {
            return;
        }

        Set<DLClause> dlClauses = this.m_tableau.getPermanentDLOntology().getDLClauses();
        dlClauses.removeAll(this.dlClausesAddedByMetamodelling);


        this.dlClausesAddedByMetamodelling.clear();
        this.defAssertions.clear();
        this.inequalityMetamodellingPairs.clear();
        this.createdDisjunction.clear();
        this.closeMetaRuleDisjunctionsMap.clear();

        this.m_tableau.resetPermanentHyperresolutionManager();
    }
}
