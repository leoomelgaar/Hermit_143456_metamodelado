package org.semanticweb.HermiT.hierarchy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import org.semanticweb.HermiT.model.Atom;
import org.semanticweb.HermiT.model.AtomicConcept;
import org.semanticweb.HermiT.model.Individual;
import org.semanticweb.HermiT.tableau.ExtensionTable;
import org.semanticweb.HermiT.tableau.Node;
import org.semanticweb.HermiT.tableau.ReasoningTaskDescription;
import org.semanticweb.HermiT.tableau.Tableau;

public class DeterministicClassification {
    protected final Tableau m_tableau;
    protected final ClassificationProgressMonitor m_progressMonitor;
    protected final AtomicConcept m_topElement;
    protected final AtomicConcept m_bottomElement;
    protected final Set<AtomicConcept> m_elements;

    public DeterministicClassification(Tableau tableau, ClassificationProgressMonitor progressMonitor, AtomicConcept topElement, AtomicConcept bottomElement, Set<AtomicConcept> elements) {
        this.m_tableau = tableau;
        this.m_progressMonitor = progressMonitor;
        this.m_topElement = topElement;
        this.m_bottomElement = bottomElement;
        this.m_elements = elements;
    }

    public Hierarchy<AtomicConcept> classify() {
        if (!this.m_tableau.isDeterministic()) {
            throw new IllegalStateException("Internal error: DeterministicClassificationManager can be used only with a deterministic tableau.");
        }
        Individual freshIndividual = Individual.createAnonymous("fresh-individual");
        if (!this.m_tableau.isSatisfiable(true, Collections.singleton(Atom.create(this.m_topElement, freshIndividual)), null, null, null, null, ReasoningTaskDescription.isConceptSatisfiable(this.m_topElement))) {
            return Hierarchy.emptyHierarchy(this.m_elements, this.m_topElement, this.m_bottomElement);
        }
        HashMap<AtomicConcept, GraphNode<AtomicConcept>> allSubsumers = new HashMap<AtomicConcept, GraphNode<AtomicConcept>>();
        for (AtomicConcept element : this.m_elements) {
            Set<AtomicConcept> subsumers;
            HashMap<Individual, Node> nodesForIndividuals = new HashMap<Individual, Node>();
            nodesForIndividuals.put(freshIndividual, null);
            if (!this.m_tableau.isSatisfiable(true, Collections.singleton(Atom.create(element, freshIndividual)), null, null, null, nodesForIndividuals, ReasoningTaskDescription.isConceptSatisfiable(element))) {
                subsumers = this.m_elements;
            } else {
                subsumers = new HashSet<AtomicConcept>();
                subsumers.add(this.m_topElement);
                ExtensionTable.Retrieval retrieval = this.m_tableau.getExtensionManager().getBinaryExtensionTable().createRetrieval(new boolean[]{false, true}, ExtensionTable.View.TOTAL);
                retrieval.getBindingsBuffer()[1] = nodesForIndividuals.get(freshIndividual).getCanonicalNode();
                retrieval.open();
                while (!retrieval.afterLast()) {
                    Object subsumer = retrieval.getTupleBuffer()[0];
                    if (subsumer instanceof AtomicConcept && this.m_elements.contains(subsumer)) {
                        subsumers.add((AtomicConcept)subsumer);
                    }
                    retrieval.next();
                }
            }
            allSubsumers.put(element, new GraphNode<AtomicConcept>(element, subsumers));
            this.m_progressMonitor.elementClassified(element);
        }
        return DeterministicClassification.buildHierarchy(this.m_topElement, this.m_bottomElement, allSubsumers);
    }

    public static <T> Hierarchy<T> buildHierarchy(T topElement, T bottomElement, Map<T, GraphNode<T>> graphNodes) {
        HierarchyNode<T> topNode = new HierarchyNode<T>(topElement);
        HierarchyNode<T> bottomNode = new HierarchyNode<T>(bottomElement);
        Hierarchy<T> hierarchy = new Hierarchy<T>(topNode, bottomNode);
        ArrayList<HierarchyNode<T>> topologicalOrder = new ArrayList<HierarchyNode<T>>();
        DeterministicClassification.visit(new Stack<GraphNode<T>>(), new DFSIndex(), graphNodes, graphNodes.get(bottomElement), hierarchy, topologicalOrder);
        HashMap reachableFrom = new HashMap();
        ArrayList<GraphNode<T>> allSuccessors = new ArrayList<GraphNode<T>>();
        for (int index = 0; index < topologicalOrder.size(); ++index) {
            HierarchyNode node = topologicalOrder.get(index);
            HashSet reachableFromNode = new HashSet();
            reachableFromNode.add(node);
            reachableFrom.put(node, reachableFromNode);
            allSuccessors.clear();
            for (Object element : node.m_equivalentElements) {
                GraphNode<T> graphNode = graphNodes.get(element);
                for (Object successor : graphNode.m_successors) {
                    GraphNode<T> successorGraphNode = graphNodes.get(successor);
                    if (successorGraphNode == null) continue;
                    allSuccessors.add(successorGraphNode);
                }
            }
            Collections.sort(allSuccessors, TopologicalOrderComparator.INSTANCE);
            for (int successorIndex = allSuccessors.size() - 1; successorIndex >= 0; --successorIndex) {
                GraphNode successorGraphNode = allSuccessors.get(successorIndex);
                HierarchyNode successorNode = hierarchy.m_nodesByElements.get(successorGraphNode.m_element);
                if (reachableFromNode.contains(successorNode)) continue;
                node.m_parentNodes.add(successorNode);
                successorNode.m_childNodes.add(node);
                reachableFromNode.add(successorNode);
                reachableFromNode.addAll((Collection)reachableFrom.get(successorNode));
            }
        }
        return hierarchy;
    }

    protected static <T> void visit(Stack<GraphNode<T>> stack, DFSIndex dfsIndex, Map<T, GraphNode<T>> graphNodes, GraphNode<T> graphNode, Hierarchy<T> hierarchy, List<HierarchyNode<T>> topologicalOrder) {
        graphNode.m_dfsIndex = dfsIndex.m_value++;
        graphNode.m_SCChead = graphNode;
        stack.push(graphNode);
        for (Object successor : graphNode.m_successors) {
            GraphNode<T> successorGraphNode = graphNodes.get(successor);
            if (successorGraphNode == null) continue;
            if (successorGraphNode.notVisited()) {
                DeterministicClassification.visit(stack, dfsIndex, graphNodes, successorGraphNode, hierarchy, topologicalOrder);
            }
            if (successorGraphNode.isAssignedToSCC() || successorGraphNode.m_SCChead.m_dfsIndex >= graphNode.m_SCChead.m_dfsIndex) continue;
            graphNode.m_SCChead = successorGraphNode.m_SCChead;
        }
        if (graphNode.m_SCChead == graphNode) {
            GraphNode<T> poppedNode;
            int nextTopologicalOrderIndex = topologicalOrder.size();
            HashSet<T> equivalentElements = new HashSet<T>();
            do {
                poppedNode = stack.pop();
                poppedNode.m_topologicalOrderIndex = nextTopologicalOrderIndex;
                equivalentElements.add(poppedNode.m_element);
            } while (poppedNode != graphNode);
            HierarchyNode<T> hierarchyNode = equivalentElements.contains(hierarchy.getTopNode().m_representative) ? hierarchy.getTopNode() : (equivalentElements.contains(hierarchy.getBottomNode().m_representative) ? hierarchy.getBottomNode() : new HierarchyNode(graphNode.m_element));
            for (T element : equivalentElements) {
                hierarchyNode.m_equivalentElements.add(element);
                hierarchy.m_nodesByElements.put(element, hierarchyNode);
            }
            topologicalOrder.add(hierarchyNode);
        }
    }

    protected static class DFSIndex {
        public int m_value;

        protected DFSIndex() {
        }
    }

    protected static class TopologicalOrderComparator
    implements Comparator<GraphNode<?>> {
        public static final TopologicalOrderComparator INSTANCE = new TopologicalOrderComparator();

        protected TopologicalOrderComparator() {
        }

        @Override
        public int compare(GraphNode<?> o1, GraphNode<?> o2) {
            return o1.m_topologicalOrderIndex - o2.m_topologicalOrderIndex;
        }
    }

    static class GraphNode<T> {
        public final T m_element;
        public final Set<T> m_successors;
        public int m_dfsIndex;
        public GraphNode<T> m_SCChead;
        public int m_topologicalOrderIndex;

        public GraphNode(T element, Set<T> successors) {
            this.m_element = element;
            this.m_successors = successors;
            this.m_dfsIndex = -1;
            this.m_SCChead = null;
            this.m_topologicalOrderIndex = -1;
        }

        public boolean notVisited() {
            return this.m_dfsIndex == -1;
        }

        public boolean isAssignedToSCC() {
            return this.m_topologicalOrderIndex != -1;
        }
    }

}

