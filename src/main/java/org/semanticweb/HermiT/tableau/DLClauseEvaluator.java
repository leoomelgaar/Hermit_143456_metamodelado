package org.semanticweb.HermiT.tableau;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.semanticweb.HermiT.existentials.ExistentialExpansionStrategy;
import org.semanticweb.HermiT.model.Atom;
import org.semanticweb.HermiT.model.DLClause;
import org.semanticweb.HermiT.model.DLPredicate;
import org.semanticweb.HermiT.model.NodeIDLessEqualThan;
import org.semanticweb.HermiT.model.NodeIDsAscendingOrEqual;
import org.semanticweb.HermiT.model.Term;
import org.semanticweb.HermiT.model.Variable;
import org.semanticweb.HermiT.monitor.TableauMonitor;
public class DLClauseEvaluator
implements Serializable {
    private static final long serialVersionUID = 4639844159658590456L;
    protected static final String CRLF = System.getProperty("line.separator");
    protected final InterruptFlag m_interruptFlag;
    protected final ExtensionManager m_extensionManager;
    protected final ExtensionTable.Retrieval[] m_retrievals;
    protected final Worker[] m_workers;
    protected final DLClause m_bodyDLClause;
    protected final List<DLClause> m_headDLClauses;

    public DLClauseEvaluator(Tableau tableau, DLClause bodyDLClause, List<DLClause> headDLClauses, ExtensionTable.Retrieval firstAtomRetrieval, BufferSupply bufferSupply, ValuesBufferManager valuesBufferManager, GroundDisjunctionHeaderManager groundDisjunctionHeaderManager, Map<Integer, UnionDependencySet> unionDependencySetsBySize) {
        this.m_interruptFlag = tableau.m_interruptFlag;
        this.m_extensionManager = tableau.m_extensionManager;
        DLClauseCompiler compiler = new DLClauseCompiler(bufferSupply, valuesBufferManager, groundDisjunctionHeaderManager, unionDependencySetsBySize, this, this.m_extensionManager, tableau.getExistentialsExpansionStrategy(), bodyDLClause, headDLClauses, firstAtomRetrieval);
        this.m_retrievals = new ExtensionTable.Retrieval[compiler.m_retrievals.size()];
        compiler.m_retrievals.toArray(this.m_retrievals);
        this.m_workers = new Worker[compiler.m_workers.size()];
        compiler.m_workers.toArray(this.m_workers);
        this.m_bodyDLClause = bodyDLClause;
        this.m_headDLClauses = headDLClauses;
    }

    public int getBodyLength() {
        return this.m_bodyDLClause.getBodyLength();
    }

    public Atom getBodyAtom(int atomIndex) {
        return this.m_bodyDLClause.getBodyAtom(atomIndex);
    }

    public int getNumberOfDLClauses() {
        return this.m_headDLClauses.size();
    }

    public DLClause getDLClause(int dlClauseIndex) {
        return this.m_headDLClauses.get(dlClauseIndex);
    }

    public int getHeadLength(int dlClauseIndex) {
        return this.m_headDLClauses.get(dlClauseIndex).getHeadLength();
    }

    public Atom getHeadAtom(int dlClauseIndex, int atomIndex) {
        return this.m_headDLClauses.get(dlClauseIndex).getHeadAtom(atomIndex);
    }

    public Object[] getTupleMatchedToBody(int atomIndex) {
        return this.m_retrievals[atomIndex].getTupleBuffer();
    }

    public void evaluate() {
        int programCounter = 0;
        while (programCounter < this.m_workers.length && !this.m_extensionManager.containsClash()) {
            this.m_interruptFlag.checkInterrupt();
            programCounter = this.m_workers[programCounter].execute(programCounter);
        }
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        int maximalPCLength = String.valueOf(this.m_workers.length - 1).length();
        for (int programCounter = 0; programCounter < this.m_workers.length; ++programCounter) {
            String programCounterString = String.valueOf(programCounter);
            for (int count = maximalPCLength - programCounterString.length(); count > 0; --count) {
                buffer.append(' ');
            }
            buffer.append(programCounterString);
            buffer.append(": ");
            buffer.append(this.m_workers[programCounter].toString());
            buffer.append(CRLF);
        }
        return buffer.toString();
    }

    public static abstract class ConjunctionCompiler {
        protected final BufferSupply m_bufferSupply;
        protected final ValuesBufferManager m_valuesBufferManager;
        protected final ExtensionManager m_extensionManager;
        protected final Atom[] m_bodyAtoms;
        protected final List<Variable> m_variables;
        protected final Set<Variable> m_boundSoFar;
        protected final UnionDependencySet m_unionDependencySet;
        protected final List<ExtensionTable.Retrieval> m_retrievals;
        public final List<Worker> m_workers;
        protected final List<Integer> m_labels;

        public ConjunctionCompiler(BufferSupply bufferSupply, ValuesBufferManager valuesBufferManager, Map<Integer, UnionDependencySet> unionDependencySetsBySize, ExtensionManager extensionManager, Atom[] bodyAtoms, List<Variable> headVariables) {
            this.m_bufferSupply = bufferSupply;
            this.m_valuesBufferManager = valuesBufferManager;
            this.m_extensionManager = extensionManager;
            this.m_bodyAtoms = bodyAtoms;
            this.m_variables = new ArrayList<Variable>();
            this.m_boundSoFar = new HashSet<Variable>();
            int numberOfRealAtoms = 0;
            for (int bodyIndex = 0; bodyIndex < this.getBodyLength(); ++bodyIndex) {
                Atom atom = this.getBodyAtom(bodyIndex);
                for (int argumentIndex = 0; argumentIndex < atom.getArity(); ++argumentIndex) {
                    Variable variable = atom.getArgumentVariable(argumentIndex);
                    if (variable == null || this.m_variables.contains(variable) || !this.occursInBodyAtomsAfter(variable, bodyIndex + 1)) continue;
                    this.m_variables.add(variable);
                }
                if (atom.getDLPredicate().equals(NodeIDLessEqualThan.INSTANCE) || atom.getDLPredicate() instanceof NodeIDsAscendingOrEqual) continue;
                ++numberOfRealAtoms;
            }
            for (Variable variable : headVariables) {
                if (this.m_variables.contains(variable)) continue;
                this.m_variables.add(variable);
            }
            if (unionDependencySetsBySize != null) {
                Integer numberOfRealAtomsInteger = numberOfRealAtoms;
                UnionDependencySet unionDependencySet = unionDependencySetsBySize.get(numberOfRealAtomsInteger);
                if (unionDependencySet == null) {
                    unionDependencySet = new UnionDependencySet(numberOfRealAtoms);
                    unionDependencySetsBySize.put(numberOfRealAtomsInteger, unionDependencySet);
                }
                this.m_unionDependencySet = unionDependencySet;
            } else {
                this.m_unionDependencySet = null;
            }
            this.m_retrievals = new ArrayList<ExtensionTable.Retrieval>();
            this.m_workers = new ArrayList<Worker>();
            this.m_labels = new ArrayList<Integer>();
        }

        protected final void generateCode(int firstBodyAtomToCompile, ExtensionTable.Retrieval firstAtomRetrieval) {
            this.m_labels.add(null);
            this.m_retrievals.add(firstAtomRetrieval);
            int afterRule = this.addLabel();
            if (firstBodyAtomToCompile > 0) {
                this.compileCheckUnboundVariableMatches(this.getBodyAtom(0), firstAtomRetrieval, afterRule);
                this.compileGenerateBindings(firstAtomRetrieval, this.getBodyAtom(0));
                if (this.m_unionDependencySet != null) {
                    this.m_workers.add(new CopyDependencySet(firstAtomRetrieval, this.m_unionDependencySet.m_dependencySets, 0));
                }
            }
            this.compileBodyAtom(firstBodyAtomToCompile, afterRule);
            this.setLabelProgramCounter(afterRule);
            for (Worker worker : this.m_workers) {
                int branchingAddress;
                BranchingWorker branchingWorker;
                if (!(worker instanceof BranchingWorker) || (branchingAddress = (branchingWorker = (BranchingWorker)worker).getBranchingAddress()) >= 0) continue;
                int resolvedAddress = this.m_labels.get(- branchingAddress);
                branchingWorker.setBranchingAddress(resolvedAddress);
            }
        }

        protected final boolean occursInBodyAtomsAfter(Variable variable, int startIndex) {
            for (int argumentIndex = startIndex; argumentIndex < this.getBodyLength(); ++argumentIndex) {
                if (!this.getBodyAtom(argumentIndex).containsVariable(variable)) continue;
                return true;
            }
            return false;
        }

        protected final void compileBodyAtom(int bodyAtomIndex, int lastAtomNextElement) {
            if (bodyAtomIndex == this.getBodyLength()) {
                this.compileHeads();
            } else if (this.getBodyAtom(bodyAtomIndex).getDLPredicate().equals(NodeIDLessEqualThan.INSTANCE)) {
                Atom atom = this.getBodyAtom(bodyAtomIndex);
                int variable1Index = this.m_variables.indexOf(atom.getArgumentVariable(0));
                int variable2Index = this.m_variables.indexOf(atom.getArgumentVariable(1));
                assert (variable1Index != -1);
                assert (variable2Index != -1);
                this.m_workers.add(new BranchIfNotNodeIDLessEqualThan(lastAtomNextElement, this.m_valuesBufferManager.m_valuesBuffer, variable1Index, variable2Index));
                this.compileBodyAtom(bodyAtomIndex + 1, lastAtomNextElement);
            } else if (this.getBodyAtom(bodyAtomIndex).getDLPredicate() instanceof NodeIDsAscendingOrEqual) {
                Atom atom = this.getBodyAtom(bodyAtomIndex);
                int[] nodeIndexes = new int[atom.getArity()];
                for (int index = 0; index < atom.getArity(); ++index) {
                    nodeIndexes[index] = this.m_variables.indexOf(atom.getArgumentVariable(index));
                    assert (nodeIndexes[index] != -1);
                }
                this.m_workers.add(new BranchIfNotNodeIDsAscendingOrEqual(lastAtomNextElement, this.m_valuesBufferManager.m_valuesBuffer, nodeIndexes));
                this.compileBodyAtom(bodyAtomIndex + 1, lastAtomNextElement);
            } else {
                int afterLoop = this.addLabel();
                int nextElement = this.addLabel();
                Atom atom = this.getBodyAtom(bodyAtomIndex);
                int[] bindingPositions = new int[atom.getArity() + 1];
                bindingPositions[0] = this.m_valuesBufferManager.m_bodyDLPredicatesToIndexes.get(atom.getDLPredicate());
                for (int argumentIndex = 0; argumentIndex < atom.getArity(); ++argumentIndex) {
                    Term term = atom.getArgument(argumentIndex);
                    if (term instanceof Variable) {
                        if (this.m_boundSoFar.contains(term)) {
                            bindingPositions[argumentIndex + 1] = this.m_variables.indexOf(term);
                            continue;
                        }
                        bindingPositions[argumentIndex + 1] = -1;
                        continue;
                    }
                    bindingPositions[argumentIndex + 1] = this.m_valuesBufferManager.m_bodyNonvariableTermsToIndexes.get(term);
                }
                ExtensionTable.Retrieval retrieval = this.m_extensionManager.getExtensionTable(atom.getArity() + 1).createRetrieval(bindingPositions, this.m_valuesBufferManager.m_valuesBuffer, this.m_bufferSupply.getBuffer(atom.getArity() + 1), false, ExtensionTable.View.EXTENSION_THIS);
                this.m_retrievals.add(retrieval);
                this.m_workers.add(new OpenRetrieval(retrieval));
                int loopStart = this.m_workers.size();
                this.m_workers.add(new HasMoreRetrieval(afterLoop, retrieval));
                this.compileCheckUnboundVariableMatches(atom, retrieval, nextElement);
                this.compileGenerateBindings(retrieval, atom);
                if (this.m_unionDependencySet != null) {
                    this.m_workers.add(new CopyDependencySet(retrieval, this.m_unionDependencySet.m_dependencySets, this.m_retrievals.size() - 1));
                }
                this.compileBodyAtom(bodyAtomIndex + 1, nextElement);
                this.setLabelProgramCounter(nextElement);
                this.m_workers.add(new NextRetrieval(retrieval));
                this.m_workers.add(new JumpTo(loopStart));
                this.setLabelProgramCounter(afterLoop);
            }
        }

        protected final int getBodyLength() {
            return this.m_bodyAtoms.length;
        }

        protected final Atom getBodyAtom(int atomIndex) {
            return this.m_bodyAtoms[atomIndex];
        }

        protected final void compileCheckUnboundVariableMatches(Atom atom, ExtensionTable.Retrieval retrieval, int jumpIndex) {
            for (int outerArgumentIndex = 0; outerArgumentIndex < atom.getArity(); ++outerArgumentIndex) {
                Variable variable = atom.getArgumentVariable(outerArgumentIndex);
                if (variable == null || this.m_boundSoFar.contains(variable)) continue;
                for (int innerArgumentIndex = outerArgumentIndex + 1; innerArgumentIndex < atom.getArity(); ++innerArgumentIndex) {
                    if (!variable.equals(atom.getArgument(innerArgumentIndex))) continue;
                    this.m_workers.add(new BranchIfNotEqual(jumpIndex, retrieval.getTupleBuffer(), outerArgumentIndex + 1, innerArgumentIndex + 1));
                }
            }
        }

        protected final void compileGenerateBindings(ExtensionTable.Retrieval retrieval, Atom atom) {
            for (int argumentIndex = 0; argumentIndex < atom.getArity(); ++argumentIndex) {
                int variableIndex;
                Variable variable = atom.getArgumentVariable(argumentIndex);
                if (variable == null || this.m_boundSoFar.contains(variable) || (variableIndex = this.m_variables.indexOf(variable)) == -1) continue;
                this.m_workers.add(new CopyValues(retrieval.getTupleBuffer(), argumentIndex + 1, this.m_valuesBufferManager.m_valuesBuffer, variableIndex));
                this.m_boundSoFar.add(variable);
            }
        }

        protected final int addLabel() {
            int labelIndex = this.m_labels.size();
            this.m_labels.add(null);
            return - labelIndex;
        }

        protected final void setLabelProgramCounter(int labelID) {
            this.m_labels.set(- labelID, this.m_workers.size());
        }

        protected abstract void compileHeads();
    }

    protected static final class DLClauseCompiler
    extends ConjunctionCompiler {
        private final DLClauseEvaluator m_dlClauseEvalautor;
        private final GroundDisjunctionHeaderManager m_groundDisjunctionHeaderManager;
        private final ExistentialExpansionStrategy m_existentialExpansionStrategy;
        private final DLClause m_bodyDLClause;
        private final List<DLClause> m_headDLClauses;
        private final boolean[] m_coreVariables;

        public DLClauseCompiler(BufferSupply bufferSupply, ValuesBufferManager valuesBufferManager, GroundDisjunctionHeaderManager groundDisjunctionHeaderManager, Map<Integer, UnionDependencySet> unionDependencySetsBySize, DLClauseEvaluator dlClauseEvalautor, ExtensionManager extensionManager, ExistentialExpansionStrategy existentialExpansionStrategy, DLClause bodyDLClause, List<DLClause> headDLClauses, ExtensionTable.Retrieval firstAtomRetrieval) {
            super(bufferSupply, valuesBufferManager, unionDependencySetsBySize, extensionManager, bodyDLClause.getBodyAtoms(), DLClauseCompiler.getHeadVariables(headDLClauses));
            this.m_groundDisjunctionHeaderManager = groundDisjunctionHeaderManager;
            this.m_dlClauseEvalautor = dlClauseEvalautor;
            this.m_existentialExpansionStrategy = existentialExpansionStrategy;
            this.m_bodyDLClause = bodyDLClause;
            this.m_headDLClauses = headDLClauses;
            this.m_coreVariables = new boolean[this.m_variables.size()];
            this.generateCode(1, firstAtomRetrieval);
        }

        private int getNumberOfHeads() {
            return this.m_headDLClauses.size();
        }

        private int getHeadLength(int dlClauseIndex) {
            return this.m_headDLClauses.get(dlClauseIndex).getHeadLength();
        }

        private Atom getHeadAtom(int dlClauseIndex, int atomIndex) {
            return this.m_headDLClauses.get(dlClauseIndex).getHeadAtom(atomIndex);
        }

        @Override
        protected void compileHeads() {
            this.m_existentialExpansionStrategy.dlClauseBodyCompiled(this.m_workers, this.m_bodyDLClause, this.m_variables, this.m_valuesBufferManager.m_valuesBuffer, this.m_coreVariables);
            for (int dlClauseIndex = 0; dlClauseIndex < this.getNumberOfHeads(); ++dlClauseIndex) {
                if (this.m_extensionManager.m_tableauMonitor != null) {
                    this.m_workers.add(new CallMatchStartedOnMonitor(this.m_extensionManager.m_tableauMonitor, this.m_dlClauseEvalautor, dlClauseIndex));
                }
                if (this.getHeadLength(dlClauseIndex) == 0) {
                    this.m_workers.add(new SetClash(this.m_extensionManager, this.m_unionDependencySet));
                } else if (this.getHeadLength(dlClauseIndex) == 1) {
                    Atom atom = this.getHeadAtom(dlClauseIndex, 0);
                    switch (atom.getArity()) {
                        case 1: {
                            this.m_workers.add(new DeriveUnaryFact(this.m_extensionManager, this.m_valuesBufferManager.m_valuesBuffer, this.m_coreVariables, this.m_unionDependencySet, atom.getDLPredicate(), this.m_variables.indexOf(atom.getArgumentVariable(0))));
                            break;
                        }
                        case 2: {
                            this.m_workers.add(new DeriveBinaryFact(this.m_extensionManager, this.m_valuesBufferManager.m_valuesBuffer, this.m_unionDependencySet, atom.getDLPredicate(), this.m_variables.indexOf(atom.getArgumentVariable(0)), this.m_variables.indexOf(atom.getArgumentVariable(1))));
                            break;
                        }
                        case 3: {
                            this.m_workers.add(new DeriveTernaryFact(this.m_extensionManager, this.m_valuesBufferManager.m_valuesBuffer, this.m_unionDependencySet, atom.getDLPredicate(), this.m_variables.indexOf(atom.getArgumentVariable(0)), this.m_variables.indexOf(atom.getArgumentVariable(1)), this.m_variables.indexOf(atom.getArgumentVariable(2))));
                            break;
                        }
                        default: {
                            throw new IllegalArgumentException("Unsupported atom arity.");
                        }
                    }
                } else {
                    int totalNumberOfArguments = 0;
                    for (int headIndex = 0; headIndex < this.getHeadLength(dlClauseIndex); ++headIndex) {
                        totalNumberOfArguments += this.getHeadAtom(dlClauseIndex, headIndex).getArity();
                    }
                    DLPredicate[] headDLPredicates = new DLPredicate[this.getHeadLength(dlClauseIndex)];
                    int[] copyIsCore = new int[this.getHeadLength(dlClauseIndex)];
                    int[] copyValuesToArguments = new int[totalNumberOfArguments];
                    int index = 0;
                    for (int headIndex = 0; headIndex < this.getHeadLength(dlClauseIndex); ++headIndex) {
                        Atom atom = this.getHeadAtom(dlClauseIndex, headIndex);
                        headDLPredicates[headIndex] = atom.getDLPredicate();
                        for (int argumentIndex = 0; argumentIndex < atom.getArity(); ++argumentIndex) {
                            Variable variable = atom.getArgumentVariable(argumentIndex);
                            int variableIndex = this.m_variables.indexOf(variable);
                            assert (variableIndex != -1);
                            copyValuesToArguments[index++] = variableIndex;
                        }
                        if (headDLPredicates[headIndex].getArity() == 1) {
                            Variable variable = atom.getArgumentVariable(0);
                            copyIsCore[headIndex] = this.m_variables.indexOf(variable);
                            continue;
                        }
                        copyIsCore[headIndex] = -1;
                    }
                    GroundDisjunctionHeader groundDisjunctionHeader = this.m_groundDisjunctionHeaderManager.get(headDLPredicates);
                    this.m_workers.add(new DeriveDisjunction(this.m_valuesBufferManager.m_valuesBuffer, this.m_coreVariables, this.m_unionDependencySet, this.m_extensionManager.m_tableau, groundDisjunctionHeader, copyIsCore, copyValuesToArguments));
                }
                if (this.m_extensionManager.m_tableauMonitor == null) continue;
                this.m_workers.add(new CallMatchFinishedOnMonitor(this.m_extensionManager.m_tableauMonitor, this.m_dlClauseEvalautor, dlClauseIndex));
            }
        }

        private static List<Variable> getHeadVariables(List<DLClause> headDLClauses) {
            ArrayList<Variable> result = new ArrayList<Variable>();
            for (DLClause dlClause : headDLClauses) {
                for (int headIndex = 0; headIndex < dlClause.getHeadLength(); ++headIndex) {
                    Atom atom = dlClause.getHeadAtom(headIndex);
                    for (int argumentIndex = 0; argumentIndex < atom.getArity(); ++argumentIndex) {
                        Variable variable = atom.getArgumentVariable(argumentIndex);
                        if (variable == null || result.contains(variable)) continue;
                        result.add(variable);
                    }
                }
            }
            return result;
        }
    }

    protected static final class DeriveDisjunction
    implements Worker,
    Serializable {
        private static final long serialVersionUID = -3546622575743138887L;
        private final Tableau m_tableau;
        private final Object[] m_valuesBuffer;
        private final boolean[] m_coreVariables;
        private final DependencySet m_dependencySet;
        private final GroundDisjunctionHeader m_groundDisjunctionHeader;
        private final int[] m_copyIsCore;
        private final int[] m_copyValuesToArguments;

        public DeriveDisjunction(Object[] valuesBuffer, boolean[] coreVariables, DependencySet dependencySet, Tableau tableau, GroundDisjunctionHeader groundDisjunctionHeader, int[] copyIsCore, int[] copyValuesToArguments) {
            this.m_valuesBuffer = valuesBuffer;
            this.m_coreVariables = coreVariables;
            this.m_dependencySet = dependencySet;
            this.m_tableau = tableau;
            this.m_groundDisjunctionHeader = groundDisjunctionHeader;
            this.m_copyIsCore = copyIsCore;
            this.m_copyValuesToArguments = copyValuesToArguments;
        }

        @Override
        public int execute(int programCounter) {
            Node[] arguments = new Node[this.m_copyValuesToArguments.length];
            for (int argumentIndex = this.m_copyValuesToArguments.length - 1; argumentIndex >= 0; --argumentIndex) {
                arguments[argumentIndex] = (Node)this.m_valuesBuffer[this.m_copyValuesToArguments[argumentIndex]];
            }
            boolean[] isCore = new boolean[this.m_copyIsCore.length];
            for (int copyIndex = this.m_copyIsCore.length - 1; copyIndex >= 0; --copyIndex) {
                int copyFrom = this.m_copyIsCore[copyIndex];
                isCore[copyIndex] = copyFrom == -1 || this.m_coreVariables[copyFrom];
            }
            GroundDisjunction groundDisjunction = new GroundDisjunction(this.m_tableau, this.m_groundDisjunctionHeader, arguments, isCore, this.m_tableau.m_dependencySetFactory.getPermanent(this.m_dependencySet));
            if (!groundDisjunction.isSatisfied(this.m_tableau)) {
                this.m_tableau.addGroundDisjunction(groundDisjunction);
            }
            return programCounter + 1;
        }

        public String toString() {
            return "Derive disjunction";
        }
    }

    protected static final class DeriveTernaryFact
    implements Worker,
    Serializable {
        private static final long serialVersionUID = 1823363493615682288L;
        private final ExtensionManager m_extensionManager;
        private final Object[] m_valuesBuffer;
        private final DependencySet m_dependencySet;
        private final DLPredicate m_dlPredicate;
        private final int m_argumentIndex1;
        private final int m_argumentIndex2;
        private final int m_argumentIndex3;

        public DeriveTernaryFact(ExtensionManager extensionManager, Object[] valuesBuffer, DependencySet dependencySet, DLPredicate dlPredicate, int argumentIndex1, int argumentIndex2, int argumentIndex3) {
            this.m_extensionManager = extensionManager;
            this.m_valuesBuffer = valuesBuffer;
            this.m_dependencySet = dependencySet;
            this.m_dlPredicate = dlPredicate;
            this.m_argumentIndex1 = argumentIndex1;
            this.m_argumentIndex2 = argumentIndex2;
            this.m_argumentIndex3 = argumentIndex3;
        }

        @Override
        public int execute(int programCounter) {
            Node argument1 = (Node)this.m_valuesBuffer[this.m_argumentIndex1];
            Node argument2 = (Node)this.m_valuesBuffer[this.m_argumentIndex2];
            Node argument3 = (Node)this.m_valuesBuffer[this.m_argumentIndex3];
            this.m_extensionManager.addAssertion(this.m_dlPredicate, argument1, argument2, argument3, this.m_dependencySet, true);
            return programCounter + 1;
        }

        public String toString() {
            return "Derive ternary fact";
        }
    }

    protected static final class DeriveBinaryFact
    implements Worker,
    Serializable {
        private static final long serialVersionUID = 1823363493615682288L;
        private final ExtensionManager m_extensionManager;
        private final Object[] m_valuesBuffer;
        private final DependencySet m_dependencySet;
        private final DLPredicate m_dlPredicate;
        private final int m_argumentIndex1;
        private final int m_argumentIndex2;

        public DeriveBinaryFact(ExtensionManager extensionManager, Object[] valuesBuffer, DependencySet dependencySet, DLPredicate dlPredicate, int argumentIndex1, int argumentIndex2) {
            this.m_extensionManager = extensionManager;
            this.m_valuesBuffer = valuesBuffer;
            this.m_dependencySet = dependencySet;
            this.m_dlPredicate = dlPredicate;
            this.m_argumentIndex1 = argumentIndex1;
            this.m_argumentIndex2 = argumentIndex2;
        }

        @Override
        public int execute(int programCounter) {
            Node argument1 = (Node)this.m_valuesBuffer[this.m_argumentIndex1];
            Node argument2 = (Node)this.m_valuesBuffer[this.m_argumentIndex2];
            this.m_extensionManager.addAssertion(this.m_dlPredicate, argument1, argument2, this.m_dependencySet, true);
            return programCounter + 1;
        }

        public String toString() {
            return "Derive binary fact";
        }
    }

    protected static final class DeriveUnaryFact
    implements Worker,
    Serializable {
        private static final long serialVersionUID = 7883620022252842010L;
        private final ExtensionManager m_extensionManager;
        private final Object[] m_valuesBuffer;
        private final boolean[] m_coreVariables;
        private final DependencySet m_dependencySet;
        private final DLPredicate m_dlPredicate;
        private final int m_argumentIndex;

        public DeriveUnaryFact(ExtensionManager extensionManager, Object[] valuesBuffer, boolean[] coreVariables, DependencySet dependencySet, DLPredicate dlPredicate, int argumentIndex) {
            this.m_extensionManager = extensionManager;
            this.m_valuesBuffer = valuesBuffer;
            this.m_coreVariables = coreVariables;
            this.m_dependencySet = dependencySet;
            this.m_argumentIndex = argumentIndex;
            this.m_dlPredicate = dlPredicate;
        }

        @Override
        public int execute(int programCounter) {
            Node argument = (Node)this.m_valuesBuffer[this.m_argumentIndex];
            boolean isCore = this.m_coreVariables[this.m_argumentIndex];
            this.m_extensionManager.addAssertion(this.m_dlPredicate, argument, this.m_dependencySet, isCore);
            return programCounter + 1;
        }

        public String toString() {
            return "Derive unary fact";
        }
    }

    protected static final class SetClash
    implements Worker,
    Serializable {
        private static final long serialVersionUID = -4981087765064918953L;
        private final ExtensionManager m_extensionManager;
        private final DependencySet m_dependencySet;

        public SetClash(ExtensionManager extensionManager, DependencySet dependencySet) {
            this.m_extensionManager = extensionManager;
            this.m_dependencySet = dependencySet;
        }

        @Override
        public int execute(int programCounter) {
            this.m_extensionManager.setClash(this.m_dependencySet);
            return programCounter + 1;
        }

        public String toString() {
            return "Set clash";
        }
    }

    protected static final class CallMatchFinishedOnMonitor
    implements Worker,
    Serializable {
        private static final long serialVersionUID = 1046400921858176361L;
        private final TableauMonitor m_tableauMonitor;
        private final DLClauseEvaluator m_dlClauseEvaluator;
        private final int m_dlClauseIndex;

        public CallMatchFinishedOnMonitor(TableauMonitor tableauMonitor, DLClauseEvaluator dlClauseEvaluator, int dlClauseIndex) {
            this.m_tableauMonitor = tableauMonitor;
            this.m_dlClauseEvaluator = dlClauseEvaluator;
            this.m_dlClauseIndex = dlClauseIndex;
        }

        @Override
        public int execute(int programCounter) {
            this.m_tableauMonitor.dlClauseMatchedFinished(this.m_dlClauseEvaluator, this.m_dlClauseIndex);
            return programCounter + 1;
        }

        public String toString() {
            return "Monitor -> Match finished";
        }
    }

    protected static final class CallMatchStartedOnMonitor
    implements Worker,
    Serializable {
        private static final long serialVersionUID = 8736659573939242252L;
        private final TableauMonitor m_tableauMonitor;
        private final DLClauseEvaluator m_dlClauseEvaluator;
        private final int m_dlClauseIndex;

        public CallMatchStartedOnMonitor(TableauMonitor tableauMonitor, DLClauseEvaluator dlClauseEvaluator, int dlClauseIndex) {
            this.m_tableauMonitor = tableauMonitor;
            this.m_dlClauseEvaluator = dlClauseEvaluator;
            this.m_dlClauseIndex = dlClauseIndex;
        }

        @Override
        public int execute(int programCounter) {
            this.m_tableauMonitor.dlClauseMatchedStarted(this.m_dlClauseEvaluator, this.m_dlClauseIndex);
            return programCounter + 1;
        }

        public String toString() {
            return "Monitor -> Match started";
        }
    }

    protected static final class JumpTo
    implements BranchingWorker,
    Serializable {
        private static final long serialVersionUID = -6957866973028474739L;
        private int m_jumpTo;

        public JumpTo(int jumpTo) {
            this.m_jumpTo = jumpTo;
        }

        @Override
        public int execute(int programCounter) {
            return this.m_jumpTo;
        }

        @Override
        public int getBranchingAddress() {
            return this.m_jumpTo;
        }

        @Override
        public void setBranchingAddress(int branchingAddress) {
            this.m_jumpTo = branchingAddress;
        }

        public String toString() {
            return "Jump to " + this.m_jumpTo;
        }
    }

    protected static final class HasMoreRetrieval
    implements BranchingWorker,
    Serializable {
        private static final long serialVersionUID = -2415094151423166585L;
        private int m_eofProgramCounter;
        private final ExtensionTable.Retrieval m_retrieval;

        public HasMoreRetrieval(int eofProgramCounter, ExtensionTable.Retrieval retrieval) {
            this.m_eofProgramCounter = eofProgramCounter;
            this.m_retrieval = retrieval;
        }

        @Override
        public int execute(int programCounter) {
            if (this.m_retrieval.afterLast()) {
                return this.m_eofProgramCounter;
            }
            return programCounter + 1;
        }

        @Override
        public int getBranchingAddress() {
            return this.m_eofProgramCounter;
        }

        @Override
        public void setBranchingAddress(int branchingAddress) {
            this.m_eofProgramCounter = branchingAddress;
        }

        public String toString() {
            return "Branch to " + this.m_eofProgramCounter + " if " + this.m_retrieval.getBindingsBuffer()[this.m_retrieval.getBindingPositions()[0]] + " is empty";
        }
    }

    protected static final class NextRetrieval
    implements Worker,
    Serializable {
        private static final long serialVersionUID = -2787897558147109082L;
        private final ExtensionTable.Retrieval m_retrieval;

        public NextRetrieval(ExtensionTable.Retrieval retrieval) {
            this.m_retrieval = retrieval;
        }

        @Override
        public int execute(int programCounter) {
            this.m_retrieval.next();
            return programCounter + 1;
        }

        public String toString() {
            return "Next " + this.m_retrieval.getBindingsBuffer()[this.m_retrieval.getBindingPositions()[0]];
        }
    }

    protected static final class OpenRetrieval
    implements Worker,
    Serializable {
        private static final long serialVersionUID = 8246610603084803950L;
        private final ExtensionTable.Retrieval m_retrieval;

        public OpenRetrieval(ExtensionTable.Retrieval retrieval) {
            this.m_retrieval = retrieval;
        }

        @Override
        public int execute(int programCounter) {
            this.m_retrieval.open();
            return programCounter + 1;
        }

        public String toString() {
            return "Open " + this.m_retrieval.getBindingsBuffer()[this.m_retrieval.getBindingPositions()[0]];
        }
    }

    protected static final class BranchIfNotNodeIDsAscendingOrEqual
    implements BranchingWorker,
    Serializable {
        private static final long serialVersionUID = 8053779312249250349L;
        private int m_branchProgramCounter;
        private final Object[] m_buffer;
        private final int[] m_nodeIndexes;

        public BranchIfNotNodeIDsAscendingOrEqual(int branchProgramCounter, Object[] buffer, int[] nodeIndexes) {
            this.m_branchProgramCounter = branchProgramCounter;
            this.m_buffer = buffer;
            this.m_nodeIndexes = nodeIndexes;
        }

        @Override
        public int execute(int programCounter) {
            boolean strictlyAscending = true;
            boolean allEqual = true;
            int lastNodeID = ((Node)this.m_buffer[this.m_nodeIndexes[0]]).getNodeID();
            for (int index = 1; index < this.m_nodeIndexes.length; ++index) {
                int nodeID = ((Node)this.m_buffer[this.m_nodeIndexes[index]]).getNodeID();
                if (lastNodeID >= nodeID) {
                    strictlyAscending = false;
                }
                if (nodeID != lastNodeID) {
                    allEqual = false;
                }
                lastNodeID = nodeID;
            }
            if (!strictlyAscending && allEqual || strictlyAscending && !allEqual) {
                return programCounter + 1;
            }
            return this.m_branchProgramCounter;
        }

        @Override
        public int getBranchingAddress() {
            return this.m_branchProgramCounter;
        }

        @Override
        public void setBranchingAddress(int branchingAddress) {
            this.m_branchProgramCounter = branchingAddress;
        }

        public String toString() {
            return "Branch to " + this.m_branchProgramCounter + " if node IDs are not ascending or equal";
        }
    }

    protected static final class BranchIfNotNodeIDLessEqualThan
    implements BranchingWorker,
    Serializable {
        private static final long serialVersionUID = 2484359261424674914L;
        private int m_notLessProgramCounter;
        private final Object[] m_buffer;
        private final int m_index1;
        private final int m_index2;

        public BranchIfNotNodeIDLessEqualThan(int notLessProgramCounter, Object[] buffer, int index1, int index2) {
            this.m_notLessProgramCounter = notLessProgramCounter;
            this.m_buffer = buffer;
            this.m_index1 = index1;
            this.m_index2 = index2;
        }

        @Override
        public int execute(int programCounter) {
            if (((Node)this.m_buffer[this.m_index1]).getNodeID() <= ((Node)this.m_buffer[this.m_index2]).getNodeID()) {
                return programCounter + 1;
            }
            return this.m_notLessProgramCounter;
        }

        @Override
        public int getBranchingAddress() {
            return this.m_notLessProgramCounter;
        }

        @Override
        public void setBranchingAddress(int branchingAddress) {
            this.m_notLessProgramCounter = branchingAddress;
        }

        public String toString() {
            return "Branch to " + this.m_notLessProgramCounter + " if " + this.m_index1 + ".ID > " + this.m_index2 + ".ID";
        }
    }

    protected static final class BranchIfNotEqual
    implements BranchingWorker,
    Serializable {
        private static final long serialVersionUID = -1880147431680856293L;
        private int m_notEqualProgramCounter;
        private final Object[] m_buffer;
        private final int m_index1;
        private final int m_index2;

        public BranchIfNotEqual(int notEqualProgramCounter, Object[] buffer, int index1, int index2) {
            this.m_notEqualProgramCounter = notEqualProgramCounter;
            this.m_buffer = buffer;
            this.m_index1 = index1;
            this.m_index2 = index2;
        }

        @Override
        public int execute(int programCounter) {
            if (this.m_buffer[this.m_index1].equals(this.m_buffer[this.m_index2])) {
                return programCounter + 1;
            }
            return this.m_notEqualProgramCounter;
        }

        @Override
        public int getBranchingAddress() {
            return this.m_notEqualProgramCounter;
        }

        @Override
        public void setBranchingAddress(int branchingAddress) {
            this.m_notEqualProgramCounter = branchingAddress;
        }

        public String toString() {
            return "Branch to " + this.m_notEqualProgramCounter + " if " + this.m_index1 + " != " + this.m_index2;
        }
    }

    protected static final class CopyDependencySet
    implements Worker,
    Serializable {
        private static final long serialVersionUID = 705172386083123813L;
        private final ExtensionTable.Retrieval m_retrieval;
        private final DependencySet[] m_targetDependencySets;
        private final int m_targetIndex;

        public CopyDependencySet(ExtensionTable.Retrieval retrieval, DependencySet[] targetDependencySets, int targetIndex) {
            this.m_retrieval = retrieval;
            this.m_targetDependencySets = targetDependencySets;
            this.m_targetIndex = targetIndex;
        }

        @Override
        public int execute(int programCounter) {
            this.m_targetDependencySets[this.m_targetIndex] = this.m_retrieval.getDependencySet();
            return programCounter + 1;
        }

        public String toString() {
            return "Copy dependency set to " + this.m_targetIndex;
        }
    }

    protected static final class CopyValues
    implements Worker,
    Serializable {
        private static final long serialVersionUID = -4323769483485648756L;
        private final Object[] m_fromBuffer;
        private final int m_fromIndex;
        private final Object[] m_toBuffer;
        private final int m_toIndex;

        public CopyValues(Object[] fromBuffer, int fromIndex, Object[] toBuffer, int toIndex) {
            this.m_fromBuffer = fromBuffer;
            this.m_fromIndex = fromIndex;
            this.m_toBuffer = toBuffer;
            this.m_toIndex = toIndex;
        }

        @Override
        public int execute(int programCounter) {
            this.m_toBuffer[this.m_toIndex] = this.m_fromBuffer[this.m_fromIndex];
            return programCounter + 1;
        }

        public String toString() {
            return "Copy " + this.m_fromIndex + " --> " + this.m_toIndex;
        }
    }

    protected interface BranchingWorker
    extends Worker {
        int getBranchingAddress();

        void setBranchingAddress(int var1);
    }

    public interface Worker {
        int execute(int var1);
    }

    static class GroundDisjunctionHeaderManager {
        protected GroundDisjunctionHeader[] m_buckets = new GroundDisjunctionHeader[1024];
        protected int m_numberOfElements = 0;
        protected int m_threshold = (int)((double)this.m_buckets.length * 0.75);

        public GroundDisjunctionHeader get(DLPredicate[] dlPredicates) {
            int hashCode = 0;
            for (int disjunctIndex = 0; disjunctIndex < dlPredicates.length; ++disjunctIndex) {
                hashCode = hashCode * 7 + dlPredicates[disjunctIndex].hashCode();
            }
            int bucketIndex = GroundDisjunctionHeaderManager.getIndexFor(hashCode, this.m_buckets.length);
            GroundDisjunctionHeader entry = this.m_buckets[bucketIndex];
            while (entry != null) {
                if (hashCode == entry.m_hashCode && entry.isEqual(dlPredicates)) {
                    return entry;
                }
                entry = entry.m_nextEntry;
            }
            this.m_buckets[bucketIndex] = entry = new GroundDisjunctionHeader(dlPredicates, hashCode, entry);
            ++this.m_numberOfElements;
            if (this.m_numberOfElements >= this.m_threshold) {
                this.resize(this.m_buckets.length * 2);
            }
            return entry;
        }

        protected void resize(int newCapacity) {
            GroundDisjunctionHeader[] newBuckets = new GroundDisjunctionHeader[newCapacity];
            for (int i = 0; i < this.m_buckets.length; ++i) {
                GroundDisjunctionHeader entry = this.m_buckets[i];
                while (entry != null) {
                    GroundDisjunctionHeader nextEntry = entry.m_nextEntry;
                    int newIndex = GroundDisjunctionHeaderManager.getIndexFor(entry.hashCode(), newCapacity);
                    entry.m_nextEntry = newBuckets[newIndex];
                    newBuckets[newIndex] = entry;
                    entry = nextEntry;
                }
            }
            this.m_buckets = newBuckets;
            this.m_threshold = (int)((double)newCapacity * 0.75);
        }

        protected static int getIndexFor(int _hashCode, int tableLength) {
            int hashCode = _hashCode;
            hashCode += ~ (hashCode << 9);
            hashCode ^= hashCode >>> 14;
            hashCode += hashCode << 4;
            hashCode ^= hashCode >>> 10;
            return hashCode & tableLength - 1;
        }
    }

    public static class ValuesBufferManager {
        public final Object[] m_valuesBuffer;
        final Map<DLPredicate, Integer> m_bodyDLPredicatesToIndexes;
        public final int m_maxNumberOfVariables;
        final Map<Term, Integer> m_bodyNonvariableTermsToIndexes;

        public ValuesBufferManager(Set<DLClause> dlClauses, Map<Term, Node> termsToNodes) {
            HashSet<DLPredicate> bodyDLPredicates = new HashSet<DLPredicate>();
            HashSet<Variable> variables = new HashSet<Variable>();
            this.m_bodyNonvariableTermsToIndexes = new HashMap<Term, Integer>();
            int maxNumberOfVariables = 0;
            for (DLClause dlClause : dlClauses) {
                variables.clear();
                for (int bodyIndex = dlClause.getBodyLength() - 1; bodyIndex >= 0; --bodyIndex) {
                    Atom atom = dlClause.getBodyAtom(bodyIndex);
                    bodyDLPredicates.add(atom.getDLPredicate());
                    for (int argumentIndex = 0; argumentIndex < atom.getArity(); ++argumentIndex) {
                        Term term = atom.getArgument(argumentIndex);
                        if (term instanceof Variable) {
                            variables.add((Variable)term);
                            continue;
                        }
                        this.m_bodyNonvariableTermsToIndexes.put(term, -1);
                    }
                }
                if (variables.size() <= maxNumberOfVariables) continue;
                maxNumberOfVariables = variables.size();
            }
            this.m_valuesBuffer = new Object[maxNumberOfVariables + bodyDLPredicates.size() + this.m_bodyNonvariableTermsToIndexes.size()];
            this.m_bodyDLPredicatesToIndexes = new HashMap<DLPredicate, Integer>();
            int bindingIndex = maxNumberOfVariables;
            for (DLPredicate bodyDLPredicate : bodyDLPredicates) {
                this.m_bodyDLPredicatesToIndexes.put(bodyDLPredicate, bindingIndex);
                this.m_valuesBuffer[bindingIndex] = bodyDLPredicate;
                ++bindingIndex;
            }
            for (Map.Entry<Term, Integer> entry : this.m_bodyNonvariableTermsToIndexes.entrySet()) {
                Node termNode = termsToNodes.get(entry.getKey());
                if (termNode == null) {
                    throw new IllegalArgumentException("Term '" + entry.getValue() + "' is unknown to the reasoner.");
                }
                entry.setValue(bindingIndex);
                this.m_valuesBuffer[bindingIndex] = termNode.getCanonicalNode();
                ++bindingIndex;
            }
            this.m_maxNumberOfVariables = maxNumberOfVariables;
        }
    }

    public static class BufferSupply {
        protected final List<Object[]> m_allBuffers = new ArrayList<Object[]>();
        protected final Map<Integer, List<Object[]>> m_availableBuffersByArity = new HashMap<Integer, List<Object[]>>();

        void reuseBuffers() {
            this.m_availableBuffersByArity.clear();
            for (Object[] buffer : this.m_allBuffers) {
                Integer arityInteger = buffer.length;
                List<Object[]> buffers = this.m_availableBuffersByArity.get(arityInteger);
                if (buffers == null) {
                    buffers = new ArrayList<Object[]>();
                    this.m_availableBuffersByArity.put(arityInteger, buffers);
                }
                buffers.add(buffer);
            }
        }

        Object[] getBuffer(int arity) {
            Object[] buffer;
            Integer arityInteger = arity;
            List<Object[]> buffers = this.m_availableBuffersByArity.get(arityInteger);
            if (buffers == null || buffers.isEmpty()) {
                buffer = new Object[arity];
                this.m_allBuffers.add(buffer);
            } else {
                buffer = buffers.remove(buffers.size() - 1);
            }
            return buffer;
        }

        public Object[][] getAllBuffers() {
            Object[][] result=new Object[m_allBuffers.size()][];
            m_allBuffers.toArray(result);
            return result;
        }
    }

}

