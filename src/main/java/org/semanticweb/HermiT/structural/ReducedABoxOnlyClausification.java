package org.semanticweb.HermiT.structural;

import java.util.HashSet;
import java.util.Set;
import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Prefixes;
import org.semanticweb.HermiT.datatypes.UnsupportedDatatypeException;
import org.semanticweb.HermiT.model.Atom;
import org.semanticweb.HermiT.model.AtomicConcept;
import org.semanticweb.HermiT.model.AtomicRole;
import org.semanticweb.HermiT.model.Constant;
import org.semanticweb.HermiT.model.Equality;
import org.semanticweb.HermiT.model.Individual;
import org.semanticweb.HermiT.model.Inequality;
import org.semanticweb.HermiT.model.Term;
import org.semanticweb.owlapi.model.OWLAxiomVisitor;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLIndividualAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNegativeDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectHasSelf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.util.OWLAxiomVisitorAdapter;

public class ReducedABoxOnlyClausification
extends OWLAxiomVisitorAdapter {
    protected final Configuration.WarningMonitor m_warningMonitor;
    protected final boolean m_ignoreUnsupportedDatatypes;
    protected final Set<AtomicConcept> m_allAtomicConcepts;
    protected final Set<AtomicRole> m_allAtomicObjectRoles;
    protected final Set<AtomicRole> m_allAtomicDataRoles;
    protected final Set<Atom> m_positiveFacts;
    protected final Set<Atom> m_negativeFacts;
    protected final Set<Individual> m_allIndividuals;

    public ReducedABoxOnlyClausification(Configuration configuration, Set<AtomicConcept> allAtomicConcepts, Set<AtomicRole> allAtomicObjectRoles, Set<AtomicRole> allAtomicDataRoles) {
        this.m_warningMonitor = configuration.warningMonitor;
        this.m_ignoreUnsupportedDatatypes = configuration.ignoreUnsupportedDatatypes;
        this.m_allAtomicConcepts = allAtomicConcepts;
        this.m_allAtomicObjectRoles = allAtomicObjectRoles;
        this.m_allAtomicDataRoles = allAtomicDataRoles;
        this.m_positiveFacts = new HashSet<Atom>();
        this.m_negativeFacts = new HashSet<Atom>();
        this.m_allIndividuals = new HashSet<Individual>();
    }

    public /* varargs */ void clausify(OWLIndividualAxiom ... axioms) {
        this.m_positiveFacts.clear();
        this.m_negativeFacts.clear();
        for (OWLIndividualAxiom fact : axioms) {
            fact.accept(this);
        }
    }

    public Set<Atom> getPositiveFacts() {
        return this.m_positiveFacts;
    }

    public Set<Atom> getNegativeFacts() {
        return this.m_negativeFacts;
    }

    public Set<Individual> getAllIndividuals() {
        return this.m_allIndividuals;
    }

    protected Atom getConceptAtom(OWLClass cls, Term term) {
        AtomicConcept atomicConcept = AtomicConcept.create(cls.getIRI().toString());
        if (this.m_allAtomicConcepts.contains(atomicConcept)) {
            return Atom.create(atomicConcept, term);
        }
        throw new IllegalArgumentException("Internal error: fresh classes in class assertions are not compatible with incremental ABox loading!");
    }

    protected Atom getRoleAtom(OWLObjectPropertyExpression objectProperty, Term first, Term second) {
        AtomicRole atomicRole;
        if (objectProperty.isAnonymous()) {
            OWLObjectProperty internalObjectProperty = objectProperty.getNamedProperty();
            atomicRole = AtomicRole.create(internalObjectProperty.getIRI().toString());
            Term tmp = first;
            first = second;
            second = tmp;
        } else {
            atomicRole = AtomicRole.create(objectProperty.asOWLObjectProperty().getIRI().toString());
        }
        if (this.m_allAtomicObjectRoles.contains(atomicRole)) {
            return Atom.create(atomicRole, first, second);
        }
        throw new IllegalArgumentException("Internal error: fresh properties in property assertions are not compatible with incremental ABox loading!");
    }

    protected Atom getRoleAtom(OWLDataPropertyExpression dataProperty, Term first, Term second) {
        if (!(dataProperty instanceof OWLDataProperty)) {
            throw new IllegalStateException("Internal error: unsupported type of data property!");
        }
        AtomicRole atomicRole = AtomicRole.create(((OWLDataProperty)dataProperty).getIRI().toString());
        if (this.m_allAtomicDataRoles.contains(atomicRole)) {
            return Atom.create(atomicRole, first, second);
        }
        throw new IllegalArgumentException("Internal error: fresh properties in property assertions are not compatible with incremental ABox loading!");
    }

    protected Individual getIndividual(OWLIndividual individual) {
        Individual ind = individual.isAnonymous() ? Individual.createAnonymous(individual.asOWLAnonymousIndividual().getID().toString()) : Individual.create(individual.asOWLNamedIndividual().getIRI().toString());
        this.m_allIndividuals.add(ind);
        return ind;
    }

    public void visit(OWLSameIndividualAxiom object) {
        OWLIndividual[] individuals = new OWLIndividual[object.getIndividuals().size()];
        object.getIndividuals().toArray(individuals);
        for (int i = 0; i < individuals.length - 1; ++i) {
            this.m_positiveFacts.add(Atom.create(Equality.create(), this.getIndividual(individuals[i]), this.getIndividual(individuals[i + 1])));
        }
    }

    public void visit(OWLDifferentIndividualsAxiom object) {
        OWLIndividual[] individuals = new OWLIndividual[object.getIndividuals().size()];
        object.getIndividuals().toArray(individuals);
        for (int i = 0; i < individuals.length; ++i) {
            for (int j = i + 1; j < individuals.length; ++j) {
                this.m_positiveFacts.add(Atom.create(Inequality.create(), this.getIndividual(individuals[i]), this.getIndividual(individuals[j])));
            }
        }
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public void visit(OWLClassAssertionAxiom object) {
        OWLClassExpression description = object.getClassExpression();
        if (description instanceof OWLClass) {
            this.m_positiveFacts.add(this.getConceptAtom((OWLClass)description, this.getIndividual(object.getIndividual())));
        } else if (description instanceof OWLObjectHasSelf) {
            this.m_positiveFacts.add(this.getRoleAtom(((OWLObjectHasSelf)description).getProperty().getNamedProperty(), this.getIndividual(object.getIndividual()), this.getIndividual(object.getIndividual())));
        } else if (description instanceof OWLObjectHasValue) {
            OWLObjectHasValue hasValue = (OWLObjectHasValue)description;
            OWLObjectPropertyExpression role = hasValue.getProperty();
            OWLIndividual filler = hasValue.getFiller();
            this.m_positiveFacts.add(this.getRoleAtom(role, this.getIndividual(object.getIndividual()), this.getIndividual(filler)));
        } else {
            if (!(description instanceof OWLObjectComplementOf)) throw new IllegalArgumentException("Internal error: invalid normal form for ABox updates.");
            OWLClassExpression negated = ((OWLObjectComplementOf)description).getOperand();
            if (negated instanceof OWLClass) {
                this.m_negativeFacts.add(this.getConceptAtom((OWLClass)negated, this.getIndividual(object.getIndividual())));
            } else if (negated instanceof OWLObjectHasSelf) {
                this.m_negativeFacts.add(this.getRoleAtom(((OWLObjectHasSelf)negated).getProperty().getNamedProperty(), this.getIndividual(object.getIndividual()), this.getIndividual(object.getIndividual())));
            } else {
                if (!(negated instanceof OWLObjectHasValue)) throw new IllegalArgumentException("Internal error: invalid normal form for ABox updates (class assertion with negated class).");
                OWLObjectHasValue hasValue = (OWLObjectHasValue)negated;
                OWLObjectPropertyExpression role = hasValue.getProperty();
                OWLIndividual filler = hasValue.getFiller();
                this.m_negativeFacts.add(this.getRoleAtom(role, this.getIndividual(object.getIndividual()), this.getIndividual(filler)));
            }
        }
    }

    public void visit(OWLObjectPropertyAssertionAxiom object) {
        this.m_positiveFacts.add(this.getRoleAtom(object.getProperty(), this.getIndividual(object.getSubject()), this.getIndividual(object.getObject())));
    }

    public void visit(OWLNegativeObjectPropertyAssertionAxiom object) {
        this.m_negativeFacts.add(this.getRoleAtom(object.getProperty(), this.getIndividual(object.getSubject()), this.getIndividual(object.getObject())));
    }

    public void visit(OWLDataPropertyAssertionAxiom object) {
        Constant targetValue = this.getConstant(object.getObject());
        this.m_positiveFacts.add(this.getRoleAtom(object.getProperty(), this.getIndividual(object.getSubject()), targetValue));
    }

    public void visit(OWLNegativeDataPropertyAssertionAxiom object) {
        Constant targetValue = this.getConstant(object.getObject());
        this.m_negativeFacts.add(this.getRoleAtom(object.getProperty(), this.getIndividual(object.getSubject()), targetValue));
    }

    protected Constant getConstant(OWLLiteral literal) {
        try {
            if (literal.isRDFPlainLiteral()) {
                if (literal.hasLang()) {
                    return Constant.create(literal.getLiteral() + "@" + literal.getLang(), Prefixes.s_semanticWebPrefixes.get("rdf:") + "PlainLiteral");
                }
                return Constant.create(literal.getLiteral() + "@", Prefixes.s_semanticWebPrefixes.get("rdf:") + "PlainLiteral");
            }
            return Constant.create(literal.getLiteral(), literal.getDatatype().getIRI().toString());
        }
        catch (UnsupportedDatatypeException e) {
            if (this.m_ignoreUnsupportedDatatypes) {
                if (this.m_warningMonitor != null) {
                    this.m_warningMonitor.warning("Ignoring unsupported datatype '" + literal + "'.");
                }
                return Constant.createAnonymous(literal.getLiteral());
            }
            throw e;
        }
    }
}

