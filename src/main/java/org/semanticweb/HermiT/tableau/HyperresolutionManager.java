package org.semanticweb.HermiT.tableau;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.semanticweb.HermiT.model.Atom;
import org.semanticweb.HermiT.model.AtomicConcept;
import org.semanticweb.HermiT.model.AtomicRole;
import org.semanticweb.HermiT.model.DLClause;
import org.semanticweb.HermiT.model.DLPredicate;
import org.semanticweb.HermiT.model.NodeIDLessEqualThan;
import org.semanticweb.HermiT.model.NodeIDsAscendingOrEqual;
import org.semanticweb.HermiT.model.Term;
import org.semanticweb.HermiT.model.Variable;

public final class HyperresolutionManager
implements Serializable {
    private static final long serialVersionUID = -4880817508962130189L;
    private final ExtensionManager m_extensionManager;
    private final ExtensionTable.Retrieval[] m_deltaOldRetrievals;
    private final ExtensionTable.Retrieval m_binaryTableRetrieval;
    final Map<DLPredicate, CompiledDLClauseInfo> m_tupleConsumersByDeltaPredicate;
    private final Map<AtomicRole, CompiledDLClauseInfo> m_atomicRoleTupleConsumersUnguarded;
    private final Map<AtomicRole, Map<AtomicConcept, CompiledDLClauseInfo>> m_atomicRoleTupleConsumersByGuardConcept1;
    private final Map<AtomicRole, Map<AtomicConcept, CompiledDLClauseInfo>> m_atomicRoleTupleConsumersByGuardConcept2;
    private final Object[][] m_buffersToClear;
    private final UnionDependencySet[] m_unionDependencySetsToClear;
    private final Object[] m_valuesBuffer;
    private final int m_maxNumberOfVariables;

    public HyperresolutionManager(Tableau tableau, Set<DLClause> dlClauses) {
        InterruptFlag interruptFlag = tableau.m_interruptFlag;
        this.m_extensionManager = tableau.m_extensionManager;
        this.m_tupleConsumersByDeltaPredicate = new HashMap<DLPredicate, CompiledDLClauseInfo>();
        this.m_atomicRoleTupleConsumersUnguarded = new HashMap<AtomicRole, CompiledDLClauseInfo>();
        this.m_atomicRoleTupleConsumersByGuardConcept1 = new HashMap<AtomicRole, Map<AtomicConcept, CompiledDLClauseInfo>>();
        this.m_atomicRoleTupleConsumersByGuardConcept2 = new HashMap<AtomicRole, Map<AtomicConcept, CompiledDLClauseInfo>>();
        HashMap<DLClauseBodyKey, ArrayList<DLClause>> dlClausesByBody = new HashMap<DLClauseBodyKey, ArrayList<DLClause>>();
        for (DLClause dlClause : dlClauses) {
            DLClauseBodyKey key = new DLClauseBodyKey(dlClause);
            ArrayList<DLClause> dlClausesForKey = dlClausesByBody.get(key);
            if (dlClausesForKey == null) {
                dlClausesForKey = new ArrayList<DLClause>();
                dlClausesByBody.put(key, dlClausesForKey);
            }
            dlClausesForKey.add(dlClause);
            interruptFlag.checkInterrupt();
        }
        HashMap<Integer, ExtensionTable.Retrieval> retrievalsByArity = new HashMap<Integer, ExtensionTable.Retrieval>();
        DLClauseEvaluator.BufferSupply bufferSupply = new DLClauseEvaluator.BufferSupply();
        Map<Term, Node> noTermsToNodes = Collections.emptyMap();
        DLClauseEvaluator.ValuesBufferManager valuesBufferManager = new DLClauseEvaluator.ValuesBufferManager(dlClauses, noTermsToNodes);
        DLClauseEvaluator.GroundDisjunctionHeaderManager groundDisjunctionHeaderManager = new DLClauseEvaluator.GroundDisjunctionHeaderManager();
        HashMap<Integer, UnionDependencySet> unionDependencySetsBySize = new HashMap<Integer, UnionDependencySet>();
        ArrayList<Atom> guardingAtomicConceptAtoms1 = new ArrayList<Atom>();
        ArrayList<Atom> guardingAtomicConceptAtoms2 = new ArrayList<Atom>();
        for (Map.Entry entry : dlClausesByBody.entrySet()) {
            DLClause bodyDLClause = ((DLClauseBodyKey)entry.getKey()).m_dlClause;
            BodyAtomsSwapper bodyAtomsSwapper = new BodyAtomsSwapper(bodyDLClause);
            for (int bodyAtomIndex = 0; bodyAtomIndex < bodyDLClause.getBodyLength(); ++bodyAtomIndex) {
                if (!HyperresolutionManager.isPredicateWithExtension(bodyDLClause.getBodyAtom(bodyAtomIndex).getDLPredicate())) continue;
                DLClause swappedDLClause = bodyAtomsSwapper.getSwappedDLClause(bodyAtomIndex);
                Atom deltaAtom = swappedDLClause.getBodyAtom(0);
                DLPredicate deltaDLPredicate = deltaAtom.getDLPredicate();
                Integer arity = deltaDLPredicate.getArity() + 1;
                ExtensionTable.Retrieval firstTableRetrieval = retrievalsByArity.get(arity);
                if (firstTableRetrieval == null) {
                    ExtensionTable extensionTable = this.m_extensionManager.getExtensionTable(arity);
                    firstTableRetrieval = extensionTable.createRetrieval(new boolean[extensionTable.getArity()], ExtensionTable.View.DELTA_OLD);
                    retrievalsByArity.put(arity, firstTableRetrieval);
                }
                DLClauseEvaluator evaluator = new DLClauseEvaluator(tableau, swappedDLClause, (List)entry.getValue(), firstTableRetrieval, bufferSupply, valuesBufferManager, groundDisjunctionHeaderManager, unionDependencySetsBySize);
                CompiledDLClauseInfo normalTupleConsumer = new CompiledDLClauseInfo(evaluator, this.m_tupleConsumersByDeltaPredicate.get(deltaDLPredicate));
                this.m_tupleConsumersByDeltaPredicate.put(deltaDLPredicate, normalTupleConsumer);
                if (deltaDLPredicate instanceof AtomicRole && deltaAtom.getArgument(0) instanceof Variable && deltaAtom.getArgument(1) instanceof Variable) {
                    Map<AtomicConcept, CompiledDLClauseInfo> compiledDLClauseInfos;
                    AtomicConcept atomicConcept;
                    CompiledDLClauseInfo optimizedTupleConsumer;
                    AtomicRole deltaAtomicRole = (AtomicRole)deltaDLPredicate;
                    HyperresolutionManager.getAtomicRoleClauseGuards(swappedDLClause, guardingAtomicConceptAtoms1, guardingAtomicConceptAtoms2);
                    if (!guardingAtomicConceptAtoms1.isEmpty()) {
                        compiledDLClauseInfos = this.m_atomicRoleTupleConsumersByGuardConcept1.get(deltaAtomicRole);
                        if (compiledDLClauseInfos == null) {
                            compiledDLClauseInfos = new HashMap<AtomicConcept, CompiledDLClauseInfo>();
                            this.m_atomicRoleTupleConsumersByGuardConcept1.put(deltaAtomicRole, compiledDLClauseInfos);
                        }
                        for (Atom guardingAtom : guardingAtomicConceptAtoms1) {
                            atomicConcept = (AtomicConcept)guardingAtom.getDLPredicate();
                            optimizedTupleConsumer = new CompiledDLClauseInfo(evaluator, compiledDLClauseInfos.get(atomicConcept));
                            compiledDLClauseInfos.put(atomicConcept, optimizedTupleConsumer);
                        }
                    }
                    if (!guardingAtomicConceptAtoms2.isEmpty()) {
                        compiledDLClauseInfos = this.m_atomicRoleTupleConsumersByGuardConcept2.get(deltaAtomicRole);
                        if (compiledDLClauseInfos == null) {
                            compiledDLClauseInfos = new HashMap<AtomicConcept, CompiledDLClauseInfo>();
                            this.m_atomicRoleTupleConsumersByGuardConcept2.put(deltaAtomicRole, compiledDLClauseInfos);
                        }
                        for (Atom guardingAtom : guardingAtomicConceptAtoms2) {
                            atomicConcept = (AtomicConcept)guardingAtom.getDLPredicate();
                            optimizedTupleConsumer = new CompiledDLClauseInfo(evaluator, compiledDLClauseInfos.get(atomicConcept));
                            compiledDLClauseInfos.put(atomicConcept, optimizedTupleConsumer);
                        }
                    }
                    if (guardingAtomicConceptAtoms1.isEmpty() && guardingAtomicConceptAtoms2.isEmpty()) {
                        CompiledDLClauseInfo unguardedTupleConsumer = new CompiledDLClauseInfo(evaluator, this.m_atomicRoleTupleConsumersUnguarded.get(deltaAtomicRole));
                        this.m_atomicRoleTupleConsumersUnguarded.put(deltaAtomicRole, unguardedTupleConsumer);
                    }
                }
                bufferSupply.reuseBuffers();
                interruptFlag.checkInterrupt();
            }
        }
        this.m_deltaOldRetrievals = new ExtensionTable.Retrieval[retrievalsByArity.size()];
        retrievalsByArity.values().toArray(this.m_deltaOldRetrievals);
        this.m_binaryTableRetrieval = this.m_extensionManager.getExtensionTable(2).createRetrieval(new boolean[]{false, true}, ExtensionTable.View.EXTENSION_THIS);
        this.m_buffersToClear = bufferSupply.getAllBuffers();
        this.m_unionDependencySetsToClear = new UnionDependencySet[unionDependencySetsBySize.size()];
        unionDependencySetsBySize.values().toArray(this.m_unionDependencySetsToClear);
        this.m_valuesBuffer = valuesBufferManager.m_valuesBuffer;
        this.m_maxNumberOfVariables = valuesBufferManager.m_maxNumberOfVariables;
    }

    private static void getAtomicRoleClauseGuards(DLClause swappedDLClause, List<Atom> guardingAtomicConceptAtoms1, List<Atom> guardingAtomicConceptAtoms2) {
        guardingAtomicConceptAtoms1.clear();
        guardingAtomicConceptAtoms2.clear();
        Atom deltaOldAtom = swappedDLClause.getBodyAtom(0);
        Variable X = deltaOldAtom.getArgumentVariable(0);
        Variable Y = deltaOldAtom.getArgumentVariable(1);
        for (int bodyIndex = 1; bodyIndex < swappedDLClause.getBodyLength(); ++bodyIndex) {
            Variable variable;
            Atom atom = swappedDLClause.getBodyAtom(bodyIndex);
            if (atom.getDLPredicate() instanceof AtomicConcept && (variable = atom.getArgumentVariable(0)) != null) {
                if (X.equals(variable)) {
                    guardingAtomicConceptAtoms1.add(atom);
                }
                if (Y.equals(variable)) {
                    guardingAtomicConceptAtoms2.add(atom);
                }
            }
            ++bodyIndex;
        }
    }

    private static boolean isPredicateWithExtension(DLPredicate dlPredicate) {
        return !NodeIDLessEqualThan.INSTANCE.equals(dlPredicate) && !(dlPredicate instanceof NodeIDsAscendingOrEqual);
    }

    public void clear() {
        for (int retrievalIndex = this.m_deltaOldRetrievals.length - 1; retrievalIndex >= 0; --retrievalIndex) {
            this.m_deltaOldRetrievals[retrievalIndex].clear();
        }
        this.m_binaryTableRetrieval.clear();
        for (int bufferIndex = this.m_buffersToClear.length - 1; bufferIndex >= 0; --bufferIndex) {
            Object[] buffer = this.m_buffersToClear[bufferIndex];
            for (int index = buffer.length - 1; index >= 0; --index) {
                buffer[index] = null;
            }
        }
        for (int unionDependencySetIndex = this.m_unionDependencySetsToClear.length - 1; unionDependencySetIndex >= 0; --unionDependencySetIndex) {
            DependencySet[] dependencySets = this.m_unionDependencySetsToClear[unionDependencySetIndex].m_dependencySets;
            for (int dependencySetIndex = dependencySets.length - 1; dependencySetIndex >= 0; --dependencySetIndex) {
                dependencySets[dependencySetIndex] = null;
            }
        }
        for (int variableIndex = 0; variableIndex < this.m_maxNumberOfVariables; ++variableIndex) {
            this.m_valuesBuffer[variableIndex] = null;
        }
    }

    public void applyDLClauses() {
        for (int index = 0; index < this.m_deltaOldRetrievals.length; ++index) {
            ExtensionTable.Retrieval deltaOldRetrieval = this.m_deltaOldRetrievals[index];
            deltaOldRetrieval.open();
            Object[] deltaOldTupleBuffer = deltaOldRetrieval.getTupleBuffer();
            while (!deltaOldRetrieval.afterLast() && !this.m_extensionManager.containsClash()) {
                CompiledDLClauseInfo unguardedCompiledDLClauseInfo;
                Object deltaOldPredicate = deltaOldTupleBuffer[0];
                CompiledDLClauseInfo unoptimizedCompiledDLClauseInfo = this.m_tupleConsumersByDeltaPredicate.get(deltaOldPredicate);
                boolean applyUnoptimized = true;
                if (unoptimizedCompiledDLClauseInfo != null && deltaOldTupleBuffer[0] instanceof AtomicRole && unoptimizedCompiledDLClauseInfo.m_indexInList > ((Node)deltaOldTupleBuffer[1]).getNumberOfPositiveAtomicConcepts() + ((Node)deltaOldTupleBuffer[2]).getNumberOfPositiveAtomicConcepts() + ((unguardedCompiledDLClauseInfo = this.m_atomicRoleTupleConsumersUnguarded.get(deltaOldPredicate)) == null ? 0 : unguardedCompiledDLClauseInfo.m_indexInList)) {
                    CompiledDLClauseInfo optimizedCompiledDLClauseInfo;
                    Object[] binaryTableTupleBuffer;
                    Object atomicConceptObject;
                    Map<AtomicConcept, CompiledDLClauseInfo> compiledDLClauseInfos;
                    applyUnoptimized = false;
                    while (unguardedCompiledDLClauseInfo != null && !this.m_extensionManager.containsClash()) {
                        unguardedCompiledDLClauseInfo.m_evaluator.evaluate();
                        unguardedCompiledDLClauseInfo = unguardedCompiledDLClauseInfo.m_next;
                    }
                    if (!this.m_extensionManager.containsClash() && (compiledDLClauseInfos = this.m_atomicRoleTupleConsumersByGuardConcept1.get(deltaOldPredicate)) != null) {
                        this.m_binaryTableRetrieval.getBindingsBuffer()[1] = deltaOldTupleBuffer[1];
                        this.m_binaryTableRetrieval.open();
                        binaryTableTupleBuffer = this.m_binaryTableRetrieval.getTupleBuffer();
                        while (!this.m_binaryTableRetrieval.afterLast() && !this.m_extensionManager.containsClash()) {
                            atomicConceptObject = binaryTableTupleBuffer[0];
                            if (atomicConceptObject instanceof AtomicConcept) {
                                optimizedCompiledDLClauseInfo = compiledDLClauseInfos.get(atomicConceptObject);
                                while (optimizedCompiledDLClauseInfo != null && !this.m_extensionManager.containsClash()) {
                                    optimizedCompiledDLClauseInfo.m_evaluator.evaluate();
                                    optimizedCompiledDLClauseInfo = optimizedCompiledDLClauseInfo.m_next;
                                }
                            }
                            this.m_binaryTableRetrieval.next();
                        }
                    }
                    if (!this.m_extensionManager.containsClash() && (compiledDLClauseInfos = this.m_atomicRoleTupleConsumersByGuardConcept2.get(deltaOldPredicate)) != null) {
                        this.m_binaryTableRetrieval.getBindingsBuffer()[1] = deltaOldTupleBuffer[2];
                        this.m_binaryTableRetrieval.open();
                        binaryTableTupleBuffer = this.m_binaryTableRetrieval.getTupleBuffer();
                        while (!this.m_binaryTableRetrieval.afterLast() && !this.m_extensionManager.containsClash()) {
                            atomicConceptObject = binaryTableTupleBuffer[0];
                            if (atomicConceptObject instanceof AtomicConcept) {
                                optimizedCompiledDLClauseInfo = compiledDLClauseInfos.get(atomicConceptObject);
                                while (optimizedCompiledDLClauseInfo != null && !this.m_extensionManager.containsClash()) {
                                    optimizedCompiledDLClauseInfo.m_evaluator.evaluate();
                                    optimizedCompiledDLClauseInfo = optimizedCompiledDLClauseInfo.m_next;
                                }
                            }
                            this.m_binaryTableRetrieval.next();
                        }
                    }
                }
                if (applyUnoptimized) {
                    while (unoptimizedCompiledDLClauseInfo != null && !this.m_extensionManager.containsClash()) {
                        unoptimizedCompiledDLClauseInfo.m_evaluator.evaluate();
                        unoptimizedCompiledDLClauseInfo = unoptimizedCompiledDLClauseInfo.m_next;
                    }
                }
                deltaOldRetrieval.next();
            }
        }
    }

    protected static final class DLClauseBodyKey {
        private final DLClause m_dlClause;
        private final int m_hashCode;

        public DLClauseBodyKey(DLClause dlClause) {
            this.m_dlClause = dlClause;
            int hashCode = 0;
            for (int atomIndex = 0; atomIndex < this.m_dlClause.getBodyLength(); ++atomIndex) {
                hashCode += this.m_dlClause.getBodyAtom(atomIndex).hashCode();
            }
            this.m_hashCode = hashCode;
        }

        public boolean equals(Object that) {
            if (that == null) {
                return false;
            }
            if (this == that) {
                return true;
            }
            if (!(that instanceof DLClauseBodyKey)) {
                return false;
            }
            DLClause thatDLClause = ((DLClauseBodyKey)that).m_dlClause;
            if (this.m_dlClause.getBodyLength() != thatDLClause.getBodyLength()) {
                return false;
            }
            for (int atomIndex = 0; atomIndex < this.m_dlClause.getBodyLength(); ++atomIndex) {
                if (this.m_dlClause.getBodyAtom(atomIndex).equals(thatDLClause.getBodyAtom(atomIndex))) continue;
                return false;
            }
            return true;
        }

        public int hashCode() {
            return this.m_hashCode;
        }
    }

    public static final class BodyAtomsSwapper {
        private final DLClause m_dlClause;
        private final List<Atom> m_nodeIDComparisonAtoms;
        private final boolean[] m_usedAtoms;
        private final List<Atom> m_reorderedAtoms;
        private final Set<Variable> m_boundVariables;

        public BodyAtomsSwapper(DLClause dlClause) {
            this.m_dlClause = dlClause;
            this.m_nodeIDComparisonAtoms = new ArrayList<Atom>(this.m_dlClause.getBodyLength());
            this.m_usedAtoms = new boolean[this.m_dlClause.getBodyLength()];
            this.m_reorderedAtoms = new ArrayList<Atom>(this.m_dlClause.getBodyLength());
            this.m_boundVariables = new HashSet<Variable>();
        }

        public DLClause getSwappedDLClause(int bodyIndex) {
            this.m_nodeIDComparisonAtoms.clear();
            for (int index = this.m_usedAtoms.length - 1; index >= 0; --index) {
                this.m_usedAtoms[index] = false;
                Atom atom = this.m_dlClause.getBodyAtom(index);
                if (!NodeIDLessEqualThan.INSTANCE.equals(atom.getDLPredicate())) continue;
                this.m_nodeIDComparisonAtoms.add(atom);
            }
            this.m_reorderedAtoms.clear();
            this.m_boundVariables.clear();
            Atom atom = this.m_dlClause.getBodyAtom(bodyIndex);
            atom.getVariables(this.m_boundVariables);
            this.m_reorderedAtoms.add(atom);
            this.m_usedAtoms[bodyIndex] = true;
            while (this.m_reorderedAtoms.size() != this.m_usedAtoms.length) {
                Atom bestAtom = null;
                int bestAtomIndex = -1;
                int bestAtomGoodness = -1000;
                for (int index = this.m_usedAtoms.length - 1; index >= 0; --index) {
                    int atomGoodness;
                    if (this.m_usedAtoms[index] || (atomGoodness = this.getAtomGoodness(atom = this.m_dlClause.getBodyAtom(index))) <= bestAtomGoodness) continue;
                    bestAtom = atom;
                    bestAtomGoodness = atomGoodness;
                    bestAtomIndex = index;
                }
                assert (bestAtom != null);
                this.m_reorderedAtoms.add(bestAtom);
                this.m_usedAtoms[bestAtomIndex] = true;
                bestAtom.getVariables(this.m_boundVariables);
                this.m_nodeIDComparisonAtoms.remove(bestAtom);
            }
            Atom[] bodyAtoms = new Atom[this.m_reorderedAtoms.size()];
            this.m_reorderedAtoms.toArray(bodyAtoms);
            return this.m_dlClause.getChangedDLClause(null, bodyAtoms);
        }

        private int getAtomGoodness(Atom atom) {
            if (NodeIDLessEqualThan.INSTANCE.equals(atom.getDLPredicate())) {
                if (this.m_boundVariables.contains(atom.getArgumentVariable(0)) && this.m_boundVariables.contains(atom.getArgumentVariable(1))) {
                    return 1000;
                }
                return -2000;
            }
            if (atom.getDLPredicate() instanceof NodeIDsAscendingOrEqual) {
                int numberOfUnboundVariables = 0;
                for (int argumentIndex = atom.getArity() - 1; argumentIndex >= 0; --argumentIndex) {
                    Term argument = atom.getArgument(argumentIndex);
                    if (!(argument instanceof Variable) || this.m_boundVariables.contains(argument)) continue;
                    ++numberOfUnboundVariables;
                }
                if (numberOfUnboundVariables > 0) {
                    return -5000;
                }
                return 5000;
            }
            int numberOfBoundVariables = 0;
            int numberOfUnboundVariables = 0;
            for (int argumentIndex = atom.getArity() - 1; argumentIndex >= 0; --argumentIndex) {
                Term argument = atom.getArgument(argumentIndex);
                if (!(argument instanceof Variable)) continue;
                if (this.m_boundVariables.contains(argument)) {
                    ++numberOfBoundVariables;
                    continue;
                }
                ++numberOfUnboundVariables;
            }
            int goodness = numberOfBoundVariables * 100 - numberOfUnboundVariables * 10;
            if (atom.getDLPredicate().getArity() == 2 && numberOfUnboundVariables == 1 && !this.m_nodeIDComparisonAtoms.isEmpty()) {
                Variable unboundVariable = atom.getArgumentVariable(0);
                if (this.m_boundVariables.contains(unboundVariable)) {
                    unboundVariable = atom.getArgumentVariable(1);
                }
                for (int compareAtomIndex = this.m_nodeIDComparisonAtoms.size() - 1; compareAtomIndex >= 0; --compareAtomIndex) {
                    Atom compareAtom = this.m_nodeIDComparisonAtoms.get(compareAtomIndex);
                    Variable argument0 = compareAtom.getArgumentVariable(0);
                    Variable argument1 = compareAtom.getArgumentVariable(1);
                    if (!this.m_boundVariables.contains(argument0) && !unboundVariable.equals(argument0) || !this.m_boundVariables.contains(argument1) && !unboundVariable.equals(argument1)) continue;
                    goodness += 5;
                    break;
                }
            }
            return goodness;
        }
    }

    protected static final class CompiledDLClauseInfo {
        private final DLClauseEvaluator m_evaluator;
        private final CompiledDLClauseInfo m_next;
        private final int m_indexInList;

        public CompiledDLClauseInfo(DLClauseEvaluator evaluator, CompiledDLClauseInfo next) {
            this.m_evaluator = evaluator;
            this.m_next = next;
            this.m_indexInList = this.m_next == null ? 1 : this.m_next.m_indexInList + 1;
        }
    }

}

