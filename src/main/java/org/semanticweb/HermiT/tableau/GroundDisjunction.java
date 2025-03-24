package org.semanticweb.HermiT.tableau;

import java.io.Serializable;
import org.semanticweb.HermiT.Prefixes;
import org.semanticweb.HermiT.model.AnnotatedEquality;
import org.semanticweb.HermiT.model.DLPredicate;
import org.semanticweb.HermiT.model.Equality;

public final class GroundDisjunction
implements Serializable {
    private static final long serialVersionUID = 6245673952732442673L;
    private final GroundDisjunctionHeader m_groundDisjunctionHeader;
    private final Node[] m_arguments;
    private final boolean[] m_isCore;
    private PermanentDependencySet m_dependencySet;
    GroundDisjunction m_previousGroundDisjunction;
    GroundDisjunction m_nextGroundDisjunction;

    public GroundDisjunction(Tableau tableau, GroundDisjunctionHeader groundDisjunctionHeader, Node[] arguments, boolean[] isCore, DependencySet dependencySet) {
        this.m_groundDisjunctionHeader = groundDisjunctionHeader;
        this.m_arguments = arguments;
        this.m_isCore = isCore;
        this.m_dependencySet = tableau.m_dependencySetFactory.getPermanent(dependencySet);
        tableau.m_dependencySetFactory.addUsage(this.m_dependencySet);
    }

    public GroundDisjunction getPreviousGroundDisjunction() {
        return this.m_previousGroundDisjunction;
    }

    public GroundDisjunction getNextGroundDisjunction() {
        return this.m_nextGroundDisjunction;
    }

    public void destroy(Tableau tableau) {
        tableau.m_dependencySetFactory.removeUsage(this.m_dependencySet);
        this.m_dependencySet = null;
    }

    public int getNumberOfDisjuncts() {
        return this.m_groundDisjunctionHeader.m_dlPredicates.length;
    }

    public DLPredicate getDLPredicate(int disjunctIndex) {
        return this.m_groundDisjunctionHeader.m_dlPredicates[disjunctIndex];
    }

    public Node getArgument(int disjunctIndex, int argumentIndex) {
        return this.m_arguments[this.m_groundDisjunctionHeader.m_disjunctStart[disjunctIndex] + argumentIndex];
    }

    public boolean isCore(int disjunctIndex) {
        return this.m_isCore[disjunctIndex];
    }

    public DependencySet getDependencySet() {
        return this.m_dependencySet;
    }

    public GroundDisjunctionHeader getGroundDisjunctionHeader() {
        return this.m_groundDisjunctionHeader;
    }

    public boolean isPruned() {
        for (int argumentIndex = this.m_arguments.length - 1; argumentIndex >= 0; --argumentIndex) {
            if (!this.m_arguments[argumentIndex].isPruned()) continue;
            return true;
        }
        return false;
    }

    public boolean isSatisfied(Tableau tableau) {
        ExtensionManager extensionManager = tableau.m_extensionManager;
        block5 : for (int disjunctIndex = 0; disjunctIndex < this.getNumberOfDisjuncts(); ++disjunctIndex) {
            DLPredicate dlPredicate = this.getDLPredicate(disjunctIndex);
            switch (dlPredicate.getArity()) {
                case 1: {
                    if (!extensionManager.containsAssertion(dlPredicate, this.getArgument(disjunctIndex, 0).getCanonicalNode())) continue block5;
                    return true;
                }
                case 2: {
                    if (!extensionManager.containsAssertion(dlPredicate, this.getArgument(disjunctIndex, 0).getCanonicalNode(), this.getArgument(disjunctIndex, 1).getCanonicalNode())) continue block5;
                    return true;
                }
                case 3: {
                    if (dlPredicate instanceof AnnotatedEquality) {
                        if (!ExtensionManager.containsAnnotatedEquality(this.getArgument(disjunctIndex, 0).getCanonicalNode(), this.getArgument(disjunctIndex, 1).getCanonicalNode(), this.getArgument(disjunctIndex, 2).getCanonicalNode())) continue block5;
                        return true;
                    }
                }
                default: {
                    throw new IllegalStateException("Invalid arity of DL-predicate.");
                }
            }
        }
        return false;
    }

    public boolean addDisjunctToTableau(Tableau tableau, int disjunctIndex, DependencySet dependencySet) {
        DLPredicate dlPredicate = this.getDLPredicate(disjunctIndex);
        switch (dlPredicate.getArity()) {
            case 1: {
                dependencySet = this.getArgument(disjunctIndex, 0).addCanonicalNodeDependencySet(dependencySet);
                return tableau.m_extensionManager.addAssertion(dlPredicate, this.getArgument(disjunctIndex, 0).getCanonicalNode(), dependencySet, this.isCore(disjunctIndex));
            }
            case 2: {
                dependencySet = this.getArgument(disjunctIndex, 0).addCanonicalNodeDependencySet(dependencySet);
                dependencySet = this.getArgument(disjunctIndex, 1).addCanonicalNodeDependencySet(dependencySet);
                return tableau.m_extensionManager.addAssertion(dlPredicate, this.getArgument(disjunctIndex, 0).getCanonicalNode(), this.getArgument(disjunctIndex, 1).getCanonicalNode(), dependencySet, this.isCore(disjunctIndex));
            }
            case 3: {
                if (!(dlPredicate instanceof AnnotatedEquality)) break;
                dependencySet = this.getArgument(disjunctIndex, 0).addCanonicalNodeDependencySet(dependencySet);
                dependencySet = this.getArgument(disjunctIndex, 1).addCanonicalNodeDependencySet(dependencySet);
                dependencySet = this.getArgument(disjunctIndex, 2).addCanonicalNodeDependencySet(dependencySet);
                return tableau.m_extensionManager.addAnnotatedEquality((AnnotatedEquality)dlPredicate, this.getArgument(disjunctIndex, 0).getCanonicalNode(), this.getArgument(disjunctIndex, 1).getCanonicalNode(), this.getArgument(disjunctIndex, 2).getCanonicalNode(), dependencySet);
            }
        }
        throw new IllegalStateException("Unsupported predicate arity.");
    }

    public String toString(Prefixes prefixes) {
        StringBuffer buffer = new StringBuffer();
        for (int disjunctIndex = 0; disjunctIndex < this.getNumberOfDisjuncts(); ++disjunctIndex) {
            DLPredicate dlPredicate;
            if (disjunctIndex != 0) {
                buffer.append(" v ");
            }
            if (Equality.INSTANCE.equals(dlPredicate = this.getDLPredicate(disjunctIndex))) {
                buffer.append(this.getArgument(disjunctIndex, 0).getNodeID());
                buffer.append(" == ");
                buffer.append(this.getArgument(disjunctIndex, 1).getNodeID());
                continue;
            }
            if (dlPredicate instanceof AnnotatedEquality) {
                AnnotatedEquality annotatedEquality = (AnnotatedEquality)dlPredicate;
                buffer.append('[');
                buffer.append(this.getArgument(disjunctIndex, 0).getNodeID());
                buffer.append(" == ");
                buffer.append(this.getArgument(disjunctIndex, 1).getNodeID());
                buffer.append("]@atMost(");
                buffer.append(annotatedEquality.getCaridnality());
                buffer.append(' ');
                buffer.append(annotatedEquality.getOnRole().toString(prefixes));
                buffer.append(' ');
                buffer.append(annotatedEquality.getToConcept().toString(prefixes));
                buffer.append(")(");
                buffer.append(this.getArgument(disjunctIndex, 2).getNodeID());
                buffer.append(')');
                continue;
            }
            buffer.append(dlPredicate.toString(prefixes));
            buffer.append('(');
            for (int argumentIndex = 0; argumentIndex < dlPredicate.getArity(); ++argumentIndex) {
                if (argumentIndex != 0) {
                    buffer.append(',');
                }
                buffer.append(this.getArgument(disjunctIndex, argumentIndex).getNodeID());
            }
            buffer.append(')');
        }
        return buffer.toString();
    }

    public String toString() {
        return this.toString(Prefixes.STANDARD_PREFIXES);
    }
}

