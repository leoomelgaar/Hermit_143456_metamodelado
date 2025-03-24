package org.semanticweb.HermiT.hierarchy;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Stack;
import org.semanticweb.HermiT.graph.Graph;
import org.semanticweb.HermiT.model.Atom;
import org.semanticweb.HermiT.model.AtomicConcept;
import org.semanticweb.HermiT.model.DLClause;
import org.semanticweb.HermiT.model.DLPredicate;
import org.semanticweb.HermiT.model.Individual;
import org.semanticweb.HermiT.tableau.ExtensionTable;
import org.semanticweb.HermiT.tableau.Node;
import org.semanticweb.HermiT.tableau.ReasoningTaskDescription;
import org.semanticweb.HermiT.tableau.Tableau;

public class QuasiOrderClassification {
    protected final Tableau m_tableau;
    protected final ClassificationProgressMonitor m_progressMonitor;
    protected final AtomicConcept m_topElement;
    protected final AtomicConcept m_bottomElement;
    protected final Set<AtomicConcept> m_elements;
    protected final Graph<AtomicConcept> m_knownSubsumptions;
    protected final Graph<AtomicConcept> m_possibleSubsumptions;

    public QuasiOrderClassification(Tableau tableau, ClassificationProgressMonitor progressMonitor, AtomicConcept topElement, AtomicConcept bottomElement, Set<AtomicConcept> elements) {
        this.m_tableau = tableau;
        this.m_progressMonitor = progressMonitor;
        this.m_topElement = topElement;
        this.m_bottomElement = bottomElement;
        this.m_elements = elements;
        this.m_knownSubsumptions = new Graph();
        this.m_possibleSubsumptions = new Graph();
    }

    public Hierarchy<AtomicConcept> classify() {
        HierarchySearch.Relation<AtomicConcept> relation = new HierarchySearch.Relation<AtomicConcept>(){

            @Override
            public boolean doesSubsume(AtomicConcept parent, AtomicConcept child) {
                boolean isSubsumedBy;
                Set<AtomicConcept> allKnownSubsumers = QuasiOrderClassification.this.getAllKnownSubsumers(child);
                if (allKnownSubsumers.contains(parent)) {
                    return true;
                }
                if (!QuasiOrderClassification.this.m_possibleSubsumptions.getSuccessors(child).contains(parent)) {
                    return false;
                }
                Individual freshIndividual = Individual.createAnonymous("fresh-individual");
                HashMap<Individual, Node> checkedNode = new HashMap<Individual, Node>();
                checkedNode.put(freshIndividual, null);
                boolean bl = isSubsumedBy = !QuasiOrderClassification.this.m_tableau.isSatisfiable(true, Collections.singleton(Atom.create(child, freshIndividual)), null, null, Collections.singleton(Atom.create(parent, freshIndividual)), checkedNode, QuasiOrderClassification.this.getSubsumptionTestDescription(child, parent));
                if (!isSubsumedBy) {
                    QuasiOrderClassification.this.prunePossibleSubsumers();
                }
                QuasiOrderClassification.this.readKnownSubsumersFromRootNode(child, checkedNode.get(freshIndividual));
                QuasiOrderClassification.this.m_possibleSubsumptions.getSuccessors(child).removeAll(QuasiOrderClassification.this.getAllKnownSubsumers(child));
                return isSubsumedBy;
            }
        };
        return this.buildHierarchy(relation);
    }

    protected Hierarchy<AtomicConcept> buildHierarchy(HierarchySearch.Relation<AtomicConcept> hierarchyRelation) {
        double totalNumberOfTasks = this.m_elements.size();
        this.makeConceptUnsatisfiable(this.m_bottomElement);
        this.initialiseKnownSubsumptionsUsingToldSubsumers();
        double tasksPerformed = this.updateSubsumptionsUsingLeafNodeStrategy(totalNumberOfTasks);
        HashSet<AtomicConcept> unclassifiedElements = new HashSet<AtomicConcept>();
        for (AtomicConcept element : this.m_elements) {
            if (this.isUnsatisfiable(element)) continue;
            this.m_possibleSubsumptions.getSuccessors(element).removeAll(this.getAllKnownSubsumers(element));
            if (this.m_possibleSubsumptions.getSuccessors(element).isEmpty()) continue;
            unclassifiedElements.add(element);
        }
        HashSet<AtomicConcept> classifiedElements = new HashSet<AtomicConcept>();
        while (!unclassifiedElements.isEmpty()) {
            AtomicConcept unclassifiedElement = null;
            for (AtomicConcept element : unclassifiedElements) {
                this.m_possibleSubsumptions.getSuccessors(element).removeAll(this.getAllKnownSubsumers(element));
                if (!this.m_possibleSubsumptions.getSuccessors(element).isEmpty()) {
                    unclassifiedElement = element;
                    break;
                }
                classifiedElements.add(element);
                while ((double)unclassifiedElements.size() < totalNumberOfTasks - tasksPerformed) {
                    this.m_progressMonitor.elementClassified(element);
                    tasksPerformed += 1.0;
                }
            }
            unclassifiedElements.removeAll(classifiedElements);
            if (unclassifiedElements.isEmpty()) break;
            Set<AtomicConcept> unknownPossibleSubsumers = this.m_possibleSubsumptions.getSuccessors(unclassifiedElement);
            if (!this.isEveryPossibleSubsumerNonSubsumer(unknownPossibleSubsumers, unclassifiedElement, 2, 7) && !unknownPossibleSubsumers.isEmpty()) {
                Hierarchy<AtomicConcept> smallHierarchy = this.buildHierarchyOfUnknownPossible(unknownPossibleSubsumers);
                this.checkUnknownSubsumersUsingEnhancedTraversal(hierarchyRelation, smallHierarchy.getTopNode(), unclassifiedElement);
            }
            unknownPossibleSubsumers.clear();
        }
        return this.buildTransitivelyReducedHierarchy(this.m_knownSubsumptions, this.m_elements);
    }

    protected Hierarchy<AtomicConcept> buildHierarchyOfUnknownPossible(Set<AtomicConcept> unknownSubsumers) {
        Graph smallKnownSubsumptions = new Graph();
        for (AtomicConcept unknownSubsumer0 : unknownSubsumers) {
            smallKnownSubsumptions.addEdge(this.m_bottomElement, unknownSubsumer0);
            smallKnownSubsumptions.addEdge(unknownSubsumer0, this.m_topElement);
            Set<AtomicConcept> knownSubsumersOfElement = this.getAllKnownSubsumers(unknownSubsumer0);
            for (AtomicConcept unknownSubsumer1 : unknownSubsumers) {
                if (!knownSubsumersOfElement.contains(unknownSubsumer1)) continue;
                smallKnownSubsumptions.addEdge(unknownSubsumer0, unknownSubsumer1);
            }
        }
        HashSet<AtomicConcept> unknownSubsumersWithTopBottom = new HashSet<AtomicConcept>(unknownSubsumers);
        unknownSubsumersWithTopBottom.add(this.m_bottomElement);
        unknownSubsumersWithTopBottom.add(this.m_topElement);
        return this.buildTransitivelyReducedHierarchy(smallKnownSubsumptions, unknownSubsumersWithTopBottom);
    }

    protected double updateSubsumptionsUsingLeafNodeStrategy(double totalNumberOfTasks) {
        double conceptsProcessed = 0.0;
        Hierarchy<AtomicConcept> hierarchy = this.buildTransitivelyReducedHierarchy(this.m_knownSubsumptions, this.m_elements);
        Stack toProcess = new Stack();
        toProcess.addAll(hierarchy.getBottomNode().getParentNodes());
        HashSet<HierarchyNode> unsatHierarchyNodes = new HashSet<HierarchyNode>();
        while (!toProcess.empty()) {
            HierarchyNode currentHierarchyElement = (HierarchyNode)toProcess.pop();
            AtomicConcept currentHierarchyConcept = (AtomicConcept)currentHierarchyElement.getRepresentative();
            if (conceptsProcessed < Math.ceil(totalNumberOfTasks * 0.85)) {
                this.m_progressMonitor.elementClassified(currentHierarchyConcept);
                conceptsProcessed += 1.0;
            }
            if (this.conceptHasBeenProcessedAlready(currentHierarchyConcept)) continue;
            Node rootNodeOfModel = this.buildModelForConcept(currentHierarchyConcept);
            if (rootNodeOfModel == null) {
                this.makeConceptUnsatisfiable(currentHierarchyConcept);
                unsatHierarchyNodes.add(currentHierarchyElement);
                toProcess.addAll(currentHierarchyElement.getParentNodes());
                HashSet<HierarchyNode> visited = new HashSet<HierarchyNode>();
                LinkedList toVisit = new LinkedList(currentHierarchyElement.getChildNodes());
                while (!toVisit.isEmpty()) {
                    HierarchyNode<AtomicConcept> current = (HierarchyNode)toVisit.poll();
                    if (!visited.add(current) || unsatHierarchyNodes.contains(current)) continue;
                    toVisit.addAll(current.getChildNodes());
                    unsatHierarchyNodes.add(current);
                    this.makeConceptUnsatisfiable(current.getRepresentative());
                    toProcess.remove(current);
                    for (HierarchyNode parentOfRemovedConcept : current.getParentNodes()) {
                        if (this.conceptHasBeenProcessedAlready((AtomicConcept)parentOfRemovedConcept.getRepresentative())) continue;
                        toProcess.add(parentOfRemovedConcept);
                    }
                }
                continue;
            }
            this.readKnownSubsumersFromRootNode(currentHierarchyConcept, rootNodeOfModel);
            this.updatePossibleSubsumers();
        }
        return conceptsProcessed;
    }

    private boolean conceptHasBeenProcessedAlready(AtomicConcept atConcept) {
        return !this.m_possibleSubsumptions.getSuccessors(atConcept).isEmpty() || this.isUnsatisfiable(atConcept);
    }

    protected Node buildModelForConcept(AtomicConcept concept) {
        Individual freshIndividual = Individual.createAnonymous("fresh-individual");
        HashMap<Individual, Node> checkedNode = new HashMap<Individual, Node>();
        checkedNode.put(freshIndividual, null);
        if (this.m_tableau.isSatisfiable(false, Collections.singleton(Atom.create(concept, freshIndividual)), null, null, null, checkedNode, this.getSatTestDescription(concept))) {
            return checkedNode.get(freshIndividual);
        }
        return null;
    }

    protected void makeConceptUnsatisfiable(AtomicConcept concept) {
        this.addKnownSubsumption(concept, this.m_bottomElement);
        this.m_possibleSubsumptions.getSuccessors(concept).clear();
    }

    protected boolean isUnsatisfiable(AtomicConcept concept) {
        return this.m_knownSubsumptions.getSuccessors(concept).contains(this.m_bottomElement);
    }

    protected void readKnownSubsumersFromRootNode(AtomicConcept subconcept, Node checkedNode) {
        if (checkedNode.getCanonicalNodeDependencySet().isEmpty()) {
            checkedNode = checkedNode.getCanonicalNode();
            ExtensionTable.Retrieval retrieval = this.m_tableau.getExtensionManager().getBinaryExtensionTable().createRetrieval(new boolean[]{false, true}, ExtensionTable.View.TOTAL);
            retrieval.getBindingsBuffer()[1] = checkedNode;
            retrieval.open();
            while (!retrieval.afterLast()) {
                Object conceptObject = retrieval.getTupleBuffer()[0];
                if (conceptObject instanceof AtomicConcept && retrieval.getDependencySet().isEmpty() && this.m_elements.contains(conceptObject)) {
                    this.addKnownSubsumption(subconcept, (AtomicConcept)conceptObject);
                }
                retrieval.next();
            }
        }
    }

    protected void updatePossibleSubsumers() {
        ExtensionTable.Retrieval retrieval = this.m_tableau.getExtensionManager().getBinaryExtensionTable().createRetrieval(new boolean[]{false, false}, ExtensionTable.View.TOTAL);
        retrieval.open();
        Object[] tupleBuffer = retrieval.getTupleBuffer();
        while (!retrieval.afterLast()) {
            Object conceptObject = tupleBuffer[0];
            if (conceptObject instanceof AtomicConcept && this.m_elements.contains(conceptObject)) {
                AtomicConcept atomicConcept = (AtomicConcept)conceptObject;
                Node node = (Node)tupleBuffer[1];
                if (node.isActive() && !node.isBlocked()) {
                    if (this.m_possibleSubsumptions.getSuccessors(atomicConcept).isEmpty()) {
                        this.readPossibleSubsumersFromNodeLabel(atomicConcept, node);
                    } else {
                        this.prunePossibleSubsumersOfConcept(atomicConcept, node);
                    }
                }
            }
            retrieval.next();
        }
    }

    protected void prunePossibleSubsumers() {
        ExtensionTable.Retrieval retrieval = this.m_tableau.getExtensionManager().getBinaryExtensionTable().createRetrieval(new boolean[]{false, false}, ExtensionTable.View.TOTAL);
        retrieval.open();
        Object[] tupleBuffer = retrieval.getTupleBuffer();
        while (!retrieval.afterLast()) {
            Node node;
            Object conceptObject = tupleBuffer[0];
            if (conceptObject instanceof AtomicConcept && this.m_elements.contains(conceptObject) && (node = (Node)tupleBuffer[1]).isActive() && !node.isBlocked()) {
                this.prunePossibleSubsumersOfConcept((AtomicConcept)conceptObject, node);
            }
            retrieval.next();
        }
    }

    protected void prunePossibleSubsumersOfConcept(AtomicConcept atomicConcept, Node node) {
        HashSet<AtomicConcept> possibleSubsumersOfConcept = new HashSet<AtomicConcept>(this.m_possibleSubsumptions.getSuccessors(atomicConcept));
        for (AtomicConcept atomicCon : possibleSubsumersOfConcept) {
            if (this.m_tableau.getExtensionManager().containsConceptAssertion(atomicCon, node)) continue;
            this.m_possibleSubsumptions.getSuccessors(atomicConcept).remove(atomicCon);
        }
    }

    protected void readPossibleSubsumersFromNodeLabel(AtomicConcept atomicConcept, Node node) {
        ExtensionTable.Retrieval retrieval = this.m_tableau.getExtensionManager().getBinaryExtensionTable().createRetrieval(new boolean[]{false, true}, ExtensionTable.View.TOTAL);
        retrieval.getBindingsBuffer()[1] = node;
        retrieval.open();
        while (!retrieval.afterLast()) {
            Object concept = retrieval.getTupleBuffer()[0];
            if (concept instanceof AtomicConcept && this.m_elements.contains(concept)) {
                this.addPossibleSubsumption(atomicConcept, (AtomicConcept)concept);
            }
            retrieval.next();
        }
    }

    protected Hierarchy<AtomicConcept> buildTransitivelyReducedHierarchy(Graph<AtomicConcept> knownSubsumptions, Set<AtomicConcept> elements) {
        HashMap<AtomicConcept, DeterministicClassification.GraphNode<AtomicConcept>> allSubsumers = new HashMap<AtomicConcept, DeterministicClassification.GraphNode<AtomicConcept>>();
        for (AtomicConcept element : elements) {
            HashSet<AtomicConcept> extendedSubs = new HashSet<AtomicConcept>(knownSubsumptions.getSuccessors(element));
            extendedSubs.add(this.m_topElement);
            extendedSubs.add(element);
            allSubsumers.put(element, new DeterministicClassification.GraphNode<AtomicConcept>(element, extendedSubs));
        }
        allSubsumers.put(this.m_bottomElement, new DeterministicClassification.GraphNode<AtomicConcept>(this.m_bottomElement, elements));
        return DeterministicClassification.buildHierarchy(this.m_topElement, this.m_bottomElement, allSubsumers);
    }

    protected void initialiseKnownSubsumptionsUsingToldSubsumers() {
        this.initialiseKnownSubsumptionsUsingToldSubsumers(this.m_tableau.getPermanentDLOntology().getDLClauses());
    }

    protected void initialiseKnownSubsumptionsUsingToldSubsumers(Set<DLClause> dlClauses) {
        for (DLClause dlClause : dlClauses) {
            if (dlClause.getHeadLength() != 1 || dlClause.getBodyLength() != 1) continue;
            DLPredicate headPredicate = dlClause.getHeadAtom(0).getDLPredicate();
            DLPredicate bodyPredicate = dlClause.getBodyAtom(0).getDLPredicate();
            if (!(headPredicate instanceof AtomicConcept) || !(bodyPredicate instanceof AtomicConcept)) continue;
            AtomicConcept headConcept = (AtomicConcept)headPredicate;
            AtomicConcept bodyConcept = (AtomicConcept)bodyPredicate;
            if (!this.m_elements.contains(headConcept) || !this.m_elements.contains(bodyConcept)) continue;
            this.addKnownSubsumption(bodyConcept, headConcept);
        }
    }

    protected void checkUnknownSubsumersUsingEnhancedTraversal(HierarchySearch.Relation<AtomicConcept> hierarchyRelation, HierarchyNode<AtomicConcept> startNode, AtomicConcept pickedElement) {
        Set<HierarchyNode<AtomicConcept>> startSearch = Collections.singleton(startNode);
        HashSet<HierarchyNode<AtomicConcept>> visited = new HashSet<HierarchyNode<AtomicConcept>>(startSearch);
        LinkedList<HierarchyNode<AtomicConcept>> toProcess = new LinkedList<HierarchyNode<AtomicConcept>>(startSearch);
        while (!toProcess.isEmpty()) {
            HierarchyNode current = toProcess.remove();
            Set<HierarchyNode> subordinateElements = current.getChildNodes();
            for (HierarchyNode subordinateElement : subordinateElements) {
                AtomicConcept element = (AtomicConcept)subordinateElement.getRepresentative();
                if (visited.contains(subordinateElement)) continue;
                if (hierarchyRelation.doesSubsume(element, pickedElement)) {
                    this.addKnownSubsumption(pickedElement, element);
                    this.addKnownSubsumptions(pickedElement, subordinateElement.getEquivalentElements());
                    if (visited.add(subordinateElement)) {
                        toProcess.add(subordinateElement);
                    }
                }
                visited.add(subordinateElement);
            }
        }
    }

    protected boolean isEveryPossibleSubsumerNonSubsumer(Set<AtomicConcept> unknownPossibleSubsumers, AtomicConcept pickedElement, int lowerBound, int upperBound) {
        if (unknownPossibleSubsumers.size() > lowerBound && unknownPossibleSubsumers.size() < upperBound) {
            boolean isSubsumedBy;
            Individual freshIndividual = Individual.createAnonymous("fresh-individual");
            Atom subconceptAssertion = Atom.create(pickedElement, freshIndividual);
            HashSet<Atom> superconceptAssertions = new HashSet<Atom>();
            Object[] superconcepts = new Object[unknownPossibleSubsumers.size()];
            int index = 0;
            for (AtomicConcept unknownSupNode : unknownPossibleSubsumers) {
                Atom atom = Atom.create(unknownSupNode, freshIndividual);
                superconceptAssertions.add(atom);
                superconcepts[index++] = atom.getDLPredicate();
            }
            HashMap<Individual, Node> checkedNode = new HashMap<Individual, Node>();
            checkedNode.put(freshIndividual, null);
            boolean bl = isSubsumedBy = !this.m_tableau.isSatisfiable(false, Collections.singleton(subconceptAssertion), null, null, superconceptAssertions, checkedNode, this.getSubsumedByListTestDescription(pickedElement, superconcepts));
            if (!isSubsumedBy) {
                this.prunePossibleSubsumers();
            } else {
                this.readKnownSubsumersFromRootNode(pickedElement, checkedNode.get(freshIndividual));
                this.m_possibleSubsumptions.getSuccessors(pickedElement).removeAll(this.getAllKnownSubsumers(pickedElement));
            }
            return !isSubsumedBy;
        }
        return false;
    }

    protected Set<AtomicConcept> getAllKnownSubsumers(AtomicConcept child) {
        return this.m_knownSubsumptions.getReachableSuccessors(child);
    }

    protected void addKnownSubsumption(AtomicConcept subConcept, AtomicConcept superConcept) {
        this.m_knownSubsumptions.addEdge(subConcept, superConcept);
    }

    protected void addKnownSubsumptions(AtomicConcept subConcept, Set<AtomicConcept> superConcepts) {
        this.m_knownSubsumptions.addEdges(subConcept, superConcepts);
    }

    protected void addPossibleSubsumption(AtomicConcept subConcept, AtomicConcept superConcept) {
        this.m_possibleSubsumptions.addEdge(subConcept, superConcept);
    }

    protected ReasoningTaskDescription getSatTestDescription(AtomicConcept atomicConcept) {
        return ReasoningTaskDescription.isConceptSatisfiable(atomicConcept);
    }

    protected ReasoningTaskDescription getSubsumptionTestDescription(AtomicConcept subConcept, AtomicConcept superConcept) {
        return ReasoningTaskDescription.isConceptSubsumedBy(subConcept, superConcept);
    }

    protected ReasoningTaskDescription getSubsumedByListTestDescription(AtomicConcept subConcept, Object[] superconcepts) {
        return ReasoningTaskDescription.isConceptSubsumedByList(subConcept, superconcepts);
    }

}

