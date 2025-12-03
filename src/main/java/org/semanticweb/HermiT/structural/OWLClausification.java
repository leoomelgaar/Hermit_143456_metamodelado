package org.semanticweb.HermiT.structural;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Prefixes;
import org.semanticweb.HermiT.datatypes.DatatypeRegistry;
import org.semanticweb.HermiT.datatypes.UnsupportedDatatypeException;
import org.semanticweb.HermiT.model.AnnotatedEquality;
import org.semanticweb.HermiT.model.AtLeastConcept;
import org.semanticweb.HermiT.model.AtLeastDataRange;
import org.semanticweb.HermiT.model.Atom;
import org.semanticweb.HermiT.model.AtomicConcept;
import org.semanticweb.HermiT.model.AtomicRole;
import org.semanticweb.HermiT.model.Constant;
import org.semanticweb.HermiT.model.ConstantEnumeration;
import org.semanticweb.HermiT.model.DLClause;
import org.semanticweb.HermiT.model.DLOntology;
import org.semanticweb.HermiT.model.DatatypeRestriction;
import org.semanticweb.HermiT.model.DescriptionGraph;
import org.semanticweb.HermiT.model.Equality;
import org.semanticweb.HermiT.model.Individual;
import org.semanticweb.HermiT.model.Inequality;
import org.semanticweb.HermiT.model.InternalDatatype;
import org.semanticweb.HermiT.model.LiteralConcept;
import org.semanticweb.HermiT.model.LiteralDataRange;
import org.semanticweb.HermiT.model.NodeIDLessEqualThan;
import org.semanticweb.HermiT.model.NodeIDsAscendingOrEqual;
import org.semanticweb.HermiT.model.Role;
import org.semanticweb.HermiT.model.Term;
import org.semanticweb.HermiT.model.Variable;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiomVisitor;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitor;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataComplementOf;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataIntersectionOf;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataOneOf;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLDataUnionOf;
import org.semanticweb.owlapi.model.OWLDataVisitor;
import org.semanticweb.owlapi.model.OWLDataVisitorEx;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDatatypeRestriction;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLFacetRestriction;
import org.semanticweb.owlapi.model.OWLHasKeyAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLIndividualAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNegativeDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasSelf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectInverseOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLBuiltInAtom;
import org.semanticweb.owlapi.model.SWRLClassAtom;
import org.semanticweb.owlapi.model.SWRLDArgument;
import org.semanticweb.owlapi.model.SWRLDataPropertyAtom;
import org.semanticweb.owlapi.model.SWRLDataRangeAtom;
import org.semanticweb.owlapi.model.SWRLDifferentIndividualsAtom;
import org.semanticweb.owlapi.model.SWRLIArgument;
import org.semanticweb.owlapi.model.SWRLIndividualArgument;
import org.semanticweb.owlapi.model.SWRLLiteralArgument;
import org.semanticweb.owlapi.model.SWRLObjectPropertyAtom;
import org.semanticweb.owlapi.model.SWRLObjectVisitorEx;
import org.semanticweb.owlapi.model.SWRLRule;
import org.semanticweb.owlapi.model.SWRLSameIndividualAtom;
import org.semanticweb.owlapi.model.SWRLVariable;
import org.semanticweb.owlapi.util.OWLAxiomVisitorAdapter;

public class OWLClausification {
    protected static final Variable X = Variable.create("X");
    protected static final Variable Y = Variable.create("Y");
    protected static final Variable Z = Variable.create("Z");
    protected final Configuration m_configuration;

    public OWLClausification(Configuration configuration) {
        this.m_configuration = configuration;
    }

    public Object[] preprocessAndClausify(OWLOntology rootOntology, Collection<DescriptionGraph> descriptionGraphs) {
        OWLDataFactory factory = rootOntology.getOWLOntologyManager().getOWLDataFactory();
        Optional defaultDocumentIRI = rootOntology.getOntologyID().getDefaultDocumentIRI();
        String ontologyIRI = defaultDocumentIRI.isPresent() ? ((IRI)defaultDocumentIRI.get()).toString() : "urn:hermit:kb";
        Set<OWLOntology> importClosure = rootOntology.getImportsClosure();
        OWLAxioms axioms = new OWLAxioms();
        OWLNormalization normalization = new OWLNormalization(factory, axioms, 0);
        for (OWLOntology ontology : importClosure) {
            normalization.processOntology(ontology);
        }
        BuiltInPropertyManager builtInPropertyManager = new BuiltInPropertyManager(factory);
        builtInPropertyManager.axiomatizeBuiltInPropertiesAsNeeded(axioms);
        ObjectPropertyInclusionManager objectPropertyInclusionManager = new ObjectPropertyInclusionManager(axioms);
        objectPropertyInclusionManager.rewriteNegativeObjectPropertyAssertions(factory, axioms, normalization.m_definitions.size());
        objectPropertyInclusionManager.rewriteAxioms(factory, axioms, 0);
        if (descriptionGraphs == null) {
            descriptionGraphs = Collections.emptySet();
        }
        OWLAxiomsExpressivity axiomsExpressivity = new OWLAxiomsExpressivity(axioms);
        DLOntology dlOntology = this.clausify(factory, ontologyIRI, axioms, axiomsExpressivity, descriptionGraphs);

        return new Object[]{objectPropertyInclusionManager, dlOntology};
    }

    public DLOntology clausify(OWLDataFactory factory, String ontologyIRI, OWLAxioms axioms, OWLAxiomsExpressivity axiomsExpressivity, Collection<DescriptionGraph> descriptionGraphs) {
        Atom roleAtom;
        LinkedHashSet<DLClause> dlClauses = new LinkedHashSet<DLClause>();
        HashSet<Atom> positiveFacts = new HashSet<Atom>();
        HashSet<Atom> negativeFacts = new HashSet<Atom>();
        HashSet<DatatypeRestriction> allUnknownDatatypeRestrictions = new HashSet<DatatypeRestriction>();
        for (OWLObjectPropertyExpression[] inclusion2 : axioms.m_simpleObjectPropertyInclusions) {
            Atom subRoleAtom = OWLClausification.getRoleAtom(inclusion2[0], X, Y);
            Atom superRoleAtom = OWLClausification.getRoleAtom(inclusion2[1], X, Y);
            DLClause dlClause = DLClause.create(new Atom[]{superRoleAtom}, new Atom[]{subRoleAtom});
            dlClauses.add(dlClause);
        }
        for (OWLDataPropertyExpression[] inclusion : axioms.m_dataPropertyInclusions) {
            Atom subProp = OWLClausification.getRoleAtom(inclusion[0], X, Y);
            Atom superProp = OWLClausification.getRoleAtom(inclusion[1], X, Y);
            DLClause dlClause = DLClause.create(new Atom[]{superProp}, new Atom[]{subProp});
            dlClauses.add(dlClause);
        }
        for (OWLObjectPropertyExpression objectPropertyExpression : axioms.m_asymmetricObjectProperties) {
            roleAtom = OWLClausification.getRoleAtom(objectPropertyExpression, X, Y);
            Atom inverseRoleAtom = OWLClausification.getRoleAtom(objectPropertyExpression, Y, X);
            DLClause dlClause = DLClause.create(new Atom[0], new Atom[]{roleAtom, inverseRoleAtom});
            dlClauses.add(dlClause);
        }
        for (OWLObjectPropertyExpression objectPropertyExpression : axioms.m_reflexiveObjectProperties) {
            roleAtom = OWLClausification.getRoleAtom(objectPropertyExpression, X, X);
            Atom bodyAtom = Atom.create(AtomicConcept.THING, X);
            DLClause dlClause = DLClause.create(new Atom[]{roleAtom}, new Atom[]{bodyAtom});
            dlClauses.add(dlClause);
        }
        for (OWLObjectPropertyExpression objectPropertyExpression : axioms.m_irreflexiveObjectProperties) {
            roleAtom = OWLClausification.getRoleAtom(objectPropertyExpression, X, X);
            DLClause dlClause2 = DLClause.create(new Atom[0], new Atom[]{roleAtom});
            dlClauses.add(dlClause2);
        }
        for (OWLObjectPropertyExpression[] properties : axioms.m_disjointObjectProperties) {
            for (int i = 0; i < properties.length; ++i) {
                int j = i + 1;
                while (++j < properties.length) {
                    Atom atom_i = OWLClausification.getRoleAtom(properties[i], X, Y);
                    Atom atom_j = OWLClausification.getRoleAtom(properties[j], X, Y);
                    DLClause dlClause3 = DLClause.create(new Atom[0], new Atom[]{atom_i, atom_j});
                    dlClauses.add(dlClause3);
                }
            }
        }
        if (OWLClausification.contains(axioms, factory.getOWLBottomDataProperty())) {
            Atom bodyAtom = Atom.create(AtomicRole.BOTTOM_DATA_ROLE, X, Y);
            dlClauses.add(DLClause.create(new Atom[0], new Atom[]{bodyAtom}));
        }
        for (OWLDataPropertyExpression[] properties : axioms.m_disjointDataProperties) {
            for (int i = 0; i < properties.length; ++i) {
                int j = i + 1;
                while (++j < properties.length) {
                    Atom atom_i = OWLClausification.getRoleAtom(properties[i], X, Y);
                    Atom atom_j2 = OWLClausification.getRoleAtom(properties[j], X, Z);
                    Object atom_ij = Atom.create(Inequality.create(), Y, Z);
                    DLClause dlClause4 = DLClause.create(new Atom[]{(Atom)atom_ij}, new Atom[]{atom_i, atom_j2});
                    dlClauses.add(dlClause4);
                }
            }
        }
        DataRangeConverter dataRangeConverter = new DataRangeConverter(this.m_configuration.warningMonitor, axioms.m_definedDatatypesIRIs, allUnknownDatatypeRestrictions, this.m_configuration.ignoreUnsupportedDatatypes);
        NormalizedAxiomClausifier clausifier = new NormalizedAxiomClausifier(dataRangeConverter, positiveFacts);
        for (OWLClassExpression[] inclusion3 : axioms.m_conceptInclusions) {
            for (OWLClassExpression description : inclusion3) {
                description.accept(clausifier);
            }
            DLClause dlClause = clausifier.getDLClause();
            dlClauses.add(dlClause.getSafeVersion(AtomicConcept.THING));
        }
        NormalizedDataRangeAxiomClausifier normalizedDataRangeAxiomClausifier = new NormalizedDataRangeAxiomClausifier(dataRangeConverter, factory, axioms.m_definedDatatypesIRIs);
        for (OWLDataRange[] inclusion4 : axioms.m_dataRangeInclusions) {
            for (OWLDataRange description2 : inclusion4) {
                description2.accept(normalizedDataRangeAxiomClausifier);
            }
            DLClause dlClause5 = normalizedDataRangeAxiomClausifier.getDLClause();
            dlClauses.add(dlClause5.getSafeVersion(InternalDatatype.RDFS_LITERAL));
        }
        for (OWLHasKeyAxiom hasKey : axioms.m_hasKeys) {
            dlClauses.add(this.clausifyKey(hasKey));
        }
        FactClausifier factClausifier = new FactClausifier(dataRangeConverter, positiveFacts, negativeFacts);
        for (OWLIndividualAxiom fact : axioms.m_facts) {
            fact.accept(factClausifier);
        }
        for (DescriptionGraph descriptionGraph : descriptionGraphs) {
            descriptionGraph.produceStartDLClauses(dlClauses);
        }
        HashSet<AtomicConcept> atomicConcepts = new HashSet<AtomicConcept>();
        HashSet<AtomicRole> atomicObjectRoles = new HashSet<AtomicRole>();
        HashSet<Role> complexObjectRoles = new HashSet<Role>();
        HashSet<AtomicRole> atomicDataRoles = new HashSet<AtomicRole>();
        for (OWLClass owlClass : axioms.m_classes) {
            atomicConcepts.add(AtomicConcept.create(owlClass.getIRI().toString()));
        }
        HashSet<Individual> individuals = new HashSet<Individual>();
        for (OWLNamedIndividual owlIndividual : axioms.m_namedIndividuals) {
            Individual individual = Individual.create(owlIndividual.getIRI().toString());
            individuals.add(individual);
            if (axioms.m_hasKeys.isEmpty() && axioms.m_rules.isEmpty()) continue;
            positiveFacts.add(Atom.create(AtomicConcept.INTERNAL_NAMED, individual));
        }
        for (OWLObjectProperty objectProperty : axioms.m_objectProperties) {
            atomicObjectRoles.add(AtomicRole.create(objectProperty.getIRI().toString()));
        }
        for (OWLObjectPropertyExpression objectPropertyExpression : axioms.m_complexObjectPropertyExpressions) {
            complexObjectRoles.add(OWLClausification.getRole(objectPropertyExpression));
        }
        for (OWLDataProperty dataProperty : axioms.m_dataProperties) {
            atomicDataRoles.add(AtomicRole.create(dataProperty.getIRI().toString()));
        }
        if (!axioms.m_rules.isEmpty()) {
            new NormalizedRuleClausifier(axioms.m_objectPropertiesOccurringInOWLAxioms, descriptionGraphs, dataRangeConverter, dlClauses).processRules(axioms.m_rules);
        }
        return new DLOntology(ontologyIRI, dlClauses, positiveFacts, negativeFacts, atomicConcepts, atomicObjectRoles, complexObjectRoles, atomicDataRoles, allUnknownDatatypeRestrictions, axioms.m_definedDatatypesIRIs, individuals, axiomsExpressivity.m_hasInverseRoles, axiomsExpressivity.m_hasAtMostRestrictions, axiomsExpressivity.m_hasNominals, axiomsExpressivity.m_hasDatatypes, axioms.m_metamodellingAxioms, axioms.m_metaRuleAxioms);
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    protected DLClause clausifyKey(OWLHasKeyAxiom object) {
        Variable y;
        ArrayList<Atom> headAtoms = new ArrayList<Atom>();
        ArrayList<Atom> bodyAtoms = new ArrayList<Atom>();
        Variable X2 = Variable.create("X2");
        Variable X1 = Variable.create("X1");
        headAtoms.add(Atom.create(Equality.INSTANCE, X1, X2));
        bodyAtoms.add(Atom.create(AtomicConcept.INTERNAL_NAMED, X1));
        bodyAtoms.add(Atom.create(AtomicConcept.INTERNAL_NAMED, X2));
        OWLClassExpression description = object.getClassExpression();
        if (description instanceof OWLClass) {
            OWLClass owlClass = (OWLClass)description;
            if (!owlClass.isOWLThing()) {
                bodyAtoms.add(Atom.create(AtomicConcept.create(owlClass.getIRI().toString()), X1));
                bodyAtoms.add(Atom.create(AtomicConcept.create(owlClass.getIRI().toString()), X2));
            }
        } else {
            if (!(description instanceof OWLObjectComplementOf)) throw new IllegalStateException("Internal error: invalid normal form.");
            OWLClassExpression internal = ((OWLObjectComplementOf)description).getOperand();
            if (!(internal instanceof OWLClass)) throw new IllegalStateException("Internal error: invalid normal form.");
            OWLClass owlClass = (OWLClass)internal;
            headAtoms.add(Atom.create(AtomicConcept.create(owlClass.getIRI().toString()), X1));
            headAtoms.add(Atom.create(AtomicConcept.create(owlClass.getIRI().toString()), X2));
        }
        int yIndex = 1;
        for (OWLObjectPropertyExpression p : object.getObjectPropertyExpressions()) {
            y = Variable.create("Y" + yIndex);
            ++yIndex;
            bodyAtoms.add(OWLClausification.getRoleAtom(p, X1, y));
            bodyAtoms.add(OWLClausification.getRoleAtom(p, X2, y));
            bodyAtoms.add(Atom.create(AtomicConcept.INTERNAL_NAMED, y));
        }
        for (OWLDataPropertyExpression d : object.getDataPropertyExpressions()) {
            y = Variable.create("Y" + yIndex);
            bodyAtoms.add(OWLClausification.getRoleAtom(d, X1, y));
            Variable y2 = Variable.create("Y" + ++yIndex);
            ++yIndex;
            bodyAtoms.add(OWLClausification.getRoleAtom(d, X2, y2));
            headAtoms.add(Atom.create(Inequality.INSTANCE, y, y2));
        }
        Atom[] hAtoms = new Atom[headAtoms.size()];
        headAtoms.toArray(hAtoms);
        Atom[] bAtoms = new Atom[bodyAtoms.size()];
        bodyAtoms.toArray(bAtoms);
        return DLClause.create(hAtoms, bAtoms);
    }

    private static boolean contains(OWLAxioms axioms, OWLDataProperty p) {
        for (OWLDataPropertyExpression[] e : axioms.m_dataPropertyInclusions) {
            for (OWLDataPropertyExpression candidate : e) {
                if (!candidate.equals(p)) continue;
                return true;
            }
        }
        return false;
    }

    protected static LiteralConcept getLiteralConcept(OWLClassExpression description) {
        if (description instanceof OWLClass) {
            return AtomicConcept.create(((OWLClass)description).getIRI().toString());
        }
        if (description instanceof OWLObjectComplementOf) {
            OWLClassExpression internal = ((OWLObjectComplementOf)description).getOperand();
            if (!(internal instanceof OWLClass)) {
                throw new IllegalStateException("Internal error: invalid normal form.");
            }
            return AtomicConcept.create(((OWLClass)internal).getIRI().toString()).getNegation();
        }
        throw new IllegalStateException("Internal error: invalid normal form.");
    }

    protected static Role getRole(OWLObjectPropertyExpression objectPropertyExpression) {
        if (objectPropertyExpression instanceof OWLObjectProperty) {
            return AtomicRole.create(((OWLObjectProperty)objectPropertyExpression).getIRI().toString());
        }
        if (objectPropertyExpression instanceof OWLObjectInverseOf) {
            OWLObjectPropertyExpression internal = ((OWLObjectInverseOf)objectPropertyExpression).getInverse();
            if (!(internal instanceof OWLObjectProperty)) {
                throw new IllegalStateException("Internal error: invalid normal form.");
            }
            return AtomicRole.create(((OWLObjectProperty)internal).getIRI().toString()).getInverse();
        }
        throw new IllegalStateException("Internal error: invalid normal form.");
    }

    protected static AtomicRole getAtomicRole(OWLDataPropertyExpression dataPropertyExpression) {
        return AtomicRole.create(((OWLDataProperty)dataPropertyExpression).getIRI().toString());
    }

    protected static Atom getRoleAtom(OWLObjectPropertyExpression objectProperty, Term first, Term second) {
        if (!objectProperty.isAnonymous()) {
            AtomicRole role = AtomicRole.create(objectProperty.asOWLObjectProperty().getIRI().toString());
            return Atom.create(role, first, second);
        }
        if (objectProperty.isAnonymous()) {
            OWLObjectProperty internalObjectProperty = objectProperty.getNamedProperty();
            AtomicRole role = AtomicRole.create(internalObjectProperty.getIRI().toString());
            return Atom.create(role, second, first);
        }
        throw new IllegalStateException("Internal error: unsupported type of object property!");
    }

    protected static Atom getRoleAtom(OWLDataPropertyExpression dataProperty, Term first, Term second) {
        if (dataProperty instanceof OWLDataProperty) {
            AtomicRole property = AtomicRole.create(((OWLDataProperty)dataProperty).getIRI().toString());
            return Atom.create(property, first, second);
        }
        throw new IllegalStateException("Internal error: unsupported type of data property!");
    }

    protected static Individual getIndividual(OWLIndividual individual) {
        if (individual.isAnonymous()) {
            return Individual.createAnonymous(individual.asOWLAnonymousIndividual().getID().toString());
        }
        return Individual.create(individual.asOWLNamedIndividual().getIRI().toString());
    }

    protected static final class NormalizedRuleClausifier
    implements SWRLObjectVisitorEx<Atom> {
        private final Set<OWLObjectProperty> m_objectPropertiesOccurringInOWLAxioms;
        private final DataRangeConverter m_dataRangeConverter;
        private final Set<DLClause> m_dlClauses;
        private final List<Atom> m_headAtoms;
        private final List<Atom> m_bodyAtoms;
        private final Set<Variable> m_abstractVariables;
        private final Set<OWLObjectProperty> m_graphObjectProperties = new HashSet<OWLObjectProperty>();
        private boolean m_containsObjectProperties;
        private boolean m_containsGraphObjectProperties;
        private boolean m_containsNonGraphObjectProperties;
        private boolean m_containsUndeterminedObjectProperties;

        public NormalizedRuleClausifier(Set<OWLObjectProperty> objectPropertiesOccurringInOWLAxioms, Collection<DescriptionGraph> descriptionGraphs, DataRangeConverter dataRangeConverter, Set<DLClause> dlClauses) {
            this.m_objectPropertiesOccurringInOWLAxioms = objectPropertiesOccurringInOWLAxioms;
            this.m_dataRangeConverter = dataRangeConverter;
            this.m_dlClauses = dlClauses;
            this.m_headAtoms = new ArrayList<Atom>();
            this.m_bodyAtoms = new ArrayList<Atom>();
            this.m_abstractVariables = new HashSet<Variable>();
            OWLDataFactory factory = OWLManager.getOWLDataFactory();
            for (DescriptionGraph descriptionGraph : descriptionGraphs) {
                for (int i = 0; i < descriptionGraph.getNumberOfEdges(); ++i) {
                    this.m_graphObjectProperties.add(factory.getOWLObjectProperty(IRI.create(descriptionGraph.getEdge(i).getAtomicRole().getIRI())));
                }
            }
            for (OWLObjectProperty objectProperty : this.m_graphObjectProperties) {
                if (!objectPropertiesOccurringInOWLAxioms.contains(objectProperty)) continue;
                throw new IllegalArgumentException("Mixing graph and non-graph object properties is not supported.");
            }
        }

        public void processRules(Collection<OWLAxioms.DisjunctiveRule> rules) {
            ArrayList<OWLAxioms.DisjunctiveRule> unprocessedRules = new ArrayList<OWLAxioms.DisjunctiveRule>(rules);
            boolean changed = true;
            while (!unprocessedRules.isEmpty() && changed) {
                changed = false;
                Iterator iterator = unprocessedRules.iterator();
                while (iterator.hasNext()) {
                    OWLAxioms.DisjunctiveRule rule = (OWLAxioms.DisjunctiveRule)iterator.next();
                    this.determineRuleType(rule);
                    if (this.m_containsGraphObjectProperties && this.m_containsNonGraphObjectProperties) {
                        throw new IllegalArgumentException("A SWRL rule mixes graph and non-graph object properties, which is not supported.");
                    }
                    this.determineUndeterminedObjectProperties(rule);
                    if (this.m_containsUndeterminedObjectProperties) continue;
                    iterator.remove();
                    this.clausify(rule, this.m_containsNonGraphObjectProperties || !this.m_containsObjectProperties);
                    changed = true;
                }
            }
            this.m_containsObjectProperties = false;
            this.m_containsGraphObjectProperties = false;
            this.m_containsNonGraphObjectProperties = true;
            this.m_containsUndeterminedObjectProperties = false;
            for (OWLAxioms.DisjunctiveRule rule : unprocessedRules) {
                this.determineUndeterminedObjectProperties(rule);
                this.clausify(rule, true);
            }
        }

        private void determineRuleType(OWLAxioms.DisjunctiveRule rule) {
            this.m_containsObjectProperties = false;
            this.m_containsGraphObjectProperties = false;
            this.m_containsNonGraphObjectProperties = false;
            this.m_containsUndeterminedObjectProperties = false;
            for (SWRLAtom atom : rule.m_body) {
                this.checkRuleAtom(atom);
            }
            for (SWRLAtom atom : rule.m_head) {
                this.checkRuleAtom(atom);
            }
        }

        private void checkRuleAtom(SWRLAtom atom) {
            if (atom instanceof SWRLObjectPropertyAtom) {
                this.m_containsObjectProperties = true;
                OWLObjectProperty objectProperty = ((SWRLObjectPropertyAtom)atom).getPredicate().getNamedProperty();
                boolean isGraphObjectProperty = this.m_graphObjectProperties.contains(objectProperty);
                boolean isNonGraphObjectProperty = this.m_objectPropertiesOccurringInOWLAxioms.contains(objectProperty);
                if (isGraphObjectProperty) {
                    this.m_containsGraphObjectProperties = true;
                }
                if (isNonGraphObjectProperty) {
                    this.m_containsNonGraphObjectProperties = true;
                }
                if (!isGraphObjectProperty && !isNonGraphObjectProperty) {
                    this.m_containsUndeterminedObjectProperties = true;
                }
            }
        }

        private void determineUndeterminedObjectProperties(OWLAxioms.DisjunctiveRule rule) {
            if (this.m_containsUndeterminedObjectProperties) {
                if (this.m_containsGraphObjectProperties) {
                    for (SWRLAtom atom : rule.m_body) {
                        this.makeGraphObjectProperty(atom);
                    }
                    for (SWRLAtom atom : rule.m_head) {
                        this.makeGraphObjectProperty(atom);
                    }
                    this.m_containsUndeterminedObjectProperties = false;
                } else if (this.m_containsNonGraphObjectProperties) {
                    for (SWRLAtom atom : rule.m_body) {
                        this.makeNonGraphObjectProperty(atom);
                    }
                    for (SWRLAtom atom : rule.m_head) {
                        this.makeNonGraphObjectProperty(atom);
                    }
                    this.m_containsUndeterminedObjectProperties = false;
                }
            }
        }

        private void makeGraphObjectProperty(SWRLAtom atom) {
            if (atom instanceof SWRLObjectPropertyAtom) {
                OWLObjectProperty objectProperty = ((SWRLObjectPropertyAtom)atom).getPredicate().getNamedProperty();
                this.m_graphObjectProperties.add(objectProperty);
            }
        }

        private void makeNonGraphObjectProperty(SWRLAtom atom) {
            if (atom instanceof SWRLObjectPropertyAtom) {
                OWLObjectProperty objectProperty = ((SWRLObjectPropertyAtom)atom).getPredicate().getNamedProperty();
                this.m_objectPropertiesOccurringInOWLAxioms.add(objectProperty);
            }
        }

        private void clausify(OWLAxioms.DisjunctiveRule rule, boolean restrictToNamed) {
            this.m_headAtoms.clear();
            this.m_bodyAtoms.clear();
            this.m_abstractVariables.clear();
            for (SWRLAtom atom : rule.m_body) {
                this.m_bodyAtoms.add((Atom)atom.accept((SWRLObjectVisitorEx)this));
            }
            for (SWRLAtom atom : rule.m_head) {
                this.m_headAtoms.add((Atom)atom.accept((SWRLObjectVisitorEx)this));
            }
            if (restrictToNamed) {
                for (Variable variable : this.m_abstractVariables) {
                    this.m_bodyAtoms.add(Atom.create(AtomicConcept.INTERNAL_NAMED, variable));
                }
            }
            DLClause dlClause = DLClause.create(this.m_headAtoms.toArray(new Atom[this.m_headAtoms.size()]), this.m_bodyAtoms.toArray(new Atom[this.m_bodyAtoms.size()]));
            this.m_dlClauses.add(dlClause);
            this.m_headAtoms.clear();
            this.m_bodyAtoms.clear();
            this.m_abstractVariables.clear();
        }

        public Atom visit(SWRLClassAtom atom) {
            if (atom.getPredicate().isAnonymous()) {
                throw new IllegalStateException("Internal error: SWRL rule class atoms should be normalized to contain only named classes, but this class atom has a complex concept: " + atom.getPredicate());
            }
            Variable variable = NormalizedRuleClausifier.toVariable(atom.getArgument());
            this.m_abstractVariables.add(variable);
            return Atom.create(AtomicConcept.create(atom.getPredicate().asOWLClass().getIRI().toString()), variable);
        }

        public Atom visit(SWRLDataRangeAtom atom) {
            Variable variable = NormalizedRuleClausifier.toVariable(atom.getArgument());
            LiteralDataRange literalRange = this.m_dataRangeConverter.convertDataRange(atom.getPredicate());
            return Atom.create(literalRange, variable);
        }

        public Atom visit(SWRLObjectPropertyAtom atom) {
            Variable variable1 = NormalizedRuleClausifier.toVariable(atom.getFirstArgument());
            Variable variable2 = NormalizedRuleClausifier.toVariable(atom.getSecondArgument());
            this.m_abstractVariables.add(variable1);
            this.m_abstractVariables.add(variable2);
            return OWLClausification.getRoleAtom(atom.getPredicate().asOWLObjectProperty(), variable1, variable2);
        }

        public Atom visit(SWRLDataPropertyAtom atom) {
            Variable variable1 = NormalizedRuleClausifier.toVariable(atom.getFirstArgument());
            Variable variable2 = NormalizedRuleClausifier.toVariable(atom.getSecondArgument());
            this.m_abstractVariables.add(variable1);
            return OWLClausification.getRoleAtom(atom.getPredicate().asOWLDataProperty(), variable1, variable2);
        }

        public Atom visit(SWRLSameIndividualAtom atom) {
            Variable variable1 = NormalizedRuleClausifier.toVariable(atom.getFirstArgument());
            Variable variable2 = NormalizedRuleClausifier.toVariable(atom.getSecondArgument());
            return Atom.create(Equality.INSTANCE, variable1, variable2);
        }

        public Atom visit(SWRLDifferentIndividualsAtom atom) {
            Variable variable1 = NormalizedRuleClausifier.toVariable(atom.getFirstArgument());
            Variable variable2 = NormalizedRuleClausifier.toVariable(atom.getSecondArgument());
            return Atom.create(Inequality.INSTANCE, variable1, variable2);
        }

        public Atom visit(SWRLBuiltInAtom node) {
            throw new UnsupportedOperationException("Rules with SWRL built-in atoms are not yet supported. ");
        }

        public Atom visit(SWRLRule rule) {
            throw new IllegalStateException("Internal error: this part of the code is unused.");
        }

        public Atom visit(SWRLVariable node) {
            throw new IllegalStateException("Internal error: this part of the code is unused.");
        }

        public Atom visit(SWRLIndividualArgument atom) {
            throw new IllegalStateException("Internal error: this part of the code is unused.");
        }

        public Atom visit(SWRLLiteralArgument arg) {
            throw new IllegalStateException("Internal error: this part of the code is unused.");
        }

        private static Variable toVariable(SWRLIArgument argument) {
            if (argument instanceof SWRLVariable) {
                return Variable.create(((SWRLVariable)argument).getIRI().toString());
            }
            throw new IllegalStateException("Internal error: all arguments in a SWRL rule should have been normalized to variables.");
        }

        private static Variable toVariable(SWRLDArgument argument) {
            if (argument instanceof SWRLVariable) {
                return Variable.create(((SWRLVariable)argument).getIRI().toString());
            }
            throw new IllegalStateException("Internal error: all arguments in a SWRL rule should have been normalized to variables.");
        }
    }

    protected static class FactClausifier
    extends OWLAxiomVisitorAdapter {
        protected final DataRangeConverter m_dataRangeConverter;
        protected final Set<Atom> m_positiveFacts;
        protected final Set<Atom> m_negativeFacts;

        public FactClausifier(DataRangeConverter dataRangeConverter, Set<Atom> positiveFacts, Set<Atom> negativeFacts) {
            this.m_dataRangeConverter = dataRangeConverter;
            this.m_positiveFacts = positiveFacts;
            this.m_negativeFacts = negativeFacts;
        }

        public void visit(OWLSameIndividualAxiom object) {
            OWLIndividual[] individuals = new OWLIndividual[object.getIndividuals().size()];
            object.getIndividuals().toArray(individuals);
            for (int i = 0; i < individuals.length - 1; ++i) {
                this.m_positiveFacts.add(Atom.create(Equality.create(), OWLClausification.getIndividual(individuals[i]), OWLClausification.getIndividual(individuals[i + 1])));
            }
        }

        public void visit(OWLDifferentIndividualsAxiom object) {
            OWLIndividual[] individuals = new OWLIndividual[object.getIndividuals().size()];
            object.getIndividuals().toArray(individuals);
            for (int i = 0; i < individuals.length; ++i) {
                for (int j = i + 1; j < individuals.length; ++j) {
                    this.m_positiveFacts.add(Atom.create(Inequality.create(), OWLClausification.getIndividual(individuals[i]), OWLClausification.getIndividual(individuals[j])));
                }
            }
        }

        public void visit(OWLClassAssertionAxiom object) {
            OWLClassExpression description = object.getClassExpression();
            if (description instanceof OWLClass) {
                AtomicConcept atomicConcept = AtomicConcept.create(((OWLClass)description).getIRI().toString());
                this.m_positiveFacts.add(Atom.create(atomicConcept, OWLClausification.getIndividual(object.getIndividual())));
            } else if (description instanceof OWLObjectComplementOf && ((OWLObjectComplementOf)description).getOperand() instanceof OWLClass) {
                AtomicConcept atomicConcept = AtomicConcept.create(((OWLClass)((OWLObjectComplementOf)description).getOperand()).getIRI().toString());
                this.m_negativeFacts.add(Atom.create(atomicConcept, OWLClausification.getIndividual(object.getIndividual())));
            } else if (description instanceof OWLObjectHasSelf) {
                OWLObjectHasSelf self = (OWLObjectHasSelf)description;
                this.m_positiveFacts.add(OWLClausification.getRoleAtom(self.getProperty(), OWLClausification.getIndividual(object.getIndividual()), OWLClausification.getIndividual(object.getIndividual())));
            } else if (description instanceof OWLObjectComplementOf && ((OWLObjectComplementOf)description).getOperand() instanceof OWLObjectHasSelf) {
                OWLObjectHasSelf self = (OWLObjectHasSelf)((OWLObjectComplementOf)description).getOperand();
                this.m_negativeFacts.add(OWLClausification.getRoleAtom(self.getProperty(), OWLClausification.getIndividual(object.getIndividual()), OWLClausification.getIndividual(object.getIndividual())));
            } else {
                throw new IllegalStateException("Internal error: invalid normal form.");
            }
        }

        public void visit(OWLObjectPropertyAssertionAxiom object) {
            this.m_positiveFacts.add(OWLClausification.getRoleAtom(object.getProperty(), OWLClausification.getIndividual(object.getSubject()), OWLClausification.getIndividual(object.getObject())));
        }

        public void visit(OWLNegativeObjectPropertyAssertionAxiom object) {
            this.m_negativeFacts.add(OWLClausification.getRoleAtom(object.getProperty(), OWLClausification.getIndividual(object.getSubject()), OWLClausification.getIndividual(object.getObject())));
        }

        public void visit(OWLDataPropertyAssertionAxiom object) {
            Constant targetValue = (Constant) object.getObject().accept(this.m_dataRangeConverter);
            this.m_positiveFacts.add(OWLClausification.getRoleAtom(object.getProperty(), OWLClausification.getIndividual(object.getSubject()), targetValue));
        }

        public void visit(OWLNegativeDataPropertyAssertionAxiom object) {
            Constant targetValue = (Constant) object.getObject().accept(this.m_dataRangeConverter);
            this.m_negativeFacts.add(OWLClausification.getRoleAtom(object.getProperty(), OWLClausification.getIndividual(object.getSubject()), targetValue));
        }
    }

    protected static class DataRangeConverter
    implements OWLDataVisitorEx<Object> {
        protected final Configuration.WarningMonitor m_warningMonitor;
        protected final boolean m_ignoreUnsupportedDatatypes;
        protected final Set<String> m_definedDatatypeIRIs;
        protected final Set<DatatypeRestriction> m_allUnknownDatatypeRestrictions;
        private final IRI langString = IRI.create("http://www.w3.org/1999/02/22-rdf-syntax-ns#langString");

        public DataRangeConverter(Configuration.WarningMonitor warningMonitor, Set<String> definedDatatypeIRIs, Set<DatatypeRestriction> allUnknownDatatypeRestrictions, boolean ignoreUnsupportedDatatypes) {
            this.m_warningMonitor = warningMonitor;
            this.m_definedDatatypeIRIs = definedDatatypeIRIs;
            this.m_ignoreUnsupportedDatatypes = ignoreUnsupportedDatatypes;
            this.m_allUnknownDatatypeRestrictions = allUnknownDatatypeRestrictions;
        }

        public LiteralDataRange convertDataRange(OWLDataRange dataRange) {
            return (LiteralDataRange)dataRange.accept(this);
        }

        public Object visit(OWLDatatype object) {
            String datatypeURI = object.getIRI().toString();
            if (InternalDatatype.RDFS_LITERAL.getIRI().equals(datatypeURI)) {
                return InternalDatatype.RDFS_LITERAL;
            }
            if (datatypeURI.startsWith("internal:defdata#") || this.m_definedDatatypeIRIs.contains(object.getIRI().toString())) {
                return InternalDatatype.create(datatypeURI);
            }
            DatatypeRestriction datatype = DatatypeRestriction.create(datatypeURI, DatatypeRestriction.NO_FACET_URIs, DatatypeRestriction.NO_FACET_VALUES);
            if (datatypeURI.startsWith("internal:unknown-datatype#")) {
                this.m_allUnknownDatatypeRestrictions.add(datatype);
            } else {
                try {
                    DatatypeRegistry.validateDatatypeRestriction(datatype);
                }
                catch (UnsupportedDatatypeException e) {
                    if (this.m_ignoreUnsupportedDatatypes) {
                        if (this.m_warningMonitor != null) {
                            this.m_warningMonitor.warning("Ignoring unsupported datatype '" + object.getIRI() + "'.");
                        }
                        this.m_allUnknownDatatypeRestrictions.add(datatype);
                    }
                    throw e;
                }
            }
            return datatype;
        }

        public Object visit(OWLDataComplementOf object) {
            return this.convertDataRange(object.getDataRange()).getNegation();
        }

        public Object visit(OWLDataOneOf object) {
            LinkedHashSet<Constant> constants2 = new LinkedHashSet<Constant>();
            for (OWLLiteral literal : object.getValues()) {
                constants2.add((Constant)literal.accept(this));
            }
            Constant[] constantsArray = new Constant[constants2.size()];
            constants2.toArray(constantsArray);
            return ConstantEnumeration.create(constantsArray);
        }

        public Object visit(OWLDatatypeRestriction object) {
            if (!object.getDatatype().isOWLDatatype()) {
                throw new IllegalArgumentException("Datatype restrictions are supported only on OWL datatypes.");
            }
            String datatypeURI = object.getDatatype().getIRI().toString();
            if (InternalDatatype.RDFS_LITERAL.getIRI().equals(datatypeURI)) {
                if (!object.getFacetRestrictions().isEmpty()) {
                    throw new IllegalArgumentException("rdfs:Literal does not support any facets.");
                }
                return InternalDatatype.RDFS_LITERAL;
            }
            String[] facetURIs = new String[object.getFacetRestrictions().size()];
            Constant[] facetValues = new Constant[object.getFacetRestrictions().size()];
            int index = 0;
            for (OWLFacetRestriction facet : object.getFacetRestrictions()) {
                facetURIs[index] = facet.getFacet().getIRI().toURI().toString();
                facetValues[index] = (Constant)facet.getFacetValue().accept(this);
                ++index;
            }
            DatatypeRestriction datatype = DatatypeRestriction.create(datatypeURI, facetURIs, facetValues);
            DatatypeRegistry.validateDatatypeRestriction(datatype);
            return datatype;
        }

        public Object visit(OWLFacetRestriction object) {
            throw new IllegalStateException("Internal error: should not get in here.");
        }

        public Object visit(OWLLiteral object) {
            try {
                if (object.isRDFPlainLiteral() || object.getDatatype().getIRI().equals(this.langString)) {
                    if (object.hasLang()) {
                        return Constant.create(object.getLiteral() + "@" + object.getLang(), Prefixes.s_semanticWebPrefixes.get("rdf:") + "PlainLiteral");
                    }
                    return Constant.create(object.getLiteral() + "@", Prefixes.s_semanticWebPrefixes.get("rdf:") + "PlainLiteral");
                }
                return Constant.create(object.getLiteral(), object.getDatatype().getIRI().toString());
            }
            catch (UnsupportedDatatypeException e) {
                if (this.m_ignoreUnsupportedDatatypes) {
                    if (this.m_warningMonitor != null) {
                        this.m_warningMonitor.warning("Ignoring unsupported datatype '" + object + "'.");
                    }
                    return Constant.createAnonymous(object.getLiteral());
                }
                throw e;
            }
        }

        public Object visit(OWLDataIntersectionOf node) {
            throw new IllegalStateException("Internal error: invalid normal form.");
        }

        public Object visit(OWLDataUnionOf node) {
            throw new IllegalStateException("Internal error: invalid normal form.");
        }
    }

    protected static class NormalizedDataRangeAxiomClausifier
    implements OWLDataVisitor {
        protected final DataRangeConverter m_dataRangeConverter;
        protected final Set<String> m_definedDatatypeIRIs;
        protected final List<Atom> m_headAtoms;
        protected final List<Atom> m_bodyAtoms;
        protected final OWLDataFactory m_factory;
        protected int m_yIndex;

        public NormalizedDataRangeAxiomClausifier(DataRangeConverter dataRangeConverter, OWLDataFactory factory, Set<String> definedDatatypeIRIs) {
            this.m_dataRangeConverter = dataRangeConverter;
            this.m_definedDatatypeIRIs = definedDatatypeIRIs;
            this.m_headAtoms = new ArrayList<Atom>();
            this.m_bodyAtoms = new ArrayList<Atom>();
            this.m_factory = factory;
        }

        protected DLClause getDLClause() {
            Atom[] headAtoms = new Atom[this.m_headAtoms.size()];
            this.m_headAtoms.toArray(headAtoms);
            Atom[] bodyAtoms = new Atom[this.m_bodyAtoms.size()];
            this.m_bodyAtoms.toArray(bodyAtoms);
            DLClause dlClause = DLClause.create(headAtoms, bodyAtoms);
            this.m_headAtoms.clear();
            this.m_bodyAtoms.clear();
            this.m_yIndex = 0;
            return dlClause;
        }

        protected void ensureYNotZero() {
            if (this.m_yIndex == 0) {
                ++this.m_yIndex;
            }
        }

        protected Variable nextY() {
            Variable result = this.m_yIndex == 0 ? OWLClausification.Y : Variable.create("Y" + this.m_yIndex);
            ++this.m_yIndex;
            return result;
        }

        public void visit(OWLDatatype dt) {
            LiteralDataRange literalRange = this.m_dataRangeConverter.convertDataRange(dt);
            this.m_headAtoms.add(Atom.create(literalRange, OWLClausification.X));
        }

        public void visit(OWLDataIntersectionOf dr) {
            throw new IllegalStateException("Internal error: invalid normal form.");
        }

        public void visit(OWLDataUnionOf dr) {
            throw new IllegalStateException("Internal error: invalid normal form.");
        }

        private static String datatypeIRI(OWLDataRange r) {
            if (r.isDatatype()) {
                return r.asOWLDatatype().getIRI().toString();
            }
            return null;
        }

        public void visit(OWLDataComplementOf dr) {
            String iri = NormalizedDataRangeAxiomClausifier.datatypeIRI(dr.getDataRange());
            if (iri != null && (Prefixes.isInternalIRI(iri) || this.m_definedDatatypeIRIs.contains(iri))) {
                this.m_bodyAtoms.add(Atom.create(InternalDatatype.create(iri), OWLClausification.X));
            } else {
                LiteralDataRange literalRange = this.m_dataRangeConverter.convertDataRange(dr);
                if (literalRange.isNegatedInternalDatatype()) {
                    InternalDatatype negatedDatatype = (InternalDatatype)literalRange.getNegation();
                    if (!negatedDatatype.isAlwaysTrue()) {
                        this.m_bodyAtoms.add(Atom.create(negatedDatatype, OWLClausification.X));
                    }
                } else if (!literalRange.isAlwaysFalse()) {
                    this.m_headAtoms.add(Atom.create(literalRange, OWLClausification.X));
                }
            }
        }

        public void visit(OWLDataOneOf object) {
            LiteralDataRange literalRange = this.m_dataRangeConverter.convertDataRange(object);
            this.m_headAtoms.add(Atom.create(literalRange, OWLClausification.X));
        }

        public void visit(OWLFacetRestriction node) {
            throw new IllegalStateException("Internal error: Invalid normal form. ");
        }

        public void visit(OWLDatatypeRestriction node) {
            LiteralDataRange literalRange = this.m_dataRangeConverter.convertDataRange(node);
            this.m_headAtoms.add(Atom.create(literalRange, OWLClausification.X));
        }

        public void visit(OWLLiteral node) {
            throw new IllegalStateException("Internal error: Invalid normal form. ");
        }
    }

    protected static class NormalizedAxiomClausifier
    implements OWLClassExpressionVisitor {
        protected final DataRangeConverter m_dataRangeConverter;
        protected final List<Atom> m_headAtoms;
        protected final List<Atom> m_bodyAtoms;
        protected final Set<Atom> m_positiveFacts;
        protected int m_yIndex;
        protected int m_zIndex;

        public NormalizedAxiomClausifier(DataRangeConverter dataRangeConverter, Set<Atom> positiveFacts) {
            this.m_dataRangeConverter = dataRangeConverter;
            this.m_headAtoms = new ArrayList<Atom>();
            this.m_bodyAtoms = new ArrayList<Atom>();
            this.m_positiveFacts = positiveFacts;
        }

        protected DLClause getDLClause() {
            Atom[] headAtoms = new Atom[this.m_headAtoms.size()];
            this.m_headAtoms.toArray(headAtoms);
            Atom[] bodyAtoms = new Atom[this.m_bodyAtoms.size()];
            this.m_bodyAtoms.toArray(bodyAtoms);
            DLClause dlClause = DLClause.create(headAtoms, bodyAtoms);
            this.m_headAtoms.clear();
            this.m_bodyAtoms.clear();
            this.m_yIndex = 0;
            this.m_zIndex = 0;
            return dlClause;
        }

        protected void ensureYNotZero() {
            if (this.m_yIndex == 0) {
                ++this.m_yIndex;
            }
        }

        protected Variable nextY() {
            Variable result = this.m_yIndex == 0 ? OWLClausification.Y : Variable.create("Y" + this.m_yIndex);
            ++this.m_yIndex;
            return result;
        }

        protected Variable nextZ() {
            Variable result = this.m_zIndex == 0 ? OWLClausification.Z : Variable.create("Z" + this.m_zIndex);
            ++this.m_zIndex;
            return result;
        }

        protected AtomicConcept getConceptForNominal(OWLIndividual individual) {
            AtomicConcept result = individual.isAnonymous() ? AtomicConcept.create("internal:anon#" + individual.asOWLAnonymousIndividual().getID()) : AtomicConcept.create("internal:nom#" + individual.asOWLNamedIndividual().getIRI());
            this.m_positiveFacts.add(Atom.create(result, OWLClausification.getIndividual(individual)));
            return result;
        }

        public void visit(OWLClass object) {
            this.m_headAtoms.add(Atom.create(AtomicConcept.create(object.getIRI().toString()), OWLClausification.X));
        }

        public void visit(OWLObjectIntersectionOf object) {
            throw new IllegalStateException("Internal error: invalid normal form.");
        }

        public void visit(OWLObjectUnionOf object) {
            throw new IllegalStateException("Internal error: invalid normal form.");
        }

        public void visit(OWLObjectComplementOf object) {
            OWLClassExpression description = object.getOperand();
            if (description instanceof OWLObjectHasSelf) {
                OWLObjectPropertyExpression objectProperty = ((OWLObjectHasSelf)description).getProperty();
                Atom roleAtom = OWLClausification.getRoleAtom(objectProperty, OWLClausification.X, OWLClausification.X);
                this.m_bodyAtoms.add(roleAtom);
            } else if (description instanceof OWLObjectOneOf && ((OWLObjectOneOf)description).getIndividuals().size() == 1) {
                OWLIndividual individual = ((OWLObjectOneOf)description).getIndividuals().iterator().next();
                this.m_bodyAtoms.add(Atom.create(this.getConceptForNominal(individual), OWLClausification.X));
            } else {
                if (!(description instanceof OWLClass)) {
                    throw new IllegalStateException("Internal error: invalid normal form.");
                }
                this.m_bodyAtoms.add(Atom.create(AtomicConcept.create(((OWLClass)description).getIRI().toString()), OWLClausification.X));
            }
        }

        public void visit(OWLObjectOneOf object) {
            for (OWLIndividual individual : object.getIndividuals()) {
                Variable z = this.nextZ();
                AtomicConcept conceptForNominal = this.getConceptForNominal(individual);
                this.m_headAtoms.add(Atom.create(Equality.INSTANCE, OWLClausification.X, z));
                this.m_bodyAtoms.add(Atom.create(conceptForNominal, z));
            }
        }

        public void visit(OWLObjectSomeValuesFrom object) {
            OWLClassExpression filler = object.getFiller();
            if (filler instanceof OWLObjectOneOf) {
                for (OWLIndividual individual : ((OWLObjectOneOf)filler).getIndividuals()) {
                    Variable z = this.nextZ();
                    this.m_bodyAtoms.add(Atom.create(this.getConceptForNominal(individual), z));
                    this.m_headAtoms.add(OWLClausification.getRoleAtom(object.getProperty(), OWLClausification.X, z));
                }
            } else {
                LiteralConcept toConcept = OWLClausification.getLiteralConcept(filler);
                Role onRole = OWLClausification.getRole(object.getProperty());
                AtLeastConcept atLeastConcept = AtLeastConcept.create(1, onRole, toConcept);
                if (!atLeastConcept.isAlwaysFalse()) {
                    this.m_headAtoms.add(Atom.create(atLeastConcept, OWLClausification.X));
                }
            }
        }

        /*
         * Enabled force condition propagation
         * Lifted jumps to return sites
         */
        public void visit(OWLObjectAllValuesFrom object) {
            Variable y = this.nextY();
            this.m_bodyAtoms.add(OWLClausification.getRoleAtom(object.getProperty(), OWLClausification.X, y));
            OWLClassExpression filler = object.getFiller();
            if (filler instanceof OWLClass) {
                AtomicConcept atomicConcept = AtomicConcept.create(((OWLClass)filler).getIRI().toString());
                if (atomicConcept.isAlwaysFalse()) return;
                this.m_headAtoms.add(Atom.create(atomicConcept, y));
            } else if (filler instanceof OWLObjectOneOf) {
                for (OWLIndividual individual : ((OWLObjectOneOf)filler).getIndividuals()) {
                    Variable zInd = this.nextZ();
                    this.m_bodyAtoms.add(Atom.create(this.getConceptForNominal(individual), zInd));
                    this.m_headAtoms.add(Atom.create(Equality.INSTANCE, y, zInd));
                }
            } else {
                if (!(filler instanceof OWLObjectComplementOf)) throw new IllegalStateException("Internal error: invalid normal form.");
                OWLClassExpression operand = ((OWLObjectComplementOf)filler).getOperand();
                if (operand instanceof OWLClass) {
                    AtomicConcept internalAtomicConcept = AtomicConcept.create(((OWLClass)operand).getIRI().toString());
                    if (internalAtomicConcept.isAlwaysTrue()) return;
                    this.m_bodyAtoms.add(Atom.create(internalAtomicConcept, y));
                } else {
                    if (!(operand instanceof OWLObjectOneOf) || ((OWLObjectOneOf)operand).getIndividuals().size() != 1) throw new IllegalStateException("Internal error: invalid normal form.");
                    OWLIndividual individual = ((OWLObjectOneOf)operand).getIndividuals().iterator().next();
                    this.m_bodyAtoms.add(Atom.create(this.getConceptForNominal(individual), y));
                }
            }
        }

        public void visit(OWLObjectHasValue object) {
            throw new IllegalStateException("Internal error: invalid normal form.");
        }

        public void visit(OWLObjectHasSelf object) {
            OWLObjectPropertyExpression objectProperty = object.getProperty();
            Atom roleAtom = OWLClausification.getRoleAtom(objectProperty, OWLClausification.X, OWLClausification.X);
            this.m_headAtoms.add(roleAtom);
        }

        public void visit(OWLObjectMinCardinality object) {
            LiteralConcept toConcept = OWLClausification.getLiteralConcept(object.getFiller());
            Role onRole = OWLClausification.getRole(object.getProperty());
            AtLeastConcept atLeastConcept = AtLeastConcept.create(object.getCardinality(), onRole, toConcept);
            if (!atLeastConcept.isAlwaysFalse()) {
                this.m_headAtoms.add(Atom.create(atLeastConcept, OWLClausification.X));
            }
        }

        public void visit(OWLObjectMaxCardinality object) {
            int i;
            AtomicConcept atomicConcept;
            boolean isPositive;
            int cardinality = object.getCardinality();
            OWLObjectPropertyExpression onObjectProperty = object.getProperty();
            OWLClassExpression filler = object.getFiller();
            this.ensureYNotZero();
            if (filler instanceof OWLClass) {
                isPositive = true;
                atomicConcept = AtomicConcept.create(((OWLClass)filler).getIRI().toString());
                if (atomicConcept.isAlwaysTrue()) {
                    atomicConcept = null;
                }
            } else if (filler instanceof OWLObjectComplementOf) {
                OWLClassExpression internal = ((OWLObjectComplementOf)filler).getOperand();
                if (!(internal instanceof OWLClass)) {
                    throw new IllegalStateException("Internal error: Invalid ontology normal form.");
                }
                isPositive = false;
                atomicConcept = AtomicConcept.create(((OWLClass)internal).getIRI().toString());
                if (atomicConcept.isAlwaysFalse()) {
                    atomicConcept = null;
                }
            } else {
                throw new IllegalStateException("Internal error: Invalid ontology normal form.");
            }
            Role onRole = OWLClausification.getRole(onObjectProperty);
            LiteralConcept toConcept = OWLClausification.getLiteralConcept(filler);
            AnnotatedEquality annotatedEquality = AnnotatedEquality.create(cardinality, onRole, toConcept);
            Term[] yVars = new Variable[cardinality + 1];
            for (i = 0; i < yVars.length; ++i) {
                yVars[i] = this.nextY();
                this.m_bodyAtoms.add(OWLClausification.getRoleAtom(onObjectProperty, OWLClausification.X, yVars[i]));
                if (atomicConcept == null) continue;
                Atom atom = Atom.create(atomicConcept, yVars[i]);
                if (isPositive) {
                    this.m_bodyAtoms.add(atom);
                    continue;
                }
                this.m_headAtoms.add(atom);
            }
            if (yVars.length > 2) {
                for (i = 0; i < yVars.length - 1; ++i) {
                    this.m_bodyAtoms.add(Atom.create(NodeIDLessEqualThan.INSTANCE, yVars[i], yVars[i + 1]));
                }
                this.m_bodyAtoms.add(Atom.create(NodeIDsAscendingOrEqual.create(yVars.length), yVars));
            }
            for (i = 0; i < yVars.length; ++i) {
                for (int j = i + 1; j < yVars.length; ++j) {
                    this.m_headAtoms.add(Atom.create(annotatedEquality, yVars[i], yVars[j], OWLClausification.X));
                }
            }
        }

        public void visit(OWLObjectExactCardinality object) {
            throw new IllegalStateException("Internal error: invalid normal form.");
        }

        public void visit(OWLDataSomeValuesFrom object) {
            AtomicRole atomicRole;
            AtLeastDataRange atLeastDataRange;
            LiteralDataRange literalRange;
            if (!object.getProperty().isOWLBottomDataProperty() && !(atLeastDataRange = AtLeastDataRange.create(1, atomicRole = OWLClausification.getAtomicRole(object.getProperty()), literalRange = this.m_dataRangeConverter.convertDataRange(object.getFiller()))).isAlwaysFalse()) {
                this.m_headAtoms.add(Atom.create(atLeastDataRange, OWLClausification.X));
            }
        }

        public void visit(OWLDataAllValuesFrom object) {
            LiteralDataRange literalRange = this.m_dataRangeConverter.convertDataRange(object.getFiller());
            if (object.getProperty().isOWLTopDataProperty() && literalRange.isAlwaysFalse()) {
                return;
            }
            Variable y = this.nextY();
            this.m_bodyAtoms.add(OWLClausification.getRoleAtom(object.getProperty(), OWLClausification.X, y));
            if (literalRange.isNegatedInternalDatatype()) {
                InternalDatatype negatedRange = (InternalDatatype)literalRange.getNegation();
                if (!negatedRange.isAlwaysTrue()) {
                    this.m_bodyAtoms.add(Atom.create(negatedRange, y));
                }
            } else if (!literalRange.isAlwaysFalse()) {
                this.m_headAtoms.add(Atom.create(literalRange, y));
            }
        }

        public void visit(OWLDataHasValue object) {
            throw new IllegalStateException("Internal error: Invalid normal form.");
        }

        public void visit(OWLDataMinCardinality object) {
            if (!object.getProperty().isOWLBottomDataProperty() || object.getCardinality() == 0) {
                AtomicRole atomicRole = OWLClausification.getAtomicRole(object.getProperty());
                LiteralDataRange literalRange = this.m_dataRangeConverter.convertDataRange(object.getFiller());
                AtLeastDataRange atLeast = AtLeastDataRange.create(object.getCardinality(), atomicRole, literalRange);
                if (!atLeast.isAlwaysFalse()) {
                    this.m_headAtoms.add(Atom.create(atLeast, OWLClausification.X));
                }
            }
        }

        public void visit(OWLDataMaxCardinality object) {
            int i;
            int number = object.getCardinality();
            LiteralDataRange negatedDataRange = this.m_dataRangeConverter.convertDataRange(object.getFiller()).getNegation();
            this.ensureYNotZero();
            Variable[] yVars = new Variable[number + 1];
            for (i = 0; i < yVars.length; ++i) {
                yVars[i] = this.nextY();
                this.m_bodyAtoms.add(OWLClausification.getRoleAtom(object.getProperty(), OWLClausification.X, yVars[i]));
                if (negatedDataRange.isNegatedInternalDatatype()) {
                    InternalDatatype negated = (InternalDatatype)negatedDataRange.getNegation();
                    if (negated.isAlwaysTrue()) continue;
                    this.m_bodyAtoms.add(Atom.create(negated, yVars[i]));
                    continue;
                }
                if (negatedDataRange.isAlwaysFalse()) continue;
                this.m_headAtoms.add(Atom.create(negatedDataRange, yVars[i]));
            }
            for (i = 0; i < yVars.length; ++i) {
                for (int j = i + 1; j < yVars.length; ++j) {
                    this.m_headAtoms.add(Atom.create(Equality.INSTANCE, yVars[i], yVars[j]));
                }
            }
        }

        public void visit(OWLDataExactCardinality object) {
            throw new IllegalStateException("Internal error: invalid normal form.");
        }
    }

}

