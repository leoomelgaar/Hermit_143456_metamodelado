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
    Map<Integer,List<Integer>> differentIndividualsMap;
    Map<Integer,Map<Integer, List<String>>> nodeProperties;

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

    /**
     * Nueva regla de inferencia de metamodelado:
     * Si clase A ≡ individuo a (por metamodelado) y clase A ≡ clase B (inclusión mutua),
     * entonces individuo a ≡ clase B (por metamodelado)
     */
    public boolean infereceIndiviualToClassAxioms() {
        boolean anyApplied = false;

        for (Node metamodellingNode : this.metamodellingNodes) {
            Individual individual = this.nodeToMetaIndividual.get(metamodellingNode.m_nodeID);
            if (individual == null) continue;

            String individualName = individual.toString();

            Set<String> metamodelledClasses = getMetamodelledClassesForIndividual(individualName);

            Set<String> allEquivalentClasses = new HashSet<>();
            for (String metamodelledClass : metamodelledClasses) {
                Set<String> equivalentClasses = findEquivalentClasses(metamodelledClass);
                allEquivalentClasses.addAll(equivalentClasses);
            }

            for (String equivalentClass : allEquivalentClasses) {
                if (!metamodelledClasses.contains(equivalentClass)) {
                    if (!hasMetamodellingForClass(equivalentClass)) {
                        boolean applied = applyMetamodellingInference(metamodellingNode, individualName, equivalentClass);
                        if (applied) {
                            anyApplied = true;
                        }
                    }
                }
            }
        }

        return anyApplied;
    }

    private Set<String> getMetamodelledClassesForIndividual(String individualName) {
        Set<String> metamodelledClasses = new HashSet<>();

        Individual individual = Individual.create(extractClassName(individualName));
        List<OWLClassExpression> classes = MetamodellingAxiomHelper.getMetamodellingClassesByIndividual(
            individual, m_tableau.getPermanentDLOntology()
        );

        for (OWLClassExpression classExpression : classes) {
            metamodelledClasses.add(extractClassName(classExpression.toString()));
        }

        return metamodelledClasses;
    }

    /**
     * Encuentra todas las clases equivalentes a una clase dada
     * Dos clases son equivalentes si tienen inclusión mutua (A ⊑ B y B ⊑ A)
     */
    private Set<String> findEquivalentClasses(String className) {
        Set<String> equivalentClasses = new HashSet<>();
        equivalentClasses.add(className);

        DLOntology ontology = m_tableau.getPermanentDLOntology();

        Set<String> allClasses = new HashSet<>();
        for (DLClause dlClause : ontology.getDLClauses()) {
            if (dlClause.isAtomicConceptInclusion()) {
                String headClass = extractClassName(dlClause.getHeadAtom(0).getDLPredicate().toString());
                String bodyClass = extractClassName(dlClause.getBodyAtom(0).getDLPredicate().toString());
                allClasses.add(headClass);
                allClasses.add(bodyClass);
            }
        }

        for (String otherClass : allClasses) {
            if (!otherClass.equals(className)) {
                boolean isClassContainedInOther = hasSubClassOfAxiom(className, otherClass, ontology);
                boolean isOtherContainedInClass = hasSubClassOfAxiom(otherClass, className, ontology);

                if (isClassContainedInOther && isOtherContainedInClass) {
                    equivalentClasses.add(otherClass);
                }
            }
        }

        return equivalentClasses;
    }

    /**
     * Verifica si existe un axioma de subclase entre dos clases
     */
    private boolean hasSubClassOfAxiom(String classA, String classB, DLOntology ontology) {
        for (DLClause dlClause : ontology.getDLClauses()) {
            if (dlClause.isAtomicConceptInclusion()) {
                String headClass = dlClause.getHeadAtom(0).getDLPredicate().toString();
                String bodyClass = dlClause.getBodyAtom(0).getDLPredicate().toString();

                if (headClass.equals("<" + classA + ">") && bodyClass.equals("<" + classB + ">")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Verifica si una clase ya tiene metamodelado
     */
    private boolean hasMetamodellingForClass(String className) {
        for (Node metamodellingNode : this.metamodellingNodes) {
            Individual individual = this.nodeToMetaIndividual.get(metamodellingNode.m_nodeID);
            if (individual != null) {
                String individualName = individual.toString();
                String metamodelledClass = extractClassName(individualName);
                if (metamodelledClass.equals(className)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Aplica la inferencia de metamodelado sin usar termsToNodes
     */
    private boolean applyMetamodellingInference(Node metamodellingNode, String individualName, String equivalentClass) {
        org.semanticweb.HermiT.model.Individual metaIndividual = this.nodeToMetaIndividual.get(metamodellingNode.m_nodeID);
        if (metaIndividual == null) {
            return false;
        }
        String individualIRI = metaIndividual.getIRI();
        String classIRI = equivalentClass;
        org.semanticweb.owlapi.model.OWLDataFactory df = org.semanticweb.owlapi.apibinding.OWLManager.getOWLDataFactory();
        org.semanticweb.owlapi.model.OWLNamedIndividual owlInd = df.getOWLNamedIndividual(org.semanticweb.owlapi.model.IRI.create(individualIRI));
        org.semanticweb.owlapi.model.OWLClass owlClass = df.getOWLClass(org.semanticweb.owlapi.model.IRI.create(classIRI));
        org.semanticweb.owlapi.model.OWLMetamodellingAxiom axiom = df.getOWLMetamodellingAxiom(owlClass, owlInd);
        java.util.Set<org.semanticweb.owlapi.model.OWLMetamodellingAxiom> axioms = this.m_tableau.getPermanentDLOntology().getMetamodellingAxioms();
        if (axioms.contains(axiom)) {
            return false;
        }
        axioms.add(axiom);
        System.out.println("[Metamodelling] Inferido por metamodelado: '" + individualIRI + "' ≡ '" + classIRI + "'");
        return true;
    }

    /**
     * Extrae el nombre de la clase de un identificador
     */
    private String extractClassName(String identifier) {
        if (identifier.startsWith("<") && identifier.endsWith(">")) {
            return identifier.substring(1, identifier.length() - 1);
        }
        return identifier;
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
}
