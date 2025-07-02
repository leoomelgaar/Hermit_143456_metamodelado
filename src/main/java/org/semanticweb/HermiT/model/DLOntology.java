package org.semanticweb.HermiT.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.semanticweb.HermiT.Prefixes;
import org.semanticweb.owlapi.model.OWLMetamodellingAxiom;

public class DLOntology
implements Serializable {
    private static final long serialVersionUID = 3189937959595369812L;
    protected static final String CRLF = "\n";
    protected final String m_ontologyIRI;
    protected final Set<DLClause> m_dlClauses;
    protected final Set<Atom> m_positiveFacts;
    protected final Set<Atom> m_negativeFacts;
    protected final Set<OWLMetamodellingAxiom> m_metamodellingAxioms;
    protected final boolean m_hasInverseRoles;
    protected final boolean m_hasAtMostRestrictions;
    protected final boolean m_hasNominals;
    protected final boolean m_hasDatatypes;
    protected final boolean m_isHorn;
    protected final Set<AtomicConcept> m_allAtomicConcepts;
    protected final int m_numberOfExternalConcepts;
    protected final Set<AtomicRole> m_allAtomicObjectRoles;
    protected final Set<Role> m_allComplexObjectRoles;
    protected final Set<AtomicRole> m_allAtomicDataRoles;
    protected final Set<DatatypeRestriction> m_allUnknownDatatypeRestrictions;
    protected final Set<String> m_definedDatatypeIRIs;
    protected final Set<Individual> m_allIndividuals;
    protected final Set<DescriptionGraph> m_allDescriptionGraphs;
    protected final Map<AtomicRole, Map<Individual, Set<Constant>>> m_dataPropertyAssertions;

    public DLOntology(String ontologyIRI, Set<DLClause> dlClauses, Set<Atom> positiveFacts, Set<Atom> negativeFacts, Set<AtomicConcept> atomicConcepts, Set<AtomicRole> atomicObjectRoles, Set<Role> allComplexObjectRoles, Set<AtomicRole> atomicDataRoles, Set<DatatypeRestriction> allUnknownDatatypeRestrictions, Set<String> definedDatatypeIRIs, Set<Individual> individuals, boolean hasInverseRoles, boolean hasAtMostRestrictions, boolean hasNominals, boolean hasDatatypes, Set<OWLMetamodellingAxiom> m_metamodellingAxioms) {
        int i;
        Term argument;
        this.m_ontologyIRI = ontologyIRI;
        this.m_dlClauses = dlClauses;
        this.m_positiveFacts = positiveFacts;
        this.m_negativeFacts = negativeFacts;
        this.m_metamodellingAxioms = m_metamodellingAxioms;
        this.m_hasInverseRoles = hasInverseRoles;
        this.m_hasAtMostRestrictions = hasAtMostRestrictions;
        this.m_hasNominals = hasNominals;
        this.m_hasDatatypes = hasDatatypes;
        this.m_allAtomicConcepts = atomicConcepts == null ? new TreeSet<AtomicConcept>(AtomicConceptComparator.INSTANCE) : atomicConcepts;
        int numberOfExternalConcepts = 0;
        for (AtomicConcept c : this.m_allAtomicConcepts) {
            if (Prefixes.isInternalIRI(c.getIRI())) continue;
            ++numberOfExternalConcepts;
        }
        this.m_numberOfExternalConcepts = numberOfExternalConcepts;
        this.m_allAtomicObjectRoles = atomicObjectRoles == null ? new TreeSet<AtomicRole>(AtomicRoleComparator.INSTANCE) : atomicObjectRoles;
        this.m_allComplexObjectRoles = allComplexObjectRoles == null ? new HashSet<Role>() : allComplexObjectRoles;
        this.m_allAtomicDataRoles = atomicDataRoles == null ? new TreeSet<AtomicRole>(AtomicRoleComparator.INSTANCE) : atomicDataRoles;
        this.m_allUnknownDatatypeRestrictions = allUnknownDatatypeRestrictions == null ? new HashSet<DatatypeRestriction>() : allUnknownDatatypeRestrictions;
        this.m_definedDatatypeIRIs = definedDatatypeIRIs == null ? new HashSet<String>() : definedDatatypeIRIs;
        this.m_allIndividuals = individuals == null ? new TreeSet<Individual>(IndividualComparator.INSTANCE) : individuals;
        this.m_allDescriptionGraphs = new HashSet<DescriptionGraph>();
        boolean isHorn = true;
        for (DLClause dlClause : this.m_dlClauses) {
            DLPredicate dlPredicate;
            if (dlClause.getHeadLength() > 1) {
                isHorn = false;
            }
            for (int bodyIndex = dlClause.getBodyLength() - 1; bodyIndex >= 0; --bodyIndex) {
                dlPredicate = dlClause.getBodyAtom(bodyIndex).getDLPredicate();
                this.addDLPredicate(dlPredicate);
            }
            for (int headIndex = dlClause.getHeadLength() - 1; headIndex >= 0; --headIndex) {
                dlPredicate = dlClause.getHeadAtom(headIndex).getDLPredicate();
                this.addDLPredicate(dlPredicate);
            }
        }
        this.m_isHorn = isHorn;
        this.m_dataPropertyAssertions = new HashMap<AtomicRole, Map<Individual, Set<Constant>>>();
        for (Atom atom : this.m_positiveFacts) {
            Term possibleConstant;
            Map<Individual,Set<Constant>> individualsToConstants;
            HashSet<Constant> constants2 = new HashSet<Constant>();
            this.addDLPredicate(atom.getDLPredicate());
            for (i = 0; i < atom.getArity(); ++i) {
                argument = atom.getArgument(i);
                if (!(argument instanceof Individual)) continue;
                this.m_allIndividuals.add((Individual)argument);
            }
            if (atom.getArity() != 2 || !((possibleConstant = atom.getArgument(1)) instanceof Constant)) continue;
            Individual sourceIndividual = (Individual)atom.getArgument(0);
            assert (atom.getDLPredicate() instanceof AtomicRole);
            AtomicRole atomicRole = (AtomicRole)atom.getDLPredicate();
            if (this.m_dataPropertyAssertions.containsKey(atomicRole)) {
                individualsToConstants = this.m_dataPropertyAssertions.get(atomicRole);
            } else {
                individualsToConstants = new HashMap();
                this.m_dataPropertyAssertions.put(atomicRole, individualsToConstants);
            }
            if (individualsToConstants.containsKey(sourceIndividual)) {
                Set constants3 = individualsToConstants.get(sourceIndividual);
            } else {
                individualsToConstants.put(sourceIndividual, constants2);
            }
            constants2.add((Constant)possibleConstant);
        }
        for (Atom atom : this.m_negativeFacts) {
            this.addDLPredicate(atom.getDLPredicate());
            for (i = 0; i < atom.getArity(); ++i) {
                argument = atom.getArgument(i);
                if (!(argument instanceof Individual)) continue;
                this.m_allIndividuals.add((Individual)argument);
            }
        }
        for (OWLMetamodellingAxiom metamodellingAxiom : m_metamodellingAxioms) {
        	this.m_allIndividuals.add(Individual.create(metamodellingAxiom.getMetamodelIndividual().toStringID()));
        }
    }

    protected void addDLPredicate(DLPredicate dlPredicate) {
        if (dlPredicate instanceof AtomicConcept) {
            this.m_allAtomicConcepts.add((AtomicConcept)dlPredicate);
        } else if (dlPredicate instanceof AtLeastConcept) {
            LiteralConcept literalConcept = ((AtLeastConcept)dlPredicate).getToConcept();
            if (literalConcept instanceof AtomicConcept) {
                this.m_allAtomicConcepts.add((AtomicConcept)literalConcept);
            }
        } else if (dlPredicate instanceof DescriptionGraph) {
            this.m_allDescriptionGraphs.add((DescriptionGraph)dlPredicate);
        } else if (dlPredicate instanceof ExistsDescriptionGraph) {
            this.m_allDescriptionGraphs.add(((ExistsDescriptionGraph)dlPredicate).getDescriptionGraph());
        }
    }

    public String getOntologyIRI() {
        return this.m_ontologyIRI;
    }

    public Set<AtomicConcept> getAllAtomicConcepts() {
        return this.m_allAtomicConcepts;
    }

    public boolean containsAtomicConcept(AtomicConcept concept) {
        return this.m_allAtomicConcepts.contains(concept);
    }

    public int getNumberOfExternalConcepts() {
        return this.m_numberOfExternalConcepts;
    }

    public Set<AtomicRole> getAllAtomicObjectRoles() {
        return this.m_allAtomicObjectRoles;
    }

    public boolean containsObjectRole(AtomicRole role) {
        return this.m_allAtomicObjectRoles.contains(role);
    }

    public Set<Role> getAllComplexObjectRoles() {
        return this.m_allComplexObjectRoles;
    }

    public boolean isComplexObjectRole(Role role) {
        return this.m_allComplexObjectRoles.contains(role);
    }

    public Set<AtomicRole> getAllAtomicDataRoles() {
        return this.m_allAtomicDataRoles;
    }

    public boolean containsDataRole(AtomicRole role) {
        return this.m_allAtomicDataRoles.contains(role);
    }

    public Set<DatatypeRestriction> getAllUnknownDatatypeRestrictions() {
        return this.m_allUnknownDatatypeRestrictions;
    }

    public Set<Individual> getAllIndividuals() {
        return this.m_allIndividuals;
    }

    public boolean containsIndividual(Individual individual) {
        return this.m_allIndividuals.contains(individual);
    }

    public Set<DescriptionGraph> getAllDescriptionGraphs() {
        return this.m_allDescriptionGraphs;
    }

    public Set<DLClause> getDLClauses() {
        return this.m_dlClauses;
    }

    public Set<Atom> getPositiveFacts() {
        return this.m_positiveFacts;
    }

    public Map<AtomicRole, Map<Individual, Set<Constant>>> getDataPropertyAssertions() {
        return this.m_dataPropertyAssertions;
    }

    public Set<Atom> getNegativeFacts() {
        return this.m_negativeFacts;
    }

    public Set<OWLMetamodellingAxiom> getMetamodellingAxioms() {
    	return this.m_metamodellingAxioms;
    }

    public boolean hasInverseRoles() {
        return this.m_hasInverseRoles;
    }

    public boolean hasAtMostRestrictions() {
        return this.m_hasAtMostRestrictions;
    }

    public boolean hasNominals() {
        return this.m_hasNominals;
    }

    public boolean hasDatatypes() {
        return this.m_hasDatatypes;
    }

    public boolean hasUnknownDatatypeRestrictions() {
        return !this.m_allUnknownDatatypeRestrictions.isEmpty();
    }

    public boolean isHorn() {
        return this.m_isHorn;
    }

    public Set<String> getDefinedDatatypeIRIs() {
        return this.m_definedDatatypeIRIs;
    }

    protected Set<AtomicConcept> getBodyOnlyAtomicConcepts() {
        HashSet<AtomicConcept> bodyOnlyAtomicConcepts = new HashSet<AtomicConcept>(this.m_allAtomicConcepts);
        for (DLClause dlClause : this.m_dlClauses) {
            for (int headIndex = 0; headIndex < dlClause.getHeadLength(); ++headIndex) {
                DLPredicate dlPredicate = dlClause.getHeadAtom(headIndex).getDLPredicate();
                bodyOnlyAtomicConcepts.remove(dlPredicate);
                if (!(dlPredicate instanceof AtLeastConcept)) continue;
                bodyOnlyAtomicConcepts.remove(((AtLeastConcept)dlPredicate).getToConcept());
            }
        }
        return bodyOnlyAtomicConcepts;
    }

    protected Set<AtomicRole> computeGraphAtomicRoles() {
        HashSet<AtomicRole> graphAtomicRoles = new HashSet<AtomicRole>();
        for (DescriptionGraph descriptionGraph : this.m_allDescriptionGraphs) {
            for (int edgeIndex = 0; edgeIndex < descriptionGraph.getNumberOfEdges(); ++edgeIndex) {
                DescriptionGraph.Edge edge = descriptionGraph.getEdge(edgeIndex);
                graphAtomicRoles.add(edge.getAtomicRole());
            }
        }
        boolean change = true;
        while (change) {
            change = false;
            for (DLClause dlClause : this.m_dlClauses) {
                if (!this.containsAtomicRoles(dlClause, graphAtomicRoles) || !this.addAtomicRoles(dlClause, graphAtomicRoles)) continue;
                change = true;
            }
        }
        return graphAtomicRoles;
    }

    protected boolean containsAtomicRoles(DLClause dlClause, Set<AtomicRole> roles) {
        DLPredicate dlPredicate;
        int atomIndex;
        for (atomIndex = 0; atomIndex < dlClause.getBodyLength(); ++atomIndex) {
            dlPredicate = dlClause.getBodyAtom(atomIndex).getDLPredicate();
            if (!(dlPredicate instanceof AtomicRole) || !roles.contains(dlPredicate)) continue;
            return true;
        }
        for (atomIndex = 0; atomIndex < dlClause.getHeadLength(); ++atomIndex) {
            dlPredicate = dlClause.getHeadAtom(atomIndex).getDLPredicate();
            if (!(dlPredicate instanceof AtomicRole) || !roles.contains(dlPredicate)) continue;
            return true;
        }
        return false;
    }

    protected boolean addAtomicRoles(DLClause dlClause, Set<AtomicRole> roles) {
        DLPredicate dlPredicate;
        int atomIndex;
        boolean change = false;
        for (atomIndex = 0; atomIndex < dlClause.getBodyLength(); ++atomIndex) {
            dlPredicate = dlClause.getBodyAtom(atomIndex).getDLPredicate();
            if (!(dlPredicate instanceof AtomicRole) || !roles.add((AtomicRole)dlPredicate)) continue;
            change = true;
        }
        for (atomIndex = 0; atomIndex < dlClause.getHeadLength(); ++atomIndex) {
            dlPredicate = dlClause.getHeadAtom(atomIndex).getDLPredicate();
            if (!(dlPredicate instanceof AtomicRole) || !roles.add((AtomicRole)dlPredicate)) continue;
            change = true;
        }
        return change;
    }

    public String toString(Prefixes prefixes) {
        StringBuilder stringBuffer = new StringBuilder("Prefixes: [").append(CRLF);
        for (Map.Entry<String, String> entry : prefixes.getPrefixIRIsByPrefixName().entrySet()) {
            stringBuffer.append("  ").append(entry.getKey()).append(" = <").append(entry.getValue()).append('>').append(CRLF);
        }
        stringBuffer.append("]").append(CRLF).append("Deterministic DL-clauses: [").append(CRLF);
        int numDeterministicClauses = 0;
        for (DLClause dlClause : this.m_dlClauses) {
            if (dlClause.getHeadLength() > 1) continue;
            ++numDeterministicClauses;
            stringBuffer.append("  ").append(dlClause.toString(prefixes)).append(CRLF);
        }
        stringBuffer.append("]").append(CRLF).append("Disjunctive DL-clauses: [").append(CRLF);
        int numNondeterministicClauses = 0;
        int numDisjunctions = 0;
        for (DLClause dlClause : this.m_dlClauses) {
            if (dlClause.getHeadLength() <= 1) continue;
            ++numNondeterministicClauses;
            numDisjunctions += dlClause.getHeadLength();
            stringBuffer.append("  ").append(dlClause.toString(prefixes)).append(CRLF);
        }
        stringBuffer.append("]").append(CRLF).append("ABox: [").append(CRLF);
        for (Atom atom : this.m_positiveFacts) {
            stringBuffer.append("  ").append(atom.toString(prefixes)).append(CRLF);
        }
        for (Atom atom : this.m_negativeFacts) {
            stringBuffer.append("  !").append(atom.toString(prefixes)).append(CRLF);
        }
        for (OWLMetamodellingAxiom metamodellingAxiom : this.m_metamodellingAxioms) {
            stringBuffer.append("  ").append("<"+metamodellingAxiom.getModelClass().toString()+", "+metamodellingAxiom.getMetamodelIndividual().toString()+">").append(CRLF);
        }
        stringBuffer.append("]").append(CRLF).append("Statistics: [").append(CRLF).append("  Number of deterministic clauses: " + numDeterministicClauses).append(CRLF).append("  Number of nondeterministic clauses: " + numNondeterministicClauses).append(CRLF).append("  Number of disjunctions: " + numDisjunctions).append(CRLF).append("  Number of positive facts: " + this.m_positiveFacts.size()).append(CRLF).append("  Number of negative facts: " + this.m_negativeFacts.size()).append(CRLF).append("]");
        return stringBuffer.toString();
    }

    public String getStatistics() {
        return this.getStatistics(null, null, null);
    }

    protected String getStatistics(Integer numDeterministicClauses, Integer numNondeterministicClauses, Integer numDisjunctions) {
        if (numDeterministicClauses == null || numNondeterministicClauses == null || numDisjunctions == null) {
            numDeterministicClauses = 0;
            numNondeterministicClauses = 0;
            numDisjunctions = 0;
            for (DLClause dlClause : this.m_dlClauses) {
                Integer n;
                Integer n2;
                if (dlClause.getHeadLength() <= 1) {
                    n = numDeterministicClauses;
                    n2 = numDeterministicClauses = Integer.valueOf(numDeterministicClauses + 1);
                    continue;
                }
                n = numNondeterministicClauses;
                n2 = numNondeterministicClauses = Integer.valueOf(numNondeterministicClauses + 1);
                numDisjunctions = numDisjunctions + dlClause.getHeadLength();
            }
        }
        String stringBuffer = "DL clauses statistics: [" + CRLF + "  Number of deterministic clauses: " + numDeterministicClauses + CRLF + "  Number of nondeterministic clauses: " + numNondeterministicClauses + CRLF + "  Overall number of disjunctions: " + numDisjunctions + CRLF + "  Number of positive facts: " + this.m_positiveFacts.size() + CRLF + "  Number of negative facts: " + this.m_negativeFacts.size() + CRLF + "  Inverses: " + this.hasInverseRoles() + CRLF + "  At-Mosts: " + this.hasAtMostRestrictions() + CRLF + "  Datatypes: " + this.hasDatatypes() + CRLF + "  Nominals: " + this.hasNominals() + CRLF + "  Number of atomic concepts: " + this.m_allAtomicConcepts.size() + CRLF + "  Number of object properties: " + this.m_allAtomicObjectRoles.size() + CRLF + "  Number of data properties: " + this.m_allAtomicDataRoles.size() + CRLF + "  Number of individuals: " + this.m_allIndividuals.size() + CRLF + "]";
        return stringBuffer;
    }

    public String toString() {
        return this.toString(Prefixes.STANDARD_PREFIXES);
    }

    public void save(OutputStream outputStream) throws IOException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(this);
        objectOutputStream.flush();
    }

    public static DLOntology load(InputStream inputStream) throws IOException {
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
            return (DLOntology)objectInputStream.readObject();
        }
        catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    public static class IndividualComparator
    implements Serializable,
    Comparator<Individual> {
        private static final long serialVersionUID = 2386841732225838685L;
        public static final Comparator<Individual> INSTANCE = new IndividualComparator();

        @Override
        public int compare(Individual o1, Individual o2) {
            return o1.getIRI().compareTo(o2.getIRI());
        }

        protected Object readResolve() {
            return INSTANCE;
        }
    }

    public static class AtomicRoleComparator
    implements Serializable,
    Comparator<AtomicRole> {
        private static final long serialVersionUID = 3483541702854959793L;
        public static final Comparator<AtomicRole> INSTANCE = new AtomicRoleComparator();

        @Override
        public int compare(AtomicRole o1, AtomicRole o2) {
            return o1.getIRI().compareTo(o2.getIRI());
        }

        protected Object readResolve() {
            return INSTANCE;
        }
    }

    public static class AtomicConceptComparator
    implements Serializable,
    Comparator<AtomicConcept> {
        private static final long serialVersionUID = 2386841732225838685L;
        public static final Comparator<AtomicConcept> INSTANCE = new AtomicConceptComparator();

        @Override
        public int compare(AtomicConcept o1, AtomicConcept o2) {
            return o1.getIRI().compareTo(o2.getIRI());
        }

        protected Object readResolve() {
            return INSTANCE;
        }
    }

}

