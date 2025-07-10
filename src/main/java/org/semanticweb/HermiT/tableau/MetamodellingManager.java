package org.semanticweb.HermiT.tableau;

import org.semanticweb.HermiT.model.*;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLMetaRuleAxiom;
import org.semanticweb.owlapi.model.OWLMetamodellingAxiom;

import java.util.*;

public final class MetamodellingManager {

    private final Tableau m_tableau;
    Map<OWLClassExpression, Map<OWLClassExpression, Atom>> inequalityMetamodellingPairs;
    List<String> defAssertions;
    Map<Integer, Individual> nodeToMetaIndividual;
    List<Node> metamodellingNodes;
    Map<Integer, Individual> mapNodeIndividual;
    Map<Integer, Node> mapNodeIdtoNodes;
    Map<Integer, List<Integer>> createdDisjunction;
    Map<String, List<Map.Entry<Node, Node>>> closeMetaRuleDisjunctionsMap;
    Map<Integer,List<Integer>> differentIndividualsMap;
    Map<Integer,Map<Integer, List<String>>> nodeProperties;

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
    }

    //	Entra con el mismo individuo.
	//	En cada nodo recibe
    public boolean checkEqualMetamodellingRuleIteration(Node node0, Node node1) {
        System.out.println("  --- checkEqualMetamodellingRuleIteration START ---");
        System.out.println("  node0 ID: " + node0.m_nodeID + ", node1 ID: " + node1.m_nodeID);
        
        List<OWLClassExpression> node0Classes = MetamodellingAxiomHelper.getMetamodellingClassesByIndividual(this.m_tableau.getNodeToMetaIndividual().get(node0.getNodeID()), this.m_tableau.getPermanentDLOntology());
        List<OWLClassExpression> node1Classes = MetamodellingAxiomHelper.getMetamodellingClassesByIndividual(this.m_tableau.getNodeToMetaIndividual().get(node1.getNodeID()), this.m_tableau.getPermanentDLOntology());
        
        System.out.println("  node0 individual: " + this.m_tableau.getNodeToMetaIndividual().get(node0.getNodeID()));
        System.out.println("  node1 individual: " + this.m_tableau.getNodeToMetaIndividual().get(node1.getNodeID()));
        System.out.println("  node0 classes: " + node0Classes);
        System.out.println("  node1 classes: " + node1Classes);
        
        if (node0Classes.isEmpty() || node1Classes.isEmpty()) {
            System.out.println("  One of the class lists is empty, returning false");
            return false;
        }

        for (OWLClassExpression node0Class : node0Classes) {
            for (OWLClassExpression node1Class : node1Classes) {
                System.out.println("  Comparing classes: " + node0Class + " vs " + node1Class);
                
                if (node1Class == node0Class) {
                    System.out.println("  Classes are the same, breaking");
                    break;
                }

                boolean isNode1ClassContainedInNode0Class = MetamodellingAxiomHelper.containsSubClassOfAxiom(node0Class, node1Class, this.m_tableau.getPermanentDLOntology());
                boolean isNode0ClassContainedInNode1Class = MetamodellingAxiomHelper.containsSubClassOfAxiom(node1Class, node0Class, this.m_tableau.getPermanentDLOntology());
                
                System.out.println("  node1Class ⊆ node0Class: " + isNode1ClassContainedInNode0Class);
                System.out.println("  node0Class ⊆ node1Class: " + isNode0ClassContainedInNode1Class);
                
                if (!isNode1ClassContainedInNode0Class || !isNode0ClassContainedInNode1Class) {
                    // CRITICAL FIX: Check if classes are disjoint before adding equivalence axioms
                    System.out.println("  Missing subsumption, checking for disjointness...");
                    boolean areDisjoint = MetamodellingAxiomHelper.areClassesDisjoint(node0Class, node1Class, this.m_tableau.getPermanentDLOntology(), this.m_tableau);
                    System.out.println("  Are classes disjoint: " + areDisjoint);
                    
                    if (areDisjoint) {
                        System.out.println("  INCONSISTENCY DETECTED: Individual has disjoint classes!");
                        System.out.println("  Creating clash...");
                        DependencySet clashDependencySet = this.m_tableau.m_dependencySetFactory.getActualDependencySet();
                        this.m_tableau.m_extensionManager.setClash(clashDependencySet);
                        System.out.println("  --- checkEqualMetamodellingRuleIteration END - returning true (clash) ---");
                        return true;
                    } else {
                        System.out.println("  Classes are not disjoint, adding SubClassOf axioms");
                    MetamodellingAxiomHelper.addSubClassOfAxioms(node0Class, node1Class, this.m_tableau.getPermanentDLOntology(), this.m_tableau);
                    System.out.println("  --- checkEqualMetamodellingRuleIteration END - returning true ---");
                    return true;
                    }
                }
            }
        }
        
        System.out.println("  --- checkEqualMetamodellingRuleIteration END - returning false ---");
        return false;
    }

    public boolean checkInequalityMetamodellingRuleIteration(Node node0, Node node1) {
        List<OWLClassExpression> node0Classes = MetamodellingAxiomHelper.getMetamodellingClassesByIndividual(this.m_tableau.getNodeToMetaIndividual().get(node0.getNodeID()), this.m_tableau.getPermanentDLOntology());
        List<OWLClassExpression> node1Classes = MetamodellingAxiomHelper.getMetamodellingClassesByIndividual(this.m_tableau.getNodeToMetaIndividual().get(node1.getNodeID()), this.m_tableau.getPermanentDLOntology());
        if (!node0Classes.isEmpty() && !node1Classes.isEmpty()) {
            for (OWLClassExpression node0Class : node0Classes) {
                for (OWLClassExpression node1Class : node1Classes) {
                    if (node1Class != node0Class) {
                        Atom def0 = null;
                        if (this.inequalityMetamodellingPairs.containsKey(node1Class) && this.inequalityMetamodellingPairs.get(node1Class).containsKey(node0Class)) {
                            def0 = this.inequalityMetamodellingPairs.get(node1Class).get(node0Class);
                        }
                        if (this.inequalityMetamodellingPairs.containsKey(node0Class) && this.inequalityMetamodellingPairs.get(node0Class).containsKey(node1Class)) {
                            def0 = this.inequalityMetamodellingPairs.get(node0Class).get(node1Class);
                        }
                        if (def0 == null || (def0 != null && !this.m_tableau.containsClassAssertion(def0.getDLPredicate().toString()))) {
                            MetamodellingAxiomHelper.addInequalityMetamodellingRuleAxiom(node0Class, node1Class, this.m_tableau.getPermanentDLOntology(), this.m_tableau, def0, this.inequalityMetamodellingPairs);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean checkCloseMetamodellingRuleIteration(Node node0, Node node1) {
        // Obs: esta es la close-rule, no la close-meta-rule, tiene el nombre confuso

        Node node0Equivalent = node0.getCanonicalNode();
        Node node1Equivalent = node1.getCanonicalNode();
        if (!this.m_tableau.areDifferentIndividual(node0Equivalent, node1Equivalent) && !this.m_tableau.areSameIndividual(node0Equivalent, node1Equivalent) && !this.m_tableau.alreadyCreateDisjunction(node0Equivalent, node1Equivalent)) {
            Atom eqAtom = Atom.create(Equality.INSTANCE, this.m_tableau.getMapNodeIndividual().get(node0Equivalent.getNodeID()), this.m_tableau.getMapNodeIndividual().get(node1Equivalent.getNodeID()));
            DLPredicate equalityPredicate = eqAtom.getDLPredicate();
            Atom ineqAtom = Atom.create(Inequality.INSTANCE, this.m_tableau.getMapNodeIndividual().get(node0Equivalent.getNodeID()), this.m_tableau.getMapNodeIndividual().get(node1Equivalent.getNodeID()));
            DLPredicate inequalityPredicate = ineqAtom.getDLPredicate();
            DLPredicate[] dlPredicates = new DLPredicate[]{equalityPredicate, inequalityPredicate};
            int hashCode = 0;
            for (int disjunctIndex = 0; disjunctIndex < dlPredicates.length; ++disjunctIndex) {
                hashCode = hashCode * 7 + dlPredicates[disjunctIndex].hashCode();
            }
            GroundDisjunctionHeader gdh = new GroundDisjunctionHeader(dlPredicates, hashCode, null);
            DependencySet dependencySet = this.m_tableau.m_dependencySetFactory.getActualDependencySet();
            GroundDisjunction groundDisjunction = new GroundDisjunction(this.m_tableau, gdh, new Node[]{node0Equivalent, node1Equivalent, node0Equivalent, node1Equivalent}, new boolean[]{true, true}, dependencySet);
            if (!this.m_tableau.alreadyCreateDisjunction(node0Equivalent, node1Equivalent) && !groundDisjunction.isSatisfied(this.m_tableau)) {
                this.m_tableau.addGroundDisjunction(groundDisjunction);
                this.m_tableau.addCreatedDisjuntcion(node0Equivalent, node1Equivalent);
                return true;
            }
        }
        return false;
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

    boolean checkCloseMetaRule() {
        for (Node node0 : this.metamodellingNodes) {
            for (Node node1 : this.metamodellingNodes) {
                Node node0Eq = node0.getCanonicalNode();
                Node node1Eq = node1.getCanonicalNode();
                List<String> propertiesRForEqNodes = getObjectProperties(node0Eq, node1Eq);
                String propertyRString = meetCloseMetaRuleCondition(propertiesRForEqNodes);
                if (!propertyRString.equals("")) {
                    if (!isCloseMetaRuleDisjunctionAdded(propertyRString, node0Eq, node1Eq)) {
                        GroundDisjunction groundDisjunction = createCloseMetaRuleDisjunction(propertyRString, node0Eq, node1Eq);
                        if (!groundDisjunction.isSatisfied(this.m_tableau)) {
                            this.m_tableau.addGroundDisjunction(groundDisjunction);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
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

    private boolean isCloseMetaRuleDisjunctionAdded(String propertyRString, Node node0, Node node1) {
        if (this.closeMetaRuleDisjunctionsMap.containsKey(propertyRString)) {
            for (Map.Entry<Node, Node> nodePair : this.closeMetaRuleDisjunctionsMap.get(propertyRString)) {
                if (nodePair.getKey().m_nodeID == node0.m_nodeID && nodePair.getValue().m_nodeID == node1.m_nodeID) {
                    return true;
                }
            }
        } else {
            this.closeMetaRuleDisjunctionsMap.put(propertyRString, new ArrayList<Map.Entry<Node, Node>>());
        }
        this.closeMetaRuleDisjunctionsMap.get(propertyRString).add(new AbstractMap.SimpleEntry<>(node0, node1));
        return false;
    }

    private GroundDisjunction createCloseMetaRuleDisjunction(String propertyRString, Node node0Eq, Node node1Eq) {
        propertyRString = propertyRString.substring(1, propertyRString.length() - 1);
        AtomicRole newProperty = AtomicRole.create("~" + propertyRString);
        AtomicRole propertyR = AtomicRole.create(propertyRString);
        Atom relationR = Atom.create(propertyR, this.mapNodeIndividual.get(node0Eq.m_nodeID), this.mapNodeIndividual.get(node1Eq.m_nodeID));
        DLPredicate relationRPredicate = relationR.getDLPredicate();
        Atom newRelationR = Atom.create(newProperty, this.mapNodeIndividual.get(node0Eq.m_nodeID), this.mapNodeIndividual.get(node1Eq.m_nodeID));
        DLPredicate newRelationRPredicate = newRelationR.getDLPredicate();
        DLPredicate[] dlPredicates = new DLPredicate[]{relationRPredicate, newRelationRPredicate};
        int hashCode = 0;
        for (int disjunctIndex = 0; disjunctIndex < dlPredicates.length; ++disjunctIndex) {
            hashCode = hashCode * 7 + dlPredicates[disjunctIndex].hashCode();
        }
        GroundDisjunctionHeader gdh = new GroundDisjunctionHeader(dlPredicates, hashCode, null);
        DependencySet dependencySet = this.m_tableau.m_dependencySetFactory.getActualDependencySet();
        GroundDisjunction groundDisjunction = new GroundDisjunction(this.m_tableau, gdh, new Node[]{node0Eq, node1Eq, node0Eq, node1Eq}, new boolean[]{true, true}, dependencySet);
        return groundDisjunction;
    }

    boolean checkMetaRule() {
        for (OWLMetamodellingAxiom metamodellingAxiom : this.m_tableau.m_permanentDLOntology.getMetamodellingAxioms()) {
            Node metamodellingNode = getMetamodellingNodeFromIndividual(metamodellingAxiom.getMetamodelIndividual());
            for (OWLMetaRuleAxiom mrAxiom : this.m_tableau.m_permanentDLOntology.getMetaRuleAxioms()) {
                String metaRulePropertyR = mrAxiom.getPropertyR().toString();
                List<Node> relatedNodes = this.m_tableau.getRelatedNodes(metamodellingNode, metaRulePropertyR);
                if (relatedNodes.size() > 0) {
                    List<String> classesImageForMetamodellingNode = getNodesClasses(relatedNodes);
                    if (!classesImageForMetamodellingNode.isEmpty() && !MetamodellingAxiomHelper.containsMetaRuleAddedAxiom(metamodellingAxiom.getModelClass().toString(), mrAxiom.getPropertyS().toString(), classesImageForMetamodellingNode, this.m_tableau)) {
                        MetamodellingAxiomHelper.addMetaRuleAddedAxiom(metamodellingAxiom.getModelClass().toString(), mrAxiom.getPropertyS().toString(), classesImageForMetamodellingNode, this.m_tableau);
                        return true;
                    }
                }
            }
        }
        return false;
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

    private List<String> getNodesClasses(List<Node> nodes) {
        List<String> classes = new ArrayList<String>();
        for (Node node : nodes) {
            int nodeId = node.m_nodeID;
            if (this.nodeToMetaIndividual.containsKey(nodeId)) {
                Individual individual = this.nodeToMetaIndividual.get(nodeId);
                for (OWLMetamodellingAxiom metamodellingAxiom : this.m_tableau.m_permanentDLOntology.getMetamodellingAxioms()) {
                    if (metamodellingAxiom.getMetamodelIndividual().toString().equals(individual.toString())) {
                        classes.add(metamodellingAxiom.getModelClass().toString());
                    }
                }
            }
        }
        return classes;
    }

    private String meetCloseMetaRuleCondition(List<String> propertiesRForEqNodes) {
        for (OWLMetaRuleAxiom mrAxiom : this.m_tableau.m_permanentDLOntology.getMetaRuleAxioms()) {
            String metaRulePropertyR = mrAxiom.getPropertyR().toString();
            if (!propertiesRForEqNodes.contains(metaRulePropertyR) && !propertiesRForEqNodes.contains(getNegativeProperty(metaRulePropertyR))) {
                return metaRulePropertyR;
            }
        }
        return "";
    }

    private String getNegativeProperty(String property) {
        String prefix = "<~";
        String negativeProperty = prefix + property.substring(1);
        return negativeProperty;
    }

    boolean checkEqualMetamodellingRule() {
        System.out.println("=== checkEqualMetamodellingRule START ===");
        System.out.println("Number of metamodelling nodes: " + this.metamodellingNodes.size());
        
        boolean ruleApplied = false;
        // Obs: aca es donde se hace el for anidado para chequear la regla de igualdad de metamodelado

        // en vez de hace for asi cada vez que se prueba esto, por que no almacenamos en algun lado los individuos iguales?
        // eso haria que iteremos una vez sola por cada nodo, ya que luego accedemos a los que son iguales
        for (Node node1 : this.metamodellingNodes) {
            for (Node node2 : this.metamodellingNodes) {
                System.out.println("Checking nodes: node1=" + node1.m_nodeID + ", node2=" + node2.m_nodeID);
                System.out.println("Are same individual: " + this.m_tableau.areSameIndividual(node1, node2));
                
                if (this.m_tableau.areSameIndividual(node1, node2)) {
                    System.out.println("Nodes are same, checking equal metamodelling rule iteration...");
                    boolean iterationResult = checkEqualMetamodellingRuleIteration(node1, node2);
                    System.out.println("Equal metamodelling rule iteration result: " + iterationResult);
                    if (iterationResult) ruleApplied = true;
                }
            }
        }
        
        System.out.println("=== checkEqualMetamodellingRule END - ruleApplied: " + ruleApplied + " ===");
        return ruleApplied;
    }

    boolean checkInequalityMetamodellingRule() {
        boolean ruleApplied = false;
        for (Map.Entry<Integer, List<Integer>> entry : differentIndividualsMap.entrySet()) {
            Node node1 = this.m_tableau.getNode(entry.getKey());
            for (Integer nodeId2 : entry.getValue()) {
                Node node2 = this.m_tableau.getNode(nodeId2);
                if (node1 != null && node2 != null && m_tableau.areDifferentIndividual(node1, node2) && checkInequalityMetamodellingRuleIteration(node1, node2))
                    ruleApplied = true;
            }
        }
        return ruleApplied;
    }

    boolean checkCloseMetamodellingRule() {
        for (Node node1 : this.metamodellingNodes) {
            for (Node node2 : this.metamodellingNodes) {
                if (this.m_tableau.m_metamodellingManager.checkCloseMetamodellingRuleIteration(node1, node2))
                    return true;
            }
        }
        return false;
    }
}
