package org.semanticweb.HermiT.model;

import java.io.Serializable;
import java.util.HashSet;
import org.semanticweb.HermiT.Prefixes;

public class DLClause
implements Serializable {
    private static final long serialVersionUID = -4513910129515151732L;
    protected final Atom[] m_headAtoms;
    protected final Atom[] m_bodyAtoms;
    protected static final InterningManager<DLClause> s_interningManager = new InterningManager<DLClause>(){

        @Override
        protected boolean equal(DLClause object1, DLClause object2) {
            int index;
            if (object1.m_headAtoms.length != object2.m_headAtoms.length || object1.m_bodyAtoms.length != object2.m_bodyAtoms.length) {
                return false;
            }
            for (index = object1.m_headAtoms.length - 1; index >= 0; --index) {
                if (object1.m_headAtoms[index] == object2.m_headAtoms[index]) continue;
                return false;
            }
            for (index = object1.m_bodyAtoms.length - 1; index >= 0; --index) {
                if (object1.m_bodyAtoms[index] == object2.m_bodyAtoms[index]) continue;
                return false;
            }
            return true;
        }

        @Override
        protected int getHashCode(DLClause object) {
            int index;
            int hashCode = 0;
            for (index = object.m_bodyAtoms.length - 1; index >= 0; --index) {
                hashCode += object.m_bodyAtoms[index].hashCode();
            }
            for (index = object.m_headAtoms.length - 1; index >= 0; --index) {
                hashCode += object.m_headAtoms[index].hashCode();
            }
            return hashCode;
        }
    };

    protected DLClause(Atom[] headAtoms, Atom[] bodyAtoms) {
        this.m_headAtoms = headAtoms;
        this.m_bodyAtoms = bodyAtoms;
    }

    public int getHeadLength() {
        return this.m_headAtoms.length;
    }

    public Atom getHeadAtom(int atomIndex) {
        return this.m_headAtoms[atomIndex];
    }

    public Atom[] getHeadAtoms() {
        return this.m_headAtoms.clone();
    }

    public int getBodyLength() {
        return this.m_bodyAtoms.length;
    }

    public Atom getBodyAtom(int atomIndex) {
        return this.m_bodyAtoms[atomIndex];
    }

    public Atom[] getBodyAtoms() {
        return this.m_bodyAtoms.clone();
    }

    public DLClause getSafeVersion(DLPredicate safeMakingPredicate) {
        Variable variable2;
        Atom atom;
        int argumentIndex;
        HashSet<Variable> variables = new HashSet<Variable>();
        for (int headIndex = 0; headIndex < this.m_headAtoms.length; ++headIndex) {
            atom = this.m_headAtoms[headIndex];
            for (argumentIndex = 0; argumentIndex < atom.getArity(); ++argumentIndex) {
                variable2 = atom.getArgumentVariable(argumentIndex);
                if (variable2 == null) continue;
                variables.add(variable2);
            }
        }
        for (int bodyIndex = 0; bodyIndex < this.m_bodyAtoms.length; ++bodyIndex) {
            atom = this.m_bodyAtoms[bodyIndex];
            for (argumentIndex = 0; argumentIndex < atom.getArity(); ++argumentIndex) {
                variable2 = atom.getArgumentVariable(argumentIndex);
                if (variable2 == null) continue;
                variables.remove(variable2);
            }
        }
        if (this.m_headAtoms.length == 0 && this.m_bodyAtoms.length == 0) {
            variables.add(Variable.create("X"));
        }
        if (variables.isEmpty()) {
            return this;
        }
        Atom[] newBodyAtoms = new Atom[this.m_bodyAtoms.length + variables.size()];
        System.arraycopy(this.m_bodyAtoms, 0, newBodyAtoms, 0, this.m_bodyAtoms.length);
        int index = this.m_bodyAtoms.length;
        for (Variable variable3 : variables) {
            newBodyAtoms[index++] = Atom.create(safeMakingPredicate, variable3);
        }
        return DLClause.create(this.m_headAtoms, newBodyAtoms);
    }

    public DLClause getChangedDLClause(Atom[] headAtoms, Atom[] bodyAtoms) {
        if (headAtoms == null) {
            headAtoms = this.m_headAtoms;
        }
        if (bodyAtoms == null) {
            bodyAtoms = this.m_bodyAtoms;
        }
        return DLClause.create(headAtoms, bodyAtoms);
    }

    public boolean isGeneralConceptInclusion() {
        if (this.m_headAtoms.length == 0) {
            if (this.m_bodyAtoms.length == 2 && this.m_bodyAtoms[0].getArity() == 2 && this.m_bodyAtoms[1].getArity() == 2) {
                return false;
            }
            for (Atom bodyAtom : this.m_bodyAtoms) {
                if (bodyAtom.getArity() == 1 && bodyAtom.getDLPredicate() instanceof DataRange) continue;
                return true;
            }
        }
        for (Atom headAtom : this.m_headAtoms) {
            DLPredicate predicate = headAtom.getDLPredicate();
            if (predicate instanceof AtLeast || predicate instanceof LiteralConcept || predicate instanceof AnnotatedEquality || predicate instanceof NodeIDLessEqualThan || predicate instanceof NodeIDsAscendingOrEqual) {
                return true;
            }
            if (predicate instanceof Equality) {
                for (Atom bodyAtom : this.m_bodyAtoms) {
                    DLPredicate bodyPredicate = bodyAtom.getDLPredicate();
                    if (bodyAtom.getArity() != 1 || !(bodyPredicate instanceof AtomicConcept) || !bodyPredicate.equals(AtomicConcept.INTERNAL_NAMED)) continue;
                    return false;
                }
            }
            if (predicate instanceof DataRange) {
                for (Atom bodyAtom : this.m_bodyAtoms) {
                    if (bodyAtom.getArity() != 2) continue;
                    return true;
                }
                return false;
            }
            if (!(predicate instanceof Role)) continue;
            return false;
        }
        return false;
    }

    public boolean isAtomicConceptInclusion() {
        if (this.m_bodyAtoms.length == 1 && this.m_headAtoms.length == 1) {
            Atom bodyAtom = this.m_bodyAtoms[0];
            Atom headAtom = this.m_headAtoms[0];
            if (bodyAtom.getArity() == 1 && headAtom.getArity() == 1 && bodyAtom.getDLPredicate() instanceof AtomicConcept && headAtom.getDLPredicate() instanceof AtomicConcept) {
                Term argument = bodyAtom.getArgument(0);
                return argument instanceof Variable && argument.equals(headAtom.getArgument(0));
            }
        }
        return false;
    }

    public boolean isAtomicRoleInclusion() {
        if (this.m_bodyAtoms.length == 1 && this.m_headAtoms.length == 1) {
            Atom bodyAtom = this.m_bodyAtoms[0];
            Atom headAtom = this.m_headAtoms[0];
            if (bodyAtom.getArity() == 2 && headAtom.getArity() == 2 && bodyAtom.getDLPredicate() instanceof AtomicRole && headAtom.getDLPredicate() instanceof AtomicRole) {
                Term argument0 = bodyAtom.getArgument(0);
                Term argument1 = bodyAtom.getArgument(1);
                return argument0 instanceof Variable && argument1 instanceof Variable && !argument0.equals(argument1) && argument0.equals(headAtom.getArgument(0)) && argument1.equals(headAtom.getArgument(1));
            }
        }
        return false;
    }

    public boolean isAtomicRoleInverseInclusion() {
        if (this.m_bodyAtoms.length == 1 && this.m_headAtoms.length == 1) {
            Atom bodyAtom = this.m_bodyAtoms[0];
            Atom headAtom = this.m_headAtoms[0];
            if (bodyAtom.getArity() == 2 && headAtom.getArity() == 2 && bodyAtom.getDLPredicate() instanceof AtomicRole && headAtom.getDLPredicate() instanceof AtomicRole) {
                Term argument0 = bodyAtom.getArgument(0);
                Term argument1 = bodyAtom.getArgument(1);
                return argument0 instanceof Variable && argument1 instanceof Variable && !argument0.equals(argument1) && argument0.equals(headAtom.getArgument(1)) && argument1.equals(headAtom.getArgument(0));
            }
        }
        return false;
    }

    public boolean isFunctionalityAxiom() {
        Variable x;
        DLPredicate atomicRole;
        if (this.m_bodyAtoms.length == 2 && this.m_headAtoms.length == 1 && (atomicRole = this.getBodyAtom(0).getDLPredicate()) instanceof AtomicRole && this.getBodyAtom(1).getDLPredicate().equals(atomicRole) && this.getHeadAtom(0).getDLPredicate() instanceof AnnotatedEquality && (x = this.getBodyAtom(0).getArgumentVariable(0)) != null && x.equals(this.getBodyAtom(1).getArgument(0))) {
            Variable y1 = this.getBodyAtom(0).getArgumentVariable(1);
            Variable y2 = this.getBodyAtom(1).getArgumentVariable(1);
            Variable headY1 = this.getHeadAtom(0).getArgumentVariable(0);
            Variable headY2 = this.getHeadAtom(0).getArgumentVariable(1);
            return y1 != null && y2 != null && !y1.equals(y2) && headY1 != null && headY2 != null && (y1.equals(headY1) && y2.equals(headY2) || y1.equals(headY2) && y2.equals(headY1));
        }
        return false;
    }

    public boolean isInverseFunctionalityAxiom() {
        Variable x;
        DLPredicate atomicRole;
        if (this.getBodyLength() == 2 && this.getHeadLength() == 1 && (atomicRole = this.getBodyAtom(0).getDLPredicate()) instanceof AtomicRole && this.getBodyAtom(1).getDLPredicate().equals(atomicRole) && this.getHeadAtom(0).getDLPredicate() instanceof AnnotatedEquality && (x = this.getBodyAtom(0).getArgumentVariable(1)) != null && x.equals(this.getBodyAtom(1).getArgument(1))) {
            Variable y1 = this.getBodyAtom(0).getArgumentVariable(0);
            Variable y2 = this.getBodyAtom(1).getArgumentVariable(0);
            Variable headY1 = this.getHeadAtom(0).getArgumentVariable(0);
            Variable headY2 = this.getHeadAtom(0).getArgumentVariable(1);
            return y1 != null && y2 != null && !y1.equals(y2) && headY1 != null && headY2 != null && (y1.equals(headY1) && y2.equals(headY2) || y1.equals(headY2) && y2.equals(headY1));
        }
        return false;
    }

    public String toString(Prefixes prefixes) {
        StringBuffer buffer = new StringBuffer();
        for (int headIndex = 0; headIndex < this.m_headAtoms.length; ++headIndex) {
            if (headIndex != 0) {
                buffer.append(" v ");
            }
            buffer.append(this.m_headAtoms[headIndex].toString(prefixes));
        }
        buffer.append(" :- ");
        for (int bodyIndex = 0; bodyIndex < this.m_bodyAtoms.length; ++bodyIndex) {
            if (bodyIndex != 0) {
                buffer.append(", ");
            }
            buffer.append(this.m_bodyAtoms[bodyIndex].toString(prefixes));
        }
        return buffer.toString();
    }

    public String toString() {
        return this.toString(Prefixes.STANDARD_PREFIXES);
    }

    public static DLClause create(Atom[] headAtoms, Atom[] bodyAtoms) {
        return s_interningManager.intern(new DLClause(headAtoms, bodyAtoms));
    }

}

