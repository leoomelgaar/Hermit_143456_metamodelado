package org.semanticweb.HermiT.hierarchy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.semanticweb.HermiT.Prefixes;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.HermiT.graph.Graph;
import org.semanticweb.HermiT.hierarchy.RoleElementManager.RoleElement;
import org.semanticweb.HermiT.model.Atom;
import org.semanticweb.HermiT.model.AtomicConcept;
import org.semanticweb.HermiT.model.AtomicRole;
import org.semanticweb.HermiT.model.DLClause;
import org.semanticweb.HermiT.model.DLOntology;
import org.semanticweb.HermiT.model.DLPredicate;
import org.semanticweb.HermiT.model.Individual;
import org.semanticweb.HermiT.model.Inequality;
import org.semanticweb.HermiT.model.InverseRole;
import org.semanticweb.HermiT.model.Role;
import org.semanticweb.HermiT.monitor.TableauMonitor;
import org.semanticweb.HermiT.tableau.ExtensionManager;
import org.semanticweb.HermiT.tableau.ExtensionTable;
import org.semanticweb.HermiT.tableau.InterruptFlag;
import org.semanticweb.HermiT.tableau.Node;
import org.semanticweb.HermiT.tableau.NodeType;
import org.semanticweb.HermiT.tableau.ReasoningTaskDescription;
import org.semanticweb.HermiT.tableau.Tableau;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.reasoner.ReasonerProgressMonitor;

public class InstanceManager {
    public static final int thresholdForAdditionalAxioms = 10000;
    protected final InterruptFlag m_interruptFlag;
    protected final Reasoner m_reasoner;
    protected final TableauMonitor m_tableauMonitor;
    protected final Individual[] m_individuals;
    protected final HashSet<AtomicRole> m_complexRoles;
    protected final Map<AtomicConcept, AtomicConceptElement> m_conceptToElement;
    protected final AtomicConcept m_topConcept;
    protected final AtomicConcept m_bottomConcept;
    protected Hierarchy<AtomicConcept> m_currentConceptHierarchy;
    protected final RoleElementManager m_roleElementManager;
    protected final RoleElementManager.RoleElement m_topRoleElement;
    protected final RoleElementManager.RoleElement m_bottomRoleElement;
    protected Hierarchy<RoleElementManager.RoleElement> m_currentRoleHierarchy;
    protected final boolean m_usesInverseRoles;
    protected final Map<Individual, Node> m_nodesForIndividuals;
    protected final Map<Node, Individual> m_individualsForNodes;
    protected final Map<Node, Set<Node>> m_canonicalNodeToDetMergedNodes;
    protected final Map<Node, Set<Node>> m_canonicalNodeToNonDetMergedNodes;
    protected boolean m_isInconsistent;
    protected boolean m_realizationCompleted;
    protected boolean m_roleRealizationCompleted;
    protected boolean m_usesClassifiedConceptHierarchy;
    protected boolean m_classesInitialised;
    protected boolean m_propertiesInitialised;
    protected boolean m_readingOffFoundPossibleConceptInstance;
    protected boolean m_readingOffFoundPossiblePropertyInstance;
    protected final Map<Individual, Set<Individual>> m_individualToEquivalenceClass;
    protected Map<Set<Individual>, Set<Set<Individual>>> m_individualToPossibleEquivalenceClass;
    protected final ExtensionTable.Retrieval m_binaryRetrieval0Bound;
    protected final ExtensionTable.Retrieval m_binaryRetrieval1Bound;
    protected final ExtensionTable.Retrieval m_ternaryRetrieval1Bound;
    protected int m_currentIndividualIndex = 0;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public InstanceManager(InterruptFlag interruptFlag, Reasoner reasoner, Hierarchy<AtomicConcept> atomicConceptHierarchy, Hierarchy<Role> objectRoleHierarchy) {
        this.m_interruptFlag = interruptFlag;
        this.m_interruptFlag.startTask();
        try {
            this.m_reasoner = reasoner;
            this.m_tableauMonitor = this.m_reasoner.getTableau().getTableauMonitor();
            DLOntology dlo = this.m_reasoner.getDLOntology();
            this.m_individuals = new ArrayList<Individual>(dlo.getAllIndividuals()).toArray(new Individual[0]);
            this.m_complexRoles = new HashSet();
            this.m_individualToEquivalenceClass = new HashMap<Individual, Set<Individual>>();
            this.m_nodesForIndividuals = new HashMap<Individual, Node>();
            for (Individual individual : this.m_individuals) {
                this.m_nodesForIndividuals.put(individual, null);
                HashSet<Individual> equivalentIndividuals = new HashSet<Individual>();
                equivalentIndividuals.add(individual);
                this.m_individualToEquivalenceClass.put(individual, equivalentIndividuals);
                this.m_interruptFlag.checkInterrupt();
            }
            this.m_individualsForNodes = new HashMap<Node, Individual>();
            this.m_canonicalNodeToDetMergedNodes = new HashMap<Node, Set<Node>>();
            this.m_canonicalNodeToNonDetMergedNodes = new HashMap<Node, Set<Node>>();
            this.m_individualToPossibleEquivalenceClass = null;
            this.m_topConcept = AtomicConcept.THING;
            this.m_bottomConcept = AtomicConcept.NOTHING;
            this.m_conceptToElement = new HashMap<AtomicConcept, AtomicConceptElement>();
            this.m_conceptToElement.put(this.m_topConcept, new AtomicConceptElement(null, null));
            Graph knownConceptSubsumptions = null;
            HashSet<AtomicConcept> atomicConcepts = null;
            if (atomicConceptHierarchy != null) {
                this.setToClassifiedConceptHierarchy(atomicConceptHierarchy);
            } else {
                knownConceptSubsumptions = new Graph();
                atomicConcepts = new HashSet<AtomicConcept>();
                atomicConcepts.add(this.m_topConcept);
                atomicConcepts.add(this.m_bottomConcept);
                for (AtomicConcept atomicConcept : dlo.getAllAtomicConcepts()) {
                    if (!Prefixes.isInternalIRI(atomicConcept.getIRI())) {
                        atomicConcepts.add(atomicConcept);
                        this.addKnownConceptSubsumption(knownConceptSubsumptions, atomicConcept, atomicConcept);
                        this.addKnownConceptSubsumption(knownConceptSubsumptions, atomicConcept, this.m_topConcept);
                        this.addKnownConceptSubsumption(knownConceptSubsumptions, this.m_bottomConcept, atomicConcept);
                    }
                    this.m_interruptFlag.checkInterrupt();
                }
                this.addKnownConceptSubsumption(knownConceptSubsumptions, this.m_bottomConcept, this.m_bottomConcept);
            }
            this.m_roleElementManager = new RoleElementManager();
            Graph knownRoleSubsumptions = null;
            this.m_topRoleElement = this.m_roleElementManager.getRoleElement(AtomicRole.TOP_OBJECT_ROLE);
            this.m_bottomRoleElement = this.m_roleElementManager.getRoleElement(AtomicRole.BOTTOM_OBJECT_ROLE);
            this.m_usesInverseRoles = dlo.hasInverseRoles();
            HashSet<Role> roles = null;
            Set<Role> complexRoles = dlo.getAllComplexObjectRoles();
            if (objectRoleHierarchy != null) {
                this.setToClassifiedRoleHierarchy(objectRoleHierarchy);
                for (Role role : complexRoles) {
                    if (!(role instanceof AtomicRole) || role == AtomicRole.TOP_OBJECT_ROLE || role == AtomicRole.BOTTOM_OBJECT_ROLE) continue;
                    this.m_complexRoles.add((AtomicRole)role);
                }
            } else {
                knownRoleSubsumptions = new Graph();
                roles = new HashSet<Role>();
                roles.add(AtomicRole.TOP_OBJECT_ROLE);
                roles.add(AtomicRole.BOTTOM_OBJECT_ROLE);
                roles.addAll(dlo.getAllAtomicObjectRoles());
                for (Role role : roles) {
                    this.addKnownRoleSubsumption(knownRoleSubsumptions, role, role);
                    this.addKnownRoleSubsumption(knownRoleSubsumptions, role, AtomicRole.TOP_OBJECT_ROLE);
                    this.addKnownRoleSubsumption(knownRoleSubsumptions, AtomicRole.BOTTOM_OBJECT_ROLE, role);
                    if (complexRoles.contains(role) && role instanceof AtomicRole && role != AtomicRole.TOP_OBJECT_ROLE && role != AtomicRole.BOTTOM_OBJECT_ROLE) {
                        this.m_complexRoles.add((AtomicRole)role);
                    }
                    this.m_interruptFlag.checkInterrupt();
                }
                this.addKnownRoleSubsumption(knownRoleSubsumptions, AtomicRole.BOTTOM_OBJECT_ROLE, AtomicRole.BOTTOM_OBJECT_ROLE);
            }
            if (atomicConceptHierarchy == null || objectRoleHierarchy == null) {
                this.updateKnownSubsumptionsUsingToldSubsumers(dlo.getDLClauses(), knownConceptSubsumptions, atomicConcepts, knownRoleSubsumptions, roles);
            }
            if (atomicConceptHierarchy == null) {
                this.m_currentConceptHierarchy = this.buildTransitivelyReducedConceptHierarchy(knownConceptSubsumptions);
            }
            if (objectRoleHierarchy == null) {
                this.m_currentRoleHierarchy = this.buildTransitivelyReducedRoleHierarchy(knownRoleSubsumptions);
            }
            ExtensionManager extensionManager = this.m_reasoner.getTableau().getExtensionManager();
            this.m_binaryRetrieval0Bound = extensionManager.getBinaryExtensionTable().createRetrieval(new boolean[]{true, false}, ExtensionTable.View.TOTAL);
            this.m_binaryRetrieval1Bound = extensionManager.getBinaryExtensionTable().createRetrieval(new boolean[]{false, true}, ExtensionTable.View.TOTAL);
            this.m_ternaryRetrieval1Bound = extensionManager.getTernaryExtensionTable().createRetrieval(new boolean[]{false, true, false}, ExtensionTable.View.TOTAL);
        }
        finally {
            this.m_interruptFlag.endTask();
        }
    }

    protected void addKnownConceptSubsumption(Graph<AtomicConcept> knownSubsumptions, AtomicConcept subConcept, AtomicConcept superConcept) {
        knownSubsumptions.addEdge(subConcept, superConcept);
    }

    protected void addKnownRoleSubsumption(Graph<Role> knownSubsumptions, Role subRole, Role superRole) {
        knownSubsumptions.addEdge(subRole, superRole);
        if (this.m_usesInverseRoles) {
            knownSubsumptions.addEdge(subRole.getInverse(), superRole.getInverse());
        }
    }

    protected void updateKnownSubsumptionsUsingToldSubsumers(Set<DLClause> dlClauses, Graph<AtomicConcept> knownConceptSubsumptions, Set<AtomicConcept> concepts, Graph<Role> knownRoleSubsumptions, Set<Role> roles) {
        boolean requiresRoleSubsumers;
        boolean requiresConceptSubsumers = knownConceptSubsumptions != null;
        boolean bl = requiresRoleSubsumers = knownRoleSubsumptions != null;
        if (requiresConceptSubsumers || requiresRoleSubsumers) {
            for (DLClause dlClause : dlClauses) {
                if (dlClause.getHeadLength() == 1 && dlClause.getBodyLength() == 1) {
                    DLPredicate headPredicate = dlClause.getHeadAtom(0).getDLPredicate();
                    DLPredicate bodyPredicate = dlClause.getBodyAtom(0).getDLPredicate();
                    if (requiresConceptSubsumers && headPredicate instanceof AtomicConcept && bodyPredicate instanceof AtomicConcept) {
                        AtomicConcept headConcept = (AtomicConcept)headPredicate;
                        AtomicConcept bodyConcept = (AtomicConcept)bodyPredicate;
                        if (concepts.contains(headConcept) && concepts.contains(bodyConcept)) {
                            this.addKnownConceptSubsumption(knownConceptSubsumptions, bodyConcept, headConcept);
                        }
                    } else if (requiresRoleSubsumers && headPredicate instanceof AtomicRole && bodyPredicate instanceof AtomicRole) {
                        AtomicRole headRole = (AtomicRole)headPredicate;
                        AtomicRole bodyRole = (AtomicRole)bodyPredicate;
                        if (roles.contains(headRole) && roles.contains(bodyRole)) {
                            if (dlClause.getBodyAtom(0).getArgument(0) != dlClause.getHeadAtom(0).getArgument(0)) {
                                this.addKnownRoleSubsumption(knownRoleSubsumptions, InverseRole.create(bodyRole), headRole);
                            } else {
                                this.addKnownRoleSubsumption(knownRoleSubsumptions, bodyRole, headRole);
                            }
                        }
                    }
                }
                this.m_interruptFlag.checkInterrupt();
            }
        }
    }

    protected Hierarchy<AtomicConcept> buildTransitivelyReducedConceptHierarchy(Graph<AtomicConcept> knownSubsumptions) {
        HashMap<AtomicConcept, DeterministicClassification.GraphNode<AtomicConcept>> allSubsumers = new HashMap<AtomicConcept, DeterministicClassification.GraphNode<AtomicConcept>>();
        for (AtomicConcept element : knownSubsumptions.getElements()) {
            allSubsumers.put(element, new DeterministicClassification.GraphNode<AtomicConcept>(element, knownSubsumptions.getSuccessors(element)));
        }
        this.m_interruptFlag.checkInterrupt();
        return DeterministicClassification.buildHierarchy(this.m_topConcept, this.m_bottomConcept, allSubsumers);
    }

    public void setToClassifiedConceptHierarchy(Hierarchy<AtomicConcept> atomicConceptHierarchy) {
        if (atomicConceptHierarchy != this.m_currentConceptHierarchy) {
            this.m_currentConceptHierarchy = atomicConceptHierarchy;
            if (this.m_classesInitialised && this.m_individuals.length > 0) {
                for (HierarchyNode<AtomicConcept> node : this.m_currentConceptHierarchy.getAllNodesSet()) {
                    if (node.m_representative == this.m_bottomConcept) continue;
                    AtomicConcept representativeConcept = node.getRepresentative();
                    HashSet<Individual> known = new HashSet<Individual>();
                    HashSet<Individual> possible = null;
                    for (AtomicConcept concept : node.getEquivalentElements()) {
                        if (!this.m_conceptToElement.containsKey(concept)) continue;
                        AtomicConceptElement element = this.m_conceptToElement.get(concept);
                        known.addAll(element.m_knownInstances);
                        if (possible == null) {
                            possible = new HashSet<Individual>(element.m_possibleInstances);
                        } else {
                            possible.retainAll(element.m_possibleInstances);
                        }
                        this.m_conceptToElement.remove(concept);
                    }
                    if (possible != null) {
                        possible.removeAll(known);
                    }
                    if (known.isEmpty() && possible == null && representativeConcept != this.m_topConcept) continue;
                    this.m_conceptToElement.put(representativeConcept, new AtomicConceptElement(known, possible));
                }
                LinkedList<HierarchyNode<AtomicConcept>> toProcess = new LinkedList<HierarchyNode<AtomicConcept>>();
                toProcess.addAll(this.m_currentConceptHierarchy.m_bottomNode.m_parentNodes);
                while (!toProcess.isEmpty()) {
                    HierarchyNode<AtomicConcept> current=toProcess.remove();
                    AtomicConcept currentConcept=current.getRepresentative();
                    AtomicConceptElement currentElement=m_conceptToElement.get(currentConcept);
                    if (currentElement!=null) {
                        Set<HierarchyNode<AtomicConcept>> ancestors=current.getAncestorNodes();
                        ancestors.remove(current);
                        for (HierarchyNode<AtomicConcept> ancestor : ancestors) {
                            AtomicConcept ancestorConcept=ancestor.getRepresentative();
                            AtomicConceptElement ancestorElement=m_conceptToElement.get(ancestorConcept);
                            if (ancestorElement!=null) {
                                ancestorElement.m_knownInstances.removeAll(currentElement.m_knownInstances);
                                ancestorElement.m_possibleInstances.removeAll(currentElement.m_knownInstances);
                                ancestorElement.m_possibleInstances.removeAll(currentElement.m_possibleInstances);
                            }
                        }
                        for (HierarchyNode<AtomicConcept> parent : current.getParentNodes())
                            if (!toProcess.contains(parent))
                                toProcess.add(parent);
                    }
                    m_interruptFlag.checkInterrupt();
                }
            }
            this.m_usesClassifiedConceptHierarchy = true;
        }
    }

    protected Hierarchy<RoleElementManager.RoleElement> buildTransitivelyReducedRoleHierarchy(Graph<Role> knownSubsumptions) {
        HashMap<Role, DeterministicClassification.GraphNode<Role>> allSubsumers = new HashMap<Role, DeterministicClassification.GraphNode<Role>>();
        for (Role role : knownSubsumptions.getElements()) {
            allSubsumers.put(role, new DeterministicClassification.GraphNode<Role>(role, knownSubsumptions.getSuccessors(role)));
        }
        this.m_interruptFlag.checkInterrupt();
        return this.transformRoleHierarchy(DeterministicClassification.buildHierarchy(AtomicRole.TOP_OBJECT_ROLE, AtomicRole.BOTTOM_OBJECT_ROLE, allSubsumers));
    }

    protected Hierarchy<RoleElementManager.RoleElement> transformRoleHierarchy(Hierarchy<Role> roleHierarchy) {
        Hierarchy<AtomicRole> newHierarchy = this.removeInverses(roleHierarchy);
        Hierarchy.Transformer<Role, RoleElementManager.RoleElement> transformer = new Hierarchy.Transformer<Role, RoleElementManager.RoleElement>(){

            @Override
            public RoleElementManager.RoleElement transform(Role role) {
                InstanceManager.this.m_interruptFlag.checkInterrupt();
                if (!(role instanceof AtomicRole)) {
                    throw new IllegalArgumentException("Internal error: The instance manager should only use atomic roles, but here we got a hierarchy element for an inverse role:" + role);
                }
                return InstanceManager.this.m_roleElementManager.getRoleElement((AtomicRole)role);
            }

            @Override
            public RoleElementManager.RoleElement determineRepresentative(Role oldRepresentative, Set<RoleElementManager.RoleElement> newEquivalentElements) {
                RoleElementManager.RoleElement representative = this.transform(oldRepresentative);
                for (RoleElementManager.RoleElement newEquiv : newEquivalentElements) {
                    Set<Individual> successors;
                    if (newEquiv.equals(representative)) continue;
                    for (Individual individual : newEquiv.m_knownRelations.keySet()) {
                        successors = representative.m_knownRelations.get(individual);
                        if (successors == null) {
                            successors = new HashSet<Individual>();
                            representative.m_knownRelations.put(individual, successors);
                        }
                        successors.addAll(newEquiv.m_knownRelations.get(individual));
                    }
                    for (Individual individual : newEquiv.m_possibleRelations.keySet()) {
                        successors = representative.m_possibleRelations.get(individual);
                        if (successors == null) continue;
                        successors.retainAll(newEquiv.m_possibleRelations.get(individual));
                    }
                    newEquiv.m_knownRelations.clear();
                    newEquiv.m_possibleRelations.clear();
                }
                InstanceManager.this.m_interruptFlag.checkInterrupt();
                return representative;
            }
        };
        return newHierarchy.transform(transformer, null);
    }

    protected Hierarchy<AtomicRole> removeInverses(Hierarchy<Role> hierarchy) {
        HashMap<AtomicRole, DeterministicClassification.GraphNode<AtomicRole>> allSubsumers = new HashMap<AtomicRole, DeterministicClassification.GraphNode<AtomicRole>>();
        HashSet<AtomicRole> toProcess = new HashSet<AtomicRole>();
        HashSet<AtomicRole> visited = new HashSet<AtomicRole>();
        toProcess.add(this.m_bottomRoleElement.m_role);
        while (!toProcess.isEmpty()) {
            AtomicRole current = toProcess.iterator().next();
            visited.add(current);
            HierarchyNode<Role> currentNode = hierarchy.getNodeForElement(current);
            HashSet<AtomicRole> atomicRepresentatives = new HashSet<AtomicRole>();
            this.findNextHierarchyNodeWithAtomic(atomicRepresentatives, currentNode);
            allSubsumers.put(current, new DeterministicClassification.GraphNode<AtomicRole>(current, atomicRepresentatives));
            toProcess.addAll(atomicRepresentatives);
            toProcess.removeAll(visited);
            this.m_interruptFlag.checkInterrupt();
        }
        Hierarchy<AtomicRole> newHierarchy = DeterministicClassification.buildHierarchy(this.m_topRoleElement.m_role, this.m_bottomRoleElement.m_role, allSubsumers);
        for (AtomicRole element : newHierarchy.m_nodesByElements.keySet()) {
            HierarchyNode<Role> oldNode = hierarchy.getNodeForElement(element);
            HierarchyNode<AtomicRole> newNode = newHierarchy.getNodeForElement(element);
            for (Role equivalent : oldNode.m_equivalentElements) {
                if (!(equivalent instanceof AtomicRole)) continue;
                newNode.m_equivalentElements.add((AtomicRole)equivalent);
            }
            this.m_interruptFlag.checkInterrupt();
        }
        return newHierarchy;
    }

    public void setToClassifiedRoleHierarchy(Hierarchy<Role> roleHierarchy) {
        this.m_currentRoleHierarchy = this.transformRoleHierarchy(roleHierarchy);
        if (this.m_propertiesInitialised && this.m_individuals.length > 0) {
            LinkedList toProcess = new LinkedList();
            toProcess.add(this.m_currentRoleHierarchy.m_bottomNode);
            while (!toProcess.isEmpty()) {
                HierarchyNode<RoleElement> current = (HierarchyNode)toProcess.remove();
                RoleElementManager.RoleElement currentRepresentative = current.getRepresentative();
                Set<HierarchyNode<RoleElement>> ancestors = current.getAncestorNodes();
                ancestors.remove(current);
                for (HierarchyNode ancestor : ancestors) {
                    Set<Individual> successors;
                    RoleElementManager.RoleElement ancestorRepresentative = (RoleElementManager.RoleElement)ancestor.m_representative;
                    Map<Individual, Set<Individual>> ancestorKnowRelations = ancestorRepresentative.m_knownRelations;
                    Map<Individual, Set<Individual>> ancestorPossibleRelations = ancestorRepresentative.m_possibleRelations;
                    for (Individual individual : currentRepresentative.m_knownRelations.keySet()) {
                        successors = ancestorKnowRelations.get(individual);
                        if (successors != null) {
                            successors.removeAll(currentRepresentative.m_knownRelations.get(individual));
                            if (successors.isEmpty()) {
                                ancestorKnowRelations.remove(individual);
                            }
                        }
                        if ((successors = ancestorPossibleRelations.get(individual)) == null) continue;
                        successors.removeAll(currentRepresentative.m_knownRelations.get(individual));
                        if (!successors.isEmpty()) continue;
                        ancestorPossibleRelations.remove(individual);
                    }
                    for (Individual individual : currentRepresentative.m_possibleRelations.keySet()) {
                        successors = ancestorPossibleRelations.get(individual);
                        if (successors == null) continue;
                        successors.removeAll(currentRepresentative.m_possibleRelations.get(individual));
                        if (!successors.isEmpty()) continue;
                        ancestorPossibleRelations.remove(individual);
                    }
                }
                for (HierarchyNode parent : current.getParentNodes()) {
                    if (toProcess.contains(parent)) continue;
                    toProcess.add(parent);
                }
                this.m_interruptFlag.checkInterrupt();
            }
        }
    }

    protected void findNextHierarchyNodeWithAtomic(Set<AtomicRole> atomicRepresentatives, HierarchyNode<Role> current) {
        for (HierarchyNode<Role> successor : current.getParentNodes()) {
            HashSet<AtomicRole> suitable = new HashSet<AtomicRole>();
            for (Role role : successor.getEquivalentElements()) {
                if (!(role instanceof AtomicRole)) continue;
                suitable.add((AtomicRole)role);
            }
            if (!suitable.isEmpty()) {
                atomicRepresentatives.add(suitable.iterator().next());
                continue;
            }
            if (successor == current) continue;
            this.findNextHierarchyNodeWithAtomic(atomicRepresentatives, successor);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public OWLAxiom[] getAxiomsForReadingOffCompexProperties(OWLDataFactory factory, ReasonerProgressMonitor monitor, int completedSteps, int steps) {
        if (m_complexRoles.size()>0) {
            int noAdditionalAxioms=0;
            List<OWLAxiom> additionalAxioms=new ArrayList<OWLAxiom>();
            m_interruptFlag.startTask();
            try {
                for (;m_currentIndividualIndex<m_individuals.length && noAdditionalAxioms < thresholdForAdditionalAxioms;m_currentIndividualIndex++) {
                    Individual ind=m_individuals[m_currentIndividualIndex];
                    for (AtomicRole objectRole : m_complexRoles) {
                        completedSteps++;
                        if (monitor!=null)
                            monitor.reasonerTaskProgressChanged(completedSteps,steps);
                        OWLObjectProperty objectProperty=factory.getOWLObjectProperty(IRI.create(objectRole.getIRI()));
                        String indIRI=ind.getIRI();
                        OWLClass classForIndividual=factory.getOWLClass(IRI.create("internal:individual-concept#"+indIRI));
                        OWLAxiom axiom=factory.getOWLClassAssertionAxiom(classForIndividual,factory.getOWLNamedIndividual(IRI.create(indIRI)));
                        additionalAxioms.add(axiom); // A_a(a)
                        AtomicConcept conceptForRole=AtomicConcept.create("internal:individual-concept#"+objectRole.getIRI()+"#"+indIRI);
                        OWLClass classForRoleAndIndividual=factory.getOWLClass(IRI.create(conceptForRole.getIRI()));
                        axiom=factory.getOWLSubClassOfAxiom(classForIndividual,factory.getOWLObjectAllValuesFrom(objectProperty,classForRoleAndIndividual));
                        additionalAxioms.add(axiom); // A_a implies forall r.A_a^r
                        noAdditionalAxioms+=2;
                        m_interruptFlag.checkInterrupt();
                    }
                }
            } finally {
                m_interruptFlag.endTask();
            }
            OWLAxiom[] additionalAxiomsArray=new OWLAxiom[additionalAxioms.size()];
            return additionalAxioms.toArray(additionalAxiomsArray);
        }
        else {
            m_currentIndividualIndex=m_individuals.length-1;
            return new OWLAxiom[0];
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void initializeKnowAndPossibleClassInstances(ReasonerProgressMonitor monitor, int completedSteps, int steps) {
        if (!this.m_classesInitialised) {
            this.m_interruptFlag.startTask();
            try {
                this.initializeIndividualsForNodes();
                if (!this.m_propertiesInitialised) {
                    this.initializeSameAs();
                }
                completedSteps = this.readOffClassInstancesByIndividual(monitor, completedSteps, steps);
                if (!this.m_readingOffFoundPossibleConceptInstance && this.m_usesClassifiedConceptHierarchy) {
                    this.m_realizationCompleted = true;
                }
                this.m_classesInitialised = true;
                this.m_individualsForNodes.clear();
                this.m_canonicalNodeToDetMergedNodes.clear();
                this.m_canonicalNodeToNonDetMergedNodes.clear();
            }
            finally {
                this.m_interruptFlag.endTask();
            }
        }
    }

    protected int readOffClassInstancesByIndividual(ReasonerProgressMonitor monitor, int completedSteps, int steps) {
        for (Individual ind : this.m_individuals) {
            Node nodeForIndividual = this.m_nodesForIndividuals.get(ind);
            boolean hasType = this.readOffTypes(ind, nodeForIndividual);
            if (!hasType) {
                AtomicConceptElement topElement = this.m_conceptToElement.get(this.m_topConcept);
                if (topElement == null) {
                    topElement = new AtomicConceptElement(null, null);
                    this.m_conceptToElement.put(this.m_topConcept, topElement);
                }
                topElement.m_knownInstances.add(ind);
            }
            ++completedSteps;
            if (monitor != null) {
                monitor.reasonerTaskProgressChanged(completedSteps, steps);
            }
            this.m_interruptFlag.checkInterrupt();
        }
        return completedSteps;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public int initializeKnowAndPossiblePropertyInstances(ReasonerProgressMonitor monitor, int startIndividualIndex, int completedSteps, int steps) {
        if (!this.m_propertiesInitialised) {
            this.m_interruptFlag.startTask();
            try {
                this.initializeIndividualsForNodes();
                if (!this.m_classesInitialised) {
                    this.initializeSameAs();
                }
                completedSteps = this.readOffPropertyInstancesByIndividual(monitor, completedSteps, steps, startIndividualIndex);
                if (this.m_currentIndividualIndex >= this.m_individuals.length - 1) {
                    if (!this.m_readingOffFoundPossiblePropertyInstance) {
                        this.m_roleRealizationCompleted = true;
                    }
                    this.m_propertiesInitialised = true;
                }
                this.m_individualsForNodes.clear();
            }
            finally {
                this.m_interruptFlag.endTask();
            }
        }
        return completedSteps;
    }

    protected int readOffPropertyInstancesByIndividual(ReasonerProgressMonitor monitor, int completedSteps, int steps, int startIndividualIndex) {
        int endIndex = startIndividualIndex == 0 ? this.m_individuals.length : this.m_currentIndividualIndex;
        for (int index = startIndividualIndex; index < endIndex; ++index) {
            Individual ind = this.m_individuals[index];
            Node nodeForIndividual = this.m_nodesForIndividuals.get(ind);
            if (startIndividualIndex == 0) {
                if (!nodeForIndividual.isMerged()) {
                    this.readOffPropertyInstances(nodeForIndividual);
                }
                ++completedSteps;
                if (monitor != null) {
                    monitor.reasonerTaskProgressChanged(completedSteps, steps);
                }
            }
            if (index < this.m_currentIndividualIndex) {
                completedSteps = this.readOffComplexRoleSuccessors(ind, monitor, completedSteps, steps);
            }
            this.m_interruptFlag.checkInterrupt();
        }
        return completedSteps;
    }

    protected void initializeIndividualsForNodes() {
        for (Individual ind : this.m_individuals) {
            Node node = this.m_nodesForIndividuals.get(ind);
            this.m_individualsForNodes.put(node, ind);
            if (node.isMerged()) {
                Set<Node> merged;
                Node canonicalNode = node.getCanonicalNode();
                if (node.getCanonicalNodeDependencySet() == null) {
                    merged = this.m_canonicalNodeToDetMergedNodes.get(canonicalNode);
                    if (merged == null) {
                        merged = new HashSet<Node>();
                        this.m_canonicalNodeToDetMergedNodes.put(canonicalNode, merged);
                    }
                    merged.add(node);
                } else {
                    merged = this.m_canonicalNodeToNonDetMergedNodes.get(canonicalNode);
                    if (merged == null) {
                        merged = new HashSet<Node>();
                        this.m_canonicalNodeToNonDetMergedNodes.put(canonicalNode, merged);
                    }
                    merged.add(node);
                }
            }
            this.m_interruptFlag.checkInterrupt();
        }
    }

    protected void initializeSameAs() {
        this.m_individualToPossibleEquivalenceClass = new HashMap<Set<Individual>, Set<Set<Individual>>>();
        for (Node node : this.m_individualsForNodes.keySet()) {
            Node mergedInto = node.getMergedInto();
            if (mergedInto != null) {
                Individual individual1 = this.m_individualsForNodes.get(node);
                Individual individual2 = this.m_individualsForNodes.get(mergedInto);
                Set<Individual> individual1Equivalences = this.m_individualToEquivalenceClass.get(individual1);
                Set<Individual> individual2Equivalences = this.m_individualToEquivalenceClass.get(individual2);
                if (node.getMergedIntoDependencySet().isEmpty()) {
                    individual1Equivalences.addAll(individual2Equivalences);
                    this.m_individualToEquivalenceClass.put(individual2, individual1Equivalences);
                } else {
                    Set<Set<Individual>> possibleEquivalenceClasses = this.m_individualToPossibleEquivalenceClass.get(individual1Equivalences);
                    if (possibleEquivalenceClasses == null) {
                        possibleEquivalenceClasses = new HashSet<Set<Individual>>();
                        this.m_individualToPossibleEquivalenceClass.put(individual1Equivalences, possibleEquivalenceClasses);
                    }
                    possibleEquivalenceClasses.add(individual2Equivalences);
                }
            }
            this.m_interruptFlag.checkInterrupt();
        }
    }

    protected boolean readOffTypes(Individual ind, Node nodeForIndividual) {
        boolean hasBeenAdded = false;
        this.m_binaryRetrieval1Bound.getBindingsBuffer()[1] = nodeForIndividual.getCanonicalNode();
        this.m_binaryRetrieval1Bound.open();
        Object[] tupleBuffer = this.m_binaryRetrieval1Bound.getTupleBuffer();
        while (!this.m_binaryRetrieval1Bound.afterLast()) {
            AtomicConcept atomicConcept;
            Object predicate = tupleBuffer[0];
            if (predicate instanceof AtomicConcept && !(atomicConcept = (AtomicConcept)predicate).equals(this.m_topConcept) && !Prefixes.isInternalIRI(atomicConcept.getIRI())) {
                HierarchyNode<AtomicConcept> node = this.m_currentConceptHierarchy.getNodeForElement(atomicConcept);
                AtomicConcept representative = node.getRepresentative();
                AtomicConceptElement element = this.m_conceptToElement.get(representative);
                if (element == null) {
                    element = new AtomicConceptElement(null, null);
                    this.m_conceptToElement.put(representative, element);
                }
                hasBeenAdded = true;
                if (this.m_binaryRetrieval1Bound.getDependencySet().isEmpty()) {
                    this.addKnownConceptInstance(node, element, ind);
                } else {
                    this.addPossibleConceptInstance(node, element, ind);
                    this.m_readingOffFoundPossibleConceptInstance = true;
                }
            }
            this.m_interruptFlag.checkInterrupt();
            this.m_binaryRetrieval1Bound.next();
        }
        return hasBeenAdded;
    }

    protected void readOffPropertyInstances(Node nodeForIndividual) {
        this.m_ternaryRetrieval1Bound.getBindingsBuffer()[1] = nodeForIndividual;
        this.m_ternaryRetrieval1Bound.open();
        Object[] tupleBuffer = this.m_ternaryRetrieval1Bound.getTupleBuffer();
        while (!this.m_ternaryRetrieval1Bound.afterLast()) {
            AtomicRole atomicrole;
            Object roleObject = tupleBuffer[0];
            Node successorNode = (Node)tupleBuffer[2];
            if (roleObject instanceof AtomicRole && !successorNode.isMerged() && successorNode.getNodeType() == NodeType.NAMED_NODE && this.m_individualsForNodes.containsKey(successorNode) && successorNode.isActive() && !(atomicrole = (AtomicRole)roleObject).equals(AtomicRole.TOP_OBJECT_ROLE) && this.m_roleElementManager.m_roleToElement.containsKey(atomicrole)) {
                Individual sourceIndividual;
                Individual targetIndividual;
                Set<Node> equivalentToSuccessor;
                RoleElementManager.RoleElement representative = this.m_currentRoleHierarchy.getNodeForElement(this.m_roleElementManager.getRoleElement(atomicrole)).getRepresentative();
                Set<Node> equivalentToNode = this.m_canonicalNodeToDetMergedNodes.get(nodeForIndividual);
                if (equivalentToNode == null) {
                    equivalentToNode = new HashSet<Node>();
                }
                equivalentToNode.add(nodeForIndividual);
                Set<Node> possiblyEquivalentToNode = this.m_canonicalNodeToNonDetMergedNodes.get(nodeForIndividual);
                if (possiblyEquivalentToNode == null) {
                    possiblyEquivalentToNode = new HashSet<Node>();
                }
                if ((equivalentToSuccessor = this.m_canonicalNodeToDetMergedNodes.get(successorNode)) == null) {
                    equivalentToSuccessor = new HashSet<Node>();
                }
                equivalentToSuccessor.add(successorNode);
                Set<Node> possiblyEquivalentToSuccessor = this.m_canonicalNodeToNonDetMergedNodes.get(successorNode);
                if (possiblyEquivalentToSuccessor == null) {
                    possiblyEquivalentToSuccessor = new HashSet<Node>();
                }
                for (Node sourceNode : equivalentToNode) {
                    sourceIndividual = this.m_individualsForNodes.get(sourceNode);
                    for (Node targetNode : equivalentToSuccessor) {
                        targetIndividual = this.m_individualsForNodes.get(targetNode);
                        if (this.m_ternaryRetrieval1Bound.getDependencySet().isEmpty()) {
                            this.addKnownRoleInstance(representative, sourceIndividual, targetIndividual);
                            continue;
                        }
                        this.m_readingOffFoundPossiblePropertyInstance = true;
                        this.addPossibleRoleInstance(representative, sourceIndividual, targetIndividual);
                    }
                    for (Node targetNode : possiblyEquivalentToSuccessor) {
                        targetIndividual = this.m_individualsForNodes.get(targetNode);
                        this.m_readingOffFoundPossiblePropertyInstance = true;
                        this.addPossibleRoleInstance(representative, sourceIndividual, targetIndividual);
                    }
                }
                for (Node sourceNode : new ArrayList<Node>(possiblyEquivalentToNode)) {
                    sourceIndividual = this.m_individualsForNodes.get(sourceNode);
                    possiblyEquivalentToSuccessor.addAll(equivalentToSuccessor);
                    for (Node targetNode : possiblyEquivalentToSuccessor) {
                        targetIndividual = this.m_individualsForNodes.get(targetNode);
                        this.m_readingOffFoundPossiblePropertyInstance = true;
                        this.addPossibleRoleInstance(representative, sourceIndividual, targetIndividual);
                    }
                }
            }
            this.m_interruptFlag.checkInterrupt();
            this.m_ternaryRetrieval1Bound.next();
        }
    }

    protected int readOffComplexRoleSuccessors(Individual ind, ReasonerProgressMonitor monitor, int completedSteps, int steps) {
        String indIRI = ind.getIRI();
        for (AtomicRole atomicRole : this.m_complexRoles) {
            AtomicConcept conceptForRole = AtomicConcept.create("internal:individual-concept#" + atomicRole.getIRI() + "#" + indIRI);
            this.m_binaryRetrieval0Bound.getBindingsBuffer()[0] = conceptForRole;
            this.m_binaryRetrieval0Bound.open();
            Object[] tupleBuffer = this.m_binaryRetrieval0Bound.getTupleBuffer();
            while (!this.m_binaryRetrieval0Bound.afterLast()) {
                Node node = (Node)tupleBuffer[1];
                if (node.isActive() && node.getNodeType() == NodeType.NAMED_NODE && this.m_individualsForNodes.containsKey(node)) {
                    Individual targetIndividual;
                    RoleElementManager.RoleElement representative = this.m_currentRoleHierarchy.getNodeForElement(this.m_roleElementManager.getRoleElement(atomicRole)).getRepresentative();
                    Set<Node> equivalentToSuccessor = this.m_canonicalNodeToDetMergedNodes.get(node);
                    if (equivalentToSuccessor == null) {
                        equivalentToSuccessor = new HashSet<Node>();
                    }
                    equivalentToSuccessor.add(node);
                    Set<Node> possiblyEquivalentToSuccessor = this.m_canonicalNodeToNonDetMergedNodes.get(node);
                    if (possiblyEquivalentToSuccessor == null) {
                        possiblyEquivalentToSuccessor = new HashSet<Node>();
                    }
                    for (Node targetNode : equivalentToSuccessor) {
                        targetIndividual = this.m_individualsForNodes.get(targetNode);
                        if (this.m_binaryRetrieval0Bound.getDependencySet().isEmpty()) {
                            this.addKnownRoleInstance(representative, ind, targetIndividual);
                            continue;
                        }
                        this.m_readingOffFoundPossiblePropertyInstance = true;
                        this.addPossibleRoleInstance(representative, ind, targetIndividual);
                    }
                    for (Node targetNode : possiblyEquivalentToSuccessor) {
                        targetIndividual = this.m_individualsForNodes.get(targetNode);
                        this.m_readingOffFoundPossiblePropertyInstance = true;
                        this.addPossibleRoleInstance(representative, ind, targetIndividual);
                    }
                }
                this.m_interruptFlag.checkInterrupt();
                this.m_binaryRetrieval0Bound.next();
            }
            ++completedSteps;
            if (monitor == null) continue;
            monitor.reasonerTaskProgressChanged(completedSteps, steps);
        }
        return completedSteps;
    }

    protected void addKnownConceptInstance(HierarchyNode<AtomicConcept> currentNode, AtomicConceptElement element, Individual instance) {
        Set<HierarchyNode<AtomicConcept>> nodes = currentNode.getDescendantNodes();
        for (HierarchyNode<AtomicConcept> node : nodes) {
            AtomicConceptElement descendantElement = this.m_conceptToElement.get(node.getRepresentative());
            if (descendantElement != null && descendantElement.m_knownInstances.contains(instance)) {
                return;
            }
            this.m_interruptFlag.checkInterrupt();
        }
        element.m_knownInstances.add(instance);
        nodes = currentNode.getAncestorNodes();
        nodes.remove(currentNode);
        for (HierarchyNode<AtomicConcept> node : nodes) {
            AtomicConceptElement ancestorElement = this.m_conceptToElement.get(node.getRepresentative());
            if (ancestorElement == null) continue;
            ancestorElement.m_knownInstances.remove(instance);
            ancestorElement.m_possibleInstances.remove(instance);
        }
    }

    protected void addPossibleConceptInstance(HierarchyNode<AtomicConcept> currentNode, AtomicConceptElement element, Individual instance) {
        Set<HierarchyNode<AtomicConcept>> nodes = currentNode.getDescendantNodes();
        for (HierarchyNode<AtomicConcept> node : nodes) {
            AtomicConceptElement descendantElement = this.m_conceptToElement.get(node.getRepresentative());
            if (descendantElement != null && (descendantElement.m_knownInstances.contains(instance) || descendantElement.m_possibleInstances.contains(instance))) {
                return;
            }
            this.m_interruptFlag.checkInterrupt();
        }
        element.m_possibleInstances.add(instance);
        nodes = currentNode.getAncestorNodes();
        nodes.remove(currentNode);
        for (HierarchyNode<AtomicConcept> node : nodes) {
            AtomicConceptElement ancestorElement = this.m_conceptToElement.get(node.getRepresentative());
            if (ancestorElement != null) {
                ancestorElement.m_possibleInstances.remove(instance);
                if (ancestorElement.m_possibleInstances.isEmpty() && ancestorElement.m_knownInstances.isEmpty() && node.getRepresentative() != this.m_topConcept) {
                    this.m_conceptToElement.remove(node.getRepresentative());
                }
            }
            this.m_interruptFlag.checkInterrupt();
        }
    }

    protected void addKnownRoleInstance(RoleElementManager.RoleElement element, Individual individual1, Individual individual2) {
        if (!element.equals(this.m_topRoleElement)) {
            HierarchyNode<RoleElementManager.RoleElement> currentNode = this.m_currentRoleHierarchy.getNodeForElement(element);
            Set<HierarchyNode<RoleElementManager.RoleElement>> nodes = currentNode.getDescendantNodes();
            for (HierarchyNode<RoleElementManager.RoleElement> node : nodes) {
                for (RoleElementManager.RoleElement descendantElement : node.getEquivalentElements()) {
                    if (!descendantElement.isKnown(individual1, individual2)) continue;
                    return;
                }
                this.m_interruptFlag.checkInterrupt();
            }
            element.addKnown(individual1, individual2);
            nodes = currentNode.getAncestorNodes();
            nodes.remove(currentNode);
            for (HierarchyNode<RoleElementManager.RoleElement> node : nodes) {
                node.getRepresentative().removeKnown(individual1, individual2);
                this.m_interruptFlag.checkInterrupt();
            }
        }
    }

    protected void addPossibleRoleInstance(RoleElementManager.RoleElement element, Individual individual1, Individual individual2) {
        if (!element.equals(this.m_topRoleElement)) {
            HierarchyNode<RoleElementManager.RoleElement> currentNode = this.m_currentRoleHierarchy.getNodeForElement(element);
            Set<HierarchyNode<RoleElementManager.RoleElement>> nodes = currentNode.getDescendantNodes();
            for (HierarchyNode<RoleElementManager.RoleElement> node : nodes) {
                for (RoleElementManager.RoleElement descendantElement : node.getEquivalentElements()) {
                    if (!descendantElement.isPossible(individual1, individual2)) continue;
                    return;
                }
                this.m_interruptFlag.checkInterrupt();
            }
            element.addPossible(individual1, individual2);
            nodes = currentNode.getAncestorNodes();
            nodes.remove(currentNode);
            for (HierarchyNode<RoleElementManager.RoleElement> node : nodes) {
                for (RoleElementManager.RoleElement ancestorElement : node.getEquivalentElements()) {
                    if (!ancestorElement.isPossible(individual1, individual2)) continue;
                    ancestorElement.removePossible(individual1, individual2);
                }
                this.m_interruptFlag.checkInterrupt();
            }
        }
    }

    public void setInconsistent() {
        this.m_isInconsistent = true;
        this.m_realizationCompleted = true;
        this.m_roleRealizationCompleted = true;
        this.m_usesClassifiedConceptHierarchy = true;
        this.m_currentConceptHierarchy = null;
        this.m_currentRoleHierarchy = null;
    }

    public void realize(ReasonerProgressMonitor monitor) {
        assert (this.m_usesClassifiedConceptHierarchy);
        if (this.m_readingOffFoundPossibleConceptInstance && !this.m_realizationCompleted) {
            if (monitor != null) {
                monitor.reasonerTaskStarted("Computing instances for all classes");
            }
            int numHierarchyNodes = this.m_currentConceptHierarchy.m_nodesByElements.values().size();
            int currentHierarchyNode = 0;
            LinkedList toProcess = new LinkedList();
            HashSet<HierarchyNode> visited = new HashSet<HierarchyNode>();
            toProcess.addAll(this.m_currentConceptHierarchy.m_bottomNode.m_parentNodes);
            while (!toProcess.isEmpty()) {
                if (monitor != null) {
                    monitor.reasonerTaskProgressChanged(currentHierarchyNode, numHierarchyNodes);
                }
                HierarchyNode current = (HierarchyNode)toProcess.remove();
                visited.add(current);
                ++currentHierarchyNode;
                AtomicConcept atomicConcept = (AtomicConcept)current.getRepresentative();
                AtomicConceptElement atomicConceptElement = this.m_conceptToElement.get(atomicConcept);
                if (atomicConceptElement != null) {
                    Set<HierarchyNode> parents = current.getParentNodes();
                    for (HierarchyNode parent : parents) {
                        if (visited.contains(parent) || toProcess.contains(parent)) continue;
                        toProcess.add(parent);
                    }
                    if (atomicConceptElement.hasPossibles()) {
                        HashSet<Individual> nonInstances = new HashSet<Individual>();
                        for (Individual individual : atomicConceptElement.getPossibleInstances()) {
                            if (this.isInstance(individual, atomicConcept)) {
                                atomicConceptElement.m_knownInstances.add(individual);
                                continue;
                            }
                            nonInstances.add(individual);
                        }
                        atomicConceptElement.m_possibleInstances.clear();
                        for (HierarchyNode parent2 : parents) {
                            AtomicConcept parentRepresentative = (AtomicConcept)parent2.getRepresentative();
                            AtomicConceptElement parentElement = this.m_conceptToElement.get(parentRepresentative);
                            if (parentElement == null) {
                                parentElement = new AtomicConceptElement(null, nonInstances);
                                this.m_conceptToElement.put(parentRepresentative, parentElement);
                                continue;
                            }
                            if (parentRepresentative.equals(this.m_topConcept)) {
                                this.m_conceptToElement.get(this.m_topConcept).m_knownInstances.addAll(nonInstances);
                                continue;
                            }
                            parentElement.addPossibles(nonInstances);
                        }
                    }
                }
                this.m_interruptFlag.checkInterrupt();
            }
            if (monitor != null) {
                monitor.reasonerTaskStopped();
            }
        }
        this.m_realizationCompleted = true;
    }

    public void realizeObjectRoles(ReasonerProgressMonitor monitor) {
        if (this.m_readingOffFoundPossiblePropertyInstance && !this.m_roleRealizationCompleted) {
            if (monitor != null) {
                monitor.reasonerTaskStarted("Computing instances for all object properties...");
            }
            int numHierarchyNodes = this.m_currentRoleHierarchy.m_nodesByElements.values().size();
            int currentHierarchyNode = 0;
            LinkedList toProcess = new LinkedList();
            HashSet<HierarchyNode> visited = new HashSet<HierarchyNode>();
            toProcess.add(this.m_currentRoleHierarchy.m_bottomNode);
            while (!toProcess.isEmpty()) {
                if (monitor != null) {
                    monitor.reasonerTaskProgressChanged(currentHierarchyNode, numHierarchyNodes);
                }
                HierarchyNode current = (HierarchyNode)toProcess.remove();
                visited.add(current);
                ++currentHierarchyNode;
                RoleElementManager.RoleElement roleElement = (RoleElementManager.RoleElement)current.getRepresentative();
                AtomicRole role = roleElement.getRole();
                Set<HierarchyNode> parents = current.getParentNodes();
                for (HierarchyNode parent : parents) {
                    if (toProcess.contains(parent) || visited.contains(parent)) continue;
                    toProcess.add(parent);
                }
                if (roleElement.hasPossibles()) {
                    for (Individual individual : roleElement.m_possibleRelations.keySet()) {
                        HashSet<Individual> nonInstances = new HashSet<Individual>();
                        for (Individual successor : roleElement.m_possibleRelations.get(individual)) {
                            if (this.isRoleInstance(role, individual, successor)) {
                                roleElement.addKnown(individual, successor);
                                continue;
                            }
                            nonInstances.add(individual);
                        }
                        for (HierarchyNode parent : parents) {
                            RoleElementManager.RoleElement parentRepresentative = (RoleElementManager.RoleElement)parent.getRepresentative();
                            if (parentRepresentative.equals(this.m_topRoleElement)) continue;
                            parentRepresentative.addPossibles(individual, nonInstances);
                        }
                    }
                    roleElement.m_possibleRelations.clear();
                }
                this.m_interruptFlag.checkInterrupt();
            }
            if (monitor != null) {
                monitor.reasonerTaskStopped();
            }
        }
        this.m_roleRealizationCompleted = true;
    }

    public Set<HierarchyNode<AtomicConcept>> getTypes(Individual individual, boolean direct) {
        if (this.m_isInconsistent) {
            return Collections.singleton(this.m_currentConceptHierarchy.m_bottomNode);
        }
        HashSet<HierarchyNode<AtomicConcept>> result = new HashSet<HierarchyNode<AtomicConcept>>();
        assert (!direct || this.m_usesClassifiedConceptHierarchy);
        LinkedList toProcess = new LinkedList();
        HashSet<HierarchyNode> visited = new HashSet<HierarchyNode>();
        toProcess.add(this.m_currentConceptHierarchy.m_bottomNode);
        while (!toProcess.isEmpty()) {
            HierarchyNode current = (HierarchyNode)toProcess.remove();
            boolean ancestor = true;
            block1 : while (ancestor && current != null) {
                ancestor = false;
                for (HierarchyNode node : result) {
                    if (!current.isDescendantElement(node.m_representative)) continue;
                    ancestor = true;
                    visited.add(current);
                    if (!toProcess.isEmpty()) {
                        current = (HierarchyNode)toProcess.remove();
                        continue block1;
                    }
                    current = null;
                    continue block1;
                }
            }
            if (current == null) continue;
            Set<HierarchyNode> parents = current.getParentNodes();
            AtomicConcept atomicConcept = (AtomicConcept)current.getRepresentative();
            AtomicConceptElement atomicConceptElement = this.m_conceptToElement.get(atomicConcept);
            if (atomicConceptElement != null && atomicConceptElement.isPossible(individual)) {
                if (this.isInstance(individual, atomicConcept)) {
                    atomicConceptElement.setToKnown(individual);
                } else {
                    for (HierarchyNode parent : parents) {
                        AtomicConcept parentRepresentative = (AtomicConcept)parent.getRepresentative();
                        AtomicConceptElement parentElement = this.m_conceptToElement.get(parentRepresentative);
                        if (parentElement == null) {
                            parentElement = new AtomicConceptElement(null, null);
                            this.m_conceptToElement.put(parentRepresentative, parentElement);
                        }
                        parentElement.addPossible(individual);
                    }
                }
            }
            if (atomicConceptElement != null && atomicConceptElement.isKnown(individual)) {
                if (direct) {
                    result.add(current);
                    continue;
                }
                result.addAll(current.getAncestorNodes());
                continue;
            }
            for (HierarchyNode parent : parents) {
                if (toProcess.contains(parent) || visited.contains(parent)) continue;
                toProcess.add(parent);
            }
        }
        return result;
    }

    public boolean hasType(Individual individual, AtomicConcept atomicConcept, boolean direct) {
        HierarchyNode<AtomicConcept> node = this.m_currentConceptHierarchy.getNodeForElement(atomicConcept);
        if (node == null) {
            return false;
        }
        return this.hasType(individual, node, direct);
    }

    public boolean hasType(Individual individual, HierarchyNode<AtomicConcept> node, boolean direct) {
        block9 : {
            block8 : {
                assert (!direct || this.m_usesClassifiedConceptHierarchy);
                AtomicConcept representative = node.getRepresentative();
                if (representative == this.m_bottomConcept) {
                    return false;
                }
                AtomicConceptElement element = this.m_conceptToElement.get(representative);
                if (element != null && element.isKnown(individual) || !direct && node == this.m_currentConceptHierarchy.m_topNode) {
                    return true;
                }
                if (element == null || !element.isPossible(individual)) break block8;
                if (this.isInstance(individual, representative)) {
                    element.setToKnown(individual);
                    return true;
                }
                element.m_possibleInstances.remove(individual);
                if (element.m_knownInstances.isEmpty() && element.m_possibleInstances.isEmpty() && representative != this.m_topConcept) {
                    this.m_conceptToElement.remove(representative);
                }
                for (HierarchyNode<AtomicConcept> parent : node.getParentNodes()) {
                    AtomicConcept parentConcept = parent.getRepresentative();
                    AtomicConceptElement parentElement = this.m_conceptToElement.get(parentConcept);
                    if (parentElement == null) {
                        parentElement = new AtomicConceptElement(null, null);
                        this.m_conceptToElement.put(parentConcept, parentElement);
                    }
                    parentElement.addPossible(individual);
                }
                break block9;
            }
            if (direct) break block9;
            for (HierarchyNode<AtomicConcept> child : node.getChildNodes()) {
                if (!this.hasType(individual, child, false)) continue;
                return true;
            }
        }
        return false;
    }

    public Set<Individual> getInstances(AtomicConcept atomicConcept, boolean direct) {
        HashSet<Individual> result = new HashSet<Individual>();
        HierarchyNode<AtomicConcept> node = this.m_currentConceptHierarchy.getNodeForElement(atomicConcept);
        if (node == null) {
            return result;
        }
        this.getInstancesForNode(node, result, direct);
        return result;
    }

    public Set<Individual> getInstances(HierarchyNode<AtomicConcept> node, boolean direct) {
        HashSet<Individual> result = new HashSet<Individual>();
        HierarchyNode<AtomicConcept> nodeFromCurrentHierarchy = this.m_currentConceptHierarchy.getNodeForElement(node.m_representative);
        if (nodeFromCurrentHierarchy == null) {
            if (!direct) {
                for (HierarchyNode<AtomicConcept> child : node.getChildNodes()) {
                    this.getInstancesForNode(child, result, direct);
                }
            }
        } else {
            this.getInstancesForNode(nodeFromCurrentHierarchy, result, direct);
        }
        return result;
    }

    protected void getInstancesForNode(HierarchyNode<AtomicConcept> node, Set<Individual> result, boolean direct) {
        assert (!direct || this.m_usesClassifiedConceptHierarchy);
        AtomicConcept representative = node.getRepresentative();
        if (!direct && representative.equals(this.m_topConcept)) {
            for (Individual individual : this.m_individuals) {
                if (!InstanceManager.isResultRelevantIndividual(individual)) continue;
                result.add(individual);
            }
            return;
        }
        AtomicConceptElement representativeElement = this.m_conceptToElement.get(representative);
        if (representativeElement != null) {
            Set<Individual> possibleInstances = representativeElement.getPossibleInstances();
            if (!possibleInstances.isEmpty()) {
                for (Individual possibleInstance : new HashSet<>(possibleInstances)) {
                    if (this.isInstance(possibleInstance, representative)) {
                        representativeElement.setToKnown(possibleInstance);
                        continue;
                    }
                    representativeElement.m_possibleInstances.remove(possibleInstance);
                    if (representativeElement.m_knownInstances.isEmpty() && representativeElement.m_possibleInstances.isEmpty() && representative != this.m_topConcept) {
                        this.m_conceptToElement.remove(representative);
                    }
                    for (HierarchyNode<AtomicConcept> parent : node.getParentNodes()) {
                        AtomicConcept parentConcept = parent.getRepresentative();
                        AtomicConceptElement parentElement = this.m_conceptToElement.get(parentConcept);
                        if (parentElement == null) {
                            parentElement = new AtomicConceptElement(null, null);
                            this.m_conceptToElement.put(parentConcept, parentElement);
                        }
                        parentElement.addPossible(possibleInstance);
                    }
                }
            }
            for (Individual individual : representativeElement.getKnownInstances()) {
                if (!InstanceManager.isResultRelevantIndividual(individual)) continue;
                boolean isDirect = true;
                if (direct) {
                    for (HierarchyNode<AtomicConcept> child : node.getChildNodes()) {
                        if (!this.hasType(individual, child, false)) continue;
                        isDirect = false;
                        break;
                    }
                }
                if (direct && !isDirect) continue;
                result.add(individual);
            }
        }
        if (!direct) {
            for (HierarchyNode child : node.getChildNodes()) {
                if (child == this.m_currentConceptHierarchy.m_bottomNode) continue;
                this.getInstancesForNode(child, result, false);
            }
        }
    }

    public boolean hasObjectRoleRelationship(AtomicRole role, Individual individual1, Individual individual2) {
        RoleElementManager.RoleElement element = this.m_roleElementManager.getRoleElement(role);
        HierarchyNode<RoleElementManager.RoleElement> currentNode = this.m_currentRoleHierarchy.getNodeForElement(element);
        if (currentNode == null) {
            return false;
        }
        return this.hasObjectRoleRelationship(currentNode, individual1, individual2);
    }

    public boolean hasObjectRoleRelationship(HierarchyNode<RoleElementManager.RoleElement> node, Individual individual1, Individual individual2) {
        boolean containsUnknown;
        RoleElementManager.RoleElement representativeElement = node.getRepresentative();
        if (representativeElement.isKnown(individual1, individual2) || representativeElement.equals(this.m_topRoleElement)) {
            return true;
        }
        List<Individual> individuals = Arrays.asList(this.m_individuals);
        boolean bl = containsUnknown = !individuals.contains(individual1) || !individuals.contains(individual2);
        if (representativeElement.isPossible(individual1, individual2) || containsUnknown) {
            if (this.isRoleInstance(representativeElement.getRole(), individual1, individual2)) {
                if (!containsUnknown) {
                    representativeElement.setToKnown(individual1, individual2);
                }
                return true;
            }
            for (HierarchyNode<RoleElementManager.RoleElement> parent : node.getParentNodes()) {
                parent.getRepresentative().addPossible(individual1, individual2);
            }
        } else {
            for (HierarchyNode<RoleElementManager.RoleElement> child : node.getChildNodes()) {
                if (!this.hasObjectRoleRelationship(child, individual1, individual2)) continue;
                return true;
            }
        }
        return false;
    }

    public Map<Individual, Set<Individual>> getObjectPropertyInstances(AtomicRole role) {
        HashMap<Individual, Set<Individual>> result = new HashMap<Individual, Set<Individual>>();
        HierarchyNode<RoleElementManager.RoleElement> node = this.m_currentRoleHierarchy.getNodeForElement(this.m_roleElementManager.getRoleElement(role));
        if (node == null) {
            return result;
        }
        this.getObjectPropertyInstances(node, result);
        return result;
    }

    protected void getObjectPropertyInstances(HierarchyNode<RoleElementManager.RoleElement> node, Map<Individual, Set<Individual>> result) {
        RoleElementManager.RoleElement representativeElement = node.getRepresentative();
        if (representativeElement.equals(this.m_topRoleElement) || this.m_isInconsistent) {
            HashSet<Individual> allResultRelevantIndividuals = new HashSet<Individual>();
            for (Individual individual : this.m_individuals) {
                if (!InstanceManager.isResultRelevantIndividual(individual)) continue;
                allResultRelevantIndividuals.add(individual);
                result.put(individual, allResultRelevantIndividuals);
            }
            return;
        }
        Map<Individual, Set<Individual>> possibleInstances = representativeElement.getPossibleRelations();
        for (Individual possibleInstance : new HashSet<Individual>(possibleInstances.keySet())) {
            for (Individual possibleSuccessor : new HashSet<>(possibleInstances.get(possibleInstance))) {
                if (this.isRoleInstance(representativeElement.getRole(), possibleInstance, possibleSuccessor)) {
                    representativeElement.setToKnown(possibleInstance, possibleSuccessor);
                    continue;
                }
                for (HierarchyNode<RoleElementManager.RoleElement> parent : node.getParentNodes()) {
                    parent.getRepresentative().addPossible(possibleInstance, possibleSuccessor);
                }
            }
        }
        Map<Individual, Set<Individual>> knownInstances = representativeElement.getKnownRelations();
        for (Individual instance1 : knownInstances.keySet()) {
            if (!InstanceManager.isResultRelevantIndividual(instance1)) continue;
            Set<Individual> successors = result.get(instance1);
            boolean isNew = false;
            if (successors == null) {
                successors = new HashSet<Individual>();
                isNew = true;
            }
            for (Individual instance2 : knownInstances.get(instance1)) {
                if (!InstanceManager.isResultRelevantIndividual(instance2)) continue;
                successors.add(instance2);
            }
            if (!isNew || successors.isEmpty()) continue;
            result.put(instance1, successors);
        }
        for (HierarchyNode<RoleElementManager.RoleElement> child : node.getChildNodes()) {
            this.getObjectPropertyInstances(child, result);
        }
    }

    public Set<Individual> getObjectPropertyValues(AtomicRole role, Individual individual) {
        HashSet<Individual> result = new HashSet<Individual>();
        HierarchyNode<RoleElementManager.RoleElement> node = this.m_currentRoleHierarchy.getNodeForElement(this.m_roleElementManager.getRoleElement(role));
        this.getObjectPropertyValues(node, individual, result);
        return result;
    }

    public Set<Individual> getObjectPropertySubjects(AtomicRole role, Individual individual) {
        HashSet<Individual> result = new HashSet<Individual>();
        HierarchyNode<RoleElementManager.RoleElement> node = this.m_currentRoleHierarchy.getNodeForElement(this.m_roleElementManager.getRoleElement(role));
        this.getObjectPropertySubjects(node, individual, result);
        return result;
    }

    protected void getObjectPropertySubjects(HierarchyNode<RoleElementManager.RoleElement> node, Individual object, Set<Individual> result) {
        RoleElementManager.RoleElement representativeElement = node.getRepresentative();
        if (representativeElement.equals(this.m_topRoleElement) || this.m_isInconsistent) {
            for (Individual ind : this.m_individuals) {
                if (!InstanceManager.isResultRelevantIndividual(ind)) continue;
                result.add(ind);
            }
            return;
        }
        Map<Individual, Set<Individual>> relevantRelations = representativeElement.getKnownRelations();
        for (Individual subject : new HashSet<Individual>(relevantRelations.keySet())) {
            if (!InstanceManager.isResultRelevantIndividual(subject) || !relevantRelations.get(subject).contains(object)) continue;
            result.add(subject);
        }
        relevantRelations = representativeElement.getPossibleRelations();
        for (Individual possibleSubject : new HashSet<Individual>(relevantRelations.keySet())) {
            if (InstanceManager.isResultRelevantIndividual(possibleSubject) && relevantRelations.get(possibleSubject).contains(object) && this.isRoleInstance(representativeElement.getRole(), possibleSubject, object)) {
                representativeElement.setToKnown(possibleSubject, object);
                result.add(possibleSubject);
                continue;
            }
            for (HierarchyNode<RoleElementManager.RoleElement> parent : node.getParentNodes()) {
                parent.getRepresentative().addPossible(possibleSubject, object);
            }
        }
        for (HierarchyNode child : node.getChildNodes()) {
            this.getObjectPropertySubjects(child, object, result);
        }
    }

    protected void getObjectPropertyValues(HierarchyNode<RoleElementManager.RoleElement> node, Individual subject, Set<Individual> result) {
        Set<Individual> knownSuccessors;
        RoleElementManager.RoleElement representativeElement = node.getRepresentative();
        if (representativeElement.equals(this.m_topRoleElement) || this.m_isInconsistent) {
            for (Individual ind : this.m_individuals) {
                if (!InstanceManager.isResultRelevantIndividual(ind)) continue;
                result.add(ind);
            }
            return;
        }
        Set<Individual> possibleSuccessors = representativeElement.getPossibleRelations().get(subject);
        if (possibleSuccessors != null) {
            for (Object possibleSuccessor : new HashSet<Individual>(possibleSuccessors)) {
                if (this.isRoleInstance(representativeElement.getRole(), subject, (Individual)possibleSuccessor)) {
                    representativeElement.setToKnown(subject, (Individual)possibleSuccessor);
                    continue;
                }
                for (HierarchyNode<RoleElementManager.RoleElement> parent : node.getParentNodes()) {
                    parent.getRepresentative().addPossible(subject, (Individual)possibleSuccessor);
                }
            }
        }
        if ((knownSuccessors = representativeElement.getKnownRelations().get(subject)) != null) {
            for (Individual successor : knownSuccessors) {
                if (!InstanceManager.isResultRelevantIndividual(successor)) continue;
                result.add(successor);
            }
        }
        for (HierarchyNode<RoleElementManager.RoleElement> child : node.getChildNodes()) {
            this.getObjectPropertyValues(child, subject, result);
        }
    }

    public Set<Individual> getSameAsIndividuals(Individual individual) {
        Set<Individual> equivalenceClass = this.m_individualToEquivalenceClass.get(individual);
        Set<Set<Individual>> possiblySameEquivalenceClasses = this.m_individualToPossibleEquivalenceClass.get(equivalenceClass);
        if (possiblySameEquivalenceClasses != null) {
            while (!possiblySameEquivalenceClasses.isEmpty()) {
                Set<Individual> possiblyEquivalentClass = possiblySameEquivalenceClasses.iterator().next();
                possiblySameEquivalenceClasses.remove(possiblyEquivalentClass);
                if (possiblySameEquivalenceClasses.isEmpty()) {
                    this.m_individualToPossibleEquivalenceClass.remove(equivalenceClass);
                }
                Individual possiblyEquivalentIndividual = possiblyEquivalentClass.iterator().next();
                if (this.isSameIndividual(equivalenceClass.iterator().next(), possiblyEquivalentIndividual)) {
                    equivalenceClass.addAll(possiblyEquivalentClass);
                    equivalenceClass.addAll(this.m_individualToEquivalenceClass.get(possiblyEquivalentIndividual));
                    Iterator iterator = possiblyEquivalentClass.iterator();
                    while (iterator.hasNext()) {
                        Individual nowKnownEquivalent = (Individual)iterator.next();
                        this.m_individualToEquivalenceClass.put(nowKnownEquivalent, equivalenceClass);
                    }
                    continue;
                }
                Set<Set<Individual>> possiblyEquivalentToNowKnownInequivalent = this.m_individualToPossibleEquivalenceClass.get(possiblyEquivalentClass);
                if (possiblyEquivalentToNowKnownInequivalent == null || !possiblyEquivalentToNowKnownInequivalent.contains(equivalenceClass)) continue;
                possiblyEquivalentToNowKnownInequivalent.remove(equivalenceClass);
                if (!possiblyEquivalentToNowKnownInequivalent.isEmpty()) continue;
                this.m_individualToPossibleEquivalenceClass.remove(possiblyEquivalentClass);
            }
        }
        for (Set<Individual> otherEquivalenceClass : new HashSet<Set<Individual>>(this.m_individualToPossibleEquivalenceClass.keySet())) {
            if (otherEquivalenceClass == equivalenceClass || !this.m_individualToPossibleEquivalenceClass.get(otherEquivalenceClass).contains(equivalenceClass) || !this.isSameIndividual(equivalenceClass.iterator().next(), otherEquivalenceClass.iterator().next())) continue;
            this.m_individualToPossibleEquivalenceClass.get(otherEquivalenceClass).remove(equivalenceClass);
            if (this.m_individualToPossibleEquivalenceClass.get(otherEquivalenceClass).isEmpty()) {
                this.m_individualToPossibleEquivalenceClass.remove(otherEquivalenceClass);
            }
            for (Individual nowKnownEquivalent : otherEquivalenceClass) {
                this.m_individualToEquivalenceClass.put(nowKnownEquivalent, equivalenceClass);
            }
            equivalenceClass.addAll(otherEquivalenceClass);
        }
        return equivalenceClass;
    }

    public boolean isSameIndividual(Individual individual1, Individual individual2) {
        return !this.m_reasoner.getTableau().isSatisfiable(true, false, Collections.singleton(Atom.create(Inequality.INSTANCE, individual1, individual2)), null, null, null, null, new ReasoningTaskDescription(true, "is {0} same as {1}", individual1, individual2));
    }

    public void computeSameAsEquivalenceClasses(ReasonerProgressMonitor progressMonitor) {
        if (!this.m_individualToPossibleEquivalenceClass.isEmpty()) {
            int steps = this.m_individualToPossibleEquivalenceClass.keySet().size();
            if (steps > 0 && progressMonitor != null) {
                progressMonitor.reasonerTaskStarted("Precompute same individuals");
            }
            while (!this.m_individualToPossibleEquivalenceClass.isEmpty()) {
                Set<Individual> equivalenceClass = this.m_individualToPossibleEquivalenceClass.keySet().iterator().next();
                this.getSameAsIndividuals(equivalenceClass.iterator().next());
                if (progressMonitor == null) continue;
                progressMonitor.reasonerTaskProgressChanged(steps - this.m_individualToPossibleEquivalenceClass.keySet().size(), steps);
            }
            if (progressMonitor != null) {
                progressMonitor.reasonerTaskStopped();
            }
        }
    }

    protected boolean isInstance(Individual individual, AtomicConcept atomicConcept) {
        boolean result;
        boolean bl = result = !this.m_reasoner.getTableau().isSatisfiable(true, false, null, Collections.singleton(Atom.create(atomicConcept, individual)), null, null, null, ReasoningTaskDescription.isInstanceOf(atomicConcept, individual));
        if (this.m_tableauMonitor != null) {
            if (result) {
                this.m_tableauMonitor.possibleInstanceIsInstance();
            } else {
                this.m_tableauMonitor.possibleInstanceIsNotInstance();
            }
        }
        return result;
    }

    protected boolean isRoleInstance(Role role, Individual individual1, Individual individual2) {
        AtomicRole atomicRole;
        boolean result;
        OWLDataFactory factory = this.m_reasoner.getDataFactory();
        if (role instanceof InverseRole) {
            Individual tmp = individual1;
            individual1 = individual2;
            individual2 = tmp;
            atomicRole = ((InverseRole)role).getInverseOf();
        } else {
            atomicRole = (AtomicRole)role;
        }
        OWLObjectProperty property = factory.getOWLObjectProperty(IRI.create(atomicRole.getIRI()));
        OWLNamedIndividual namedIndividual1 = factory.getOWLNamedIndividual(IRI.create(individual1.getIRI()));
        OWLNamedIndividual namedIndividual2 = factory.getOWLNamedIndividual(IRI.create(individual2.getIRI()));
        OWLClass pseudoNominal = factory.getOWLClass(IRI.create("internal:pseudo-nominal"));
        OWLObjectAllValuesFrom allNotPseudoNominal = factory.getOWLObjectAllValuesFrom(property, pseudoNominal.getObjectComplementOf());
        OWLClassAssertionAxiom allNotPseudoNominalAssertion = factory.getOWLClassAssertionAxiom(allNotPseudoNominal, namedIndividual1);
        OWLClassAssertionAxiom pseudoNominalAssertion = factory.getOWLClassAssertionAxiom(pseudoNominal, namedIndividual2);
        Tableau tableau = this.m_reasoner.getTableau(allNotPseudoNominalAssertion, pseudoNominalAssertion);
        boolean bl = result = !tableau.isSatisfiable(true, true, null, null, null, null, null, new ReasoningTaskDescription(true, "is {0} connected to {1} via {2}", individual1, individual2, atomicRole));
        if (this.m_tableauMonitor != null) {
            if (result) {
                this.m_tableauMonitor.possibleInstanceIsInstance();
            } else {
                this.m_tableauMonitor.possibleInstanceIsNotInstance();
            }
        }
        return result;
    }

    protected static boolean isResultRelevantIndividual(Individual individual) {
        return !individual.isAnonymous() && !Prefixes.isInternalIRI(individual.getIRI());
    }

    public boolean realizationCompleted() {
        return this.m_realizationCompleted;
    }

    public boolean objectPropertyRealizationCompleted() {
        return this.m_roleRealizationCompleted;
    }

    public boolean sameAsIndividualsComputed() {
        return this.m_individualToPossibleEquivalenceClass.isEmpty();
    }

    public boolean areClassesInitialised() {
        return this.m_classesInitialised;
    }

    public boolean arePropertiesInitialised() {
        return this.m_propertiesInitialised;
    }

    public int getCurrentIndividualIndex() {
        return this.m_currentIndividualIndex;
    }

    public Map<Individual, Node> getNodesForIndividuals() {
        return this.m_nodesForIndividuals;
    }

}

