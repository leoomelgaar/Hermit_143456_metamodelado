package org.semanticweb.HermiT.tableau;

import java.io.Serializable;
import org.semanticweb.HermiT.model.AnnotatedEquality;

final class NominalIntroductionManager
implements Serializable {
    private static final long serialVersionUID = 5863617010809297861L;
    private final Tableau m_tableau;
    private final DependencySetFactory m_dependencySetFactory;
    private final InterruptFlag m_interruptFlag;
    private final MergingManager m_mergingManager;
    final TupleTable m_annotatedEqualities;
    private final Object[] m_bufferForAnnotatedEquality;
    final TupleTable m_newRootNodesTable;
    final TupleTableFullIndex m_newRootNodesIndex;
    private final Object[] m_bufferForRootNodes;
    private int[] m_indicesByBranchingPoint;
    private int m_firstUnprocessedAnnotatedEquality;

    public NominalIntroductionManager(Tableau tableau) {
        this.m_tableau = tableau;
        this.m_dependencySetFactory = this.m_tableau.m_dependencySetFactory;
        this.m_interruptFlag = this.m_tableau.m_interruptFlag;
        this.m_mergingManager = this.m_tableau.m_mergingManager;
        this.m_annotatedEqualities = new TupleTable(5);
        this.m_bufferForAnnotatedEquality = new Object[5];
        this.m_newRootNodesTable = new TupleTable(4);
        this.m_newRootNodesIndex = new TupleTableFullIndex(this.m_newRootNodesTable, 3);
        this.m_bufferForRootNodes = new Object[4];
        this.m_indicesByBranchingPoint = new int[20];
        this.m_firstUnprocessedAnnotatedEquality = 0;
    }

    public void clear() {
        int index;
        this.m_annotatedEqualities.clear();
        for (index = this.m_bufferForAnnotatedEquality.length - 1; index >= 0; --index) {
            this.m_bufferForAnnotatedEquality[index] = null;
        }
        this.m_newRootNodesTable.clear();
        this.m_newRootNodesIndex.clear();
        for (index = this.m_bufferForRootNodes.length - 1; index >= 0; --index) {
            this.m_bufferForRootNodes[index] = null;
        }
        this.m_firstUnprocessedAnnotatedEquality = 0;
    }

    public void branchingPointPushed() {
        int start = this.m_tableau.getCurrentBranchingPoint().getLevel() * 3;
        int requiredSize = start + 3;
        if (requiredSize > this.m_indicesByBranchingPoint.length) {
            int newSize = this.m_indicesByBranchingPoint.length * 3 / 2;
            while (requiredSize > newSize) {
                newSize = newSize * 3 / 2;
            }
            int[] newIndicesByBranchingPoint = new int[newSize];
            System.arraycopy(this.m_indicesByBranchingPoint, 0, newIndicesByBranchingPoint, 0, this.m_indicesByBranchingPoint.length);
            this.m_indicesByBranchingPoint = newIndicesByBranchingPoint;
        }
        this.m_indicesByBranchingPoint[start] = this.m_firstUnprocessedAnnotatedEquality;
        this.m_indicesByBranchingPoint[start + 1] = this.m_annotatedEqualities.getFirstFreeTupleIndex();
        this.m_indicesByBranchingPoint[start + 2] = this.m_newRootNodesTable.getFirstFreeTupleIndex();
    }

    public void backtrack() {
        int start = this.m_tableau.getCurrentBranchingPoint().getLevel() * 3;
        this.m_firstUnprocessedAnnotatedEquality = this.m_indicesByBranchingPoint[start];
        int firstFreeAnnotatedEqualityShouldBe = this.m_indicesByBranchingPoint[start + 1];
        for (int tupleIndex = this.m_annotatedEqualities.getFirstFreeTupleIndex() - 1; tupleIndex >= firstFreeAnnotatedEqualityShouldBe; --tupleIndex) {
            this.m_dependencySetFactory.removeUsage((PermanentDependencySet)this.m_annotatedEqualities.getTupleObject(tupleIndex, 4));
        }
        this.m_annotatedEqualities.truncate(firstFreeAnnotatedEqualityShouldBe);
        int firstFreeNewRootNodeShouldBe = this.m_indicesByBranchingPoint[start + 2];
        for (int tupleIndex = this.m_newRootNodesTable.getFirstFreeTupleIndex() - 1; tupleIndex >= firstFreeNewRootNodeShouldBe; --tupleIndex) {
            this.m_newRootNodesIndex.removeTuple(tupleIndex);
        }
        this.m_newRootNodesTable.truncate(firstFreeNewRootNodeShouldBe);
    }

    public boolean processAnnotatedEqualities() {
        boolean result = false;
        while (this.m_firstUnprocessedAnnotatedEquality < this.m_annotatedEqualities.getFirstFreeTupleIndex()) {
            this.m_annotatedEqualities.retrieveTuple(this.m_bufferForAnnotatedEquality, this.m_firstUnprocessedAnnotatedEquality);
            ++this.m_firstUnprocessedAnnotatedEquality;
            AnnotatedEquality annotatedEquality = (AnnotatedEquality)this.m_bufferForAnnotatedEquality[0];
            Node node0 = (Node)this.m_bufferForAnnotatedEquality[1];
            Node node1 = (Node)this.m_bufferForAnnotatedEquality[2];
            Node node2 = (Node)this.m_bufferForAnnotatedEquality[3];
            DependencySet dependencySet = (DependencySet)this.m_bufferForAnnotatedEquality[4];
            if (this.applyNIRule(annotatedEquality, node0, node1, node2, dependencySet)) {
                result = true;
            }
            this.m_interruptFlag.checkInterrupt();
        }
        return result;
    }

    public static boolean canForgetAnnotation(Node node0, Node node1, Node node2) {
        return node0.isRootNode() || node1.isRootNode() || !node2.isRootNode() || node2.isParentOf(node0) && node2.isParentOf(node1);
    }

    public boolean addAnnotatedEquality(AnnotatedEquality annotatedEquality, Node node0, Node node1, Node node2, DependencySet dependencySet) {
        if (!(node0.isActive() && node1.isActive() && node2.isActive())) {
            return false;
        }
        if (NominalIntroductionManager.canForgetAnnotation(node0, node1, node2)) {
            return this.m_mergingManager.mergeNodes(node0, node1, dependencySet);
        }
        if (annotatedEquality.getCaridnality() == 1) {
            return this.applyNIRule(annotatedEquality, node0, node1, node2, dependencySet);
        }
        PermanentDependencySet permanentDependencySet = this.m_dependencySetFactory.getPermanent(dependencySet);
        this.m_bufferForAnnotatedEquality[0] = annotatedEquality;
        this.m_bufferForAnnotatedEquality[1] = node0;
        this.m_bufferForAnnotatedEquality[2] = node1;
        this.m_bufferForAnnotatedEquality[3] = node2;
        this.m_bufferForAnnotatedEquality[4] = permanentDependencySet;
        this.m_dependencySetFactory.addUsage(permanentDependencySet);
        this.m_annotatedEqualities.addTuple(this.m_bufferForAnnotatedEquality);
        return true;
    }

    private boolean applyNIRule(AnnotatedEquality annotatedEquality, Node node0, Node node1, Node node2, DependencySet dependencySet) {
        Node otherNode;
        Node niTargetNode;
        Node newRootNode;
        if (node0.isPruned() || node1.isPruned() || node2.isPruned()) {
            return false;
        }
        dependencySet = node0.addCanonicalNodeDependencySet(dependencySet);
        dependencySet = node1.addCanonicalNodeDependencySet(dependencySet);
        dependencySet = node2.addCanonicalNodeDependencySet(dependencySet);
        if (NominalIntroductionManager.canForgetAnnotation(node0 = node0.getCanonicalNode(), node1 = node1.getCanonicalNode(), node2 = node2.getCanonicalNode())) {
            return this.m_mergingManager.mergeNodes(node0, node1, dependencySet);
        }
        if (!node0.isRootNode() && !node2.isParentOf(node0)) {
            niTargetNode = node0;
            otherNode = node1;
        } else {
            niTargetNode = node1;
            otherNode = node0;
        }
        if (this.m_tableau.m_tableauMonitor != null) {
            this.m_tableau.m_tableauMonitor.nominalIntorductionStarted(node2, niTargetNode, annotatedEquality, node0, node1);
        }
        if (annotatedEquality.getCaridnality() > 1) {
            NominalIntroductionBranchingPoint branchingPoint = new NominalIntroductionBranchingPoint(this.m_tableau, node2, niTargetNode, otherNode, annotatedEquality);
            this.m_tableau.pushBranchingPoint(branchingPoint);
            dependencySet = this.m_tableau.getDependencySetFactory().addBranchingPoint(dependencySet, branchingPoint.getLevel());
        }
        if (!(newRootNode = this.getNIRootFor(dependencySet, node2, annotatedEquality, 1)).isActive()) {
            assert (newRootNode.isMerged());
            dependencySet = newRootNode.addCanonicalNodeDependencySet(dependencySet);
            newRootNode = newRootNode.getCanonicalNode();
        }
        this.m_mergingManager.mergeNodes(niTargetNode, newRootNode, dependencySet);
        if (!otherNode.isPruned()) {
            dependencySet = otherNode.addCanonicalNodeDependencySet(dependencySet);
            this.m_mergingManager.mergeNodes(otherNode.getCanonicalNode(), newRootNode, dependencySet);
        }
        if (this.m_tableau.m_tableauMonitor != null) {
            this.m_tableau.m_tableauMonitor.nominalIntorductionFinished(node2, niTargetNode, annotatedEquality, node0, node1);
        }
        return true;
    }

    private Node getNIRootFor(DependencySet dependencySet, Node rootNode, AnnotatedEquality annotatedEquality, int number) {
        this.m_bufferForRootNodes[0] = rootNode;
        this.m_bufferForRootNodes[1] = annotatedEquality;
        this.m_bufferForRootNodes[2] = number;
        int tupleIndex = this.m_newRootNodesIndex.getTupleIndex(this.m_bufferForRootNodes);
        if (tupleIndex == -1) {
            Node newRootNode = this.m_tableau.createNewNINode(dependencySet);
            this.m_bufferForRootNodes[3] = newRootNode;
            this.m_newRootNodesIndex.addTuple(this.m_bufferForRootNodes, this.m_newRootNodesTable.getFirstFreeTupleIndex());
            this.m_newRootNodesTable.addTuple(this.m_bufferForRootNodes);
            return newRootNode;
        }
        return (Node)this.m_newRootNodesTable.getTupleObject(tupleIndex, 3);
    }

    protected class NominalIntroductionBranchingPoint
    extends BranchingPoint {
        private static final long serialVersionUID = 6678113479704184263L;
        protected final Node m_rootNode;
        protected final Node m_niTargetNode;
        protected final Node m_otherNode;
        protected final AnnotatedEquality m_annotatedEquality;
        protected int m_currentRootNode;

        public NominalIntroductionBranchingPoint(Tableau tableau, Node rootNode, Node niTargetNode, Node otherNode, AnnotatedEquality annotatedEquality) {
            super(tableau);
            this.m_rootNode = rootNode;
            this.m_niTargetNode = niTargetNode;
            this.m_otherNode = otherNode;
            this.m_annotatedEquality = annotatedEquality;
            this.m_currentRootNode = 1;
        }

        @Override
        public void startNextChoice(Tableau tableau, DependencySet clashDepdendencySet) {
            Node newRootNode;
            ++this.m_currentRootNode;
            assert (this.m_currentRootNode <= this.m_annotatedEquality.getCaridnality());
            DependencySet dependencySet = clashDepdendencySet;
            if (this.m_currentRootNode == this.m_annotatedEquality.getCaridnality()) {
                dependencySet = tableau.getDependencySetFactory().removeBranchingPoint(dependencySet, this.m_level);
            }
            if (!(newRootNode = NominalIntroductionManager.this.getNIRootFor(dependencySet, this.m_rootNode, this.m_annotatedEquality, this.m_currentRootNode)).isActive()) {
                assert (newRootNode.isMerged());
                dependencySet = newRootNode.addCanonicalNodeDependencySet(dependencySet);
                newRootNode = newRootNode.getCanonicalNode();
            }
            NominalIntroductionManager.this.m_mergingManager.mergeNodes(this.m_niTargetNode, newRootNode, dependencySet);
            if (!this.m_otherNode.isPruned()) {
                dependencySet = this.m_otherNode.addCanonicalNodeDependencySet(dependencySet);
                NominalIntroductionManager.this.m_mergingManager.mergeNodes(this.m_otherNode.getCanonicalNode(), newRootNode, dependencySet);
            }
        }
    }

}

