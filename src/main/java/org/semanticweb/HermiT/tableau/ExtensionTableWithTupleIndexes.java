package org.semanticweb.HermiT.tableau;

import java.io.Serializable;
import org.semanticweb.HermiT.model.AtomicConcept;
import org.semanticweb.HermiT.model.AtomicRole;
import org.semanticweb.HermiT.model.Concept;
import org.semanticweb.HermiT.model.InternalDatatype;

import static org.eclipse.osgi.framework.debug.Debug.print;
import static org.eclipse.osgi.framework.debug.Debug.println;

public class ExtensionTableWithTupleIndexes
extends ExtensionTable {
    private static final long serialVersionUID = -684536236157965372L;
    protected final TupleIndex[] m_tupleIndexes;
    protected final Object[] m_auxiliaryTuple;

    public ExtensionTableWithTupleIndexes(Tableau tableau, int tupleArity, boolean needsDependencySets, TupleIndex[] tupleIndexes) {
        super(tableau, tupleArity, needsDependencySets);
        this.m_tupleIndexes = tupleIndexes;
        this.m_auxiliaryTuple = new Object[this.m_tupleArity];
    }

    @Override
    public int sizeInMemory() {
        int size = this.m_tupleTable.sizeInMemory();
        for (int i = this.m_tupleIndexes.length - 1; i >= 0; --i) {
            size += this.m_tupleIndexes[i].sizeInMemoy();
        }
        return size;
    }

    @Override
    public boolean addTuple(Object[] tuple, DependencySet dependencySet, boolean isCore) {
        if (this.m_tableauMonitor != null) {
            this.m_tableauMonitor.addFactStarted(tuple, isCore);
        }
        if (!(!this.isTupleActive(tuple) || !this.m_tableau.m_needsThingExtension && AtomicConcept.THING.equals(tuple[0]) || !this.m_tableau.m_needsRDFSLiteralExtension && InternalDatatype.RDFS_LITERAL.equals(tuple[0]))) {
            print("en add tuple: ");
            println(tuple[0]);
            int firstFreeTupleIndex = this.m_tupleTable.getFirstFreeTupleIndex();
            int addTupleIndex = this.m_tupleIndexes[0].addTuple(tuple, firstFreeTupleIndex);
            if (addTupleIndex == firstFreeTupleIndex) {
                for (int index = 1; index < this.m_tupleIndexes.length; ++index) {
                    this.m_tupleIndexes[index].addTuple(tuple, addTupleIndex);
                }
                this.m_tupleTable.addTuple(tuple);
                this.m_dependencySetManager.setDependencySet(addTupleIndex, dependencySet);
                this.m_coreManager.setCore(addTupleIndex, isCore);
                this.m_afterDeltaNewTupleIndex = this.m_tupleTable.getFirstFreeTupleIndex();
                if (this.m_tableauMonitor != null) {
                    this.m_tableauMonitor.addFactFinished(tuple, isCore, true);
                }
                this.postAdd(tuple, dependencySet, addTupleIndex, isCore);
                print("agregado en el indice: ");
                print(addTupleIndex);
                println(this);
                return true;
            }
            println("no se agrego");
            if (isCore && !this.m_coreManager.isCore(addTupleIndex)) {
                this.m_coreManager.addCore(addTupleIndex);
                Object dlPredicateObject = tuple[0];
                if (dlPredicateObject instanceof Concept) {
                    this.m_tableau.m_existentialExpansionStrategy.assertionCoreSet((Concept)dlPredicateObject, (Node)tuple[1]);
                } else if (dlPredicateObject instanceof AtomicRole) {
                    this.m_tableau.m_existentialExpansionStrategy.assertionCoreSet((AtomicRole)dlPredicateObject, (Node)tuple[1], (Node)tuple[2]);
                }
            }
        }
        if (this.m_tableauMonitor != null) {
            this.m_tableauMonitor.addFactFinished(tuple, isCore, false);
        }
        return false;
    }

    @Override
    public boolean containsTuple(Object[] tuple) {
        int tupleIndex = this.m_tupleIndexes[0].getTupleIndex(tuple);
        return tupleIndex != -1 && this.isTupleActive(tupleIndex);
    }

    @Override
    public DependencySet getDependencySet(Object[] tuple) {
        int tupleIndex = this.m_tupleIndexes[0].getTupleIndex(tuple);
        if (tupleIndex == -1) {
            return null;
        }
        return this.m_dependencySetManager.getDependencySet(tupleIndex);
    }

    @Override
    public boolean isCore(Object[] tuple) {
        int tupleIndex = this.m_tupleIndexes[0].getTupleIndex(tuple);
        if (tupleIndex == -1) {
            return false;
        }
        return this.m_coreManager.isCore(tupleIndex);
    }

    @Override
    public ExtensionTable.Retrieval createRetrieval(int[] bindingPositions, Object[] bindingsBuffer, Object[] tupleBuffer, boolean ownsBuffers, ExtensionTable.View extensionView) {
        TupleIndex selectedTupleIndex = null;
        int boundPrefixSizeInSelected = 0;
        for (int index = this.m_tupleIndexes.length - 1; index >= 0; --index) {
            int[] indexingSequence = this.m_tupleIndexes[index].getIndexingSequence();
            int boundPrefixSize = 0;
            for (int position = 0; position < indexingSequence.length && bindingPositions[indexingSequence[position]] != -1; ++position) {
                ++boundPrefixSize;
            }
            if (boundPrefixSize <= boundPrefixSizeInSelected) continue;
            selectedTupleIndex = this.m_tupleIndexes[index];
            boundPrefixSizeInSelected = boundPrefixSize;
        }
        if (selectedTupleIndex == null) {
            return new ExtensionTable.UnindexedRetrieval(bindingPositions, bindingsBuffer, tupleBuffer, ownsBuffers, extensionView);
        }
        return new IndexedRetrieval(selectedTupleIndex, bindingPositions, bindingsBuffer, tupleBuffer, ownsBuffers, extensionView);
    }

    @Override
    protected void removeTuple(int tupleIndex) {
        this.m_tupleTable.retrieveTuple(this.m_auxiliaryTuple, tupleIndex);
        for (int index = this.m_tupleIndexes.length - 1; index >= 0; --index) {
            this.m_tupleIndexes[index].removeTuple(this.m_auxiliaryTuple);
        }
        this.postRemove(this.m_auxiliaryTuple, tupleIndex);
    }

    @Override
    public void clear() {
        super.clear();
        for (int index = this.m_tupleIndexes.length - 1; index >= 0; --index) {
            this.m_tupleIndexes[index].clear();
        }
    }

    protected static int[] createSelectionArray(int[] bindingPositions, int[] indexingSequence) {
        int boundPrefixLength = 0;
        for (int index = 0; index < indexingSequence.length && bindingPositions[indexingSequence[index]] != -1; ++index) {
            ++boundPrefixLength;
        }
        int[] selection = new int[boundPrefixLength];
        for (int index = 0; index < boundPrefixLength; ++index) {
            selection[index] = bindingPositions[indexingSequence[index]];
        }
        return selection;
    }

    protected class IndexedRetrieval
    extends TupleIndex.TupleIndexRetrieval
    implements ExtensionTable.Retrieval,
    Serializable {
        private static final long serialVersionUID = 2180748099314801734L;
        protected final int[] m_bindingPositions;
        protected final Object[] m_tupleBuffer;
        protected final boolean m_ownsBuffers;
        protected final ExtensionTable.View m_extensionView;
        protected final boolean m_checkTupleSelection;
        protected DependencySet m_dependencySet;
        protected boolean m_isCore;
        protected int m_firstTupleIndex;
        protected int m_afterLastTupleIndex;

        public IndexedRetrieval(TupleIndex tupleIndex, int[] bindingPositions, Object[] bindingsBuffer, Object[] tupleBuffer, boolean ownsBuffers, ExtensionTable.View extensionView) {
            super(tupleIndex, bindingsBuffer, ExtensionTableWithTupleIndexes.createSelectionArray(bindingPositions, tupleIndex.m_indexingSequence));
            this.m_ownsBuffers = ownsBuffers;
            this.m_bindingPositions = bindingPositions;
            this.m_extensionView = extensionView;
            this.m_tupleBuffer = tupleBuffer;
            int numberOfBoundPositions = 0;
            for (int index = this.m_bindingPositions.length - 1; index >= 0; --index) {
                if (this.m_bindingPositions[index] == -1) continue;
                ++numberOfBoundPositions;
            }
            this.m_checkTupleSelection = numberOfBoundPositions > this.m_selectionIndices.length;
        }

        @Override
        public ExtensionTable getExtensionTable() {
            return ExtensionTableWithTupleIndexes.this;
        }

        @Override
        public ExtensionTable.View getExtensionView() {
            return this.m_extensionView;
        }

        @Override
        public void clear() {
            if (this.m_ownsBuffers) {
                int index;
                for (index = this.m_bindingsBuffer.length - 1; index >= 0; --index) {
                    this.m_bindingsBuffer[index] = null;
                }
                for (index = this.m_tupleBuffer.length - 1; index >= 0; --index) {
                    this.m_tupleBuffer[index] = null;
                }
            }
        }

        @Override
        public int[] getBindingPositions() {
            return this.m_bindingPositions;
        }

        @Override
        public Object[] getBindingsBuffer() {
            return this.m_bindingsBuffer;
        }

        @Override
        public Object[] getTupleBuffer() {
            return this.m_tupleBuffer;
        }

        @Override
        public DependencySet getDependencySet() {
            return this.m_dependencySet;
        }

        @Override
        public boolean isCore() {
            return this.m_isCore;
        }

        @Override
        public void open() {
            switch (this.m_extensionView) {
                case EXTENSION_THIS: {
                    this.m_firstTupleIndex = 0;
                    this.m_afterLastTupleIndex = ExtensionTableWithTupleIndexes.this.m_afterExtensionThisTupleIndex;
                    break;
                }
                case EXTENSION_OLD: {
                    this.m_firstTupleIndex = 0;
                    this.m_afterLastTupleIndex = ExtensionTableWithTupleIndexes.this.m_afterExtensionOldTupleIndex;
                    break;
                }
                case DELTA_OLD: {
                    this.m_firstTupleIndex = ExtensionTableWithTupleIndexes.this.m_afterExtensionOldTupleIndex;
                    this.m_afterLastTupleIndex = ExtensionTableWithTupleIndexes.this.m_afterExtensionThisTupleIndex;
                    break;
                }
                case TOTAL: {
                    this.m_firstTupleIndex = 0;
                    this.m_afterLastTupleIndex = ExtensionTableWithTupleIndexes.this.m_afterDeltaNewTupleIndex;
                    break;
                }
            }
            super.open();
            while (!this.afterLast()) {
                int tupleIndex = this.getCurrentTupleIndex();
                if (this.m_firstTupleIndex <= tupleIndex && tupleIndex < this.m_afterLastTupleIndex) {
                    ExtensionTableWithTupleIndexes.this.m_tupleTable.retrieveTuple(this.m_tupleBuffer, tupleIndex);
                    if (this.isTupleValid()) {
                        this.m_dependencySet = ExtensionTableWithTupleIndexes.this.m_dependencySetManager.getDependencySet(tupleIndex);
                        this.m_isCore = ExtensionTableWithTupleIndexes.this.m_coreManager.isCore(tupleIndex);
                        return;
                    }
                }
                super.next();
            }
        }

        @Override
        public void next() {
            super.next();
            while (!this.afterLast()) {
                int tupleIndex = this.getCurrentTupleIndex();
                if (this.m_firstTupleIndex <= tupleIndex && tupleIndex < this.m_afterLastTupleIndex) {
                    ExtensionTableWithTupleIndexes.this.m_tupleTable.retrieveTuple(this.m_tupleBuffer, tupleIndex);
                    if (this.isTupleValid()) {
                        this.m_dependencySet = ExtensionTableWithTupleIndexes.this.m_dependencySetManager.getDependencySet(tupleIndex);
                        this.m_isCore = ExtensionTableWithTupleIndexes.this.m_coreManager.isCore(tupleIndex);
                        return;
                    }
                }
                super.next();
            }
        }

        protected boolean isTupleValid() {
            if (!ExtensionTableWithTupleIndexes.this.isTupleActive(this.m_tupleBuffer)) {
                return false;
            }
            if (this.m_checkTupleSelection) {
                for (int index = this.m_bindingPositions.length - 1; index >= 0; --index) {
                    if (this.m_bindingPositions[index] == -1 || this.m_tupleBuffer[index].equals(this.m_bindingsBuffer[this.m_bindingPositions[index]])) continue;
                    return false;
                }
            }
            return true;
        }
    }

}

