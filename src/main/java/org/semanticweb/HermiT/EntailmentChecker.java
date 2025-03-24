package org.semanticweb.HermiT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.HermiT.model.Atom;
import org.semanticweb.HermiT.model.AtomicRole;
import org.semanticweb.HermiT.model.DLOntology;
import org.semanticweb.HermiT.model.Individual;
import org.semanticweb.HermiT.model.InternalDatatype;
import org.semanticweb.HermiT.tableau.Node;
import org.semanticweb.HermiT.tableau.ReasoningTaskDescription;
import org.semanticweb.HermiT.tableau.Tableau;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.NodeID;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLAsymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLAxiomVisitor;
import org.semanticweb.owlapi.model.OWLAxiomVisitorEx;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataComplementOf;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataIntersectionOf;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLDataUnionOf;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDatatypeDefinitionAxiom;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointUnionAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLHasKeyAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLIrreflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLMetaRuleAxiom;
import org.semanticweb.owlapi.model.OWLMetamodellingAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNegativeDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLPropertyAssertionObject;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLPropertyRange;
import org.semanticweb.owlapi.model.OWLReflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.SWRLRule;

public class EntailmentChecker
implements OWLAxiomVisitorEx<Boolean> {
    protected final OWLDataFactory factory;
    private final Reasoner reasoner;
    protected final Set<OWLAxiom> anonymousIndividualAxioms = new HashSet<OWLAxiom>();

    public EntailmentChecker(Reasoner reasoner, OWLDataFactory factory) {
        this.reasoner = reasoner;
        this.factory = factory;
    }

    public boolean entails(Set<? extends OWLAxiom> axioms) {
        this.anonymousIndividualAxioms.clear();
        for (OWLAxiom axiom : axioms) {
            if (!axiom.isLogicalAxiom() || ((Boolean)axiom.accept((OWLAxiomVisitorEx)this)).booleanValue()) continue;
            return false;
        }
        return this.checkAnonymousIndividuals();
    }

    public boolean entails(OWLAxiom axiom) {
        if (!((Boolean)axiom.accept((OWLAxiomVisitorEx)this)).booleanValue()) {
            return false;
        }
        return this.checkAnonymousIndividuals();
    }

    protected boolean checkAnonymousIndividuals() {
        if (this.anonymousIndividualAxioms.isEmpty()) {
            return true;
        }
        AnonymousIndividualForestBuilder anonIndChecker = new AnonymousIndividualForestBuilder();
        anonIndChecker.constructConceptsForAnonymousIndividuals(this.factory, this.anonymousIndividualAxioms);
        for (OWLAxiom ax : anonIndChecker.getAnonIndAxioms()) {
            if (((Boolean)ax.accept((OWLAxiomVisitorEx)this)).booleanValue()) continue;
            return false;
        }
        for (OWLAxiom ax : anonIndChecker.getAnonNoNamedIndAxioms()) {
            Tableau t = this.reasoner.getTableau(ax);
            if (!t.isSatisfiable(true, true, null, null, null, null, null, new ReasoningTaskDescription(false, "Anonymous individual check: " + ax.toString()))) continue;
            return false;
        }
        return true;
    }

    public Boolean visit(OWLAnnotationAssertionAxiom axiom) {
        return Boolean.TRUE;
    }

    public Boolean visit(OWLSubAnnotationPropertyOfAxiom axiom) {
        return Boolean.TRUE;
    }

    public Boolean visit(OWLAnnotationPropertyDomainAxiom axiom) {
        return Boolean.TRUE;
    }

    public Boolean visit(OWLAnnotationPropertyRangeAxiom axiom) {
        return Boolean.TRUE;
    }

    public Boolean visit(OWLDeclarationAxiom axiom) {
        return Boolean.TRUE;
    }

    public Boolean visit(OWLDifferentIndividualsAxiom axiom) {
        ArrayList<OWLIndividual> list = new ArrayList(axiom.getIndividuals());
        for (OWLIndividual i : list) {
            if (!i.isAnonymous()) continue;
            throw new IllegalArgumentException("OWLDifferentIndividualsAxiom axioms are not allowed to be used with anonymous individuals (see OWL 2 Syntax Sec 11.2) but the axiom " + axiom + " cotains an anonymous individual. ");
        }
        for (int i = 0; i < list.size() - 1; ++i) {
            OWLNamedIndividual head = list.get(i).asOWLNamedIndividual();
            for (int j = i + 1; j < list.size(); ++j) {
                OWLNamedIndividual next = list.get(j).asOWLNamedIndividual();
                if (this.reasoner.hasType(head, this.factory.getOWLObjectComplementOf(this.factory.getOWLObjectOneOf(new OWLIndividual[]{next})), false)) continue;
                return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }

    public Boolean visit(OWLSameIndividualAxiom axiom) {
        for (OWLIndividual i : axiom.getIndividuals()) {
            if (!i.isAnonymous()) continue;
            throw new IllegalArgumentException("OWLSameIndividualAxiom axioms are not allowed to be used with anonymous individuals (see OWL 2 Syntax Sec 11.2) but the axiom " + axiom + " cotains an anonymous individual. ");
        }
        Iterator i = axiom.getIndividuals().iterator();
        if (i.hasNext()) {
            OWLNamedIndividual first = ((OWLIndividual)i.next()).asOWLNamedIndividual();
            while (i.hasNext()) {
                OWLNamedIndividual next = ((OWLIndividual)i.next()).asOWLNamedIndividual();
                if (this.reasoner.isSameIndividual(first, next)) continue;
                return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }

    public Boolean visit(OWLClassAssertionAxiom axiom) {
        OWLIndividual ind = axiom.getIndividual();
        if (ind.isAnonymous()) {
            this.anonymousIndividualAxioms.add(axiom);
            return true;
        }
        OWLClassExpression c = axiom.getClassExpression();
        return this.reasoner.hasType(ind.asOWLNamedIndividual(), c, false);
    }

    public Boolean visit(OWLObjectPropertyAssertionAxiom axiom) {
        OWLIndividual sub = axiom.getSubject();
        OWLIndividual obj = axiom.getObject();
        if (sub.isAnonymous() || obj.isAnonymous()) {
            this.anonymousIndividualAxioms.add(axiom);
            return true;
        }
        return this.reasoner.hasObjectPropertyRelationship(sub.asOWLNamedIndividual(), axiom.getProperty(), obj.asOWLNamedIndividual());
    }

    public Boolean visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
        if (axiom.getSubject().isAnonymous() || axiom.getObject().isAnonymous()) {
            throw new IllegalArgumentException("NegativeObjectPropertyAssertion axioms are not allowed to be used with anonymous individuals (see OWL 2 Syntax Sec 11.2) but the axiom " + axiom + " cotains an anonymous subject or object. ");
        }
        OWLObjectHasValue hasValue = this.factory.getOWLObjectHasValue(axiom.getProperty(), axiom.getObject());
        OWLObjectComplementOf doesNotHaveValue = this.factory.getOWLObjectComplementOf(hasValue);
        return this.reasoner.hasType(axiom.getSubject().asOWLNamedIndividual(), doesNotHaveValue, false);
    }

    public Boolean visit(OWLDataPropertyAssertionAxiom axiom) {
        OWLIndividual sub = axiom.getSubject();
        if (sub.isAnonymous()) {
            this.anonymousIndividualAxioms.add(axiom);
            return true;
        }
        OWLDataHasValue hasValue = this.factory.getOWLDataHasValue(axiom.getProperty(), axiom.getObject());
        return this.reasoner.hasType(axiom.getSubject().asOWLNamedIndividual(), hasValue, false);
    }

    public Boolean visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
        if (axiom.getSubject().isAnonymous()) {
            throw new IllegalArgumentException("NegativeDataPropertyAssertion axioms are not allowed to be used with anonymous individuals (see OWL 2 Syntax Sec 11.2) and the subject " + axiom.getSubject() + " of the axiom " + axiom + " is anonymous. ");
        }
        OWLDataHasValue hasValue = this.factory.getOWLDataHasValue(axiom.getProperty(), axiom.getObject());
        OWLObjectComplementOf doesNotHaveValue = this.factory.getOWLObjectComplementOf(hasValue);
        return this.reasoner.hasType(axiom.getSubject().asOWLNamedIndividual(), doesNotHaveValue, false);
    }

    public Boolean visit(OWLObjectPropertyDomainAxiom axiom) {
        return this.reasoner.isSubClassOf(this.factory.getOWLObjectSomeValuesFrom(axiom.getProperty(), this.factory.getOWLThing()), axiom.getDomain());
    }

    public Boolean visit(OWLObjectPropertyRangeAxiom axiom) {
        return this.reasoner.isSubClassOf(this.factory.getOWLThing(), this.factory.getOWLObjectAllValuesFrom(axiom.getProperty(), axiom.getRange()));
    }

    public Boolean visit(OWLInverseObjectPropertiesAxiom axiom) {
        OWLObjectPropertyExpression objectPropertyExpression2;
        OWLObjectPropertyExpression objectPropertyExpression1 = axiom.getFirstProperty().getInverseProperty();
        return this.reasoner.isSubObjectPropertyExpressionOf(objectPropertyExpression1, objectPropertyExpression2 = axiom.getSecondProperty()) && this.reasoner.isSubObjectPropertyExpressionOf(objectPropertyExpression2, objectPropertyExpression1);
    }

    public Boolean visit(OWLSymmetricObjectPropertyAxiom axiom) {
        return this.reasoner.isSymmetric(axiom.getProperty());
    }

    public Boolean visit(OWLTransitiveObjectPropertyAxiom axiom) {
        return this.reasoner.isTransitive(axiom.getProperty());
    }

    public Boolean visit(OWLReflexiveObjectPropertyAxiom axiom) {
        return this.reasoner.isReflexive(axiom.getProperty());
    }

    public Boolean visit(OWLIrreflexiveObjectPropertyAxiom axiom) {
        return this.reasoner.isIrreflexive(axiom.getProperty());
    }

    public Boolean visit(OWLAsymmetricObjectPropertyAxiom axiom) {
        return this.reasoner.isAsymmetric(axiom.getProperty());
    }

    public Boolean visit(OWLEquivalentObjectPropertiesAxiom axiom) {
        Set props = axiom.getProperties();
        Iterator it = props.iterator();
        if (it.hasNext()) {
            OWLObjectPropertyExpression objectPropertyExpression1 = (OWLObjectPropertyExpression)it.next();
            while (it.hasNext()) {
                OWLObjectPropertyExpression objectPropertyExpression2 = (OWLObjectPropertyExpression)it.next();
                if (this.reasoner.isSubObjectPropertyExpressionOf(objectPropertyExpression1, objectPropertyExpression2) && this.reasoner.isSubObjectPropertyExpressionOf(objectPropertyExpression2, objectPropertyExpression1)) continue;
                return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }

    public Boolean visit(OWLSubObjectPropertyOfAxiom axiom) {
        return this.reasoner.isSubObjectPropertyExpressionOf(axiom.getSubProperty(), axiom.getSuperProperty());
    }

    public Boolean visit(OWLSubPropertyChainOfAxiom axiom) {
        return this.reasoner.isSubObjectPropertyExpressionOf(axiom.getPropertyChain(), axiom.getSuperProperty());
    }

    public Boolean visit(OWLDisjointObjectPropertiesAxiom axiom) {
        int n = axiom.getProperties().size();
        OWLObjectPropertyExpression[] props=axiom.getProperties().toArray(new OWLObjectPropertyExpression[n]);
        for (int i = 0; i < n - 1; ++i) {
            for (int j = i + 1; j < n; ++j) {
                if (this.reasoner.isDisjointObjectProperty(props[i], props[j])) continue;
                return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }

    public Boolean visit(OWLFunctionalObjectPropertyAxiom axiom) {
        return this.reasoner.isFunctional(axiom.getProperty());
    }

    public Boolean visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
        return this.reasoner.isInverseFunctional(axiom.getProperty());
    }

    public Boolean visit(OWLDataPropertyDomainAxiom axiom) {
        return this.reasoner.isSubClassOf(this.factory.getOWLDataSomeValuesFrom(axiom.getProperty(), this.factory.getTopDatatype()), axiom.getDomain());
    }

    public Boolean visit(OWLDataPropertyRangeAxiom axiom) {
        return this.reasoner.isSubClassOf(this.factory.getOWLThing(), this.factory.getOWLDataAllValuesFrom(axiom.getProperty(), axiom.getRange()));
    }

    public Boolean visit(OWLEquivalentDataPropertiesAxiom axiom) {
        Set props = axiom.getProperties();
        Iterator it = props.iterator();
        if (it.hasNext()) {
            OWLDataProperty prop1 = ((OWLDataPropertyExpression)it.next()).asOWLDataProperty();
            while (it.hasNext()) {
                OWLDataProperty dataProperty2;
                OWLDataProperty dataProperty1 = prop1.asOWLDataProperty();
                if (this.reasoner.isSubDataPropertyOf(dataProperty1, dataProperty2 = ((OWLDataPropertyExpression)it.next()).asOWLDataProperty()) && this.reasoner.isSubDataPropertyOf(dataProperty2, dataProperty1)) continue;
                return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }

    public Boolean visit(OWLSubDataPropertyOfAxiom axiom) {
        return this.reasoner.isSubDataPropertyOf(axiom.getSubProperty().asOWLDataProperty(), axiom.getSuperProperty().asOWLDataProperty());
    }

    public Boolean visit(OWLDisjointDataPropertiesAxiom axiom) {
        int n = axiom.getProperties().size();
        OWLDataPropertyExpression[] props = axiom.getProperties().toArray(new OWLDataPropertyExpression[n]);
        for (int i = 0; i < n - 1; ++i) {
            for (int j = i + 1; j < n; ++j) {
                OWLDataSomeValuesFrom some_i = this.factory.getOWLDataSomeValuesFrom(props[i], this.factory.getOWLDatatype(IRI.create(InternalDatatype.RDFS_LITERAL.getIRI())));
                OWLDataSomeValuesFrom some_j = this.factory.getOWLDataSomeValuesFrom(props[j], this.factory.getOWLDatatype(IRI.create(InternalDatatype.RDFS_LITERAL.getIRI())));
                OWLDataMaxCardinality max1 = this.factory.getOWLDataMaxCardinality(1, this.factory.getOWLDataProperty(IRI.create(AtomicRole.TOP_DATA_ROLE.getIRI())));
                OWLObjectIntersectionOf desc = this.factory.getOWLObjectIntersectionOf(some_i, some_j, max1);
                if (!this.reasoner.isSatisfiable(desc)) continue;
                return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }

    public Boolean visit(OWLFunctionalDataPropertyAxiom axiom) {
        return this.reasoner.isFunctional(axiom.getProperty().asOWLDataProperty());
    }

    public Boolean visit(OWLSubClassOfAxiom axiom) {
        return this.reasoner.isSubClassOf(axiom.getSubClass(), axiom.getSuperClass());
    }

    public Boolean visit(OWLEquivalentClassesAxiom axiom) {
        boolean isEntailed = true;
        Iterator i = axiom.getClassExpressions().iterator();
        if (i.hasNext()) {
            OWLClassExpression first = (OWLClassExpression)i.next();
            while (i.hasNext() && isEntailed) {
                OWLClassExpression next = (OWLClassExpression)i.next();
                isEntailed = this.reasoner.isSubClassOf(first, next) && this.reasoner.isSubClassOf(next, first);
            }
        }
        return isEntailed;
    }

    public Boolean visit(OWLDisjointClassesAxiom axiom) {
        int n = axiom.getClassExpressions().size();
        OWLClassExpression[] classes = axiom.getClassExpressions().toArray(new OWLClassExpression[n]);
        for (int i = 0; i < n - 1; ++i) {
            for (int j = i + 1; j < n; ++j) {
                OWLObjectComplementOf notj = this.factory.getOWLObjectComplementOf(classes[j]);
                if (this.reasoner.isSubClassOf(classes[i], notj)) continue;
                return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }

    public Boolean visit(OWLDisjointUnionAxiom axiom) {
        OWLClass c = axiom.getOWLClass();
        Set<OWLClassExpression> cs = new HashSet<OWLClassExpression>(axiom.getClassExpressions());
        cs.add(this.factory.getOWLObjectComplementOf(c));
        OWLObjectUnionOf incl1 = this.factory.getOWLObjectUnionOf(cs);
        OWLObjectUnionOf incl2 = this.factory.getOWLObjectUnionOf(this.factory.getOWLObjectComplementOf(this.factory.getOWLObjectUnionOf(axiom.getClassExpressions())), c);
        HashSet<OWLObjectUnionOf> conjuncts = new HashSet<OWLObjectUnionOf>();
        conjuncts.add(incl1);
        conjuncts.add(incl2);
        int n = axiom.getClassExpressions().size();
        OWLClassExpression[] descs = axiom.getClassExpressions().toArray(new OWLClassExpression[n]);
        for (int i = 0; i < n - 1; ++i) {
            for (int j = i + 1; j < n; ++j) {
                conjuncts.add(this.factory.getOWLObjectUnionOf(this.factory.getOWLObjectComplementOf(descs[i]), this.factory.getOWLObjectComplementOf(descs[j])));
            }
        }
        OWLObjectIntersectionOf entailmentDesc = this.factory.getOWLObjectIntersectionOf(conjuncts);
        return !this.reasoner.isSatisfiable(this.factory.getOWLObjectComplementOf(entailmentDesc));
    }

    public Boolean visit(OWLDatatypeDefinitionAxiom axiom) {
        this.reasoner.throwInconsistentOntologyExceptionIfNecessary();
        if (!this.reasoner.isConsistent()) {
            return true;
        }
        if (this.reasoner.m_dlOntology.hasDatatypes()) {
            OWLDataFactory df = this.reasoner.getDataFactory();
            OWLAnonymousIndividual freshIndividual = df.getOWLAnonymousIndividual("fresh-individual");
            OWLDataProperty freshDataProperty = df.getOWLDataProperty(IRI.create("fresh-data-property"));
            OWLDataRange dataRange = axiom.getDataRange();
            OWLDatatype dt = axiom.getDatatype();
            OWLDataIntersectionOf dr1 = df.getOWLDataIntersectionOf(df.getOWLDataComplementOf(dataRange), dt);
            OWLDataIntersectionOf dr2 = df.getOWLDataIntersectionOf(df.getOWLDataComplementOf(dt), dataRange);
            OWLDataUnionOf union = df.getOWLDataUnionOf(dr1, dr2);
            OWLDataSomeValuesFrom c = df.getOWLDataSomeValuesFrom(freshDataProperty, union);
            OWLClassAssertionAxiom ax = df.getOWLClassAssertionAxiom(c, freshIndividual);
            Tableau tableau = this.reasoner.getTableau(ax);
            return !tableau.isSatisfiable(true, true, null, null, null, null, null, ReasoningTaskDescription.isAxiomEntailed(axiom));
        }
        return false;
    }
    
	public Boolean visit(OWLMetamodellingAxiom axiom) {
		//TODO 
		return true;
	}
	
	public Boolean visit(OWLMetaRuleAxiom axiom) {
		//TODO 
		return true;
	}

    public Boolean visit(SWRLRule rule) {
        throw new UnsupportedOperationException();
    }

    public Boolean visit(OWLHasKeyAxiom axiom) {
        this.reasoner.throwFreshEntityExceptionIfNecessary(axiom);
        this.reasoner.throwInconsistentOntologyExceptionIfNecessary();
        if (!this.reasoner.isConsistent()) {
            return true;
        }
        OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
        OWLDataFactory df = ontologyManager.getOWLDataFactory();
        OWLNamedIndividual individualA = df.getOWLNamedIndividual(IRI.create("internal:named-fresh-individual-A"));
        OWLNamedIndividual individualB = df.getOWLNamedIndividual(IRI.create("internal:named-fresh-individual-B"));
        HashSet<Object> axioms = new HashSet<Object>();
        axioms.add(df.getOWLClassAssertionAxiom(axiom.getClassExpression(), individualA));
        axioms.add(df.getOWLClassAssertionAxiom(axiom.getClassExpression(), individualB));
        int i = 0;
        for (OWLObjectPropertyExpression p : axiom.getObjectPropertyExpressions()) {
            OWLNamedIndividual tmp = df.getOWLNamedIndividual(IRI.create("internal:named-fresh-individual-" + i));
            axioms.add(df.getOWLObjectPropertyAssertionAxiom(p, individualA, tmp));
            axioms.add(df.getOWLObjectPropertyAssertionAxiom(p, individualB, tmp));
            ++i;
        }
        for (OWLDataPropertyExpression p : axiom.getDataPropertyExpressions()) {
            OWLDatatype anonymousConstantsDatatype = df.getOWLDatatype(IRI.create("internal:anonymous-constants"));
            OWLLiteral constant = df.getOWLLiteral("internal:constant-" + i, anonymousConstantsDatatype);
            axioms.add(df.getOWLDataPropertyAssertionAxiom(p, individualA, constant));
            axioms.add(df.getOWLDataPropertyAssertionAxiom(p, individualB, constant));
            ++i;
        }
        axioms.add(df.getOWLDifferentIndividualsAxiom(new OWLIndividual[]{individualA, individualB}));
        Tableau tableau = this.reasoner.getTableau(axioms.toArray(new OWLAxiom[axioms.size()]));
        return !tableau.isSatisfiable(true, true, null, null, null, null, null, ReasoningTaskDescription.isAxiomEntailed(axiom));
    }

    protected class Edge {
        public final OWLAnonymousIndividual first;
        public final OWLAnonymousIndividual second;

        public Edge(OWLAnonymousIndividual first, OWLAnonymousIndividual second) {
            this.first = first;
            this.second = second;
        }

        public int hashCode() {
            return 13 + 3 * (this.first != null ? this.first.hashCode() : 0) + 7 * (this.second != null ? this.second.hashCode() : 0);
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            Edge other = (Edge)o;
            return this.first.equals(other.first) && this.second.equals(other.second);
        }

        public String toString() {
            return "(" + this.first + ", " + this.second + ")";
        }
    }

    protected class AnonymousIndividualForestBuilder
    implements OWLAxiomVisitor {
        protected final Set<OWLNamedIndividual> namedNodes = new HashSet<OWLNamedIndividual>();
        protected final Set<OWLAnonymousIndividual> nodes = new HashSet<OWLAnonymousIndividual>();
        protected final Map<OWLAnonymousIndividual, Set<OWLAnonymousIndividual>> edges = new HashMap<OWLAnonymousIndividual, Set<OWLAnonymousIndividual>>();
        protected final Map<OWLAnonymousIndividual, Map<OWLNamedIndividual, Set<OWLObjectPropertyExpression>>> specialOPEdges = new HashMap<OWLAnonymousIndividual, Map<OWLNamedIndividual, Set<OWLObjectPropertyExpression>>>();
        protected final Map<OWLAnonymousIndividual, Set<OWLClassExpression>> nodelLabels = new HashMap<OWLAnonymousIndividual, Set<OWLClassExpression>>();
        protected final Map<Edge, OWLObjectProperty> edgeOPLabels = new HashMap<Edge, OWLObjectProperty>();
        protected final Set<OWLAxiom> anonIndAxioms = new HashSet<OWLAxiom>();
        protected final Set<OWLAxiom> anonNoNamedIndAxioms = new HashSet<OWLAxiom>();

        protected AnonymousIndividualForestBuilder() {
        }

        public void constructConceptsForAnonymousIndividuals(OWLDataFactory df, Set<OWLAxiom> axioms) {
            for (OWLAxiom ax : axioms) {
                ax.accept(this);
            }
            Set<Set<OWLAnonymousIndividual>> components = this.getComponents();
            Map<Set<OWLAnonymousIndividual>, OWLAnonymousIndividual> componentsToRoots = this.findSuitableRoots(components);
            for (Set<OWLAnonymousIndividual> component : componentsToRoots.keySet()) {
                OWLAnonymousIndividual root = componentsToRoots.get(component);
                if (!this.specialOPEdges.containsKey(root)) {
                    OWLClassExpression c = this.getClassExpressionFor(df, root, null);
                    this.anonNoNamedIndAxioms.add(df.getOWLSubClassOfAxiom(df.getOWLThing(), df.getOWLObjectComplementOf(c)));
                    continue;
                }
                Map<OWLNamedIndividual, Set<OWLObjectPropertyExpression>> ind2OP = this.specialOPEdges.get(root);
                if (ind2OP.size() != 1) {
                    throw new RuntimeException("Internal error: HermiT decided that the anonymous individuals form a valid forest, but actually they do not. ");
                }
                OWLNamedIndividual subject = ind2OP.keySet().iterator().next();
                Set<OWLObjectPropertyExpression> ops = ind2OP.get(subject);
                if (ops.size() != 1) {
                    throw new RuntimeException("Internal error: HermiT decided that the anonymous individuals form a valid forest, but actually they do not. ");
                }
                OWLObjectPropertyExpression op = ops.iterator().next().getInverseProperty();
                OWLClassExpression c = this.getClassExpressionFor(df, root, null);
                this.anonIndAxioms.add(df.getOWLClassAssertionAxiom(df.getOWLObjectSomeValuesFrom(op, c), subject));
            }
        }

        public Set<OWLAxiom> getAnonIndAxioms() {
            return this.anonIndAxioms;
        }

        public Set<OWLAxiom> getAnonNoNamedIndAxioms() {
            return this.anonNoNamedIndAxioms;
        }

        protected OWLClassExpression getClassExpressionFor(OWLDataFactory df, OWLAnonymousIndividual node, OWLAnonymousIndividual predecessor) {
            Set<OWLAnonymousIndividual> successors = this.edges.get(node);
            if (successors == null || successors.size() == 1 && successors.iterator().next() == predecessor) {
                if (!this.nodelLabels.containsKey(node)) {
                    return df.getOWLThing();
                }
                if (this.nodelLabels.get(node).size() == 1) {
                    return this.nodelLabels.get(node).iterator().next();
                }
                return df.getOWLObjectIntersectionOf(this.nodelLabels.get(node));
            }
            HashSet<OWLObjectSomeValuesFrom> concepts = new HashSet<OWLObjectSomeValuesFrom>();
            for (OWLAnonymousIndividual successor : successors) {
                OWLObjectProperty op;
                Edge pair = new Edge(node, successor);
                if (this.edgeOPLabels.containsKey(pair)) {
                    op = this.edgeOPLabels.get(pair);
                } else {
                    pair = new Edge(successor, node);
                    if (!this.edgeOPLabels.containsKey(pair)) {
                        throw new RuntimeException("Internal error: some edge in the forest of anonymous individuals has no edge label although it should. ");
                    }
                    op = this.edgeOPLabels.get(pair);
                }
                concepts.add(df.getOWLObjectSomeValuesFrom(op, this.getClassExpressionFor(df, successor, node)));
            }
            return concepts.size() == 1 ? concepts.iterator().next() : df.getOWLObjectIntersectionOf(concepts);
        }

        protected Map<Set<OWLAnonymousIndividual>, OWLAnonymousIndividual> findSuitableRoots(Set<Set<OWLAnonymousIndividual>> components) {
            HashMap<Set<OWLAnonymousIndividual>, OWLAnonymousIndividual> componentsToRoots = new HashMap<Set<OWLAnonymousIndividual>, OWLAnonymousIndividual>();
            for (Set<OWLAnonymousIndividual> component : components) {
                OWLAnonymousIndividual root = null;
                OWLAnonymousIndividual rootWithOneNamedRelation = null;
                for (OWLAnonymousIndividual ind : component) {
                    if (this.specialOPEdges.containsKey(ind)) {
                        if (this.specialOPEdges.get(ind).size() >= 2) continue;
                        rootWithOneNamedRelation = ind;
                        continue;
                    }
                    root = ind;
                }
                if (root == null && rootWithOneNamedRelation == null) {
                    throw new IllegalArgumentException("Invalid input ontology: One of the trees in the forst of anomnymous individuals has no root that satisfies the criteria on roots (cf. OWL 2 Structural Specification and Functional-Style Syntax, Sec. 11.2).");
                }
                if (rootWithOneNamedRelation != null) {
                    componentsToRoots.put(component, rootWithOneNamedRelation);
                    continue;
                }
                componentsToRoots.put(component, root);
            }
            return componentsToRoots;
        }

        protected Set<Set<OWLAnonymousIndividual>> getComponents() {
            HashSet<Set<OWLAnonymousIndividual>> components = new HashSet<Set<OWLAnonymousIndividual>>();
            if (this.nodes.isEmpty()) {
                return components;
            }
            Set<OWLAnonymousIndividual> toProcess = this.nodes;
            ArrayList<Edge> workQueue = new ArrayList<Edge>();
            while (!toProcess.isEmpty()) {
                HashSet<OWLAnonymousIndividual> currentComponent = new HashSet<OWLAnonymousIndividual>();
                Edge nodePlusPredecessor = new Edge(toProcess.iterator().next(), null);
                workQueue.add(nodePlusPredecessor);
                while (!workQueue.isEmpty()) {
                    nodePlusPredecessor = workQueue.remove(0);
                    currentComponent.add(nodePlusPredecessor.first);
                    if (!this.edges.containsKey(nodePlusPredecessor.first)) continue;
                    for (OWLAnonymousIndividual ind : this.edges.get(nodePlusPredecessor.first)) {
                        if (nodePlusPredecessor.second != null && ind.getID().equals(nodePlusPredecessor.second.getID())) continue;
                        for (Edge pair : workQueue) {
                            if (pair.first != ind) continue;
                            throw new IllegalArgumentException("Invalid input ontology: The anonymous individuals cannot be arranged into a forest as required (cf. OWL 2 Structural Specification and Functional-Style Syntax, Sec. 11.2) because there is a cycle. ");
                        }
                        workQueue.add(new Edge(ind, nodePlusPredecessor.first));
                    }
                }
                components.add(currentComponent);
                toProcess.removeAll(currentComponent);
            }
            return components;
        }

        public void visit(OWLClassAssertionAxiom axiom) {
            if (axiom.getClassExpression().isOWLThing()) {
                return;
            }
            OWLIndividual node = axiom.getIndividual();
            if (!node.isAnonymous()) {
                this.namedNodes.add(node.asOWLNamedIndividual());
            } else {
                this.nodes.add(node.asOWLAnonymousIndividual());
                if (this.nodelLabels.containsKey((Object)node)) {
                    this.nodelLabels.get((Object)node).add(axiom.getClassExpression());
                } else {
                    HashSet<OWLClassExpression> label = new HashSet<OWLClassExpression>();
                    label.add(axiom.getClassExpression());
                    this.nodelLabels.put(node.asOWLAnonymousIndividual(), label);
                }
            }
        }

        public void visit(OWLObjectPropertyAssertionAxiom axiom) {
            OWLIndividual sub = axiom.getSubject();
            OWLIndividual obj = axiom.getObject();
            OWLObjectPropertyExpression ope = axiom.getProperty();
            if (!sub.isAnonymous() && !obj.isAnonymous()) {
                return;
            }
            if (!sub.isAnonymous() && obj.isAnonymous() || sub.isAnonymous() && !obj.isAnonymous()) {
                if (!sub.isAnonymous() && obj.isAnonymous()) {
                    OWLIndividual tmp = sub;
                    sub = obj;
                    obj = tmp;
                    ope = ope.getInverseProperty();
                }
                OWLNamedIndividual named = obj.asOWLNamedIndividual();
                OWLAnonymousIndividual unnamed = sub.asOWLAnonymousIndividual();
                this.namedNodes.add(named);
                this.nodes.add(unnamed);
                if (this.specialOPEdges.containsKey(unnamed)) {
                    Map<OWLNamedIndividual, Set<OWLObjectPropertyExpression>> specialEdges = this.specialOPEdges.get(unnamed);
                    if (specialEdges.containsKey(named)) {
                        specialEdges.get(named).add(ope);
                    } else {
                        specialEdges = new HashMap<OWLNamedIndividual, Set<OWLObjectPropertyExpression>>();
                        HashSet<OWLObjectPropertyExpression> label = new HashSet<OWLObjectPropertyExpression>();
                        label.add(ope);
                        specialEdges.put(named, label);
                        this.specialOPEdges.put(unnamed, specialEdges);
                    }
                } else {
                    HashMap specialEdge = new HashMap();
                    HashSet<OWLObjectPropertyExpression> label = new HashSet<OWLObjectPropertyExpression>();
                    label.add(ope);
                    specialEdge.put(named, label);
                    this.specialOPEdges.put(unnamed, specialEdge);
                }
            } else {
                OWLObjectProperty op;
                HashSet<OWLAnonymousIndividual> successors;
                if (ope.isAnonymous()) {
                    op = ope.getNamedProperty();
                    OWLIndividual tmp = sub;
                    sub = obj;
                    obj = tmp;
                } else {
                    op = ope.asOWLObjectProperty();
                }
                OWLAnonymousIndividual subAnon = sub.asOWLAnonymousIndividual();
                OWLAnonymousIndividual objAnon = obj.asOWLAnonymousIndividual();
                this.nodes.add(subAnon);
                this.nodes.add(objAnon);
                if (this.edges.containsKey(subAnon) && this.edges.get(subAnon).contains(objAnon) || this.edges.containsKey(objAnon) && this.edges.get(objAnon).contains(subAnon)) {
                    throw new IllegalArgumentException("Invalid input ontology: There are two object property assertions for the same anonymous individuals, which is not allowed (see OWL 2 Syntax Sec 11.2). ");
                }
                if (this.edges.containsKey(subAnon)) {
                    this.edges.get(subAnon).add(objAnon);
                } else {
                    successors = new HashSet<OWLAnonymousIndividual>();
                    successors.add(objAnon);
                    this.edges.put(subAnon, successors);
                }
                if (this.edges.containsKey(objAnon)) {
                    this.edges.get(objAnon).add(subAnon);
                } else {
                    successors = new HashSet();
                    successors.add(subAnon);
                    this.edges.put(objAnon, successors);
                }
                this.edgeOPLabels.put(new Edge(subAnon, objAnon), op);
            }
        }

        public void visit(OWLDataPropertyAssertionAxiom axiom) {
            if (!axiom.getSubject().isAnonymous()) {
                return;
            }
            OWLAnonymousIndividual sub = axiom.getSubject().asOWLAnonymousIndividual();
            this.nodes.add(sub);
            OWLDataHasValue c = EntailmentChecker.this.factory.getOWLDataHasValue(axiom.getProperty(), axiom.getObject());
            if (this.nodelLabels.containsKey(sub)) {
                this.nodelLabels.get(sub).add(c);
            } else {
                HashSet<OWLClassExpression> labels = new HashSet<OWLClassExpression>();
                labels.add(c);
                this.nodelLabels.put(sub, labels);
            }
        }

        public void visit(OWLDeclarationAxiom axiom) {
        }

        public void visit(OWLSubClassOfAxiom axiom) {
        }

        public void visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
        }

        public void visit(OWLAsymmetricObjectPropertyAxiom axiom) {
        }

        public void visit(OWLReflexiveObjectPropertyAxiom axiom) {
        }

        public void visit(OWLDisjointClassesAxiom axiom) {
        }

        public void visit(OWLDataPropertyDomainAxiom axiom) {
        }

        public void visit(OWLObjectPropertyDomainAxiom axiom) {
        }

        public void visit(OWLEquivalentObjectPropertiesAxiom axiom) {
        }

        public void visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
        }

        public void visit(OWLDifferentIndividualsAxiom axiom) {
        }

        public void visit(OWLDisjointDataPropertiesAxiom axiom) {
        }

        public void visit(OWLDisjointObjectPropertiesAxiom axiom) {
        }

        public void visit(OWLObjectPropertyRangeAxiom axiom) {
        }

        public void visit(OWLFunctionalObjectPropertyAxiom axiom) {
        }

        public void visit(OWLSubObjectPropertyOfAxiom axiom) {
        }

        public void visit(OWLDisjointUnionAxiom axiom) {
        }

        public void visit(OWLSymmetricObjectPropertyAxiom axiom) {
        }

        public void visit(OWLDataPropertyRangeAxiom axiom) {
        }

        public void visit(OWLFunctionalDataPropertyAxiom axiom) {
        }

        public void visit(OWLEquivalentDataPropertiesAxiom axiom) {
        }

        public void visit(OWLEquivalentClassesAxiom axiom) {
        }

        public void visit(OWLTransitiveObjectPropertyAxiom axiom) {
        }

        public void visit(OWLIrreflexiveObjectPropertyAxiom axiom) {
        }

        public void visit(OWLSubDataPropertyOfAxiom axiom) {
        }

        public void visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
        }

        public void visit(OWLSameIndividualAxiom axiom) {
        }

        public void visit(OWLSubPropertyChainOfAxiom axiom) {
        }

        public void visit(OWLInverseObjectPropertiesAxiom axiom) {
        }

        public void visit(OWLHasKeyAxiom axiom) {
        }

        public void visit(OWLDatatypeDefinitionAxiom axiom) {
        }

        public void visit(SWRLRule rule) {
        }

        public void visit(OWLAnnotationAssertionAxiom axiom) {
        }

        public void visit(OWLSubAnnotationPropertyOfAxiom axiom) {
        }

        public void visit(OWLAnnotationPropertyDomainAxiom axiom) {
        }

        public void visit(OWLAnnotationPropertyRangeAxiom axiom) {
        }

		public void visit(OWLMetamodellingAxiom axiom) {
		}
		
		public void visit(OWLMetaRuleAxiom axiom) {
		}
    }

}

