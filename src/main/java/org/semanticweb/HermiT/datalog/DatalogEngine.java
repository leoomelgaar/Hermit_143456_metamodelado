package org.semanticweb.HermiT.datalog;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.semanticweb.HermiT.existentials.ExistentialExpansionStrategy;
import org.semanticweb.HermiT.model.Atom;
import org.semanticweb.HermiT.model.AtomicRole;
import org.semanticweb.HermiT.model.Concept;
import org.semanticweb.HermiT.model.DLClause;
import org.semanticweb.HermiT.model.DLOntology;
import org.semanticweb.HermiT.model.DataRange;
import org.semanticweb.HermiT.model.Term;
import org.semanticweb.HermiT.model.Variable;
import org.semanticweb.HermiT.tableau.DLClauseEvaluator;
import org.semanticweb.HermiT.tableau.ExtensionManager;
import org.semanticweb.HermiT.tableau.InterruptFlag;
import org.semanticweb.HermiT.tableau.Node;
import org.semanticweb.HermiT.tableau.Tableau;

public final class DatalogEngine {
    private final InterruptFlag m_interruptFlag;
    private final DLOntology m_dlOntology;
    final Map<Term, Node> m_termsToNodes;
    final Map<Node, Term> m_nodesToTerms;
    private final Map<Term, Set<Term>> m_termsToEquivalenceClasses;
    private final Map<Term, Term> m_termsToRepresentatives;
    ExtensionManager m_extensionManager;

    public DatalogEngine(DLOntology dlOntology) {
        for (DLClause dlClause : dlOntology.getDLClauses()) {
            if (dlClause.getHeadLength() <= 1) continue;
            throw new IllegalArgumentException("The supplied DL ontology contains rules with disjunctive heads.");
        }
        this.m_interruptFlag = new InterruptFlag(0L);
        this.m_dlOntology = dlOntology;
        this.m_termsToNodes = new HashMap<Term, Node>();
        this.m_nodesToTerms = new HashMap<Node, Term>();
        this.m_termsToEquivalenceClasses = new HashMap<Term, Set<Term>>();
        this.m_termsToRepresentatives = new HashMap<Term, Term>();
    }

    public void interrupt() {
        this.m_interruptFlag.interrupt();
    }

    public boolean materialize() {
        if (this.m_extensionManager == null) {
            this.m_termsToNodes.clear();
            this.m_nodesToTerms.clear();
            this.m_termsToEquivalenceClasses.clear();
            this.m_termsToRepresentatives.clear();
            Tableau tableau = new Tableau(this.m_interruptFlag, null, NullExistentialExpansionStrategy.INSTANCE, false, this.m_dlOntology, null, new HashMap<String, Object>());
            Set<Atom> noAtoms = Collections.emptySet();
            tableau.isSatisfiable(true, false, noAtoms, noAtoms, noAtoms, noAtoms, this.m_termsToNodes, null, null);
            for (Map.Entry<Term, Node> entry : this.m_termsToNodes.entrySet()) {
                this.m_nodesToTerms.put(entry.getValue(), entry.getKey());
            }
            this.m_extensionManager = tableau.getExtensionManager();
            for (Node node = tableau.getFirstTableauNode(); node != null; node = node.getNextTableauNode()) {
                Term term = this.m_nodesToTerms.get(node);
                Term canonicalTerm = this.m_nodesToTerms.get(node.getCanonicalNode());
                Set<Term> equivalenceClass = this.m_termsToEquivalenceClasses.get(canonicalTerm);
                if (equivalenceClass == null) {
                    equivalenceClass = new HashSet<Term>();
                    this.m_termsToEquivalenceClasses.put(canonicalTerm, equivalenceClass);
                }
                if (!term.equals(canonicalTerm)) {
                    this.m_termsToEquivalenceClasses.put(term, equivalenceClass);
                }
                equivalenceClass.add(term);
                this.m_termsToRepresentatives.put(term, canonicalTerm);
            }
        }
        return !this.m_extensionManager.containsClash();
    }

    public DLOntology getDLOntology() {
        return this.m_dlOntology;
    }

    public Set<Term> getEquivalenceClass(Term term) {
        return this.m_termsToEquivalenceClasses.get(term);
    }

    public Term getRepresentative(Term term) {
        return this.m_termsToRepresentatives.get(term);
    }

    protected static class NullExistentialExpansionStrategy
    implements ExistentialExpansionStrategy {
        public static final ExistentialExpansionStrategy INSTANCE = new NullExistentialExpansionStrategy();

        protected NullExistentialExpansionStrategy() {
        }

        @Override
        public void initialize(Tableau tableau) {
        }

        @Override
        public void additionalDLOntologySet(DLOntology additionalDLOntology) {
        }

        @Override
        public void additionalDLOntologyCleared() {
        }

        @Override
        public void clear() {
        }

        @Override
        public boolean expandExistentials(boolean finalChance) {
            return false;
        }

        @Override
        public void assertionAdded(Concept concept, Node node, boolean isCore) {
        }

        @Override
        public void assertionAdded(DataRange dataRange, Node node, boolean isCore) {
        }

        @Override
        public void assertionCoreSet(Concept concept, Node node) {
        }

        @Override
        public void assertionCoreSet(DataRange dataRange, Node node) {
        }

        @Override
        public void assertionRemoved(Concept concept, Node node, boolean isCore) {
        }

        @Override
        public void assertionRemoved(DataRange dataRange, Node node, boolean isCore) {
        }

        @Override
        public void assertionAdded(AtomicRole atomicRole, Node nodeFrom, Node nodeTo, boolean isCore) {
        }

        @Override
        public void assertionCoreSet(AtomicRole atomicRole, Node nodeFrom, Node nodeTo) {
        }

        @Override
        public void assertionRemoved(AtomicRole atomicRole, Node nodeFrom, Node nodeTo, boolean isCore) {
        }

        @Override
        public void nodesMerged(Node mergeFrom, Node mergeInto) {
        }

        @Override
        public void nodesUnmerged(Node mergeFrom, Node mergeInto) {
        }

        @Override
        public void nodeStatusChanged(Node node) {
        }

        @Override
        public void nodeInitialized(Node node) {
        }

        @Override
        public void nodeDestroyed(Node node) {
        }

        @Override
        public void branchingPointPushed() {
        }

        @Override
        public void backtrack() {
        }

        @Override
        public void modelFound() {
        }

        @Override
        public boolean isDeterministic() {
            return true;
        }

        @Override
        public boolean isExact() {
            return true;
        }

        @Override
        public void dlClauseBodyCompiled(List<DLClauseEvaluator.Worker> workers, DLClause dlClause, List<Variable> variables, Object[] valuesBuffer, boolean[] coreVariables) {
        }
    }

}

