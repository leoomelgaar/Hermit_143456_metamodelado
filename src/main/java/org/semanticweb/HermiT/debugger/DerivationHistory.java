package org.semanticweb.HermiT.debugger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import org.semanticweb.HermiT.Prefixes;
import org.semanticweb.HermiT.model.Concept;
import org.semanticweb.HermiT.model.ConstantEnumeration;
import org.semanticweb.HermiT.model.DLClause;
import org.semanticweb.HermiT.model.DLPredicate;
import org.semanticweb.HermiT.model.DataRange;
import org.semanticweb.HermiT.model.DatatypeRestriction;
import org.semanticweb.HermiT.model.Equality;
import org.semanticweb.HermiT.model.ExistentialConcept;
import org.semanticweb.HermiT.model.Inequality;
import org.semanticweb.HermiT.model.NodeIDLessEqualThan;
import org.semanticweb.HermiT.model.NodeIDsAscendingOrEqual;
import org.semanticweb.HermiT.monitor.TableauMonitorAdapter;
import org.semanticweb.HermiT.tableau.BranchingPoint;
import org.semanticweb.HermiT.tableau.DLClauseEvaluator;
import org.semanticweb.HermiT.tableau.DatatypeManager;
import org.semanticweb.HermiT.tableau.GroundDisjunction;
import org.semanticweb.HermiT.tableau.Node;

public class DerivationHistory
extends TableauMonitorAdapter {
    private static final long serialVersionUID = -3963478091986772947L;
    protected static final Object[] EMPTY_TUPLE = new Object[0];
    protected final Map<AtomKey, Atom> m_derivedAtoms = new HashMap<AtomKey, Atom>();
    protected final Map<GroundDisjunction, Disjunction> m_derivedDisjunctions = new HashMap<GroundDisjunction, Disjunction>();
    protected final Stack<Derivation> m_derivations = new Stack();
    protected final Stack<Atom> m_mergeAtoms = new Stack();

    @Override
    public void tableauCleared() {
        this.m_derivedAtoms.clear();
        this.m_derivedDisjunctions.clear();
        this.m_derivations.clear();
        this.m_derivations.push(BaseFact.INSTANCE);
        this.m_mergeAtoms.clear();
    }

    @Override
    public void dlClauseMatchedStarted(DLClauseEvaluator dlClauseEvaluator, int dlClauseIndex) {
        int regularBodyAtomsNumber = 0;
        for (int index = 0; index < dlClauseEvaluator.getBodyLength(); ++index) {
            DLPredicate dlPredicate = dlClauseEvaluator.getBodyAtom(index).getDLPredicate();
            if (dlPredicate instanceof NodeIDLessEqualThan || dlPredicate instanceof NodeIDsAscendingOrEqual) continue;
            ++regularBodyAtomsNumber;
        }
        Atom[] premises = new Atom[regularBodyAtomsNumber];
        int atomIndex = 0;
        for (int index = 0; index < premises.length; ++index) {
            DLPredicate dlPredicate = dlClauseEvaluator.getBodyAtom(index).getDLPredicate();
            if (dlPredicate instanceof NodeIDLessEqualThan && dlPredicate instanceof NodeIDsAscendingOrEqual) continue;
            premises[atomIndex++] = this.getAtom(dlClauseEvaluator.getTupleMatchedToBody(index));
        }
        this.m_derivations.push(new DLClauseApplication(dlClauseEvaluator.getDLClause(dlClauseIndex), premises));
    }

    @Override
    public void addFactFinished(Object[] tuple, boolean isCore, boolean factAdded) {
        if (factAdded) {
            this.addAtom(tuple);
        }
    }

    @Override
    public void mergeStarted(Node nodeFrom, Node nodeInto) {
        Atom equalityAtom = this.addAtom(new Object[]{Equality.INSTANCE, nodeFrom, nodeInto});
        this.m_mergeAtoms.add(equalityAtom);
    }

    @Override
    public void mergeFactStarted(Node mergeFrom, Node mergeInto, Object[] sourceTuple, Object[] targetTuple) {
        this.m_derivations.push(new Merging(this.m_mergeAtoms.peek(), this.getAtom(sourceTuple)));
    }

    @Override
    public void mergeFactFinished(Node mergeFrom, Node mergeInto, Object[] sourceTuple, Object[] targetTuple) {
        this.m_derivations.pop();
    }

    @Override
    public void mergeFinished(Node nodeFrom, Node nodeInto) {
        this.m_mergeAtoms.pop();
    }

    @Override
    public /* varargs */ void clashDetectionStarted(Object[] ... tuples) {
        Atom[] atoms = new Atom[tuples.length];
        for (int index = 0; index < tuples.length; ++index) {
            atoms[index] = this.getAtom(tuples[index]);
        }
        this.m_derivations.push(new ClashDetection(atoms));
    }

    @Override
    public /* varargs */ void clashDetectionFinished(Object[] ... tuples) {
        this.m_derivations.pop();
    }

    @Override
    public void clashDetected() {
        this.addAtom(EMPTY_TUPLE);
    }

    @Override
    public void tupleRemoved(Object[] tuple) {
        this.m_derivedAtoms.remove(new AtomKey(tuple));
    }

    @Override
    public void backtrackToFinished(BranchingPoint newCurrentBrancingPoint) {
        this.m_derivedAtoms.remove(new AtomKey(EMPTY_TUPLE));
    }

    @Override
    public void groundDisjunctionDerived(GroundDisjunction groundDisjunction) {
        Disjunction disjunction = new Disjunction(groundDisjunction, this.m_derivations.peek());
        this.m_derivedDisjunctions.put(groundDisjunction, disjunction);
    }

    @Override
    public void disjunctProcessingStarted(GroundDisjunction groundDisjunction, int disjunct) {
        Disjunction disjunction = this.getDisjunction(groundDisjunction);
        this.m_derivations.push(new DisjunctApplication(disjunction, disjunct));
    }

    @Override
    public void disjunctProcessingFinished(GroundDisjunction groundDisjunction, int disjunct) {
        this.m_derivations.pop();
    }

    @Override
    public void existentialExpansionStarted(ExistentialConcept existentialConcept, Node forNode) {
        Atom existentialAtom = this.getAtom(new Object[]{existentialConcept, forNode});
        this.m_derivations.push(new ExistentialExpansion(existentialAtom));
    }

    @Override
    public void existentialExpansionFinished(ExistentialConcept existentialConcept, Node forNode) {
        this.m_derivations.pop();
    }

    @Override
    public void descriptionGraphCheckingStarted(int graphIndex1, int tupleIndex1, int position1, int graphIndex2, int tupleIndex2, int position2) {
        Atom graph1 = this.getAtom(this.m_tableau.getDescriptionGraphManager().getDescriptionGraphTuple(graphIndex1, tupleIndex1));
        Atom graph2 = this.getAtom(this.m_tableau.getDescriptionGraphManager().getDescriptionGraphTuple(graphIndex2, tupleIndex2));
        this.m_derivations.push(new GraphChecking(graph1, position1, graph2, position2));
    }

    @Override
    public void descriptionGraphCheckingFinished(int graphIndex1, int tupleIndex1, int position1, int graphIndex2, int tupleIndex2, int position2) {
        this.m_derivations.pop();
    }

    @Override
    public void unknownDatatypeRestrictionDetectionStarted(DataRange dataRange1, Node node1, DataRange dataRange2, Node node2) {
        Atom atom1 = this.getAtom(new Object[]{dataRange1, node1});
        Atom atom2 = this.getAtom(new Object[]{dataRange2, node2});
        this.m_derivations.push(new UnknownDatatypeRestrictionDetection(new Atom[]{atom1, atom2}));
    }

    @Override
    public void unknownDatatypeRestrictionDetectionFinished(DataRange dataRange1, Node node1, DataRange dataRange2, Node node2) {
        this.m_derivations.pop();
    }

    @Override
    public void datatypeConjunctionCheckingStarted(DatatypeManager.DConjunction conjunction) {
        ArrayList<Atom> atoms = new ArrayList<Atom>();
        for (DatatypeManager.DVariable variable : conjunction.getActiveVariables()) {
            Node node = variable.getNode();
            for (DatatypeRestriction datatypeRestriction : variable.getPositiveDatatypeRestrictions()) {
                atoms.add(this.getAtom(new Object[]{datatypeRestriction, node}));
            }
            for (DatatypeRestriction datatypeRestriction : variable.getNegativeDatatypeRestrictions()) {
                atoms.add(this.getAtom(new Object[]{datatypeRestriction.getNegation(), node}));
            }
            for (ConstantEnumeration dataValueEnumeration : variable.getPositiveDataValueEnumerations()) {
                atoms.add(this.getAtom(new Object[]{dataValueEnumeration, node}));
            }
            for (ConstantEnumeration dataValueEnumeration : variable.getNegativeDataValueEnumerations()) {
                atoms.add(this.getAtom(new Object[]{dataValueEnumeration.getNegation(), node}));
            }
            for (DatatypeManager.DVariable neighborVariable : variable.getUnequalToDirect()) {
                atoms.add(this.getAtom(new Object[]{Inequality.INSTANCE, node, neighborVariable.getNode()}));
            }
        }
        Atom[] atomsArray = new Atom[atoms.size()];
        atoms.toArray(atomsArray);
        this.m_derivations.push(new DatatypeChecking(atomsArray));
    }

    @Override
    public void datatypeConjunctionCheckingFinished(DatatypeManager.DConjunction conjunction, boolean result) {
        this.m_derivations.pop();
    }

    public Atom getAtom(Object[] tuple) {
        return this.m_derivedAtoms.get(new AtomKey(tuple));
    }

    public Disjunction getDisjunction(GroundDisjunction groundDisjunction) {
        return this.m_derivedDisjunctions.get(groundDisjunction);
    }

    protected Atom addAtom(Object[] tuple) {
        Object[] clonedTuple = tuple.clone();
        Atom newAtom = new Atom(clonedTuple, this.m_derivations.peek());
        this.m_derivedAtoms.put(new AtomKey(clonedTuple), newAtom);
        return newAtom;
    }

    public static class BaseFact
    extends Derivation {
        private static final long serialVersionUID = -5998349862414502218L;
        public static final Derivation INSTANCE = new BaseFact();

        @Override
        public int getNumberOfPremises() {
            return 0;
        }

        @Override
        public Fact getPremise(int premiseIndex) {
            throw new IndexOutOfBoundsException();
        }

        @Override
        public String toString(Prefixes prefixes) {
            return ".";
        }
    }

    public static class UnknownDatatypeRestrictionDetection
    extends Derivation {
        private static final long serialVersionUID = -7824360133765453948L;
        protected final Atom[] m_causes;

        public UnknownDatatypeRestrictionDetection(Atom[] causes) {
            this.m_causes = causes;
        }

        @Override
        public int getNumberOfPremises() {
            return this.m_causes.length;
        }

        @Override
        public Fact getPremise(int premiseIndex) {
            return this.m_causes[premiseIndex];
        }

        @Override
        public String toString(Prefixes prefixes) {
            return "   << UNKNOWN DATATYPE";
        }
    }

    public static class DatatypeChecking
    extends Derivation {
        private static final long serialVersionUID = -7833124370362424190L;
        protected final Atom[] m_causes;

        public DatatypeChecking(Atom[] causes) {
            this.m_causes = causes;
        }

        @Override
        public int getNumberOfPremises() {
            return this.m_causes.length;
        }

        @Override
        public Fact getPremise(int premiseIndex) {
            return this.m_causes[premiseIndex];
        }

        @Override
        public String toString(Prefixes prefixes) {
            return "   << DATATYPES";
        }
    }

    public static class ClashDetection
    extends Derivation {
        private static final long serialVersionUID = -1046733682276190587L;
        protected final Atom[] m_causes;

        public ClashDetection(Atom[] causes) {
            this.m_causes = causes;
        }

        @Override
        public int getNumberOfPremises() {
            return this.m_causes.length;
        }

        @Override
        public Fact getPremise(int premiseIndex) {
            return this.m_causes[premiseIndex];
        }

        @Override
        public String toString(Prefixes prefixes) {
            return "   << CLASH";
        }
    }

    public static class ExistentialExpansion
    extends Derivation {
        private static final long serialVersionUID = -1266097745277870260L;
        protected final Atom m_existentialAtom;

        public ExistentialExpansion(Atom existentialAtom) {
            this.m_existentialAtom = existentialAtom;
        }

        @Override
        public int getNumberOfPremises() {
            return 1;
        }

        @Override
        public Fact getPremise(int premiseIndex) {
            switch (premiseIndex) {
                case 0: {
                    return this.m_existentialAtom;
                }
            }
            throw new IndexOutOfBoundsException();
        }

        @Override
        public String toString(Prefixes prefixes) {
            return " <<  EXISTS";
        }
    }

    public static class GraphChecking
    extends Derivation {
        private static final long serialVersionUID = -3671522413313454739L;
        protected final Atom m_graph1;
        protected final int m_position1;
        protected final Atom m_graph2;
        protected final int m_position2;

        public GraphChecking(Atom graph1, int position1, Atom graph2, int position2) {
            this.m_graph1 = graph1;
            this.m_position1 = position1;
            this.m_graph2 = graph2;
            this.m_position2 = position2;
        }

        @Override
        public int getNumberOfPremises() {
            return 2;
        }

        @Override
        public Fact getPremise(int premiseIndex) {
            switch (premiseIndex) {
                case 0: {
                    return this.m_graph1;
                }
                case 1: {
                    return this.m_graph2;
                }
            }
            throw new IndexOutOfBoundsException();
        }

        @Override
        public String toString(Prefixes prefixes) {
            return "   << DGRAPHS | " + this.m_position1 + " and " + this.m_position2;
        }
    }

    public static class Merging
    extends Derivation {
        private static final long serialVersionUID = 6815119442652251306L;
        protected final Atom m_equality;
        protected final Atom m_fromAtom;

        public Merging(Atom equality, Atom fromAtom) {
            this.m_equality = equality;
            this.m_fromAtom = fromAtom;
        }

        @Override
        public int getNumberOfPremises() {
            return 2;
        }

        @Override
        public Fact getPremise(int premiseIndex) {
            switch (premiseIndex) {
                case 0: {
                    return this.m_equality;
                }
                case 1: {
                    return this.m_fromAtom;
                }
            }
            throw new IndexOutOfBoundsException();
        }

        @Override
        public String toString(Prefixes prefixes) {
            return "   <--|";
        }
    }

    public static class DisjunctApplication
    extends Derivation {
        private static final long serialVersionUID = 6657356873675430986L;
        protected final Disjunction m_disjunction;
        protected final int m_disjunctIndex;

        public DisjunctApplication(Disjunction disjunction, int disjunctIndex) {
            this.m_disjunction = disjunction;
            this.m_disjunctIndex = disjunctIndex;
        }

        public int getDisjunctIndex() {
            return this.m_disjunctIndex;
        }

        @Override
        public int getNumberOfPremises() {
            return 1;
        }

        @Override
        public Fact getPremise(int premiseIndex) {
            switch (premiseIndex) {
                case 0: {
                    return this.m_disjunction;
                }
            }
            throw new IndexOutOfBoundsException();
        }

        @Override
        public String toString(Prefixes prefixes) {
            return "  |  " + this.m_disjunctIndex;
        }
    }

    public static class DLClauseApplication
    extends Derivation {
        private static final long serialVersionUID = 5841561027229354512L;
        protected final DLClause m_dlClause;
        protected final Atom[] m_premises;

        public DLClauseApplication(DLClause dlClause, Atom[] premises) {
            this.m_dlClause = dlClause;
            this.m_premises = premises;
        }

        public DLClause getDLClause() {
            return this.m_dlClause;
        }

        @Override
        public int getNumberOfPremises() {
            return this.m_premises.length;
        }

        @Override
        public Fact getPremise(int premiseIndex) {
            return this.m_premises[premiseIndex];
        }

        @Override
        public String toString(Prefixes prefixes) {
            return "  <--  " + this.m_dlClause.toString(prefixes);
        }
    }

    public static abstract class Derivation
    implements Serializable {
        public abstract String toString(Prefixes var1);

        public String toString() {
            return this.toString(Prefixes.STANDARD_PREFIXES);
        }

        public abstract int getNumberOfPremises();

        public abstract Fact getPremise(int var1);
    }

    public static class Disjunction
    implements Fact {
        private static final long serialVersionUID = -6645342875287836609L;
        protected final Object[][] m_atoms;
        protected final Derivation m_derivedBy;

        public Disjunction(GroundDisjunction groundDisjunction, Derivation derivedBy) {
            this.m_atoms = new Object[groundDisjunction.getNumberOfDisjuncts()][];
            for (int disjunctIndex = 0; disjunctIndex < groundDisjunction.getNumberOfDisjuncts(); ++disjunctIndex) {
                DLPredicate dlPredicate = groundDisjunction.getDLPredicate(disjunctIndex);
                Object[] tuple = new Object[dlPredicate.getArity() + 1];
                tuple[0] = dlPredicate;
                for (int argumentIndex = 0; argumentIndex < dlPredicate.getArity(); ++argumentIndex) {
                    tuple[argumentIndex + 1] = groundDisjunction.getArgument(disjunctIndex, argumentIndex);
                }
                this.m_atoms[disjunctIndex] = tuple;
            }
            this.m_derivedBy = derivedBy;
        }

        public int getNumberOfDisjuncts() {
            return this.m_atoms.length;
        }

        public Object getDLPredicate(int disjunctIndex) {
            return this.m_atoms[disjunctIndex][0];
        }

        public Node getArgument(int disjunctIndex, int argumentIndex) {
            return (Node)this.m_atoms[disjunctIndex][argumentIndex + 1];
        }

        @Override
        public Derivation getDerivation() {
            return this.m_derivedBy;
        }

        @Override
        public String toString(Prefixes prefixes) {
            StringBuffer buffer = new StringBuffer();
            for (int disjunctIndex = 0; disjunctIndex < this.m_atoms.length; ++disjunctIndex) {
                Object[] tuple;
                if (disjunctIndex != 0) {
                    buffer.append(" v ");
                }
                if ((tuple = this.m_atoms[disjunctIndex])[0] instanceof DLPredicate) {
                    buffer.append(((DLPredicate)tuple[0]).toString(prefixes));
                } else if (tuple[0] instanceof Concept) {
                    buffer.append(((Concept)tuple[0]).toString(prefixes));
                } else {
                    throw new IllegalStateException("Internal error: invalid DL-predicate.");
                }
                buffer.append('(');
                for (int argumentIndex = 1; argumentIndex < tuple.length; ++argumentIndex) {
                    if (argumentIndex != 1) {
                        buffer.append(',');
                    }
                    buffer.append(((Node)tuple[argumentIndex]).getNodeID());
                }
                buffer.append(')');
            }
            return buffer.toString();
        }

        public String toString() {
            return this.toString(Prefixes.STANDARD_PREFIXES);
        }
    }

    public static class Atom
    implements Fact {
        private static final long serialVersionUID = -6136317748590721560L;
        protected final Object[] m_tuple;
        protected final Derivation m_derivedBy;

        public Atom(Object[] tuple, Derivation derivedBy) {
            this.m_tuple = tuple;
            this.m_derivedBy = derivedBy;
        }

        public Object getDLPredicate() {
            return this.m_tuple[0];
        }

        public int getArity() {
            return this.m_tuple.length - 1;
        }

        public Node getArgument(int index) {
            return (Node)this.m_tuple[index + 1];
        }

        @Override
        public Derivation getDerivation() {
            return this.m_derivedBy;
        }

        @Override
        public String toString(Prefixes prefixes) {
            if (this.m_tuple.length == 0) {
                return "[ ]";
            }
            StringBuffer buffer = new StringBuffer();
            Object dlPredicate = this.getDLPredicate();
            if (org.semanticweb.HermiT.model.Atom.s_infixPredicates.contains(dlPredicate)) {
                buffer.append(this.getArgument(0).getNodeID());
                buffer.append(' ');
                buffer.append(((DLPredicate)dlPredicate).toString(prefixes));
                buffer.append(' ');
                buffer.append(this.getArgument(1).getNodeID());
            } else {
                if (dlPredicate instanceof DLPredicate) {
                    buffer.append(((DLPredicate)dlPredicate).toString(prefixes));
                } else if (dlPredicate instanceof Concept) {
                    buffer.append(((Concept)dlPredicate).toString(prefixes));
                } else {
                    throw new IllegalStateException("Internal error: invalid DL-predicate.");
                }
                buffer.append('(');
                for (int argumentIndex = 0; argumentIndex < this.getArity(); ++argumentIndex) {
                    if (argumentIndex != 0) {
                        buffer.append(',');
                    }
                    buffer.append(this.getArgument(argumentIndex).getNodeID());
                }
                buffer.append(')');
            }
            return buffer.toString();
        }

        public String toString() {
            return this.toString(Prefixes.STANDARD_PREFIXES);
        }
    }

    public interface Fact
    extends Serializable {
        String toString(Prefixes var1);

        Derivation getDerivation();
    }

    protected static class AtomKey
    implements Serializable {
        private static final long serialVersionUID = 1409033744982881556L;
        protected final Object[] m_tuple;
        protected final int m_hashCode;

        public AtomKey(Object[] tuple) {
            this.m_tuple = tuple;
            int hashCode = 0;
            for (int index = 0; index < tuple.length; ++index) {
                hashCode += tuple[index].hashCode();
            }
            this.m_hashCode = hashCode;
        }

        public int hashCode() {
            return this.m_hashCode;
        }

        public boolean equals(Object that) {
            if (this == that) {
                return true;
            }
            if (!(that instanceof AtomKey)) {
                return false;
            }
            AtomKey thatAtomKey = (AtomKey)that;
            if (this.m_tuple.length != thatAtomKey.m_tuple.length) {
                return false;
            }
            for (int index = 0; index < this.m_tuple.length; ++index) {
                if (this.m_tuple[index].equals(thatAtomKey.m_tuple[index])) continue;
                return false;
            }
            return true;
        }
    }

}

