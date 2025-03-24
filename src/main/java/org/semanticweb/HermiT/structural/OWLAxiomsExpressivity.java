package org.semanticweb.HermiT.structural;

import org.semanticweb.owlapi.model.OWLAxiomVisitor;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitor;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLIndividualAxiom;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasSelf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.util.OWLAxiomVisitorAdapter;

public class OWLAxiomsExpressivity
extends OWLAxiomVisitorAdapter
implements OWLClassExpressionVisitor {
    public boolean m_hasAtMostRestrictions;
    public boolean m_hasInverseRoles;
    public boolean m_hasNominals;
    public boolean m_hasDatatypes;

    public OWLAxiomsExpressivity(OWLAxioms axioms) {
        for (OWLClassExpression[] inclusion : axioms.m_conceptInclusions) {
            for (OWLClassExpression description : inclusion) {
                description.accept(this);
            }
        }
        for (OWLObjectPropertyExpression[] inclusion : axioms.m_simpleObjectPropertyInclusions) {
            this.visitProperty(inclusion[0]);
            this.visitProperty(inclusion[1]);
        }
        for (OWLAxioms.ComplexObjectPropertyInclusion inclusion : axioms.m_complexObjectPropertyInclusions) {
            for (OWLObjectPropertyExpression subObjectProperty : inclusion.m_subObjectProperties)
                visitProperty(subObjectProperty);
            visitProperty(inclusion.m_superObjectProperty);
        }
        for (OWLObjectPropertyExpression[] disjoint : axioms.m_disjointObjectProperties) {
            for (int index = 0; index < disjoint.length; ++index) {
                this.visitProperty(disjoint[index]);
            }
        }
        for (OWLObjectPropertyExpression property : axioms.m_reflexiveObjectProperties) {
            this.visitProperty(property);
        }
        for (OWLObjectPropertyExpression property : axioms.m_irreflexiveObjectProperties) {
            this.visitProperty(property);
        }
        for (OWLObjectPropertyExpression property : axioms.m_asymmetricObjectProperties) {
            this.visitProperty(property);
        }
        if (!(axioms.m_dataProperties.isEmpty() && axioms.m_disjointDataProperties.isEmpty() && axioms.m_dataPropertyInclusions.isEmpty() && axioms.m_dataRangeInclusions.isEmpty() && axioms.m_definedDatatypesIRIs.isEmpty())) {
            this.m_hasDatatypes = true;
        }
        for (OWLIndividualAxiom fact : axioms.m_facts) {
            fact.accept(this);
        }
    }

    protected void visitProperty(OWLObjectPropertyExpression object) {
        if (object.isAnonymous()) {
            this.m_hasInverseRoles = true;
        }
    }

    public void visit(OWLClass desc) {
    }

    public void visit(OWLObjectComplementOf object) {
        object.getOperand().accept(this);
    }

    public void visit(OWLObjectIntersectionOf object) {
        for (OWLClassExpression description : object.getOperands()) {
            description.accept(this);
        }
    }

    public void visit(OWLObjectUnionOf object) {
        for (OWLClassExpression description : object.getOperands()) {
            description.accept(this);
        }
    }

    public void visit(OWLObjectOneOf object) {
        this.m_hasNominals = true;
    }

    public void visit(OWLObjectSomeValuesFrom object) {
        this.visitProperty(object.getProperty());
        object.getFiller().accept(this);
    }

    public void visit(OWLObjectHasValue object) {
        this.m_hasNominals = true;
        this.visitProperty(object.getProperty());
    }

    public void visit(OWLObjectHasSelf object) {
        this.visitProperty(object.getProperty());
    }

    public void visit(OWLObjectAllValuesFrom object) {
        this.visitProperty(object.getProperty());
        object.getFiller().accept(this);
    }

    public void visit(OWLObjectMinCardinality object) {
        this.visitProperty(object.getProperty());
        object.getFiller().accept(this);
    }

    public void visit(OWLObjectMaxCardinality object) {
        this.m_hasAtMostRestrictions = true;
        this.visitProperty(object.getProperty());
        object.getFiller().accept(this);
    }

    public void visit(OWLObjectExactCardinality object) {
        this.m_hasAtMostRestrictions = true;
        this.visitProperty(object.getProperty());
        object.getFiller().accept(this);
    }

    public void visit(OWLDataHasValue object) {
        this.m_hasDatatypes = true;
    }

    public void visit(OWLDataSomeValuesFrom object) {
        this.m_hasDatatypes = true;
    }

    public void visit(OWLDataAllValuesFrom object) {
        this.m_hasDatatypes = true;
    }

    public void visit(OWLDataMinCardinality object) {
        this.m_hasDatatypes = true;
    }

    public void visit(OWLDataMaxCardinality object) {
        this.m_hasDatatypes = true;
    }

    public void visit(OWLDataExactCardinality object) {
        this.m_hasDatatypes = true;
    }

    public void visit(OWLClassAssertionAxiom object) {
        object.getClassExpression().accept(this);
    }

    public void visit(OWLObjectPropertyAssertionAxiom object) {
        this.visitProperty(object.getProperty());
    }

    public void visit(OWLNegativeObjectPropertyAssertionAxiom object) {
        this.visitProperty(object.getProperty());
    }

    public void visit(OWLDataPropertyAssertionAxiom object) {
        this.m_hasDatatypes = true;
    }
}

