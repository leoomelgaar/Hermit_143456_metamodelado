package org.semanticweb.HermiT.tableau;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.semanticweb.HermiT.model.AtomicConcept;
import org.semanticweb.HermiT.model.AtomicNegationConcept;
import org.semanticweb.HermiT.model.AtomicRole;
import org.semanticweb.HermiT.model.Concept;
import org.semanticweb.HermiT.model.DescriptionGraph;
import org.semanticweb.HermiT.model.ExistentialConcept;
import org.semanticweb.HermiT.model.NegatedAtomicRole;
import org.semanticweb.HermiT.monitor.TableauMonitor;

import static org.eclipse.osgi.framework.debug.Debug.println;

public abstract class ExtensionTable
implements Serializable {
    private static final long serialVersionUID = -5029938218056017193L;
    protected final Tableau m_tableau;
    protected final TableauMonitor m_tableauMonitor;
    protected final int m_tupleArity;
    protected final TupleTable m_tupleTable;
    protected final DependencySetManager m_dependencySetManager;
    protected final CoreManager m_coreManager;
    protected int m_afterExtensionOldTupleIndex;
    protected int m_afterExtensionThisTupleIndex;
    protected int m_afterDeltaNewTupleIndex;
    protected int[] m_indicesByBranchingPoint;

    public ExtensionTable(Tableau tableau, int tupleArity, boolean needsDependencySets) {
        this.m_tableau = tableau;
        this.m_tableauMonitor = this.m_tableau.m_tableauMonitor;
        this.m_tupleArity = tupleArity;
        this.m_tupleTable = new TupleTable(this.m_tupleArity + (needsDependencySets ? 1 : 0));
        this.m_dependencySetManager = needsDependencySets ? new LastObjectDependencySetManager(this) : new DeterministicDependencySetManager(this);
        this.m_coreManager = this.m_tupleArity == 2 ? new RealCoreManager() : new NoCoreManager();
        this.m_indicesByBranchingPoint = new int[6];
        this.m_afterExtensionOldTupleIndex = 0;
        this.m_afterExtensionThisTupleIndex = 0;
        this.m_afterDeltaNewTupleIndex = 0;
    }

    public abstract int sizeInMemory();

    public int getArity() {
        return this.m_tupleArity;
    }

    public void retrieveTuple(Object[] tupleBuffer, int tupleIndex) {
        this.m_tupleTable.retrieveTuple(tupleBuffer, tupleIndex);
    }

    public Object getTupleObject(int tupleIndex, int objectIndex) {
        return this.m_tupleTable.getTupleObject(tupleIndex, objectIndex);
    }

    public DependencySet getDependencySet(int tupleIndex) {
        return this.m_dependencySetManager.getDependencySet(tupleIndex);
    }

    public boolean isCore(int tupleIndex) {
        return this.m_coreManager.isCore(tupleIndex);
    }

    public abstract boolean addTuple(Object[] var1, DependencySet var2, boolean var3);

    protected void postAdd(Object[] tuple, DependencySet dependencySet, int tupleIndex, boolean isCore) {
        Object dlPredicateObject = tuple[0];
        if (dlPredicateObject instanceof Concept) {
            Node node = (Node)tuple[1];
            System.out.println("DEBUG: Added concept assertion: " + dlPredicateObject + " to node " + node.m_nodeID);
            if (dlPredicateObject instanceof AtomicConcept) {
                ++node.m_numberOfPositiveAtomicConcepts;
            } else if (dlPredicateObject instanceof ExistentialConcept) {
                node.addToUnprocessedExistentials((ExistentialConcept)dlPredicateObject);
            } else if (dlPredicateObject instanceof AtomicNegationConcept) {
                ++node.m_numberOfNegatedAtomicConcepts;
            }
            this.m_tableau.m_existentialExpansionStrategy.assertionAdded((Concept)dlPredicateObject, node, isCore);
        } else if (dlPredicateObject instanceof AtomicRole) {
            this.m_tableau.m_existentialExpansionStrategy.assertionAdded((AtomicRole)dlPredicateObject, (Node)tuple[1], (Node)tuple[2], isCore);
        } else if (dlPredicateObject instanceof NegatedAtomicRole) {
            ++((Node)tuple[1]).m_numberOfNegatedRoleAssertions;
        } else if (dlPredicateObject instanceof DescriptionGraph) {
            this.m_tableau.m_descriptionGraphManager.descriptionGraphTupleAdded(tupleIndex, tuple);
        }
        this.m_tableau.m_clashManager.tupleAdded(this, tuple, dependencySet);
        if (tuple.length>2 && tuple[1] instanceof Node && tuple[2] instanceof Node) {
        	Node node0 = (Node) tuple[1];
        	Node node1 = (Node) tuple[2];
        	if (tuple[0].toString().equals("!=")) {
            	this.m_tableau.metamodellingFlag = true;
            	this.m_tableau.m_metamodellingManager.differentIndividualsMap.putIfAbsent(node0.m_nodeID, new ArrayList<Integer>());
            	this.m_tableau.m_metamodellingManager.differentIndividualsMap.get(node0.m_nodeID).add(node1.m_nodeID);
            	System.out.println("DEBUG: Added != tuple: " + tuple[0] + "(" + node0.m_nodeID + "," + node1.m_nodeID + ")");
            } else {
            	this.m_tableau.m_metamodellingManager.nodeProperties.putIfAbsent(node0.m_nodeID, new HashMap<Integer, List<String>>());
				this.m_tableau.m_metamodellingManager.nodeProperties.get(node0.m_nodeID).putIfAbsent(node1.m_nodeID, new ArrayList<String>());
				this.m_tableau.m_metamodellingManager.nodeProperties.get(node0.m_nodeID).get(node1.m_nodeID).add(tuple[0].toString());
				System.out.println("DEBUG: Added metamodelling tuple: " + tuple[0] + "(" + node0.m_nodeID + "," + node1.m_nodeID + ")");
            }
        }
    }

    public abstract boolean containsTuple(Object[] var1);

    public Retrieval createRetrieval(boolean[] bindingPattern, View extensionView) {
        int[] bindingPositions = new int[bindingPattern.length];
        for (int index = 0; index < bindingPattern.length; ++index) {
            bindingPositions[index] = bindingPattern[index] ? index : -1;
        }
        return this.createRetrieval(bindingPositions, new Object[bindingPattern.length], new Object[bindingPattern.length], true, extensionView);
    }

    public abstract Retrieval createRetrieval(int[] var1, Object[] var2, Object[] var3, boolean var4, View var5);

    public abstract DependencySet getDependencySet(Object[] var1);

    public abstract boolean isCore(Object[] var1);

    public boolean propagateDeltaNew() {
        boolean deltaNewNotEmpty = this.m_afterExtensionThisTupleIndex != this.m_afterDeltaNewTupleIndex;
        this.m_afterExtensionOldTupleIndex = this.m_afterExtensionThisTupleIndex;
        this.m_afterExtensionThisTupleIndex = this.m_afterDeltaNewTupleIndex;
        this.m_afterDeltaNewTupleIndex = this.m_tupleTable.getFirstFreeTupleIndex();
        return deltaNewNotEmpty;
    }
    
    public boolean checkDeltaNewPropagation() {
        return this.m_afterExtensionThisTupleIndex != this.m_afterDeltaNewTupleIndex;
    }
    
    public void resetDeltaNew() {
    	this.m_afterExtensionOldTupleIndex = 0;
    	this.m_afterExtensionThisTupleIndex = 0;
    	this.m_afterDeltaNewTupleIndex = 0;
    }

    public void branchingPointPushed() {
        int start = this.m_tableau.getCurrentBranchingPoint().m_level * 3;
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
        this.m_indicesByBranchingPoint[start] = this.m_afterExtensionOldTupleIndex;
        this.m_indicesByBranchingPoint[start + 1] = this.m_afterExtensionThisTupleIndex;
        this.m_indicesByBranchingPoint[start + 2] = this.m_afterDeltaNewTupleIndex;
    }

    public void backtrack() {
        int start = this.m_tableau.getCurrentBranchingPoint().m_level * 3;
        int newAfterDeltaNewTupleIndex = this.m_indicesByBranchingPoint[start + 2];
        
        for (int tupleIndex = this.m_afterDeltaNewTupleIndex - 1; tupleIndex >= newAfterDeltaNewTupleIndex; --tupleIndex) {
            this.removeTuple(tupleIndex);
            this.m_dependencySetManager.forgetDependencySet(tupleIndex);
            this.m_tupleTable.nullifyTuple(tupleIndex);
        }
        
        this.m_tupleTable.truncate(newAfterDeltaNewTupleIndex);
        
        this.m_afterExtensionOldTupleIndex = this.m_indicesByBranchingPoint[start];
        this.m_afterExtensionThisTupleIndex = this.m_indicesByBranchingPoint[start + 1];
        this.m_afterDeltaNewTupleIndex = newAfterDeltaNewTupleIndex;
    }

    protected abstract void removeTuple(int var1);

    protected void postRemove(Object[] tuple, int tupleIndex) {
        Object dlPredicateObject = tuple[0];
        if (dlPredicateObject instanceof Concept) {
            Node node = (Node)tuple[1];
            this.m_tableau.m_existentialExpansionStrategy.assertionRemoved((Concept)dlPredicateObject, node, this.m_coreManager.isCore(tupleIndex));
            if (dlPredicateObject instanceof AtomicConcept) {
                --node.m_numberOfPositiveAtomicConcepts;
            } else if (dlPredicateObject instanceof ExistentialConcept) {
                node.removeFromUnprocessedExistentials((ExistentialConcept)dlPredicateObject);
            } else if (dlPredicateObject instanceof AtomicNegationConcept) {
                --node.m_numberOfNegatedAtomicConcepts;
            }
        } else if (dlPredicateObject instanceof AtomicRole) {
            this.m_tableau.m_existentialExpansionStrategy.assertionRemoved((AtomicRole)dlPredicateObject, (Node)tuple[1], (Node)tuple[2], this.m_coreManager.isCore(tupleIndex));
        } else if (dlPredicateObject instanceof NegatedAtomicRole) {
            --((Node)tuple[1]).m_numberOfNegatedRoleAssertions;
        } else if (dlPredicateObject instanceof DescriptionGraph) {
            this.m_tableau.m_descriptionGraphManager.descriptionGraphTupleRemoved(tupleIndex, tuple);
        }
        
        if (tuple.length > 2) {
        	Node node0toDelete = null;
            Node node1toDelete = null;
            int j = -1;
            boolean flag = false;
            if (tuple[0].toString().equals("!=")) {
            	Node node0 = (Node) tuple[1];
            	Node node1 = (Node) tuple[2];
            	for (Integer node0iter : this.m_tableau.m_metamodellingManager.differentIndividualsMap.keySet()) {
            		if (node0iter == node0.m_nodeID) {
            			j = 0;
            			for (Integer node1iter : this.m_tableau.m_metamodellingManager.differentIndividualsMap.get(node0.m_nodeID)) {
            				if (node1iter == node1.m_nodeID) {
            					node0toDelete = node0;
            					node1toDelete = node1;
            					flag = true;
            					break;
            				}
            				j++;
            			}
            		}
            		if (flag) {
            			break;
            		}
            	}
            	if (node0toDelete != null && node1toDelete != null) {
                    this.m_tableau.m_metamodellingManager.differentIndividualsMap.get(node0toDelete.m_nodeID).remove(j);
                }
            } else {
            	Node node0 = (Node) tuple[1];
            	Node node1 = (Node) tuple[2];
        		String propertyToDelete = null;
            	for (Integer node0iter : this.m_tableau.m_metamodellingManager.nodeProperties.keySet()) {
            		if (node0iter == node0.m_nodeID) {
            			for (Integer node1iter : this.m_tableau.m_metamodellingManager.nodeProperties.get(node0.m_nodeID).keySet()) {
            				if (node1iter == node1.m_nodeID) {
            					for (String property :  this.m_tableau.m_metamodellingManager.nodeProperties.get(node0.m_nodeID).get(node1.m_nodeID)) {
            						if (property.equals(tuple[0].toString())) {
            							node0toDelete = node0;
                    					node1toDelete = node1;
                    					propertyToDelete = property;
                    					flag = true;
                    					break;
            						}
            					}
            				}
            			}
            		}
            		if (flag) {
            			break;
            		}
            	}
            	if (node0toDelete != null && node1toDelete != null) {
                    this.m_tableau.m_metamodellingManager.nodeProperties.get(node0toDelete.m_nodeID).get(node1toDelete.m_nodeID).remove(propertyToDelete);
                }
            }
        } else if (tuple.length == 2) {
        	this.m_tableau.m_metamodellingManager.defAssertions.remove(tuple[0].toString());
        }
        
        
        if (this.m_tableauMonitor != null) {
            this.m_tableauMonitor.tupleRemoved(tuple);
        }
    }

    public void clear() {
        this.m_tupleTable.clear();
        this.m_afterExtensionOldTupleIndex = 0;
        this.m_afterExtensionThisTupleIndex = 0;
        this.m_afterDeltaNewTupleIndex = 0;
    }

    public boolean isTupleActive(Object[] tuple) {
        for (int objectIndex = this.m_tupleArity - 1; objectIndex > 0; --objectIndex) {
            if (((Node)tuple[objectIndex]).isActive()) continue;
            return false;
        }
        return true;
    }

    public boolean isTupleActive(int tupleIndex) {
        for (int objectIndex = this.m_tupleArity - 1; objectIndex > 0; --objectIndex) {
            if (((Node)this.m_tupleTable.getTupleObject(tupleIndex, objectIndex)).isActive()) continue;
            return false;
        }
        return true;
    }

    protected static class RealCoreManager
    implements CoreManager,
    Serializable {
        private static final long serialVersionUID = 3276377301185845284L;
        protected int[] m_bits = new int[16];

        @Override
        public boolean isCore(int tupleIndex) {
            int frameIndex = tupleIndex / 32;
            int mask = 1 << tupleIndex % 32;
            return (this.m_bits[frameIndex] & mask) != 0;
        }

        @Override
        public void addCore(int tupleIndex) {
            int frameIndex = tupleIndex / 32;
            int mask = 1 << tupleIndex % 32;
            int[] arrn = this.m_bits;
            int n = frameIndex;
            arrn[n] = arrn[n] | mask;
        }

        @Override
        public void setCore(int tupleIndex, boolean isCore) {
            int frameIndex = tupleIndex / 32;
            int mask = 1 << tupleIndex % 32;
            if (frameIndex >= this.m_bits.length) {
                int newSize = 3 * this.m_bits.length / 2;
                while (frameIndex >= newSize) {
                    newSize = 3 * newSize / 2;
                }
                int[] newBits = new int[newSize];
                System.arraycopy(this.m_bits, 0, newBits, 0, this.m_bits.length);
                this.m_bits = newBits;
            }
            if (isCore) {
                int[] arrn = this.m_bits;
                int n = frameIndex;
                arrn[n] = arrn[n] | mask;
            } else {
                int[] arrn = this.m_bits;
                int n = frameIndex;
                arrn[n] = arrn[n] & ~ mask;
            }
        }
    }

    protected static class NoCoreManager
    implements CoreManager,
    Serializable {
        private static final long serialVersionUID = 3252994135060928432L;

        protected NoCoreManager() {
        }

        @Override
        public boolean isCore(int tupleIndex) {
            return true;
        }

        @Override
        public void addCore(int tupleIndex) {
        }

        @Override
        public void setCore(int tupleIndex, boolean isCore) {
        }
    }

    protected interface CoreManager {
        boolean isCore(int var1);

        void addCore(int var1);

        void setCore(int var1, boolean var2);
    }

    protected class LastObjectDependencySetManager
    implements DependencySetManager,
    Serializable {
        private static final long serialVersionUID = -8097612469749016470L;
        protected final DependencySetFactory m_dependencySetFactory;

        public LastObjectDependencySetManager(ExtensionTable extensionTable) {
            this.m_dependencySetFactory = extensionTable.m_tableau.getDependencySetFactory();
        }

        @Override
        public DependencySet getDependencySet(int tupleIndex) {
            return (DependencySet)ExtensionTable.this.m_tupleTable.getTupleObject(tupleIndex, ExtensionTable.this.m_tupleArity);
        }

        @Override
        public void setDependencySet(int tupleIndex, DependencySet dependencySet) {
            PermanentDependencySet permanentDependencySet = this.m_dependencySetFactory.getPermanent(dependencySet);
            ExtensionTable.this.m_tupleTable.setTupleObject(tupleIndex, ExtensionTable.this.m_tupleArity, permanentDependencySet);
            this.m_dependencySetFactory.addUsage(permanentDependencySet);
        }

        @Override
        public void forgetDependencySet(int tupleIndex) {
            PermanentDependencySet permanentDependencySet = (PermanentDependencySet)ExtensionTable.this.m_tupleTable.getTupleObject(tupleIndex, ExtensionTable.this.m_tupleArity);
            this.m_dependencySetFactory.removeUsage(permanentDependencySet);
        }
    }

    protected static class DeterministicDependencySetManager
    implements DependencySetManager,
    Serializable {
        private static final long serialVersionUID = 7982627098607954806L;
        protected final DependencySetFactory m_dependencySetFactory;

        public DeterministicDependencySetManager(ExtensionTable extensionTable) {
            this.m_dependencySetFactory = extensionTable.m_tableau.getDependencySetFactory();
        }

        @Override
        public DependencySet getDependencySet(int tupleIndex) {
            return this.m_dependencySetFactory.emptySet();
        }

        @Override
        public void setDependencySet(int tupleIndex, DependencySet dependencySet) {
        }

        @Override
        public void forgetDependencySet(int tupleIndex) {
        }
    }

    protected interface DependencySetManager {
        DependencySet getDependencySet(int var1);

        void setDependencySet(int var1, DependencySet var2);

        void forgetDependencySet(int var1);
    }

    protected class UnindexedRetrieval
    implements Retrieval,
    Serializable {
        private static final long serialVersionUID = 6395072458663267969L;
        protected final View m_extensionView;
        protected final int[] m_bindingPositions;
        protected final Object[] m_bindingsBuffer;
        protected final Object[] m_tupleBuffer;
        protected final boolean m_ownsBuffers;
        protected final boolean m_checkTupleSelection;
        protected int m_currentTupleIndex;
        private int m_afterLastTupleIndex;

        public UnindexedRetrieval(int[] bindingPositions, Object[] bindingsBuffer, Object[] tupleBuffer, boolean ownsBuffers, View extensionView) {
            this.m_bindingPositions = bindingPositions;
            this.m_extensionView = extensionView;
            this.m_bindingsBuffer = bindingsBuffer;
            this.m_tupleBuffer = tupleBuffer;
            this.m_ownsBuffers = ownsBuffers;
            int numberOfBoundPositions = 0;
            for (int index = this.m_bindingPositions.length - 1; index >= 0; --index) {
                if (this.m_bindingPositions[index] == -1) continue;
                ++numberOfBoundPositions;
            }
            this.m_checkTupleSelection = numberOfBoundPositions > 0;
        }

        @Override
        public ExtensionTable getExtensionTable() {
            return ExtensionTable.this;
        }

        @Override
        public View getExtensionView() {
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
            return ExtensionTable.this.m_dependencySetManager.getDependencySet(this.m_currentTupleIndex);
        }

        @Override
        public boolean isCore() {
            return ExtensionTable.this.m_coreManager.isCore(this.m_currentTupleIndex);
        }

        @Override
        public void open() {
            switch (this.m_extensionView) {
                case EXTENSION_THIS: {
                    this.m_currentTupleIndex = 0;
                    this.m_afterLastTupleIndex = ExtensionTable.this.m_afterExtensionThisTupleIndex;
                    break;
                }
                case EXTENSION_OLD: {
                    this.m_currentTupleIndex = 0;
                    this.m_afterLastTupleIndex = ExtensionTable.this.m_afterExtensionOldTupleIndex;
                    break;
                }
                case DELTA_OLD: {
                    this.m_currentTupleIndex = ExtensionTable.this.m_afterExtensionOldTupleIndex;
                    this.m_afterLastTupleIndex = ExtensionTable.this.m_afterExtensionThisTupleIndex;
                    break;
                }
                case TOTAL: {
                    this.m_currentTupleIndex = 0;
                    this.m_afterLastTupleIndex = ExtensionTable.this.m_afterDeltaNewTupleIndex;
                    if (this.m_afterLastTupleIndex > 1000) {
                        System.out.println("UnindexedRetrieval.open: TOTAL view with large afterLastTupleIndex=" + this.m_afterLastTupleIndex);
                    }
                    break;
                }
            }
            while (this.m_currentTupleIndex < this.m_afterLastTupleIndex) {
                try {
                    ExtensionTable.this.m_tupleTable.retrieveTuple(this.m_tupleBuffer, this.m_currentTupleIndex);
                    if (this.isTupleActive()) {
                        return;
                    }
                } catch (Exception e) {
                    System.out.println("UnindexedRetrieval.open: ERROR at index " + this.m_currentTupleIndex + 
                                      " (afterLast=" + this.m_afterLastTupleIndex + "): " + e.getMessage());
                    throw e;
                }
                ++this.m_currentTupleIndex;
            }
        }

        @Override
        public boolean afterLast() {
            return this.m_currentTupleIndex >= this.m_afterLastTupleIndex;
        }

        @Override
        public int getCurrentTupleIndex() {
            return this.m_currentTupleIndex;
        }

        @Override
        public void next() {
            if (this.m_currentTupleIndex < this.m_afterLastTupleIndex) {
                ++this.m_currentTupleIndex;
                while (this.m_currentTupleIndex < this.m_afterLastTupleIndex) {
                    ExtensionTable.this.m_tupleTable.retrieveTuple(this.m_tupleBuffer, this.m_currentTupleIndex);
                    if (this.isTupleActive()) {
                        return;
                    }
                    ++this.m_currentTupleIndex;
                }
            }
        }

        protected boolean isTupleActive() {
            if (!ExtensionTable.this.isTupleActive(this.m_tupleBuffer)) {
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

    public interface Retrieval {
        ExtensionTable getExtensionTable();

        View getExtensionView();

        void clear();

        int[] getBindingPositions();

        Object[] getBindingsBuffer();

        Object[] getTupleBuffer();

        DependencySet getDependencySet();

        boolean isCore();

        void open();

        boolean afterLast();

        int getCurrentTupleIndex();

        void next();
    }

    public enum View {
        EXTENSION_THIS,
        EXTENSION_OLD,
        DELTA_OLD,
        TOTAL

    }

}

