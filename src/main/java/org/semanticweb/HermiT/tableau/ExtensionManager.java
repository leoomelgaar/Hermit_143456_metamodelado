package org.semanticweb.HermiT.tableau;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.semanticweb.HermiT.model.AnnotatedEquality;
import org.semanticweb.HermiT.model.AtomicConcept;
import org.semanticweb.HermiT.model.AtomicRole;
import org.semanticweb.HermiT.model.Concept;
import org.semanticweb.HermiT.model.DLPredicate;
import org.semanticweb.HermiT.model.DataRange;
import org.semanticweb.HermiT.model.DescriptionGraph;
import org.semanticweb.HermiT.model.Equality;
import org.semanticweb.HermiT.model.InternalDatatype;
import org.semanticweb.HermiT.model.InverseRole;
import org.semanticweb.HermiT.model.Role;
import org.semanticweb.HermiT.monitor.TableauMonitor;

public final class ExtensionManager
implements Serializable {
    private static final long serialVersionUID = 5900300914631070591L;
    final Tableau m_tableau;
    final TableauMonitor m_tableauMonitor;
    private final DependencySetFactory m_dependencySetFactory;
    private final Map<Integer, ExtensionTable> m_extensionTablesByArity;
    private final ExtensionTable[] m_allExtensionTablesArray;
    final ExtensionTable m_binaryExtensionTable;
    final ExtensionTable m_ternaryExtensionTable;
    private final Object[] m_binaryAuxiliaryTupleContains;
    private final Object[] m_binaryAuxiliaryTupleAdd;
    private final Object[] m_ternaryAuxiliaryTupleContains;
    final Object[] m_ternaryAuxiliaryTupleAdd;
    private final Object[] m_fouraryAuxiliaryTupleContains;
    private final Object[] m_fouraryAuxiliaryTupleAdd;
    private PermanentDependencySet m_clashDependencySet;
    private boolean m_addActive;

    public ExtensionManager(Tableau tableau) {
        this.m_tableau = tableau;
        this.m_tableauMonitor = this.m_tableau.m_tableauMonitor;
        this.m_dependencySetFactory = this.m_tableau.m_dependencySetFactory;
        this.m_extensionTablesByArity = new HashMap<Integer, ExtensionTable>();
        this.m_binaryExtensionTable = new ExtensionTableWithTupleIndexes(this.m_tableau, 2, !this.m_tableau.isDeterministic(), new TupleIndex[]{new TupleIndex(new int[]{1, 0}), new TupleIndex(new int[]{0, 1})}){
            private static final long serialVersionUID = 1462821385000191875L;

            @Override
            public boolean isTupleActive(Object[] tuple) {
                return ((Node)tuple[1]).isActive();
            }

            @Override
            public boolean isTupleActive(int tupleIndex) {
                return ((Node)this.m_tupleTable.getTupleObject(tupleIndex, 1)).isActive();
            }
        };
        this.m_extensionTablesByArity.put(Integer.valueOf(2), this.m_binaryExtensionTable);
        this.m_ternaryExtensionTable = new ExtensionTableWithTupleIndexes(this.m_tableau, 3, !this.m_tableau.isDeterministic(), new TupleIndex[]{new TupleIndex(new int[]{0, 1, 2}), new TupleIndex(new int[]{1, 2, 0}), new TupleIndex(new int[]{2, 0, 1})}){
            private static final long serialVersionUID = -731201626401421877L;

            @Override
            public boolean isTupleActive(Object[] tuple) {
                return ((Node)tuple[1]).isActive() && ((Node)tuple[2]).isActive();
            }

            @Override
            public boolean isTupleActive(int tupleIndex) {
                return ((Node)this.m_tupleTable.getTupleObject(tupleIndex, 1)).isActive() && ((Node)this.m_tupleTable.getTupleObject(tupleIndex, 2)).isActive();
            }
        };
        this.m_extensionTablesByArity.put(Integer.valueOf(3), this.m_ternaryExtensionTable);
        for (DescriptionGraph descriptionGraph : this.m_tableau.m_permanentDLOntology.getAllDescriptionGraphs()) {
            Integer arityInteger = descriptionGraph.getNumberOfVertices() + 1;
            if (this.m_extensionTablesByArity.containsKey(arityInteger)) continue;
            this.m_extensionTablesByArity.put(arityInteger, new ExtensionTableWithFullIndex(this.m_tableau, descriptionGraph.getNumberOfVertices() + 1, !this.m_tableau.isDeterministic()));
        }
        this.m_allExtensionTablesArray = new ExtensionTable[this.m_extensionTablesByArity.size()];
        this.m_extensionTablesByArity.values().toArray(this.m_allExtensionTablesArray);
        this.m_binaryAuxiliaryTupleContains = new Object[2];
        this.m_binaryAuxiliaryTupleAdd = new Object[2];
        this.m_ternaryAuxiliaryTupleContains = new Object[3];
        this.m_ternaryAuxiliaryTupleAdd = new Object[3];
        this.m_fouraryAuxiliaryTupleContains = new Object[4];
        this.m_fouraryAuxiliaryTupleAdd = new Object[4];
    }

    public void clear() {
        for (int index = this.m_allExtensionTablesArray.length - 1; index >= 0; --index) {
            this.m_allExtensionTablesArray[index].clear();
        }
        this.m_clashDependencySet = null;
        this.m_binaryAuxiliaryTupleContains[0] = null;
        this.m_binaryAuxiliaryTupleContains[1] = null;
        this.m_binaryAuxiliaryTupleAdd[0] = null;
        this.m_binaryAuxiliaryTupleAdd[1] = null;
        this.m_ternaryAuxiliaryTupleContains[0] = null;
        this.m_ternaryAuxiliaryTupleContains[1] = null;
        this.m_ternaryAuxiliaryTupleContains[2] = null;
        this.m_ternaryAuxiliaryTupleAdd[0] = null;
        this.m_ternaryAuxiliaryTupleAdd[1] = null;
        this.m_ternaryAuxiliaryTupleAdd[2] = null;
        this.m_fouraryAuxiliaryTupleContains[0] = null;
        this.m_fouraryAuxiliaryTupleContains[1] = null;
        this.m_fouraryAuxiliaryTupleContains[2] = null;
        this.m_fouraryAuxiliaryTupleContains[3] = null;
        this.m_fouraryAuxiliaryTupleAdd[0] = null;
        this.m_fouraryAuxiliaryTupleAdd[1] = null;
        this.m_fouraryAuxiliaryTupleAdd[2] = null;
        this.m_fouraryAuxiliaryTupleAdd[3] = null;
    }

    public void branchingPointPushed() {
        for (int index = this.m_allExtensionTablesArray.length - 1; index >= 0; --index) {
            this.m_allExtensionTablesArray[index].branchingPointPushed();
        }
    }

    public void backtrack() {
        for (int index = this.m_allExtensionTablesArray.length - 1; index >= 0; --index) {
            this.m_allExtensionTablesArray[index].backtrack();
        }
    }

    public ExtensionTable getBinaryExtensionTable() {
        return this.m_binaryExtensionTable;
    }

    public ExtensionTable getTernaryExtensionTable() {
        return this.m_ternaryExtensionTable;
    }

    public ExtensionTable getExtensionTable(int arity) {
        switch (arity) {
            case 2: {
                return this.m_binaryExtensionTable;
            }
            case 3: {
                return this.m_ternaryExtensionTable;
            }
        }
        return this.m_extensionTablesByArity.get(arity);
    }

    public Collection<ExtensionTable> getExtensionTables() {
        return this.m_extensionTablesByArity.values();
    }

    public boolean propagateDeltaNew() {
        boolean hasChange = false;
        for (int index = 0; index < this.m_allExtensionTablesArray.length; ++index) {
            if (!this.m_allExtensionTablesArray[index].propagateDeltaNew()) continue;
            hasChange = true;
        }
        return hasChange;
    }
    
    public boolean checkDeltaNewPropagation() {
    	boolean hasChange = false;
        for (int index = 0; index < this.m_allExtensionTablesArray.length; ++index) {
            if (!this.m_allExtensionTablesArray[index].checkDeltaNewPropagation()) continue;
            hasChange = true;
        }
        return hasChange;
    }
    
    public void resetDeltaNew() {
    	for (int index = 0; index < this.m_allExtensionTablesArray.length; ++index) {
            this.m_allExtensionTablesArray[index].resetDeltaNew();
        }
    }

    public void clearClash() {
        if (this.m_clashDependencySet != null) {
            this.m_dependencySetFactory.removeUsage(this.m_clashDependencySet);
            this.m_clashDependencySet = null;
        }
    }

    public void setClash(DependencySet clashDependencySet) {
        if (this.m_clashDependencySet != null) {
            this.m_dependencySetFactory.removeUsage(this.m_clashDependencySet);
        }
        this.m_clashDependencySet = this.m_dependencySetFactory.getPermanent(clashDependencySet);
        if (this.m_clashDependencySet != null) {
            this.m_dependencySetFactory.addUsage(this.m_clashDependencySet);
        }
        if (this.m_tableauMonitor != null) {
            this.m_tableauMonitor.clashDetected();
        }
    }

    public DependencySet getClashDependencySet() {
        return this.m_clashDependencySet;
    }

//    Te devuelve si hay alguna contradiccón en la ontología
    public boolean containsClash() {
        return this.m_clashDependencySet != null && !this.m_clashDependencySet.isEmpty();
    }

    public boolean containsConceptAssertion(Concept concept, Node node) {
        if (node.getNodeType().isAbstract() && AtomicConcept.THING.equals(concept)) {
            return true;
        }
        this.m_binaryAuxiliaryTupleContains[0] = concept;
        this.m_binaryAuxiliaryTupleContains[1] = node;
        return this.m_binaryExtensionTable.containsTuple(this.m_binaryAuxiliaryTupleContains);
    }

    public boolean containsDataRangeAssertion(DataRange range, Node node) {
        if (!node.getNodeType().isAbstract() && InternalDatatype.RDFS_LITERAL.equals(range)) {
            return true;
        }
        this.m_binaryAuxiliaryTupleContains[0] = range;
        this.m_binaryAuxiliaryTupleContains[1] = node;
        return this.m_binaryExtensionTable.containsTuple(this.m_binaryAuxiliaryTupleContains);
    }

    public boolean containsRoleAssertion(Role role, Node nodeFrom, Node nodeTo) {
        if (role instanceof AtomicRole) {
            this.m_ternaryAuxiliaryTupleContains[0] = role;
            this.m_ternaryAuxiliaryTupleContains[1] = nodeFrom;
            this.m_ternaryAuxiliaryTupleContains[2] = nodeTo;
        } else {
            this.m_ternaryAuxiliaryTupleContains[0] = ((InverseRole)role).getInverseOf();
            this.m_ternaryAuxiliaryTupleContains[1] = nodeTo;
            this.m_ternaryAuxiliaryTupleContains[2] = nodeFrom;
        }
        return this.m_ternaryExtensionTable.containsTuple(this.m_ternaryAuxiliaryTupleContains);
    }

    public boolean containsAssertion(DLPredicate dlPredicate, Node node) {
        if (AtomicConcept.THING.equals(dlPredicate)) {
            return true;
        }
        this.m_binaryAuxiliaryTupleContains[0] = dlPredicate;
        this.m_binaryAuxiliaryTupleContains[1] = node;
        return this.m_binaryExtensionTable.containsTuple(this.m_binaryAuxiliaryTupleContains);
    }

    public boolean containsAssertion(DLPredicate dlPredicate, Node node0, Node node1) {
        if (Equality.INSTANCE.equals(dlPredicate)) {
            return node0 == node1;
        }
        this.m_ternaryAuxiliaryTupleContains[0] = dlPredicate;
        this.m_ternaryAuxiliaryTupleContains[1] = node0;
        this.m_ternaryAuxiliaryTupleContains[2] = node1;
        return this.m_ternaryExtensionTable.containsTuple(this.m_ternaryAuxiliaryTupleContains);
    }

    public boolean containsAssertion(DLPredicate dlPredicate, Node node0, Node node1, Node node2) {
        this.m_fouraryAuxiliaryTupleContains[0] = dlPredicate;
        this.m_fouraryAuxiliaryTupleContains[1] = node0;
        this.m_fouraryAuxiliaryTupleContains[2] = node1;
        this.m_fouraryAuxiliaryTupleContains[3] = node2;
        return this.containsTuple(this.m_fouraryAuxiliaryTupleContains);
    }

    public static boolean containsAnnotatedEquality(Node node0, Node node1, Node node2) {
        return NominalIntroductionManager.canForgetAnnotation(node0, node1, node2) && node0 == node1;
    }

    public boolean containsTuple(Object[] tuple) {
        if (tuple.length == 0) {
            return this.containsClash();
        }
        if (AtomicConcept.THING.equals(tuple[0])) {
            return true;
        }
        if (Equality.INSTANCE.equals(tuple[0])) {
            return tuple[1] == tuple[2];
        }
        if (tuple[0] instanceof AnnotatedEquality) {
            return NominalIntroductionManager.canForgetAnnotation((Node)tuple[1], (Node)tuple[2], (Node)tuple[3]) && tuple[1] == tuple[2];
        }
        return this.getExtensionTable(tuple.length).containsTuple(tuple);
    }

    public DependencySet getConceptAssertionDependencySet(Concept concept, Node node) {
        if (AtomicConcept.THING.equals(concept)) {
            return this.m_dependencySetFactory.emptySet();
        }
        this.m_binaryAuxiliaryTupleContains[0] = concept;
        this.m_binaryAuxiliaryTupleContains[1] = node;
        return this.m_binaryExtensionTable.getDependencySet(this.m_binaryAuxiliaryTupleContains);
    }

    public DependencySet getDataRangeAssertionDependencySet(DataRange range, Node node) {
        if (InternalDatatype.RDFS_LITERAL.equals(range)) {
            return this.m_dependencySetFactory.emptySet();
        }
        this.m_binaryAuxiliaryTupleContains[0] = range;
        this.m_binaryAuxiliaryTupleContains[1] = node;
        return this.m_binaryExtensionTable.getDependencySet(this.m_binaryAuxiliaryTupleContains);
    }

    public DependencySet getRoleAssertionDependencySet(Role role, Node nodeFrom, Node nodeTo) {
        if (role instanceof AtomicRole) {
            this.m_ternaryAuxiliaryTupleContains[0] = role;
            this.m_ternaryAuxiliaryTupleContains[1] = nodeFrom;
            this.m_ternaryAuxiliaryTupleContains[2] = nodeTo;
        } else {
            this.m_ternaryAuxiliaryTupleContains[0] = ((InverseRole)role).getInverseOf();
            this.m_ternaryAuxiliaryTupleContains[1] = nodeTo;
            this.m_ternaryAuxiliaryTupleContains[2] = nodeFrom;
        }
        return this.m_ternaryExtensionTable.getDependencySet(this.m_ternaryAuxiliaryTupleContains);
    }

    public DependencySet getAssertionDependencySet(DLPredicate dlPredicate, Node node) {
        this.m_binaryAuxiliaryTupleContains[0] = dlPredicate;
        this.m_binaryAuxiliaryTupleContains[1] = node;
        return this.m_binaryExtensionTable.getDependencySet(this.m_binaryAuxiliaryTupleContains);
    }

    public DependencySet getAssertionDependencySet(DLPredicate dlPredicate, Node node0, Node node1) {
        if (Equality.INSTANCE.equals(dlPredicate)) {
            return node0 == node1 ? this.m_dependencySetFactory.emptySet() : null;
        }
        this.m_ternaryAuxiliaryTupleContains[0] = dlPredicate;
        this.m_ternaryAuxiliaryTupleContains[1] = node0;
        this.m_ternaryAuxiliaryTupleContains[2] = node1;
        return this.m_ternaryExtensionTable.getDependencySet(this.m_ternaryAuxiliaryTupleContains);
    }

    public DependencySet getAssertionDependencySet(DLPredicate dlPredicate, Node node0, Node node1, Node node2) {
        this.m_fouraryAuxiliaryTupleContains[0] = dlPredicate;
        this.m_fouraryAuxiliaryTupleContains[1] = node0;
        this.m_fouraryAuxiliaryTupleContains[2] = node1;
        this.m_fouraryAuxiliaryTupleContains[3] = node2;
        return this.getTupleDependencySet(this.m_fouraryAuxiliaryTupleContains);
    }

    public DependencySet getTupleDependencySet(Object[] tuple) {
        if (tuple.length == 0) {
            return this.m_clashDependencySet;
        }
        return this.getExtensionTable(tuple.length).getDependencySet(tuple);
    }

    public boolean isCore(Object[] tuple) {
        if (tuple.length == 0) {
            return true;
        }
        return this.getExtensionTable(tuple.length).isCore(tuple);
    }

    public boolean addConceptAssertion(Concept concept, Node node, DependencySet dependencySet, boolean isCore) {
        if (this.m_addActive) {
            throw new IllegalStateException("ExtensionManager is not reentrant.");
        }
        this.m_addActive = true;
        try {
            this.m_binaryAuxiliaryTupleAdd[0] = concept;
            this.m_binaryAuxiliaryTupleAdd[1] = node;
            boolean bl = this.m_binaryExtensionTable.addTuple(this.m_binaryAuxiliaryTupleAdd, dependencySet, isCore);
            return bl;
        }
        finally {
            this.m_addActive = false;
        }
    }

    public boolean addDataRangeAssertion(DataRange dataRange, Node node, DependencySet dependencySet, boolean isCore) {
        if (this.m_addActive) {
            throw new IllegalStateException("ExtensionManager is not reentrant.");
        }
        this.m_addActive = true;
        try {
            this.m_binaryAuxiliaryTupleAdd[0] = dataRange;
            this.m_binaryAuxiliaryTupleAdd[1] = node;
            boolean bl = this.m_binaryExtensionTable.addTuple(this.m_binaryAuxiliaryTupleAdd, dependencySet, isCore);
            return bl;
        }
        finally {
            this.m_addActive = false;
        }
    }

    public boolean addRoleAssertion(Role role, Node nodeFrom, Node nodeTo, DependencySet dependencySet, boolean isCore) {
        if (role instanceof AtomicRole) {
            return this.addAssertion((AtomicRole)role, nodeFrom, nodeTo, dependencySet, isCore);
        }
        return this.addAssertion(((InverseRole)role).getInverseOf(), nodeTo, nodeFrom, dependencySet, isCore);
    }

    public boolean addAssertion(DLPredicate dlPredicate, Node node, DependencySet dependencySet, boolean isCore) {
        if (this.m_addActive) {
            throw new IllegalStateException("ExtensionManager is not reentrant.");
        }
        this.m_addActive = true;
        try {
            this.m_binaryAuxiliaryTupleAdd[0] = dlPredicate;
            this.m_binaryAuxiliaryTupleAdd[1] = node;
            boolean bl = this.m_binaryExtensionTable.addTuple(this.m_binaryAuxiliaryTupleAdd, dependencySet, isCore);
            return bl;
        }
        finally {
            this.m_addActive = false;
        }
    }

    public boolean addAssertion(DLPredicate dlPredicate, Node node0, Node node1, DependencySet dependencySet, boolean isCore) {
        if (Equality.INSTANCE.equals(dlPredicate)) {
            return this.m_tableau.m_mergingManager.mergeNodes(node0, node1, dependencySet);
        }
        if (this.m_addActive) {
            throw new IllegalStateException("ExtensionManager is not reentrant.");
        }
        this.m_addActive = true;
        try {
            this.m_ternaryAuxiliaryTupleAdd[0] = dlPredicate;
            this.m_ternaryAuxiliaryTupleAdd[1] = node0;
            this.m_ternaryAuxiliaryTupleAdd[2] = node1;
            boolean bl = this.m_ternaryExtensionTable.addTuple(this.m_ternaryAuxiliaryTupleAdd, dependencySet, isCore);
            return bl;
        }
        finally {
            this.m_addActive = false;
        }
    }
	
    public boolean addAssertion(DLPredicate dlPredicate, Node node0, Node node1, Node node2, DependencySet dependencySet, boolean isCore) {
        if (this.m_addActive) {
            throw new IllegalStateException("ExtensionManager is not reentrant.");
        }
        this.m_fouraryAuxiliaryTupleAdd[0] = dlPredicate;
        this.m_fouraryAuxiliaryTupleAdd[1] = node0;
        this.m_fouraryAuxiliaryTupleAdd[2] = node1;
        this.m_fouraryAuxiliaryTupleAdd[3] = node2;
        return this.addTuple(this.m_fouraryAuxiliaryTupleAdd, dependencySet, isCore);
    }

    public boolean addAnnotatedEquality(AnnotatedEquality annotatedEquality, Node node0, Node node1, Node node2, DependencySet dependencySet) {
        return this.m_tableau.m_nominalIntroductionManager.addAnnotatedEquality(annotatedEquality, node0, node1, node2, dependencySet);
    }

    public boolean addTuple(Object[] tuple, DependencySet dependencySet, boolean isCore) {
        if (tuple.length == 0) {
            boolean result = this.m_clashDependencySet == null;
            this.setClash(dependencySet);
            return result;
        }
        if (Equality.INSTANCE.equals(tuple[0])) {
            return this.m_tableau.m_mergingManager.mergeNodes((Node)tuple[1], (Node)tuple[2], dependencySet);
        }
        if (tuple[0] instanceof AnnotatedEquality) {
            return this.m_tableau.m_nominalIntroductionManager.addAnnotatedEquality((AnnotatedEquality)tuple[0], (Node)tuple[1], (Node)tuple[2], (Node)tuple[3], dependencySet);
        }
        if (this.m_addActive) {
            throw new IllegalStateException("ExtensionManager is not reentrant.");
        }
        this.m_addActive = true;
        try {
            return this.getExtensionTable(tuple.length).addTuple(tuple, dependencySet, isCore);
        }
        finally {
            this.m_addActive = false;
        }
    }

}

