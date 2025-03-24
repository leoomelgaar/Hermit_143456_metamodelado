package org.semanticweb.HermiT.tableau;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.semanticweb.HermiT.model.DescriptionGraph;
import org.semanticweb.HermiT.model.ExistsDescriptionGraph;
import org.semanticweb.HermiT.monitor.TableauMonitor;

public final class DescriptionGraphManager
implements Serializable {
    private static final long serialVersionUID = 4536271856850424712L;
    private final Tableau m_tableau;
    private final InterruptFlag m_interruptFlag;
    private final TableauMonitor m_tableauMonitor;
    private final ExtensionManager m_extensionManager;
    private final MergingManager m_mergingManager;
    private final OccurrenceManager m_occurrenceManager;
    private final Map<DescriptionGraph, Integer> m_descriptionGraphIndices;
    private final DescriptionGraph[] m_descriptionGraphsByIndex;
    private final ExtensionTable[] m_extensionTablesByIndex;
    private final Object[][] m_auxiliaryTuples1;
    private final Object[][] m_auxiliaryTuples2;
    private final List<Node> m_newNodes;
    private final UnionDependencySet m_binaryUnionDependencySet;
    private final ExtensionTable.Retrieval[] m_deltaOldRetrievals;

    public DescriptionGraphManager(Tableau tableau) {
        int index;
        this.m_tableau = tableau;
        this.m_interruptFlag = this.m_tableau.m_interruptFlag;
        this.m_tableauMonitor = this.m_tableau.m_tableauMonitor;
        this.m_extensionManager = this.m_tableau.m_extensionManager;
        this.m_mergingManager = this.m_tableau.m_mergingManager;
        this.m_occurrenceManager = new OccurrenceManager();
        this.m_descriptionGraphIndices = new HashMap<DescriptionGraph, Integer>();
        HashSet<ExtensionTable> extensionTables = new HashSet<ExtensionTable>();
        ArrayList<DescriptionGraph> descriptionGraphsByIndex = new ArrayList<DescriptionGraph>();
        ArrayList<ExtensionTable> extensionTablesByIndex = new ArrayList<ExtensionTable>();
        for (DescriptionGraph descriptionGraph : this.m_tableau.m_permanentDLOntology.getAllDescriptionGraphs()) {
            this.m_descriptionGraphIndices.put(descriptionGraph, descriptionGraphsByIndex.size());
            descriptionGraphsByIndex.add(descriptionGraph);
            ExtensionTable extensionTable = this.m_extensionManager.getExtensionTable(descriptionGraph.getNumberOfVertices() + 1);
            extensionTablesByIndex.add(extensionTable);
            extensionTables.add(extensionTable);
        }
        this.m_descriptionGraphsByIndex = new DescriptionGraph[descriptionGraphsByIndex.size()];
        descriptionGraphsByIndex.toArray(this.m_descriptionGraphsByIndex);
        this.m_extensionTablesByIndex = new ExtensionTable[extensionTablesByIndex.size()];
        extensionTablesByIndex.toArray(this.m_extensionTablesByIndex);
        this.m_auxiliaryTuples1 = new Object[this.m_descriptionGraphsByIndex.length][];
        this.m_auxiliaryTuples2 = new Object[this.m_descriptionGraphsByIndex.length][];
        for (index = 0; index < this.m_descriptionGraphsByIndex.length; ++index) {
            DescriptionGraph descriptionGraph = this.m_descriptionGraphsByIndex[index];
            this.m_auxiliaryTuples1[index] = new Object[descriptionGraph.getNumberOfVertices() + 1];
            this.m_auxiliaryTuples2[index] = new Object[descriptionGraph.getNumberOfVertices() + 1];
        }
        this.m_newNodes = new ArrayList<Node>();
        this.m_binaryUnionDependencySet = new UnionDependencySet(2);
        this.m_deltaOldRetrievals = new ExtensionTable.Retrieval[extensionTables.size()];
        index = 0;
        for (ExtensionTable extensionTable : extensionTables) {
            this.m_deltaOldRetrievals[index++] = extensionTable.createRetrieval(new boolean[extensionTable.getArity()], ExtensionTable.View.DELTA_OLD);
        }
    }

    public void clear() {
        for (int index = 0; index < this.m_auxiliaryTuples1.length; ++index) {
            Arrays.fill(this.m_auxiliaryTuples1[index], null);
            Arrays.fill(this.m_auxiliaryTuples2[index], null);
        }
        this.m_occurrenceManager.clear();
        for (Object[] tuple : this.m_auxiliaryTuples1) {
            Arrays.fill(tuple, null);
        }
        for (Object[] tuple : this.m_auxiliaryTuples2) {
            Arrays.fill(tuple, null);
        }
        this.m_newNodes.clear();
        this.m_binaryUnionDependencySet.m_dependencySets[0] = null;
        this.m_binaryUnionDependencySet.m_dependencySets[1] = null;
        for (ExtensionTable.Retrieval retrieval : this.m_deltaOldRetrievals) {
            retrieval.clear();
        }
    }

    public Object[] getDescriptionGraphTuple(int graphIndex, int tupleIndex) {
        DescriptionGraph descriptionGraph = this.m_descriptionGraphsByIndex[graphIndex];
        ExtensionTable extensionTable = this.m_extensionTablesByIndex[graphIndex];
        Object[] tuple = new Object[descriptionGraph.getNumberOfVertices() + 1];
        extensionTable.retrieveTuple(tuple, tupleIndex);
        return tuple;
    }

    public boolean checkGraphConstraints() {
        boolean hasChange = false;
        for (int retrievalIndex = 0; retrievalIndex < this.m_deltaOldRetrievals.length && !this.m_extensionManager.containsClash(); ++retrievalIndex) {
            ExtensionTable.Retrieval retrieval = this.m_deltaOldRetrievals[retrievalIndex];
            ExtensionTable extensionTable = retrieval.getExtensionTable();
            retrieval.open();
            Object[] tupleBuffer = retrieval.getTupleBuffer();
            int arity = tupleBuffer.length;
            while (!retrieval.afterLast() && !this.m_extensionManager.containsClash()) {
                if (tupleBuffer[0] instanceof DescriptionGraph) {
                    int thisGraphIndex = this.m_descriptionGraphIndices.get(tupleBuffer[0]);
                    int thisTupleIndex = retrieval.getCurrentTupleIndex();
                    for (int thisPositionInTuple = 1; thisPositionInTuple < arity; ++thisPositionInTuple) {
                        Node node = (Node)tupleBuffer[thisPositionInTuple];
                        int listNode = node.m_firstGraphOccurrenceNode;
                        while (listNode != -1) {
                            int graphIndex = this.m_occurrenceManager.getListNodeComponent(listNode, 0);
                            int tupleIndex = this.m_occurrenceManager.getListNodeComponent(listNode, 1);
                            int positionInTuple = this.m_occurrenceManager.getListNodeComponent(listNode, 2);
                            if (thisGraphIndex == graphIndex && (thisTupleIndex != tupleIndex || thisPositionInTuple != positionInTuple) && extensionTable.isTupleActive(tupleIndex)) {
                                this.m_binaryUnionDependencySet.m_dependencySets[0] = retrieval.getDependencySet();
                                this.m_binaryUnionDependencySet.m_dependencySets[1] = extensionTable.getDependencySet(tupleIndex);
                                if (this.m_tableauMonitor != null) {
                                    this.m_tableauMonitor.descriptionGraphCheckingStarted(thisGraphIndex, thisTupleIndex, thisPositionInTuple, graphIndex, tupleIndex, positionInTuple);
                                }
                                if (thisPositionInTuple == positionInTuple) {
                                    for (int mergePosition = arity - 1; mergePosition >= 1; --mergePosition) {
                                        Node nodeSecond;
                                        Node nodeFirst = (Node)extensionTable.getTupleObject(thisTupleIndex, mergePosition);
                                        if (nodeFirst != (nodeSecond = (Node)extensionTable.getTupleObject(tupleIndex, mergePosition))) {
                                            this.m_mergingManager.mergeNodes(nodeFirst, nodeSecond, this.m_binaryUnionDependencySet);
                                            hasChange = true;
                                        }
                                        this.m_interruptFlag.checkInterrupt();
                                    }
                                } else {
                                    this.m_extensionManager.setClash(this.m_binaryUnionDependencySet);
                                    hasChange = true;
                                }
                                if (this.m_tableauMonitor != null) {
                                    this.m_tableauMonitor.descriptionGraphCheckingFinished(thisGraphIndex, thisTupleIndex, thisPositionInTuple, graphIndex, tupleIndex, positionInTuple);
                                }
                            }
                            listNode = this.m_occurrenceManager.getListNodeComponent(listNode, 3);
                            this.m_interruptFlag.checkInterrupt();
                        }
                    }
                }
                retrieval.next();
            }
            this.m_interruptFlag.checkInterrupt();
        }
        return hasChange;
    }

    public boolean isSatisfied(ExistsDescriptionGraph existsDescriptionGraph, Node node) {
        int graphIndex = this.m_descriptionGraphIndices.get(existsDescriptionGraph.getDescriptionGraph());
        int positionInTuple = existsDescriptionGraph.getVertex() + 1;
        int listNode = node.m_firstGraphOccurrenceNode;
        while (listNode != -1) {
            if (graphIndex == this.m_occurrenceManager.getListNodeComponent(listNode, 0) && positionInTuple == this.m_occurrenceManager.getListNodeComponent(listNode, 2)) {
                return true;
            }
            listNode = this.m_occurrenceManager.getListNodeComponent(listNode, 3);
        }
        return false;
    }

    public void mergeGraphs(Node mergeFrom, Node mergeInto) {
        int listNode = mergeFrom.m_firstGraphOccurrenceNode;
        while (listNode != -1) {
            int graphIndex = this.m_occurrenceManager.getListNodeComponent(listNode, 0);
            int tupleIndex = this.m_occurrenceManager.getListNodeComponent(listNode, 1);
            int positionInTuple = this.m_occurrenceManager.getListNodeComponent(listNode, 2);
            ExtensionTable extensionTable = this.m_extensionTablesByIndex[graphIndex];
            Object[] auxiliaryTuple = this.m_auxiliaryTuples1[graphIndex];
            extensionTable.retrieveTuple(auxiliaryTuple, tupleIndex);
            if (extensionTable.isTupleActive(auxiliaryTuple)) {
                this.m_binaryUnionDependencySet.m_dependencySets[0] = extensionTable.getDependencySet(tupleIndex);
                boolean isCore = extensionTable.isCore(tupleIndex);
                if (this.m_tableauMonitor != null) {
                    Object[] sourceTuple = this.m_auxiliaryTuples2[graphIndex];
                    System.arraycopy(auxiliaryTuple, 0, sourceTuple, 0, auxiliaryTuple.length);
                    auxiliaryTuple[positionInTuple] = mergeInto;
                    this.m_tableauMonitor.mergeFactStarted(mergeFrom, mergeInto, sourceTuple, auxiliaryTuple);
                    this.m_extensionManager.addTuple(auxiliaryTuple, this.m_binaryUnionDependencySet, isCore);
                    this.m_tableauMonitor.mergeFactFinished(mergeFrom, mergeInto, sourceTuple, auxiliaryTuple);
                } else {
                    auxiliaryTuple[positionInTuple] = mergeInto;
                    this.m_extensionManager.addTuple(auxiliaryTuple, this.m_binaryUnionDependencySet, isCore);
                }
            }
            listNode = this.m_occurrenceManager.getListNodeComponent(listNode, 3);
        }
    }

    public void descriptionGraphTupleAdded(int tupleIndex, Object[] tuple) {
        int graphIndex = this.m_descriptionGraphIndices.get(tuple[0]);
        for (int positionInTuple = tuple.length - 1; positionInTuple >= 1; --positionInTuple) {
            Node node = (Node)tuple[positionInTuple];
            int listNode = this.m_occurrenceManager.newListNode();
            this.m_occurrenceManager.initializeListNode(listNode, graphIndex, tupleIndex, positionInTuple, node.m_firstGraphOccurrenceNode);
            node.m_firstGraphOccurrenceNode = listNode;
        }
    }

    public void descriptionGraphTupleRemoved(int tupleIndex, Object[] tuple) {
        for (int positionInTuple = tuple.length - 1; positionInTuple >= 1; --positionInTuple) {
            Node node = (Node)tuple[positionInTuple];
            int listNode = node.m_firstGraphOccurrenceNode;
            assert (this.m_occurrenceManager.getListNodeComponent(listNode, 0) == this.m_descriptionGraphIndices.get(tuple[0]).intValue());
            assert (this.m_occurrenceManager.getListNodeComponent(listNode, 1) == tupleIndex);
            assert (this.m_occurrenceManager.getListNodeComponent(listNode, 2) == positionInTuple);
            node.m_firstGraphOccurrenceNode = this.m_occurrenceManager.getListNodeComponent(listNode, 3);
            this.m_occurrenceManager.deleteListNode(listNode);
        }
    }

    public void expand(ExistsDescriptionGraph existsDescriptionGraph, Node forNode) {
        int vertex;
        Node newNode;
        if (this.m_tableau.m_tableauMonitor != null) {
            this.m_tableau.m_tableauMonitor.existentialExpansionStarted(existsDescriptionGraph, forNode);
        }
        this.m_newNodes.clear();
        DescriptionGraph descriptionGraph = existsDescriptionGraph.getDescriptionGraph();
        DependencySet dependencySet = this.m_extensionManager.getConceptAssertionDependencySet(existsDescriptionGraph, forNode);
        Object[] auxiliaryTuple = this.m_auxiliaryTuples1[this.m_descriptionGraphIndices.get(descriptionGraph)];
        auxiliaryTuple[0] = descriptionGraph;
        for (vertex = 0; vertex < descriptionGraph.getNumberOfVertices(); ++vertex) {
            newNode = vertex == existsDescriptionGraph.getVertex() ? forNode : this.m_tableau.createNewGraphNode(forNode.getClusterAnchor(), dependencySet);
            this.m_newNodes.add(newNode);
            auxiliaryTuple[vertex + 1] = newNode;
        }
        this.m_extensionManager.addTuple(auxiliaryTuple, dependencySet, true);
        for (vertex = 0; vertex < descriptionGraph.getNumberOfVertices(); ++vertex) {
            newNode = this.m_newNodes.get(vertex);
            dependencySet = newNode.addCanonicalNodeDependencySet(dependencySet);
            this.m_newNodes.set(vertex, newNode.getCanonicalNode());
        }
        for (vertex = 0; vertex < descriptionGraph.getNumberOfVertices(); ++vertex) {
            this.m_extensionManager.addConceptAssertion(descriptionGraph.getAtomicConceptForVertex(vertex), this.m_newNodes.get(vertex), dependencySet, true);
        }
        for (int edgeIndex = 0; edgeIndex < descriptionGraph.getNumberOfEdges(); ++edgeIndex) {
            DescriptionGraph.Edge edge = descriptionGraph.getEdge(edgeIndex);
            this.m_extensionManager.addRoleAssertion(edge.getAtomicRole(), this.m_newNodes.get(edge.getFromVertex()), this.m_newNodes.get(edge.getToVertex()), dependencySet, true);
        }
        this.m_newNodes.clear();
        if (this.m_tableau.m_tableauMonitor != null) {
            this.m_tableau.m_tableauMonitor.existentialExpansionFinished(existsDescriptionGraph, forNode);
        }
    }

    public static void intializeNode(Node node) {
        node.m_firstGraphOccurrenceNode = -1;
    }

    public void destroyNode(Node node) {
        int listNode = node.m_firstGraphOccurrenceNode;
        while (listNode != -1) {
            int nextListNode = this.m_occurrenceManager.getListNodeComponent(listNode, 3);
            this.m_occurrenceManager.deleteListNode(listNode);
            listNode = nextListNode;
        }
        node.m_firstGraphOccurrenceNode = -1;
    }

    protected static class OccurrenceManager
    implements Serializable {
        private static final long serialVersionUID = 7594355731105478918L;
        public static final int GRAPH_INDEX = 0;
        public static final int TUPLE_INDEX = 1;
        public static final int POSITION_IN_TUPLE = 2;
        public static final int NEXT_NODE = 3;
        public static final int LIST_NODE_SIZE = 4;
        public static final int LIST_NODE_PAGE_SIZE = 2048;
        protected int[][] m_nodePages = new int[10][];
        protected int m_firstFreeListNode;
        protected int m_numberOfPages;

        public OccurrenceManager() {
            this.m_nodePages[0] = new int[2048];
            this.m_numberOfPages = 1;
            this.m_firstFreeListNode = 0;
            this.setListNodeComponent(this.m_firstFreeListNode, 3, -1);
        }

        public void clear() {
            this.m_firstFreeListNode = 0;
            this.setListNodeComponent(this.m_firstFreeListNode, 3, -1);
        }

        public int getListNodeComponent(int listNode, int component) {
            return this.m_nodePages[listNode / 2048][listNode % 2048 + component];
        }

        public void setListNodeComponent(int listNode, int component, int value) {
            this.m_nodePages[listNode / 2048][listNode % 2048 + component] = value;
        }

        public void initializeListNode(int listNode, int graphIndex, int tupleIndex, int positionInTuple, int nextListNode) {
            int pageIndex = listNode / 2048;
            int indexInPage = listNode % 2048;
            int[] nodePage = this.m_nodePages[pageIndex];
            nodePage[indexInPage] = graphIndex;
            nodePage[indexInPage + 1] = tupleIndex;
            nodePage[indexInPage + 2] = positionInTuple;
            nodePage[indexInPage + 3] = nextListNode;
        }

        public int newListNode() {
            int newListNode = this.m_firstFreeListNode;
            int nextFreeListNode = this.getListNodeComponent(this.m_firstFreeListNode, 3);
            if (nextFreeListNode != -1) {
                this.m_firstFreeListNode = nextFreeListNode;
            } else {
                this.m_firstFreeListNode += 4;
                int pageIndex = this.m_firstFreeListNode / 2048;
                if (pageIndex >= this.m_numberOfPages) {
                    if (pageIndex >= this.m_nodePages.length) {
                        int[][] newNodePages = new int[this.m_nodePages.length * 3 / 2][];
                        System.arraycopy(this.m_nodePages, 0, newNodePages, 0, this.m_nodePages.length);
                        this.m_nodePages = newNodePages;
                    }
                    this.m_nodePages[pageIndex] = new int[2048];
                    ++this.m_numberOfPages;
                }
                this.setListNodeComponent(this.m_firstFreeListNode, 3, -1);
            }
            return newListNode;
        }

        public void deleteListNode(int listNode) {
            this.setListNodeComponent(listNode, 3, this.m_firstFreeListNode);
            this.m_firstFreeListNode = listNode;
        }
    }

}

